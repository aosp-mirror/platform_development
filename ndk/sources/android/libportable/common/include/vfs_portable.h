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

#ifndef _VFS_PORTABLE_H_
#define _VFS_PORTABLE_H_

#include <portability.h>
#include <stdint.h>
#include <stdlib.h>
#include <sys/vfs.h>

typedef __fsid_t fsid_t;

#define __STATFS64_BODY_PORTABLE \
  uint64_t f_type; \
  uint64_t f_bsize; \
  uint64_t f_blocks; \
  uint64_t f_bfree; \
  uint64_t f_bavail; \
  uint64_t f_files; \
  uint64_t f_ffree; \
  fsid_t f_fsid; \
  uint64_t f_namelen; \
  uint64_t f_frsize; \
  uint64_t f_flags; \
  uint64_t f_spare[5];


struct StatfsPortable { __STATFS64_BODY_PORTABLE };
typedef struct StatfsPortable Statfs64Portable;

#undef __STATFS64_BODY_PORTABLE

static void statfs_n2p(const struct statfs* pn, struct StatfsPortable* pp)
{
  memset(pp, '\0', sizeof(struct StatfsPortable));
  pp->f_type    = pn->f_type;
  pp->f_bsize   = pn->f_bsize;
  pp->f_blocks  = pn->f_blocks;
  pp->f_bfree   = pn->f_bfree;
  pp->f_bavail  = pn->f_bavail;
  pp->f_files   = pn->f_files;
  pp->f_ffree   = pn->f_ffree;
  memcpy(&pp->f_fsid, &pn->f_fsid, sizeof(int)*2);
  pp->f_namelen = pn->f_namelen;
  pp->f_frsize  = pn->f_frsize;
  pp->f_flags   = pn->f_flags;
#ifdef __mips__
  memcpy(&pp->f_spare, &pn->f_spare, 4);
#else
  memcpy(&pp->f_spare, &pn->f_spare, 5);
#endif
}

int WRAP(statfs)(const char* path, struct StatfsPortable* stat)
{
  struct statfs target_stat;
  int ret = REAL(statfs)(path, &target_stat);
  statfs_n2p(&target_stat, stat);
  return ret;
}

int WRAP(statfs64)(const char* path, Statfs64Portable* stat)
{
  return WRAP(statfs)(path, stat);
}

int WRAP(fstatfs)(int fd, struct StatfsPortable* stat)
{
  struct statfs target_stat;
  int ret = REAL(fstatfs)(fd, &target_stat);
  statfs_n2p(&target_stat, stat);
  return ret;
}

int WRAP(fstatfs64)(int fd, Statfs64Portable* stat)
{
  return WRAP(fstatfs)(fd, stat);
}

#endif /* _VFS_PORTABLE_H */
