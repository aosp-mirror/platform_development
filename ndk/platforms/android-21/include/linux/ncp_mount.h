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
#ifndef _LINUX_NCP_MOUNT_H
#define _LINUX_NCP_MOUNT_H
#include <linux/types.h>
#include <linux/ncp.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NCP_MOUNT_VERSION 3
#define NCP_MOUNT_SOFT 0x0001
#define NCP_MOUNT_INTR 0x0002
#define NCP_MOUNT_STRONG 0x0004
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NCP_MOUNT_NO_OS2 0x0008
#define NCP_MOUNT_NO_NFS 0x0010
#define NCP_MOUNT_EXTRAS 0x0020
#define NCP_MOUNT_SYMLINKS 0x0040
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NCP_MOUNT_NFS_EXTRAS 0x0080
struct ncp_mount_data {
 int version;
 unsigned int ncp_fd;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __kernel_uid_t mounted_uid;
 __kernel_pid_t wdog_pid;
 unsigned char mounted_vol[NCP_VOLNAME_LEN + 1];
 unsigned int time_out;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int retry_count;
 unsigned int flags;
 __kernel_uid_t uid;
 __kernel_gid_t gid;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __kernel_mode_t file_mode;
 __kernel_mode_t dir_mode;
};
#define NCP_MOUNT_VERSION_V4 (4)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct ncp_mount_data_v4 {
 int version;
 unsigned long flags;
 unsigned long mounted_uid;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 long wdog_pid;
 unsigned int ncp_fd;
 unsigned int time_out;
 unsigned int retry_count;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long uid;
 unsigned long gid;
 unsigned long file_mode;
 unsigned long dir_mode;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define NCP_MOUNT_VERSION_V5 (5)
#endif
