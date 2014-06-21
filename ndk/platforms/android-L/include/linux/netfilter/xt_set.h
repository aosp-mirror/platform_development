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
#ifndef _XT_SET_H
#define _XT_SET_H
#include <linux/types.h>
#include <linux/netfilter/ipset/ip_set.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IPSET_SRC 0x01
#define IPSET_DST 0x02
#define IPSET_MATCH_INV 0x04
struct xt_set_info_v0 {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ip_set_id_t index;
 union {
 __u32 flags[IPSET_DIM_MAX + 1];
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 __flags[IPSET_DIM_MAX];
 __u8 dim;
 __u8 flags;
 } compat;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 } u;
};
struct xt_set_info_match_v0 {
 struct xt_set_info_v0 match_set;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct xt_set_info_target_v0 {
 struct xt_set_info_v0 add_set;
 struct xt_set_info_v0 del_set;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct xt_set_info {
 ip_set_id_t index;
 __u8 dim;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 flags;
};
struct xt_set_info_match_v1 {
 struct xt_set_info match_set;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct xt_set_info_target_v1 {
 struct xt_set_info add_set;
 struct xt_set_info del_set;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct xt_set_info_target_v2 {
 struct xt_set_info add_set;
 struct xt_set_info del_set;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 flags;
 __u32 timeout;
};
struct xt_set_info_match_v3 {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct xt_set_info match_set;
 struct ip_set_counter_match packets;
 struct ip_set_counter_match bytes;
 __u32 flags;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#endif
