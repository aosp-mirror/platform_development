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
#ifndef CAN_BCM_H
#define CAN_BCM_H
#include <linux/types.h>
#include <linux/can.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct bcm_msg_head {
 __u32 opcode;
 __u32 flags;
 __u32 count;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct timeval ival1, ival2;
 canid_t can_id;
 __u32 nframes;
 struct can_frame frames[0];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
enum {
 TX_SETUP = 1,
 TX_DELETE,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 TX_READ,
 TX_SEND,
 RX_SETUP,
 RX_DELETE,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 RX_READ,
 TX_STATUS,
 TX_EXPIRED,
 RX_STATUS,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 RX_TIMEOUT,
 RX_CHANGED
};
#define SETTIMER 0x0001
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define STARTTIMER 0x0002
#define TX_COUNTEVT 0x0004
#define TX_ANNOUNCE 0x0008
#define TX_CP_CAN_ID 0x0010
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define RX_FILTER_ID 0x0020
#define RX_CHECK_DLC 0x0040
#define RX_NO_AUTOTIMER 0x0080
#define RX_ANNOUNCE_RESUME 0x0100
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define TX_RESET_MULTI_IDX 0x0200
#define RX_RTR_FRAME 0x0400
#endif
