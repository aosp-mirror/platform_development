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
 * The program acts as a simple TCP server that accepts any data and
 * discards it immediately.
 */
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>

/* Default port number */
#define  DEFAULT_PORT  8012

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

/* Main program */
int main(void)
{
    int sock, client;
    int port = DEFAULT_PORT;

    printf("Starting pipe test server on local port %d\n", port);
    sock = socket_loopback_server( port, SOCK_STREAM );
    if (sock < 0) {
        fprintf(stderr, "Could not start server: %s\n", strerror(errno));
        return 1;
    }

RESTART:
    client = TFR(accept(sock, NULL, NULL));
    if (client < 0) {
        fprintf(stderr, "Server error: %s\n", strerror(errno));
        return 2;
    }
    printf("Client connected!\n");

    /* Now, accept any incoming data, and send it back */
    for (;;) {
        char  buff[8192], *p;
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
    }
    printf("Client closed connection\n");
    socket_close(client);
    goto RESTART;

    return 0;
}
