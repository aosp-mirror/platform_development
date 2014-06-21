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
#ifndef _NF_CONNTRACK_TUPLE_COMMON_H
#define _NF_CONNTRACK_TUPLE_COMMON_H
enum ip_conntrack_dir {
 IP_CT_DIR_ORIGINAL,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 IP_CT_DIR_REPLY,
 IP_CT_DIR_MAX
};
union nf_conntrack_man_proto {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __be16 all;
 struct {
 __be16 port;
 } tcp;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct {
 __be16 port;
 } udp;
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __be16 id;
 } icmp;
 struct {
 __be16 port;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 } dccp;
 struct {
 __be16 port;
 } sctp;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct {
 __be16 key;
 } gre;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CTINFO2DIR(ctinfo) ((ctinfo) >= IP_CT_IS_REPLY ? IP_CT_DIR_REPLY : IP_CT_DIR_ORIGINAL)
#endif
