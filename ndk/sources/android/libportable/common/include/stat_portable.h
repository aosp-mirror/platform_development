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

#ifndef _StatPortable_H_
#define _StatPortable_H_

#include <portability.h>
#include <stdint.h>
#include <stdlib.h>
#include <sys/stat.h>

#define __STAT64_BODY_PORTABLE \
  unsigned long st_dev; \
  unsigned long st_ino; \
  unsigned long st_mode; \
  unsigned long st_nlink; \
  uid_t st_uid; /* 32-bit uid_t */ \
  unsigned char padding[4]; \
  gid_t st_gid; /* 32-bit gid_t */ \
  unsigned char padding2[4]; \
  unsigned long st_rdev; \
  long st_size; \
  long st_blksize; \
  long st_blocks; \
  long st_atime; \
  unsigned long st_atime_nsec; \
  long st_mtime; \
  unsigned long st_mtime_nsec; \
  long st_ctime; \
  unsigned long st_ctime_nsec; \
  unsigned char padding3[8];

struct StatPortable { __STAT64_BODY_PORTABLE };
typedef struct StatPortable Stat64Portable;

static inline void stat_n2p(struct stat* pn, struct StatPortable* pp)
{
  memset(pp, '\0', sizeof(struct StatPortable));
  pp->st_dev        = pn->st_dev;
  pp->st_ino        = pn->st_ino;
  pp->st_mode       = pn->st_mode;
  pp->st_nlink      = pn->st_nlink;
  pp->st_uid        = pn->st_uid;
  pp->st_gid        = pn->st_gid;
  pp->st_rdev       = pn->st_rdev;
  pp->st_size       = pn->st_size;
  pp->st_blksize    = pn->st_blksize;
  pp->st_blocks     = pn->st_blocks;
  pp->st_atime      = pn->st_atime;
  pp->st_atime_nsec = pn->st_atime_nsec;
  pp->st_mtime      = pn->st_mtime;
  pp->st_mtime_nsec = pn->st_mtime_nsec;
  pp->st_ctime      = pn->st_ctime;
  pp->st_ctime_nsec = pn->st_ctime_nsec;
}

int WRAP(fstat)(int a, struct StatPortable* p)
{
  struct stat target_stat_obj;
  int ret = REAL(fstat)(a, &target_stat_obj);
  stat_n2p(&target_stat_obj, p);
  return ret;
}

int WRAP(fstat64)(int a, Stat64Portable* p)
{
  return WRAP(fstat)(a, p);
}

int WRAP(fstatat)(int a, const char* p1, struct StatPortable* p2, int b)
{
  struct stat target_stat_obj;
  int ret = REAL(fstatat)(a, p1, &target_stat_obj, b);
  stat_n2p(&target_stat_obj, p2);
  return ret;
}

int WRAP(fstatat64)(int a, const char* b, Stat64Portable* c, int d)
{
  return WRAP(fstatat)(a, b, c, d);
}

int WRAP(lstat)(const char* a, struct StatPortable* p)
{
  struct stat target_stat_obj;
  int ret = REAL(lstat)(a, &target_stat_obj);
  stat_n2p(&target_stat_obj, p);
  return ret;
}

int WRAP(lstat64)(const char* a, Stat64Portable* p)
{
  return WRAP(lstat)(a, p);
}

int WRAP(stat)(const char* a, struct StatPortable* p)
{
  struct stat target_stat_obj;
  int ret = REAL(stat)(a, &target_stat_obj);
  stat_n2p(&target_stat_obj, p);
  return ret;
}

int WRAP(stat64)(const char* a, Stat64Portable* p)
{
  return WRAP(stat)(a, p);
}

#endif /* _StatPortable_H */
