/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define _GNU_SOURCE             /* GLibc compatibility to declare pipe2(2) */
#include <portability.h>
#include <unistd.h>
#include <fcntl.h>

#include <portability.h>
#include <asm/unistd.h>
#include <asm/unistd-portable.h>

#include <fcntl_portable.h>
#include <asm/unistd-portable.h>
#include <asm/unistd.h>
#include <filefd_portable.h>


#define PORTABLE_TAG "pipe_portable"
#include <log_portable.h>

extern int syscall(int, ...);


/* NOTE: LTP defaults to using O_NONBLOCK even if O_NONBLOCK is defined */


/*
 * Portable to Native event flags mapper.
 */
static inline int tdf_flags_pton(int portable_flags)
{
    int native_flags = 0;

    ALOGV("%s(portable_flags:0x%x) {", __func__, portable_flags);

    if (portable_flags & O_NONBLOCK_PORTABLE) {
        native_flags |= O_NONBLOCK;
    }

    if (portable_flags & O_CLOEXEC_PORTABLE) {
        native_flags |= O_CLOEXEC;
    }

    ALOGV("%s: return(native_flags:%d); }", __func__, native_flags);
    return native_flags;
}


int WRAP(pipe2)(int pipefd[2], int portable_flags) {
    int native_flags;
    int rv;

    ALOGV(" ");
    ALOGV("%s(pipefd[2]:%p, portable_flags:0x%x) {", __func__,
              pipefd,       portable_flags);

    native_flags = tdf_flags_pton(portable_flags);

    rv = REAL(pipe2)(pipefd, native_flags);
    if (rv >= 0) {
        ALOGV("%s: pipe2() returned pipefd[0]:%d, pipefd[1]:%d", __func__,
                                    pipefd[0],    pipefd[1]);

        if (native_flags & O_CLOEXEC) {
            filefd_CLOEXEC_enabled(pipefd[0]);
            filefd_CLOEXEC_enabled(pipefd[1]);
        }
    }

    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}

