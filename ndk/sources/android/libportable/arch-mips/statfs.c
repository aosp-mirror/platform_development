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
#include <string.h>
#include <errno.h>
#include <errno_portable.h>
#include <statfs_portable.h>

static inline void statfs_ntop(struct statfs *n_statfs, struct statfs_portable *p_statfs)
{
    memset(p_statfs, '\0', sizeof(struct statfs_portable));
    p_statfs->f_type = n_statfs->f_type;
    p_statfs->f_bsize = n_statfs->f_bsize;
    p_statfs->f_blocks = n_statfs->f_blocks;
    p_statfs->f_bfree = n_statfs->f_bfree;
    p_statfs->f_bavail = n_statfs->f_bavail;
    p_statfs->f_files = n_statfs->f_files;
    p_statfs->f_ffree = n_statfs->f_ffree;
    p_statfs->f_fsid = n_statfs->f_fsid;
    p_statfs->f_namelen = n_statfs->f_namelen;
    p_statfs->f_frsize = n_statfs->f_frsize;
    p_statfs->f_flags = n_statfs->f_flags;
}

int WRAP(statfs)(const char*  path, struct statfs_portable*  stat)
{
    struct statfs mips_stat;
    int ret;

    if (invalid_pointer(stat)) {
        *REAL(__errno)() = EFAULT;
        return -1;
    }
    ret = REAL(statfs)(path, &mips_stat);
    statfs_ntop(&mips_stat, stat);
    return ret;
}

int WRAP(fstatfs)(int fd, struct statfs_portable*  stat)
{
    struct statfs mips_stat;
    int ret;

    if (invalid_pointer(stat)) {
        *REAL(__errno)() = EFAULT;
        return -1;
    }
    ret = REAL(fstatfs)(fd, &mips_stat);
    statfs_ntop(&mips_stat, stat);
    return ret;
}
