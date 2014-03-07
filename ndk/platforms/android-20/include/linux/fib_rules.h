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
#ifndef __LINUX_FIB_RULES_H
#define __LINUX_FIB_RULES_H
#include <linux/types.h>
#include <linux/rtnetlink.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FIB_RULE_PERMANENT 0x00000001
#define FIB_RULE_INVERT 0x00000002
#define FIB_RULE_UNRESOLVED 0x00000004
#define FIB_RULE_IIF_DETACHED 0x00000008
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FIB_RULE_DEV_DETACHED FIB_RULE_IIF_DETACHED
#define FIB_RULE_OIF_DETACHED 0x00000010
#define FIB_RULE_FIND_SADDR 0x00010000
struct fib_rule_hdr {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 family;
 __u8 dst_len;
 __u8 src_len;
 __u8 tos;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 table;
 __u8 res1;
 __u8 res2;
 __u8 action;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 flags;
};
enum {
 FRA_UNSPEC,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 FRA_DST,
 FRA_SRC,
 FRA_IIFNAME,
#define FRA_IFNAME FRA_IIFNAME
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 FRA_GOTO,
 FRA_UNUSED2,
 FRA_PRIORITY,
 FRA_UNUSED3,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 FRA_UNUSED4,
 FRA_UNUSED5,
 FRA_FWMARK,
 FRA_FLOW,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 FRA_UNUSED6,
 FRA_UNUSED7,
 FRA_UNUSED8,
 FRA_TABLE,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 FRA_FWMASK,
 FRA_OIFNAME,
 __FRA_MAX
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FRA_MAX (__FRA_MAX - 1)
enum {
 FR_ACT_UNSPEC,
 FR_ACT_TO_TBL,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 FR_ACT_GOTO,
 FR_ACT_NOP,
 FR_ACT_RES3,
 FR_ACT_RES4,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 FR_ACT_BLACKHOLE,
 FR_ACT_UNREACHABLE,
 FR_ACT_PROHIBIT,
 __FR_ACT_MAX,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define FR_ACT_MAX (__FR_ACT_MAX - 1)
#endif
