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
#ifndef _EBT_ULOG_H
#define _EBT_ULOG_H
#include <linux/types.h>
#define EBT_ULOG_DEFAULT_NLGROUP 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define EBT_ULOG_DEFAULT_QTHRESHOLD 1
#define EBT_ULOG_MAXNLGROUPS 32
#define EBT_ULOG_PREFIX_LEN 32
#define EBT_ULOG_MAX_QLEN 50
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define EBT_ULOG_WATCHER "ulog"
#define EBT_ULOG_VERSION 1
struct ebt_ulog_info {
 __u32 nlgroup;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int cprange;
 unsigned int qthreshold;
 char prefix[EBT_ULOG_PREFIX_LEN];
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef struct ebt_ulog_packet_msg {
 int version;
 char indev[IFNAMSIZ];
 char outdev[IFNAMSIZ];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char physindev[IFNAMSIZ];
 char physoutdev[IFNAMSIZ];
 char prefix[EBT_ULOG_PREFIX_LEN];
 struct timeval stamp;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long mark;
 unsigned int hook;
 size_t data_len;
 unsigned char data[0] __attribute__
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ((aligned (__alignof__(struct ebt_ulog_info))));
} ebt_ulog_packet_msg_t;
#endif
