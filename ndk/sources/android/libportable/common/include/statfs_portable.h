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

#ifndef _STATFS_PORTABLE_H_
#define _STATFS_PORTABLE_H_

#include <sys/vfs.h>

/* It's easy to change kernel to support statfs */
struct statfs_portable {
    uint32_t        f_type;
    uint32_t        f_bsize;
    uint64_t        f_blocks;
    uint64_t        f_bfree;
    uint64_t        f_bavail;
    uint64_t        f_files;
    uint64_t        f_ffree;
    __fsid_t        f_fsid;
    uint32_t        f_namelen;
    uint32_t        f_frsize;
    uint32_t        f_flags;
    uint32_t        f_spare[4];
};

/*
The MIPS Version is
struct statfs {
    uint32_t        f_type;
    uint32_t        f_bsize;
    uint32_t        f_frsize;
    uint32_t        __pad;
    uint64_t        f_blocks;
    uint64_t        f_bfree;
    uint64_t        f_files;
    uint64_t        f_ffree;
    uint64_t        f_bavail;
    __fsid_t        f_fsid;
    uint32_t        f_namelen;
    uint32_t        f_flags;
    uint32_t        f_spare[5];
};
*/
#endif /* _STATFS_PORTABLE_H_ */
