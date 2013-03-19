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
#include <fcntl.h>
#include <stdarg.h>
#include <fcntl_portable.h>

extern int __fcntl64(int, int, void *);

int WRAP(fcntl)(int fd, int cmd, ...)
{
    va_list ap;
    void * arg;

    va_start(ap, cmd);
    arg = va_arg(ap, void *);
    va_end(ap);

    if (cmd == F_GETLK64 || 
        cmd == F_SETLK64 ||
        cmd == F_SETLKW64) {
        struct flock64 x86_flock64;
        int result = __fcntl64(fd, cmd, (void *) &x86_flock64);

        struct flock64_portable * flock64 = (struct flock64_portable *) arg;

        flock64->l_type = x86_flock64.l_type;
        flock64->l_whence = x86_flock64.l_whence;
        flock64->l_start = x86_flock64.l_start;
        flock64->l_len = x86_flock64.l_len;
        flock64->l_pid = x86_flock64.l_pid;

        return result;
    }
    else {
        return __fcntl64(fd, cmd, arg);
    }
}

