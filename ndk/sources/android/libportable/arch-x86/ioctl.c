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

#include <stdarg.h>
#include <sys/ioctl.h>
#include <ioctls_portable.h>

#if FIOQSIZE_PORTABLE == FIOQSIZE
#error Bad build environment
#endif

static inline int x86_change_request(int request)
{
    if (request == FIOQSIZE_PORTABLE)
        return FIOQSIZE;

    return request;
}

extern int __ioctl(int, int, void *);
int ioctl_portable(int fd, int request, ...)
{
    va_list ap;
    void * arg;

    va_start(ap, request);
    arg = va_arg(ap, void *);
    va_end(ap);

    return __ioctl(fd, x86_change_request(request), arg);
}
