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

#include <fcntl.h>
#include <stdarg.h>
#include <fcntl_portable.h>

#if F_GETLK_PORTABLE==F_GETLK
#error Bad build environment
#endif

static inline int mips_change_cmd(int cmd)
{
    switch(cmd) {
    case F_GETLK_PORTABLE:
        return F_GETLK;
    case F_SETLK_PORTABLE:
        return F_SETLK;
    case F_SETLKW_PORTABLE:
        return F_SETLKW;
    case F_SETOWN_PORTABLE:
        return F_SETOWN;
    case F_GETOWN_PORTABLE:
        return F_GETOWN;
    case F_GETLK64_PORTABLE:
        return F_GETLK64;
    case F_SETLK64_PORTABLE:
        return F_SETLK64;
    case F_SETLKW64_PORTABLE:
        return F_SETLKW64;
    }
    return cmd;
}

extern int __fcntl64(int, int, void *);

int fcntl_portable(int fd, int cmd, ...)
{
    va_list ap;
    void * arg;

    va_start(ap, cmd);
    arg = va_arg(ap, void *);
    va_end(ap);

    return __fcntl64(fd, mips_change_cmd(cmd), arg);
}

