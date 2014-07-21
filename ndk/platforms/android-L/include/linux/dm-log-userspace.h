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
#ifndef __DM_LOG_USERSPACE_H__
#define __DM_LOG_USERSPACE_H__
#include <linux/dm-ioctl.h>
#define DM_ULOG_CTR 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DM_ULOG_DTR 2
#define DM_ULOG_PRESUSPEND 3
#define DM_ULOG_POSTSUSPEND 4
#define DM_ULOG_RESUME 5
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DM_ULOG_GET_REGION_SIZE 6
#define DM_ULOG_IS_CLEAN 7
#define DM_ULOG_IN_SYNC 8
#define DM_ULOG_FLUSH 9
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DM_ULOG_MARK_REGION 10
#define DM_ULOG_CLEAR_REGION 11
#define DM_ULOG_GET_RESYNC_WORK 12
#define DM_ULOG_SET_REGION_SYNC 13
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DM_ULOG_GET_SYNC_COUNT 14
#define DM_ULOG_STATUS_INFO 15
#define DM_ULOG_STATUS_TABLE 16
#define DM_ULOG_IS_REMOTE_RECOVERING 17
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DM_ULOG_REQUEST_MASK 0xFF
#define DM_ULOG_REQUEST_TYPE(request_type)   (DM_ULOG_REQUEST_MASK & (request_type))
#define DM_ULOG_REQUEST_VERSION 3
struct dm_ulog_request {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint64_t luid;
 char uuid[DM_UUID_LEN];
 char padding[3];
 uint32_t version;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int32_t error;
 uint32_t seq;
 uint32_t request_type;
 uint32_t data_size;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char data[0];
};
#endif
