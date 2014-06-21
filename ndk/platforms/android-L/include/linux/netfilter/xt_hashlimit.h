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
#ifndef _UAPI_XT_HASHLIMIT_H
#define _UAPI_XT_HASHLIMIT_H
#include <linux/types.h>
#define XT_HASHLIMIT_SCALE 10000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define XT_HASHLIMIT_BYTE_SHIFT 4
struct xt_hashlimit_htable;
enum {
 XT_HASHLIMIT_HASH_DIP = 1 << 0,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 XT_HASHLIMIT_HASH_DPT = 1 << 1,
 XT_HASHLIMIT_HASH_SIP = 1 << 2,
 XT_HASHLIMIT_HASH_SPT = 1 << 3,
 XT_HASHLIMIT_INVERT = 1 << 4,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 XT_HASHLIMIT_BYTES = 1 << 5,
};
struct hashlimit_cfg {
 __u32 mode;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 avg;
 __u32 burst;
 __u32 size;
 __u32 max;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 gc_interval;
 __u32 expire;
};
struct xt_hashlimit_info {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char name [IFNAMSIZ];
 struct hashlimit_cfg cfg;
 struct xt_hashlimit_htable *hinfo;
 union {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 void *ptr;
 struct xt_hashlimit_info *master;
 } u;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct hashlimit_cfg1 {
 __u32 mode;
 __u32 avg;
 __u32 burst;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 size;
 __u32 max;
 __u32 gc_interval;
 __u32 expire;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 srcmask, dstmask;
};
struct xt_hashlimit_mtinfo1 {
 char name[IFNAMSIZ];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct hashlimit_cfg1 cfg;
 struct xt_hashlimit_htable *hinfo __attribute__((aligned(8)));
};
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
