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

/* This program uses a QEMUD pipe to exchange data with a test
 * server. It's very simple:
 *
 *    for count in range(0,100):
 *       msg = "Hello Word " + count
 *       qemud_pipe_send(msg)
 *       qemud_pipe_recv(msg2)
 *       if (msg != msg2):
 *          error()
 *
 *
 * See test_host_1.c for the corresponding server code, which simply
 * sends back anything it receives from the client.
 */
#include "test_util.h"
#include <errno.h>
#include <string.h>
#include <stddef.h>
#include <stdio.h>

#define  PIPE_NAME  "pingpong"


int main(void)
{
    Pipe  pipe[1];
    const int maxCount = 100;
    int port = 8012;

#if 0
    if (pipe_openSocket(pipe, port) < 0) {
        fprintf(stderr, "Could not open tcp socket!\n");
        return 1;
    }
    printf("Connected to tcp:host:%d\n", port);
#else
    if (pipe_openQemuPipe(pipe, PIPE_NAME) < 0) {
        fprintf(stderr, "Could not open '%s' pipe: %s\n", PIPE_NAME, strerror(errno));
        return 1;
    }
    printf("Connected to '%s' pipe\n", PIPE_NAME);
#endif

    char  buff[64];
    char  buff2[64];
    int   count;
    double time0 = now_secs();
    size_t total = 0;

    for (count = 0; count < maxCount; count++) {
        /* First, send a small message */
        int  len = snprintf(buff, sizeof(buff), "Hello World %d\n", count);
        printf("%4d: Sending %d bytes\n", count, len);
        int ret = pipe_send(pipe, buff, len);
        if (ret < 0) {
            fprintf(stderr,"Sending %d bytes failed: %s\n", len, strerror(errno));
            return 1;
        }

        total += len;

        /* The server is supposed to send the message back */
        ret = pipe_recv(pipe, buff2, len);
        if (ret < 0) {
            fprintf(stderr, "Receiving failed (ret=%d): %s\n", ret, strerror(errno));
            return 3;
        }
        printf("%4d: Received %d bytes\n", count, ret);
        /* Check the message's content */
        if (ret != len) {
            fprintf(stderr, "Message size mismatch sent=%d received=%d\n", len, ret);
            return 5;
        }
        if (memcmp(buff, buff2, len) != 0) {
            fprintf(stderr, "Message content mismatch!\n");
            return 6;
        }
    }

    double time1 = now_secs();

    printf("Closing pipe\n");
    pipe_close(pipe);

    printf("Bandwidth: %g MB/s, %g bytes in %g seconds.\n",
           total*1.0 / (1024.*1024.*(time1-time0)), 1.0*total, time1-time0);

    return 0;
}
