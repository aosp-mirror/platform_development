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

#ifndef _FCNTL_PORTABLE_H_
#define _FCNTL_PORTABLE_H_

/* Derived from development/ndk/platforms/android-3/arch-arm/include/asm/fcntl.h */
/* NB x86 does not have these and only uses the generic definitions. */
#define O_DIRECTORY_PORTABLE    040000
#define O_NOFOLLOW_PORTABLE     0100000
#define O_DIRECT_PORTABLE       0200000
#define O_LARGEFILE_PORTABLE    0400000

/* Derived from development/ndk/platforms/android-3/include/asm-generic/fcntl.h */
#define O_ACCMODE_PORTABLE  00000003
#define O_RDONLY_PORTABLE   00000000
#define O_WRONLY_PORTABLE   00000001
#define O_RDWR_PORTABLE     00000002
#ifndef O_CREAT_PORTABLE
#define O_CREAT_PORTABLE    00000100
#endif
#ifndef O_EXCL_PORTABLE
#define O_EXCL_PORTABLE     00000200
#endif
#ifndef O_NOCTTY_PORTABLE
#define O_NOCTTY_PORTABLE   00000400
#endif
#ifndef O_TRUNC_PORTABLE
#define O_TRUNC_PORTABLE    00001000
#endif
#ifndef O_APPEND_PORTABLE
#define O_APPEND_PORTABLE   00002000
#endif
#ifndef O_NONBLOCK_PORTABLE
#define O_NONBLOCK_PORTABLE 00004000
#endif
#ifndef O_SYNC_PORTABLE
#define O_SYNC_PORTABLE     00010000
#endif
#ifndef FASYNC_PORTABLE
#define FASYNC_PORTABLE     00020000
#endif
#ifndef O_DIRECT_PORTABLE
#define O_DIRECT_PORTABLE   00040000
#endif
#ifndef O_LARGEFILE_PORTABLE
#define O_LARGEFILE_PORTABLE    00100000
#endif
#ifndef O_DIRECTORY_PORTABLE
#define O_DIRECTORY_PORTABLE    00200000
#endif
#ifndef O_NOFOLLOW_PORTABLE
#define O_NOFOLLOW_PORTABLE 00400000
#endif
#ifndef O_NOATIME_PORTABLE
#define O_NOATIME_PORTABLE  01000000
#endif
#ifndef O_NDELAY_PORTABLE
#define O_NDELAY_PORTABLE   O_NONBLOCK_PORTABLE
#endif

/* From Bionic libc/kernel/common/asm-generic/fcntl.h */
#ifndef O_CLOEXEC_PORTABLE
#define O_CLOEXEC_PORTABLE 02000000
#endif

#ifndef __ARCH_FLOCK64_PAD
#define __ARCH_FLOCK64_PAD
#endif

/*
 * For use with F_GETLK and F_SETLK
 */
struct flock_portable {
   short l_type;
   short l_whence;
   off_t l_start;
   off_t l_len;
   pid_t l_pid;
   __ARCH_FLOCK64_PAD
};

/*
 * For use with F_GETLK64 and F_SETLK64
 */
struct flock64_portable {
   short l_type;
   short l_whence;
   unsigned char __padding[4];
   loff_t l_start;
   loff_t l_len;
   pid_t l_pid;
   __ARCH_FLOCK64_PAD
};

#if 0
/*
 * The X86 Version is
 */
struct flock64 {
   short l_type;
   short l_whence;
   loff_t l_start;
   loff_t l_len;
   pid_t l_pid;
   __ARCH_FLOCK64_PAD
};
#endif /* 0 */


#ifndef F_DUPFD_PORTABLE
#define F_DUPFD_PORTABLE 0
#define F_GETFD_PORTABLE 1
#define F_SETFD_PORTABLE 2
#define F_GETFL_PORTABLE 3
#define F_SETFL_PORTABLE 4
#endif

#ifndef F_GETLK_PORTABLE
#define F_GETLK_PORTABLE 5
#define F_SETLK_PORTABLE 6
#define F_SETLKW_PORTABLE 7
#endif

#ifndef F_SETOWN_PORTABLE
#define F_SETOWN_PORTABLE 8
#define F_GETOWN_PORTABLE 9
#endif

#ifndef F_SETSIG_PORTABLE
#define F_SETSIG_PORTABLE 10
#define F_GETSIG_PORTABLE 11
#endif

#ifndef F_GETLK64_PORTABLE
#define F_GETLK64_PORTABLE 12
#define F_SETLK64_PORTABLE 13
#define F_SETLKW64_PORTABLE 14
#endif

/* This constant seems to be the same for all ARCH's */
#define F_LINUX_SPECIFIC_BASE_PORTABLE 1024

#define F_SETLEASE_PORTABLE             (F_LINUX_SPECIFIC_BASE+0)       /* 1024 */
#define F_GETLEASE_PORTABLE             (F_LINUX_SPECIFIC_BASE+1)       /* 1025 */
#define F_NOTIFY_PORTABLE               (F_LINUX_SPECIFIC_BASE+2)       /* 1026 */

/* Currently these are only supported by X86_64 */
#define F_CANCELLK_PORTABLE             (F_LINUX_SPECIFIC_BASE+5)       /* 1029 */
#define F_DUPFD_CLOEXEC_PORTABLE        (F_LINUX_SPECIFIC_BASE+6)       /* 1030 */

#endif /* _FCNTL_PORTABLE_H */
