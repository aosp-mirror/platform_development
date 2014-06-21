/****************************************************************************
 ****************************************************************************
 ***
 ***   This header was automatically generated from a Linux kernel header
 ***   of the same name, to make information necessary for userspace to
 ***   call into the kernel available to libc.  It contains only constants,
 ***   structures, and macros generated from the original header, and thus,
 ***   contains no copyrightable information.
 ***
 ***   To edit the content of this header, modify the corresponding
 ***   source file (e.g. under external/kernel-headers/original/) then
 ***   run bionic/libc/kernel/tools/update_all.py
 ***
 ***   Any manual change here will be lost the next time this script will
 ***   be run. You've been warned!
 ***
 ****************************************************************************
 ****************************************************************************/
#ifndef _UAPI_LINUX_FCNTL_H
#define _UAPI_LINUX_FCNTL_H
#include <asm/fcntl.h>
#define F_SETLEASE (F_LINUX_SPECIFIC_BASE + 0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define F_GETLEASE (F_LINUX_SPECIFIC_BASE + 1)
#define F_CANCELLK (F_LINUX_SPECIFIC_BASE + 5)
#define F_DUPFD_CLOEXEC (F_LINUX_SPECIFIC_BASE + 6)
#define F_NOTIFY (F_LINUX_SPECIFIC_BASE+2)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define F_SETPIPE_SZ (F_LINUX_SPECIFIC_BASE + 7)
#define F_GETPIPE_SZ (F_LINUX_SPECIFIC_BASE + 8)
#define DN_ACCESS 0x00000001
#define DN_MODIFY 0x00000002
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DN_CREATE 0x00000004
#define DN_DELETE 0x00000008
#define DN_RENAME 0x00000010
#define DN_ATTRIB 0x00000020
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DN_MULTISHOT 0x80000000
#define AT_FDCWD -100
#define AT_SYMLINK_NOFOLLOW 0x100
#define AT_REMOVEDIR 0x200
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define AT_SYMLINK_FOLLOW 0x400
#define AT_NO_AUTOMOUNT 0x800
#define AT_EMPTY_PATH 0x1000
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
