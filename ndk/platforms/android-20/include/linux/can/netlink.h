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
#ifndef CAN_NETLINK_H
#define CAN_NETLINK_H
#include <linux/types.h>
struct can_bittiming {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 bitrate;
 __u32 sample_point;
 __u32 tq;
 __u32 prop_seg;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 phase_seg1;
 __u32 phase_seg2;
 __u32 sjw;
 __u32 brp;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct can_bittiming_const {
 char name[16];
 __u32 tseg1_min;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 tseg1_max;
 __u32 tseg2_min;
 __u32 tseg2_max;
 __u32 sjw_max;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 brp_min;
 __u32 brp_max;
 __u32 brp_inc;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct can_clock {
 __u32 freq;
};
enum can_state {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 CAN_STATE_ERROR_ACTIVE = 0,
 CAN_STATE_ERROR_WARNING,
 CAN_STATE_ERROR_PASSIVE,
 CAN_STATE_BUS_OFF,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 CAN_STATE_STOPPED,
 CAN_STATE_SLEEPING,
 CAN_STATE_MAX
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct can_berr_counter {
 __u16 txerr;
 __u16 rxerr;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct can_ctrlmode {
 __u32 mask;
 __u32 flags;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CAN_CTRLMODE_LOOPBACK 0x01
#define CAN_CTRLMODE_LISTENONLY 0x02
#define CAN_CTRLMODE_3_SAMPLES 0x04
#define CAN_CTRLMODE_ONE_SHOT 0x08
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CAN_CTRLMODE_BERR_REPORTING 0x10
struct can_device_stats {
 __u32 bus_error;
 __u32 error_warning;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 error_passive;
 __u32 bus_off;
 __u32 arbitration_lost;
 __u32 restarts;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
enum {
 IFLA_CAN_UNSPEC,
 IFLA_CAN_BITTIMING,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 IFLA_CAN_BITTIMING_CONST,
 IFLA_CAN_CLOCK,
 IFLA_CAN_STATE,
 IFLA_CAN_CTRLMODE,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 IFLA_CAN_RESTART_MS,
 IFLA_CAN_RESTART,
 IFLA_CAN_BERR_COUNTER,
 __IFLA_CAN_MAX
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define IFLA_CAN_MAX (__IFLA_CAN_MAX - 1)
#endif
