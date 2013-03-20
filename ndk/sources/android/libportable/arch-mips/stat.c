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
#include <errno.h>
#include <errno_portable.h>
#include <stat_portable.h>

/* Note: The Portable Header will define stat to stat_portable */
int WRAP(stat)(const char *path, struct stat_portable *s)
{
    struct stat mips_stat;
    int ret;

    if (invalid_pointer(s)) {
        *REAL(__errno)() = EFAULT;
        return -1;
    }
    ret = REAL(stat)(path, &mips_stat);
    stat_ntop(&mips_stat, s);
    return ret;
}

int WRAP(fstat)(int fd, struct stat_portable *s)
{
    struct stat mips_stat;
    int ret;

    if (invalid_pointer(s)) {
        *REAL(__errno)() = EFAULT;
        return -1;
    }
    ret = REAL(fstat)(fd, &mips_stat);
    stat_ntop(&mips_stat, s);
    return ret;
}   

int WRAP(lstat)(const char *path, struct stat_portable *s)
{
    struct stat mips_stat;
    int ret;

    if (invalid_pointer(s)) {
        *REAL(__errno)() = EFAULT;
        return -1;
    }
    ret = REAL(lstat)(path, &mips_stat);
    stat_ntop(&mips_stat, s);
    return ret;
}

int WRAP(fstatat)(int dirfd, const char *path, struct stat_portable *s, int flags)
{
    struct stat mips_stat;
    int ret;

    if (invalid_pointer(s)) {
        *REAL(__errno)() = EFAULT;
        return -1;
    }
    ret = REAL(fstatat)(dirfd, path, &mips_stat, flags);
    stat_ntop(&mips_stat, s);
    return ret;
}
