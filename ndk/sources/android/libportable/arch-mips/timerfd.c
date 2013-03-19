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

#include <portability.h>
#include <unistd.h>
#include <fcntl.h>

#include <asm/unistd.h>
#include <asm/unistd-portable.h>

#include <fcntl_portable.h>
#include <timerfd_portable.h>

#include <filefd_portable.h>


#define PORTABLE_TAG "timerfd_portable"
#include <log_portable.h>

extern int syscall(int, ...);


/* NOTE: LTP defaults to using O_NONBLOCK even if TFD_NONBLOCK is defined */


/*
 * Portable to Native event flags mapper.
 */
static inline int tdf_flags_pton(int portable_flags)
{
    int native_flags = 0;

    ALOGV("%s(portable_flags:0x%x) {", __func__, portable_flags);

    if (portable_flags & TFD_NONBLOCK_PORTABLE) {
        native_flags |= TFD_NONBLOCK;
    }

    if (portable_flags & TFD_CLOEXEC_PORTABLE) {
        native_flags |= TFD_CLOEXEC;
    }

    ALOGV("%s: return(native_flags:%d); }", __func__, native_flags);
    return native_flags;
}


int WRAP(timerfd_create)(int clockid, int portable_flags) {
    int rv;
    int native_flags;

    ALOGV(" ");
    ALOGV("%s(clockid:%d, portable_flags:%d) {", __func__,
              clockid,    portable_flags);

    native_flags = tdf_flags_pton(portable_flags);

    rv = REAL(syscall)(__NR_timerfd_create, clockid, native_flags);
    if (rv >= 0) {
        if (native_flags & TFD_CLOEXEC) {
            filefd_CLOEXEC_enabled(rv);
        }
        filefd_opened(rv, TIMER_FD_TYPE);
    }

    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}

