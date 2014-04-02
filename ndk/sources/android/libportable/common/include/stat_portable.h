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

#ifndef _STAT_PORTABLE_H_
#define _STAT_PORTABLE_H_

#include <sys/stat.h>
#include <string.h>

#ifdef __LP64__

#define __STAT64_BODY_PORTABLE \
  unsigned long st_dev; \
  unsigned long st_ino; \
  unsigned int st_mode; \
  unsigned int st_nlink; \
  uid_t st_uid; \
  gid_t st_gid; \
  unsigned long st_rdev; \
  unsigned long __pad1; \
  long st_size; \
  int st_blksize; \
  int __pad2; \
  long st_blocks; \
  long st_atime; \
  unsigned long st_atime_nsec; \
  long st_mtime; \
  unsigned long st_mtime_nsec; \
  long st_ctime; \
  unsigned long st_ctime_nsec; \
  unsigned int __unused4; \
  unsigned int __unused5; \
  unsigned long __unused_for_largest_size; \


struct stat_portable { __STAT64_BODY_PORTABLE };
struct stat64_portable { __STAT64_BODY_PORTABLE };

static inline
void stat_ntop(struct stat *n_stat, struct stat_portable *p_stat)
{
    memset(p_stat, '\0', sizeof(struct stat_portable));
    p_stat->st_dev        = n_stat->st_dev;
    p_stat->st_ino        = n_stat->st_ino;
    p_stat->st_mode       = n_stat->st_mode;
    p_stat->st_nlink      = n_stat->st_nlink;
    p_stat->st_uid        = n_stat->st_uid;
    p_stat->st_gid        = n_stat->st_gid;
    p_stat->st_rdev       = n_stat->st_rdev;
    p_stat->st_size       = n_stat->st_size;
    p_stat->st_blksize    = n_stat->st_blksize;
    p_stat->st_blocks     = n_stat->st_blocks;
    p_stat->st_atime      = n_stat->st_atime;
    p_stat->st_atime_nsec = n_stat->st_atime_nsec;
    p_stat->st_mtime      = n_stat->st_mtime;
    p_stat->st_mtime_nsec = n_stat->st_mtime_nsec;
    p_stat->st_ctime      = n_stat->st_ctime;
    p_stat->st_ctime_nsec = n_stat->st_ctime_nsec;
}

#else // ! __LP64__

/* It's easy to change kernel to support stat */
struct stat_portable {
    unsigned long long  st_dev;
    unsigned char       __pad0[4];

    unsigned long       __st_ino;
    unsigned int        st_mode;
    unsigned int        st_nlink;

    unsigned long       st_uid;
    unsigned long       st_gid;

    unsigned long long  st_rdev;
    unsigned char       __pad3[4];

    unsigned char       __pad4[4];
    long long           st_size;
    unsigned long       st_blksize;
    unsigned char       __pad5[4];
    unsigned long long  st_blocks;

    unsigned long       st_atime;
    unsigned long       st_atime_nsec;

    unsigned long       st_mtime;
    unsigned long       st_mtime_nsec;

    unsigned long       st_ctime;
    unsigned long       st_ctime_nsec;

    unsigned long long  st_ino;
};

/*
The X86 Version is
struct stat {
    unsigned long long  st_dev;
    unsigned char       __pad0[4];

    unsigned long       __st_ino;
    unsigned int        st_mode;
    unsigned int        st_nlink;

    unsigned long       st_uid;
    unsigned long       st_gid;

    unsigned long long  st_rdev;
    unsigned char       __pad3[4];

    long long           st_size;
    unsigned long       st_blksize;
    unsigned long long  st_blocks;

    unsigned long       st_atime;
    unsigned long       st_atime_nsec;

    unsigned long       st_mtime;
    unsigned long       st_mtime_nsec;

    unsigned long       st_ctime;
    unsigned long       st_ctime_nsec;

    unsigned long long  st_ino;
};
*/

/*
The MIPS Version is
struct stat {
    unsigned long       st_dev;
    unsigned long       __pad0[3];

    unsigned long long  st_ino;

    unsigned int        st_mode;
    unsigned int        st_nlink;

    unsigned long       st_uid;
    unsigned long       st_gid;

    unsigned long       st_rdev;
    unsigned long       __pad1[3];

    long long           st_size;

    unsigned long       st_atime;
    unsigned long       st_atime_nsec;

    unsigned long       st_mtime;
    unsigned long       st_mtime_nsec;

    unsigned long       st_ctime;
    unsigned long       st_ctime_nsec;

    unsigned long       st_blksize;
    unsigned long       __pad2;

    unsigned long long  st_blocks;
};
*/

static inline void stat_ntop(struct stat *n_stat, struct stat_portable *p_stat)
{
    memset(p_stat, '\0', sizeof(struct stat_portable));
    p_stat->st_dev        = n_stat->st_dev;
#if defined(__mips__)
    /* MIPS doesn't have __st_ino */
    p_stat->__st_ino      = 0;
#else
    p_stat->__st_ino      = n_stat->__st_ino;
#endif
    p_stat->st_mode       = n_stat->st_mode;
    p_stat->st_nlink      = n_stat->st_nlink;
    p_stat->st_uid        = n_stat->st_uid;
    p_stat->st_gid        = n_stat->st_gid;
    p_stat->st_rdev       = n_stat->st_rdev;
    p_stat->st_size       = n_stat->st_size;
    p_stat->st_blksize    = n_stat->st_blksize;
    p_stat->st_blocks     = n_stat->st_blocks;
    p_stat->st_atime      = n_stat->st_atime;
    p_stat->st_atime_nsec = n_stat->st_atime_nsec;
    p_stat->st_mtime      = n_stat->st_mtime;
    p_stat->st_mtime_nsec = n_stat->st_mtime_nsec;
    p_stat->st_ctime      = n_stat->st_ctime;
    p_stat->st_ctime_nsec = n_stat->st_ctime_nsec;
    p_stat->st_ino        = n_stat->st_ino;
}

#endif // __LP64__

#endif /* _STAT_PORTABLE_H */
