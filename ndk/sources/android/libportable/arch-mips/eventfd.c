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

#include <asm/unistd-portable.h>
#include <asm/unistd.h>

#include <fcntl_portable.h>
#include <sys/eventfd.h>
#include <eventfd_portable.h>
#include <filefd_portable.h>

#define PORTABLE_TAG "eventfd_portable"
#include <log_portable.h>


/* NOTE: LTP defaults to using O_NONBLOCK even if EFD_NONBLOCK is defined */


/*
 * Portable to Native event flags mapper.
 */
static inline int efd_flags_pton(int portable_flags)
{
    int native_flags = 0;

    ALOGV("%s(portable_flags:0x%x) {", __func__, portable_flags);

    if (portable_flags & EFD_NONBLOCK_PORTABLE) {
        native_flags |= EFD_NONBLOCK;
        portable_flags &= ~EFD_NONBLOCK_PORTABLE;
    }

    if (portable_flags & EFD_CLOEXEC_PORTABLE) {
        native_flags |= EFD_CLOEXEC;
        portable_flags &= EFD_CLOEXEC_PORTABLE;
    }

    if (portable_flags & EFD_SEMAPHORE_PORTABLE) {
        native_flags |= EFD_SEMAPHORE;
        portable_flags &= EFD_SEMAPHORE_PORTABLE;
    }

    if (portable_flags != 0) {
        ALOGW("%s: portable_flags:0x%x != 0; Unsupported Flags being used!",
        __func__,  portable_flags);
    }
    ALOGV("%s: return(native_flags:%d); }", __func__, native_flags);
    return native_flags;
}


/*
 * In the original eventfd() the portable_flags were unused up to
 * linux 2.6.26 and had to be zero. Android simply uses the
 * new eventfd2 system call number, so it likely best to just use
 * the Android eventfd() for both eventfd and eventfd2 system calls.
 */
int WRAP(eventfd)(unsigned int initval, int portable_flags) {
    int rv;
    int native_flags;

    ALOGV(" ");
    ALOGV("%s(initval:%u, portable_flags:%d) {", __func__,
              initval,    portable_flags);

    native_flags = efd_flags_pton(portable_flags);

    rv = REAL(eventfd)(initval, native_flags);
    if (rv >= 0) {
        if (native_flags & EFD_CLOEXEC) {
            filefd_CLOEXEC_enabled(rv);
        }
        filefd_opened(rv, EVENT_FD_TYPE);
    }

    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}

