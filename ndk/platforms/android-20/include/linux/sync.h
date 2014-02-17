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
#ifndef _UAPI_LINUX_SYNC_H
#define _UAPI_LINUX_SYNC_H
#include <linux/ioctl.h>
#include <linux/types.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct sync_merge_data {
 __s32 fd2;
 char name[32];
 __s32 fence;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct sync_pt_info {
 __u32 len;
 char obj_name[32];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char driver_name[32];
 __s32 status;
 __u64 timestamp_ns;
 __u8 driver_data[0];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct sync_fence_info_data {
 __u32 len;
 char name[32];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __s32 status;
 __u8 pt_info[0];
};
#define SYNC_IOC_MAGIC '>'
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SYNC_IOC_WAIT _IOW(SYNC_IOC_MAGIC, 0, __s32)
#define SYNC_IOC_MERGE _IOWR(SYNC_IOC_MAGIC, 1, struct sync_merge_data)
#define SYNC_IOC_FENCE_INFO _IOWR(SYNC_IOC_MAGIC, 2,  struct sync_fence_info_data)
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
