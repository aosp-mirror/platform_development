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
#ifndef _UAPI_LINUX_ERRQUEUE_H
#define _UAPI_LINUX_ERRQUEUE_H
#include <linux/types.h>
struct sock_extended_err {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 ee_errno;
 __u8 ee_origin;
 __u8 ee_type;
 __u8 ee_code;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 ee_pad;
 __u32 ee_info;
 __u32 ee_data;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SO_EE_ORIGIN_NONE 0
#define SO_EE_ORIGIN_LOCAL 1
#define SO_EE_ORIGIN_ICMP 2
#define SO_EE_ORIGIN_ICMP6 3
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SO_EE_ORIGIN_TXSTATUS 4
#define SO_EE_ORIGIN_TIMESTAMPING SO_EE_ORIGIN_TXSTATUS
#define SO_EE_OFFENDER(ee) ((struct sockaddr*)((ee)+1))
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
