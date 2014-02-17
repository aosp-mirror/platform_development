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
#ifndef _PPS_H_
#define _PPS_H_
#include <linux/types.h>
#define PPS_VERSION "5.3.6"
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PPS_MAX_SOURCES 16
#define PPS_API_VERS_1 1
#define PPS_API_VERS PPS_API_VERS_1
#define PPS_MAX_NAME_LEN 32
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct pps_ktime {
 __s64 sec;
 __s32 nsec;
 __u32 flags;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define PPS_TIME_INVALID (1<<0)
struct pps_kinfo {
 __u32 assert_sequence;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 clear_sequence;
 struct pps_ktime assert_tu;
 struct pps_ktime clear_tu;
 int current_mode;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct pps_kparams {
 int api_version;
 int mode;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct pps_ktime assert_off_tu;
 struct pps_ktime clear_off_tu;
};
#define PPS_CAPTUREASSERT 0x01
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PPS_CAPTURECLEAR 0x02
#define PPS_CAPTUREBOTH 0x03
#define PPS_OFFSETASSERT 0x10
#define PPS_OFFSETCLEAR 0x20
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PPS_CANWAIT 0x100
#define PPS_CANPOLL 0x200
#define PPS_ECHOASSERT 0x40
#define PPS_ECHOCLEAR 0x80
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PPS_TSFMT_TSPEC 0x1000
#define PPS_TSFMT_NTPFP 0x2000
#define PPS_KC_HARDPPS 0
#define PPS_KC_HARDPPS_PLL 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PPS_KC_HARDPPS_FLL 2
struct pps_fdata {
 struct pps_kinfo info;
 struct pps_ktime timeout;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct pps_bind_args {
 int tsformat;
 int edge;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int consumer;
};
#include <linux/ioctl.h>
#define PPS_GETPARAMS _IOR('p', 0xa1, struct pps_kparams *)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PPS_SETPARAMS _IOW('p', 0xa2, struct pps_kparams *)
#define PPS_GETCAP _IOR('p', 0xa3, int *)
#define PPS_FETCH _IOWR('p', 0xa4, struct pps_fdata *)
#define PPS_KC_BIND _IOW('p', 0xa5, struct pps_bind_args *)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
