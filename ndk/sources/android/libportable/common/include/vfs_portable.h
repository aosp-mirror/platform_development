/*
 * Copyright 2014, The Android Open Source Project
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

#ifndef _VFS_PORTABLE_H
#define _VFS_PORTABLE_H

#include <stdlib.h>
#include <sys/vfs.h>

/* The kernel's __kernel_fsid_t has a 'val' member but glibc uses '__val'. */
typedef struct { int __val[2]; } __fsid_t_portable;
typedef __fsid_t_portable fsid_t_portable;

#define __STATFS64_BODY_PORTABLE \
  uint64_t f_type; \
  uint64_t f_bsize; \
  uint64_t f_blocks; \
  uint64_t f_bfree; \
  uint64_t f_bavail; \
  uint64_t f_files; \
  uint64_t f_ffree; \
  fsid_t_portable f_fsid; \
  uint64_t f_namelen; \
  uint64_t f_frsize; \
  uint64_t f_flags; \
  uint64_t f_spare[5]; \


struct statfs_portable { __STATFS64_BODY_PORTABLE };
struct statfs64_portable { __STATFS64_BODY_PORTABLE };

static inline
void statfs_ntop(struct statfs *n_statfs, struct statfs_portable *p_statfs) {
  memset(p_statfs, 0, sizeof(struct statfs_portable));
  p_statfs->f_type = n_statfs->f_type;
  p_statfs->f_bsize = n_statfs->f_bsize;
  p_statfs->f_blocks = n_statfs->f_blocks;
  p_statfs->f_bfree = n_statfs->f_bfree;
  p_statfs->f_bavail = n_statfs->f_bavail;
  p_statfs->f_files = n_statfs->f_files;
  p_statfs->f_ffree = n_statfs->f_ffree;
  memcpy(&p_statfs->f_fsid, &n_statfs->f_fsid, sizeof(int)*2);
  p_statfs->f_namelen = n_statfs->f_namelen;
  p_statfs->f_frsize = n_statfs->f_frsize;
  p_statfs->f_flags = n_statfs->f_flags;
#ifdef __mips__
  memcpy(&p_statfs->f_spare, &n_statfs->f_spare, 4);
#else
  memcpy(&p_statfs->f_spare, &n_statfs->f_spare, 5);
#endif
}


static inline
int WRAP(statfs)(const char* path, struct statfs_portable* stat) {
  struct statfs native_stat;

  int ret = REAL(statfs)(path, &native_stat);
  statfs_ntop(&native_stat, stat);
  return ret;
}

static inline
int WRAP(statfs64)(const char* path, struct statfs64_portable* stat) {
  return WRAP(statfs)(path, (struct statfs_portable*)stat);
}


static inline
int WRAP(fstatfs)(int fd, struct statfs_portable* stat) {
  struct statfs native_stat;

  int ret = REAL(fstatfs)(fd, &native_stat);
  statfs_ntop(&native_stat, stat);
  return ret;
}

static inline
int WRAP(fstatfs64)(int fd, struct statfs64_portable* stat) {
  return WRAP(fstatfs)(fd, (struct statfs_portable*)stat);
}

#endif
