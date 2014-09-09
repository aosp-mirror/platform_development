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

#ifndef _FCNTL_PORTABLE_H_
#define _FCNTL_PORTABLE_H_
#endif

#include <portability.h>
#include <fcntl.h>
#include <stdarg.h>

#define O_DIRECTORY_PORTABLE  040000
#define O_NOFOLLOW_PORTABLE   0100000
#define O_DIRECT_PORTABLE     0200000
#define O_LARGEFILE_PORTABLE  0400000

static int flags_p2n(int p_flags)
{
  int machine_flags = p_flags;
  if (p_flags & O_DIRECTORY_PORTABLE) {
    machine_flags ^= O_DIRECTORY_PORTABLE;
    machine_flags |= O_DIRECTORY;
  }
  if (p_flags & O_NOFOLLOW_PORTABLE) {
    machine_flags ^= O_NOFOLLOW_PORTABLE;
    machine_flags |= O_NOFOLLOW;
  }
  if (p_flags & O_DIRECT_PORTABLE) {
    machine_flags ^= O_DIRECT_PORTABLE;
    machine_flags |= O_DIRECT;
  }
  if (p_flags & O_LARGEFILE_PORTABLE) {
    machine_flags ^= O_LARGEFILE_PORTABLE;
    machine_flags |= O_LARGEFILE;
  }

  return machine_flags;
}

#define FLAGS_VAARGS_TRANSLATE \
  flags = flags_p2n(flags); \
  mode_t mode = 0; \
  if ((flags & O_CREAT) != 0) { \
    va_list args; \
    va_start(args, flags); \
    mode = (mode_t) va_arg(args, int); \
    va_end(args);\
  }


int WRAP(openat)(int fd, const char* path, int flags, ...)
{
  FLAGS_VAARGS_TRANSLATE
  return REAL(openat)(fd, path, flags, mode);
}

int WRAP(openat64)(int fd, const char* path, int flags, ...)
{
  FLAGS_VAARGS_TRANSLATE
  return REAL(openat64)(fd, path, flags, mode);
}

int WRAP(open)(const char* path, int flags, ...)
{
  FLAGS_VAARGS_TRANSLATE
  return REAL(open)(path, flags, mode);
}

int WRAP(open64)(const char* path, int flags, ...)
{
  FLAGS_VAARGS_TRANSLATE
  return REAL(open64)(path, flags, mode);
}
