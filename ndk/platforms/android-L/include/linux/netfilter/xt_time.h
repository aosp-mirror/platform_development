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
#ifndef _XT_TIME_H
#define _XT_TIME_H 1
#include <linux/types.h>
struct xt_time_info {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 date_start;
 __u32 date_stop;
 __u32 daytime_start;
 __u32 daytime_stop;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 monthdays_match;
 __u8 weekdays_match;
 __u8 flags;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum {
 XT_TIME_LOCAL_TZ = 1 << 0,
 XT_TIME_CONTIGUOUS = 1 << 1,
 XT_TIME_ALL_MONTHDAYS = 0xFFFFFFFE,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 XT_TIME_ALL_WEEKDAYS = 0xFE,
 XT_TIME_MIN_DAYTIME = 0,
 XT_TIME_MAX_DAYTIME = 24 * 60 * 60 - 1,
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define XT_TIME_ALL_FLAGS (XT_TIME_LOCAL_TZ|XT_TIME_CONTIGUOUS)
#endif
