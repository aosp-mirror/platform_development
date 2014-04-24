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
#include <stdarg.h>
#include <portability.h>
#include <fcntl_portable.h>
#include <filefd_portable.h>

#define PORTABLE_TAG "open_portable"
#include <log_portable.h>


#if O_CREAT_PORTABLE==O_CREAT
#error Bad build environment
#endif


static inline int open_flags_pton(int flags)
{
    int mipsflags = flags & O_ACCMODE_PORTABLE;

    ALOGV("%s(flags:0x%x) {", __func__, flags);

    if (flags & O_CREAT_PORTABLE)
        mipsflags |= O_CREAT;
    if (flags & O_EXCL_PORTABLE)
        mipsflags |= O_EXCL;
    if (flags & O_NOCTTY_PORTABLE)
        mipsflags |= O_NOCTTY;
    if (flags & O_TRUNC_PORTABLE)
        mipsflags |= O_TRUNC;
    if (flags & O_APPEND_PORTABLE)
        mipsflags |= O_APPEND;
    if (flags & O_NONBLOCK_PORTABLE)
        mipsflags |= O_NONBLOCK;
    if (flags & O_SYNC_PORTABLE)
        mipsflags |= O_SYNC;
    if (flags & FASYNC_PORTABLE)
        mipsflags |= FASYNC;
    if (flags & O_DIRECT_PORTABLE)
        mipsflags |= O_DIRECT;
    if (flags & O_LARGEFILE_PORTABLE)
        mipsflags |= O_LARGEFILE;
    if (flags & O_DIRECTORY_PORTABLE)
        mipsflags |= O_DIRECTORY;
    if (flags & O_NOFOLLOW_PORTABLE)
        mipsflags |= O_NOFOLLOW;
    if (flags & O_NOATIME_PORTABLE)
        mipsflags |= O_NOATIME;
    if (flags & O_NDELAY_PORTABLE)
        mipsflags |= O_NDELAY;

    ALOGV("%s: return(mipsflags:0x%x); }", __func__, mipsflags);
    return mipsflags;
}


extern int  __openat(int, const char*, int, int);

int WRAP(open)(const char *pathname, int flags, ...)
{
    mode_t mode = 0;
    if ((flags & O_CREAT_PORTABLE) != 0) {
        va_list args;
        va_start(args, flags);
        mode = (mode_t) va_arg(args, int);
        va_end(args);
    }
    flags |= O_LARGEFILE_PORTABLE;
    flags = open_flags_pton(flags);

    return __openat(AT_FDCWD, pathname, flags, mode);
}

int WRAP(openat)(int dirfd, const char *pathname, int flags, ...)
{
    mode_t mode = 0;
    if ((flags & O_CREAT_PORTABLE) != 0) {
        va_list args;
        va_start(args, flags);
        mode = (mode_t) va_arg(args, int);
        va_end(args);
    }
    flags |= O_LARGEFILE_PORTABLE;
    flags = open_flags_pton(flags);

    return __openat(dirfd, pathname, flags, mode);
}
