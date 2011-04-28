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

/* Set to 1 to enable debugging */
#define DEBUG  0

#if DEBUG >= 1
#  define D(...)  fprintf(stderr,"libqemud:" __VA_ARGS__), fprintf(stderr, "\n")
#endif

#include <unistd.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <hardware/qemud.h>
#include <hardware/qemu_pipe.h>
#include <pthread.h>  /* for pthread_once() */
#include <stdlib.h>
#include <stdio.h>

/* Used for debugging */

#ifndef D
#  define  D(...)   do{}while(0)
#endif

/* Try to open a qemud pipe, 'pipeName' must be a generic pipe service
 * name (e.g. "opengles" or "camera"). The emulator will be in charge of
 * connecting the corresponding pipe/client to an internal service or an
 * external socket, these details are hidden from the caller.
 *
 * Return a new QemuPipe pointer, or NULL in case of error
 */
int
qemu_pipe_open(const char*  pipeName)
{
    char  buff[256];
    int   buffLen;
    int   fd, ret;

    if (pipeName == NULL || pipeName[0] == '\0') {
        errno = EINVAL;
        return -1;
    }

    snprintf(buff, sizeof buff, "pipe:%s", pipeName);

    fd = open("/dev/qemu_pipe", O_RDWR);
    if (fd < 0) {
        D("%s: Could not open /dev/qemu_pipe: %s", __FUNCTION__, strerror(errno));
        errno = ENOSYS;
        return -1;
    }

    buffLen = strlen(buff);

    ret = TEMP_FAILURE_RETRY(write(fd, buff, buffLen+1));
    if (ret != buffLen+1) {
        D("%s: Could not connect to %s pipe service: %s", __FUNCTION__, pipeName, strerror(errno));
        if (ret == 0) {
            errno = ECONNRESET;
        } else if (ret > 0) {
            errno = EINVAL;
        }
        return -1;
    }

    return fd;
}
