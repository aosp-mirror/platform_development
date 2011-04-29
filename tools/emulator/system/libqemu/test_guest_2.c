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

/* This program benchmarks a QEMUD pipe to exchange data with a test
 * server.
 *
 * See test_host_1.c for the corresponding server code, which simply
 * sends back anything it receives from the client.
 */
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include "test_util.h"

#define  PIPE_NAME  "pingpong"

char* progname;

static void usage(int code)
{
    printf("Usage: %s [options]\n\n", progname);
    printf(
      "Valid options are:\n\n"
      "  -? -h --help  Print this message\n"
      "  -pipe <name>  Use pipe name (default: " PIPE_NAME ")\n"
      "  -tcp <port>   Use local tcp port\n"
      "  -size <size>  Specify packet size\n"
      "\n"
    );
    exit(code);
}

int main(int argc, char** argv)
{
    Pipe        pipe[1];
    const char* tcpPort = NULL;
    int         localPort = 0;
    const char* pipeName = NULL;
    const char* packetSize = NULL;
    int         port = 8012;
    int         maxCount   = 1000;
    int         bufferSize = 16384;
    uint8_t*    buffer;
    uint8_t*    buffer2;
    int         nn, count;
    double      time0, time1;

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
        } else if (!strcmp(arg, "-pipe")) {
            if (argc < 3) {
                fprintf(stderr, "-pipe option needs an argument! See --help for details.\n");
                exit(1);
            }
            argc--;
            argv++;
            pipeName = argv[1];
        } else if (!strcmp(arg, "-tcp")) {
            if (argc < 3) {
                fprintf(stderr, "-tcp option needs an argument! See --help for details.\n");
                exit(1);
            }
            argc--;
            argv++;
            tcpPort = argv[1];
        } else if (!strcmp(arg, "-size")) {
            if (argc < 3) {
                fprintf(stderr, "-tcp option needs an argument! See --help for details.\n");
                exit(1);
            }
            argc--;
            argv++;
            packetSize = argv[1];
        } else {
            fprintf(stderr, "UNKNOWN OPTION: %s\n\n", arg);
            usage(1);
        }
        argc--;
        argv++;
    }

    /* Check arguments */
    if (tcpPort && pipeName) {
        fprintf(stderr, "You can't use both -pipe and -tcp at the same time\n");
        exit(2);
    }

    if (tcpPort != NULL) {
        localPort = atoi(tcpPort);
        if (localPort <= 0 || localPort > 65535) {
            fprintf(stderr, "Invalid port number: %s\n", tcpPort);
            exit(2);
        }
    } else if (pipeName == NULL) {
        /* Use default pipe name */
        pipeName = PIPE_NAME;
    }

    if (packetSize != NULL) {
        int  size = atoi(packetSize);
        if (size <= 0) {
            fprintf(stderr, "Invalid byte size: %s\n", packetSize);
            exit(3);
        }
        bufferSize = size;
    }

    /* Open the pipe */
    if (tcpPort != NULL) {
        if (pipe_openSocket(pipe, localPort) < 0) {
            fprintf(stderr, "Could not open tcp socket!\n");
            return 1;
        }
        printf("Connected to tcp:localhost:%d\n", port);
    }
    else {
        if (pipe_openQemuPipe(pipe, pipeName) < 0) {
            fprintf(stderr, "Could not open '%s' pipe: %s\n", pipeName, strerror(errno));
            return 1;
        }
        printf("Connected to '%s' pipe\n", pipeName);
    }

    /* Allocate buffers, setup their data */
    buffer  = malloc(bufferSize);
    buffer2 = malloc(bufferSize);

    for (nn = 0; nn < bufferSize; nn++) {
        buffer[nn] = (uint8_t)nn;
    }

    /* Do the work! */
    time0 = now_secs();

    for (count = 0; count < maxCount; count++) {
        int ret = pipe_send(pipe, buffer, bufferSize);
        int pos, len;

        if (ret < 0) {
            fprintf(stderr,"%d: Sending %d bytes failed: %s\n", count, bufferSize, strerror(errno));
            return 1;
        }

#if 1
        /* The server is supposed to send the message back */
        pos = 0;
        len = bufferSize;
        while (len > 0) {
            ret = pipe_recv(pipe, buffer2 + pos, len);
            if (ret < 0) {
                fprintf(stderr, "Receiving failed (ret=%d): %s\n", ret, strerror(errno));
                return 3;
            }
            if (ret == 0) {
                fprintf(stderr, "Disconnection while receiving!\n");
                return 4;
            }
            pos += ret;
            len -= ret;
        }

        if (memcmp(buffer, buffer2, bufferSize) != 0) {
            fprintf(stderr, "Message content mismatch!\n");
            const int maxAvail = 16;
            const int maxLines = 12;
            int numLines = 0;
            for (nn = 0; nn < bufferSize; ) {
                int avail = bufferSize - nn;
                int mm;
                if (avail > maxAvail)
                    avail = maxAvail;

                if (memcmp(buffer+nn, buffer2+nn, avail) != 0) {
                    if (++numLines >= maxLines) {
                        printf(".... to be continued ...\n");
                        break;
                    }
                    printf("%04x:", nn);

                    for (mm = 0; mm < avail; mm++)
                        printf(" %02x", buffer[nn+mm]);
                    for ( ; mm < maxAvail; mm++ )
                        printf("   ");

                    printf( " -- " );

                    for (mm = 0; mm < avail; mm++)
                        printf(" %02x", buffer2[nn+mm]);

                    printf ("\n");
                }
                nn += avail;
            }
            return 6;
        }

#endif

        if (count > 0 && (count % 200) == 0) {
            printf("... %d\n", count);
        }
    }

    time1 = now_secs();

    printf("Closing pipe\n");
    pipe_close(pipe);

    printf("Total time: %g seconds\n", time1 - time0);
    printf("Total bytes: %g bytes\n", 1.0*maxCount*bufferSize);
    printf("Bandwidth: %g MB/s\n", (maxCount*bufferSize/(1024.0*1024.0))/(time1 - time0) );
    return 0;
}
