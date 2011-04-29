/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* This program is used to test the QEMUD fast pipes.
 * See external/qemu/docs/ANDROID-QEMUD-PIPES.TXT for details.
 *
 * The program acts as a simple TCP server that accepts data and sends
 * them back to the client as is.
 */
#include <sys/socket.h>
#include <netinet/in.h>
#include <sys/un.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>

/* Default port number */
#define  DEFAULT_PORT  8012
#define  DEFAULT_PATH  "/tmp/libqemu-socket"

/* Try to execute x, looping around EINTR errors. */
#undef TEMP_FAILURE_RETRY
#define TEMP_FAILURE_RETRY(exp) ({         \
    typeof (exp) _rc;                      \
    do {                                   \
        _rc = (exp);                       \
    } while (_rc == -1 && errno == EINTR); \
    _rc; })

#define TFR TEMP_FAILURE_RETRY

/* Close a socket, preserving the value of errno */
static void
socket_close(int  sock)
{
    int  old_errno = errno;
    close(sock);
    errno = old_errno;
}

/* Create a server socket bound to a loopback port */
static int
socket_loopback_server( int port, int type )
{
    struct sockaddr_in  addr;

    int  sock = socket(AF_INET, type, 0);
    if (sock < 0) {
        return -1;
    }

    memset(&addr, 0, sizeof(addr));
    addr.sin_family      = AF_INET;
    addr.sin_port        = htons(port);
    addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);

    int n = 1;
    setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &n, sizeof(n));

    if (TFR(bind(sock, (struct sockaddr*)&addr, sizeof(addr))) < 0) {
        socket_close(sock);
        return -1;
    }

    if (type == SOCK_STREAM) {
        if (TFR(listen(sock, 4)) < 0) {
            socket_close(sock);
            return -1;
        }
    }

    return sock;
}

static int
socket_unix_server( const char* path, int type )
{
    struct sockaddr_un  addr;

    int  sock = socket(AF_UNIX, type, 0);
    if (sock < 0) {
        return -1;
    }

    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    snprintf(addr.sun_path, sizeof(addr.sun_path), "%s", path);

    unlink(addr.sun_path);

    printf("Unix path: '%s'\n", addr.sun_path);

    int n = 1;
    setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &n, sizeof(n));

    if (TFR(bind(sock, (struct sockaddr*)&addr, sizeof(addr))) < 0) {
        socket_close(sock);
        return -1;
    }

    if (type == SOCK_STREAM) {
        if (TFR(listen(sock, 4)) < 0) {
            socket_close(sock);
            return -1;
        }
    }

    return sock;
}

char* progname;

static void usage(int code)
{
    printf("Usage: %s [options]\n\n", progname);
    printf(
      "Valid options are:\n\n"
      "  -? -h --help  Print this message\n"
      "  -unix <path>  Use unix server socket\n"
      "  -tcp <port>   Use local tcp port (default %d)\n"
      "\n", DEFAULT_PORT
    );
    exit(code);
}

/* Main program */
int main(int argc, char** argv)
{
    int sock, client;
    int port = DEFAULT_PORT;
    const char* path = NULL;
    const char* tcpPort = NULL;

    /* Extract program name */
    {
        char* p = strrchr(argv[0], '/');
        if (p == NULL)
            progname = argv[0];
        else
            progname = p+1;
    }

    /* Parse options */
    while (argc > 1 && argv[1][0] == '-') {
        char* arg = argv[1];
        if (!strcmp(arg, "-?") || !strcmp(arg, "-h") || !strcmp(arg, "--help")) {
            usage(0);
        } else if (!strcmp(arg, "-unix")) {
            if (argc < 3) {
                fprintf(stderr, "-unix option needs an argument! See --help for details.\n");
                exit(1);
            }
            argc--;
            argv++;
            path = argv[1];
        } else if (!strcmp(arg, "-tcp")) {
            if (argc < 3) {
                fprintf(stderr, "-tcp option needs an argument! See --help for details.\n");
                exit(1);
            }
            argc--;
            argv++;
            tcpPort = argv[1];
        } else {
            fprintf(stderr, "UNKNOWN OPTION: %s\n\n", arg);
            usage(1);
        }
        argc--;
        argv++;
    }

    if (path != NULL) {
        printf("Starting pipe test server on unix path: %s\n", path);
        sock = socket_unix_server( path, SOCK_STREAM );
    } else {
        printf("Starting pipe test server on local port %d\n", port);
        sock = socket_loopback_server( port, SOCK_STREAM );
    }
    if (sock < 0) {
        fprintf(stderr, "Could not start server: %s\n", strerror(errno));
        return 1;
    }
    printf("Server ready!\n");

RESTART:
    client = TFR(accept(sock, NULL, NULL));
    if (client < 0) {
        fprintf(stderr, "Server error: %s\n", strerror(errno));
        return 2;
    }
    printf("Client connected!\n");

    /* Now, accept any incoming data, and send it back */
    for (;;) {
        char  buff[32768], *p;
        int   ret, count;

        ret = TFR(read(client, buff, sizeof(buff)));
        if (ret < 0) {
            fprintf(stderr, "Client read error: %s\n", strerror(errno));
            socket_close(client);
            return 3;
        }
        if (ret == 0) {
            break;
        }
        count = ret;
        p     = buff;
        //printf("   received: %d bytes\n", count);

        while (count > 0) {
            ret = TFR(write(client, p, count));
            if (ret < 0) {
                fprintf(stderr, "Client write error: %s\n", strerror(errno));
                socket_close(client);
                return 4;
            }
            //printf("   sent: %d bytes\n", ret);

            p     += ret;
            count -= ret;
        }
    }
    printf("Client closed connection\n");
    socket_close(client);
    goto RESTART;

    return 0;
}
