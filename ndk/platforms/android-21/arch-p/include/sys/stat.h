/*
 * Copyright (C) 2008 The Android Open Source Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
#ifndef _SYS_STAT_H_
#define _SYS_STAT_H_

#include <sys/cdefs.h>
#include <sys/types.h>
#include <sys/time.h>
#include <linux/stat.h>

#include <endian.h>

__BEGIN_DECLS

#define __STAT64_BODY \
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

struct stat { __STAT64_BODY };
struct stat64 { __STAT64_BODY };

#undef __STAT64_BODY

#define st_atimensec st_atime_nsec
#define st_mtimensec st_mtime_nsec
#define st_ctimensec st_ctime_nsec

#ifdef __USE_BSD
/* Permission macros provided by glibc for compatibility with BSDs. */
#define ACCESSPERMS (S_IRWXU | S_IRWXG | S_IRWXO) /* 0777 */
#define ALLPERMS    (S_ISUID | S_ISGID | S_ISVTX | S_IRWXU | S_IRWXG | S_IRWXO) /* 07777 */
#define DEFFILEMODE (S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH) /* 0666 */
#endif

extern int chmod(const char*, mode_t);
extern int fchmod(int, mode_t);
extern int mkdir(const char*, mode_t);

extern int fstat(int, struct stat*);
extern int fstat64(int, struct stat64*);
extern int fstatat(int, const char*, struct stat*, int);
extern int fstatat64(int, const char*, struct stat64*, int);
extern int lstat(const char*, struct stat*);
extern int lstat64(const char*, struct stat64*);
extern int stat(const char*, struct stat*);
extern int stat64(const char*, struct stat64*);

extern int mknod(const char*, mode_t, dev_t);
extern mode_t umask(mode_t);

#if defined(__BIONIC_FORTIFY)

extern mode_t __umask_chk(mode_t);
extern mode_t __umask_real(mode_t) __asm__(__USER_LABEL_PREFIX__ "umask");
__errordecl(__umask_invalid_mode, "umask called with invalid mode");

__BIONIC_FORTIFY_INLINE
mode_t umask(mode_t mode) {
#if !defined(__clang__)
  if (__builtin_constant_p(mode)) {
    if ((mode & 0777) != mode) {
      __umask_invalid_mode();
    }
    return __umask_real(mode);
  }
#endif
  return __umask_chk(mode);
}
#endif /* defined(__BIONIC_FORTIFY) */

extern int mkfifo(const char*, mode_t);

extern int fchmodat(int, const char*, mode_t, int);
extern int mkdirat(int, const char*, mode_t);
extern int mknodat(int, const char*, mode_t, dev_t);

#define UTIME_NOW  ((1L << 30) - 1L)
#define UTIME_OMIT ((1L << 30) - 2L)
extern int utimensat(int fd, const char *path, const struct timespec times[2], int flags);
extern int futimens(int fd, const struct timespec times[2]);

__END_DECLS

#endif /* _SYS_STAT_H_ */
