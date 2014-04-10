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

#include <portability.h>
#include <stat_portable.h>
#include <sys/stat.h>


int WRAP(fstat)(int fd, struct stat_portable *s) {
  struct stat x86_64_stat;
  int ret = REAL(fstat)(fd, &x86_64_stat);
  stat_ntop(&x86_64_stat, s);
  return ret;
}
static inline int WRAP(fstat64)(int fd, struct stat64_portable *s) {
  return WRAP(fstat)(fd, (struct stat_portable*)s);
}


int WRAP(fstatat)(int dirfd, const char *path, struct stat_portable *s, int flags) {
  struct stat x86_64_stat;
  int ret = REAL(fstatat)(dirfd, path, &x86_64_stat, flags);
  stat_ntop(&x86_64_stat, s);
  return ret;
}
static inline int WRAP(fstatat64)(int dirfd, const char *path,
                                  struct stat64_portable *s, int flags) {
  return WRAP(fstatat)(dirfd, path, (struct stat_portable*)s, flags);
}


int WRAP(lstat)(const char *path, struct stat_portable *s) {
  struct stat x86_64_stat;
  int ret = REAL(lstat)(path, &x86_64_stat);
  stat_ntop(&x86_64_stat, s);
  return ret;
}
static inline int WRAP(lstat64)(const char *path, struct stat64_portable *s) {
  return WRAP(lstat)(path, (struct stat_portable*)s);
}


int WRAP(stat)(const char* path, struct stat_portable* s) {
  struct stat x86_64_stat;
  int ret = REAL(stat)(path, &x86_64_stat);
  stat_ntop(&x86_64_stat, s);
  return ret;
}
static inline int WRAP(stat64)(const char* path, struct stat64_portable *s) {
  return WRAP(stat)(path, (struct stat_portable*)s);
}

