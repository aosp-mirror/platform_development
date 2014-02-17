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
#ifndef _UAPI_LINUX_NETCONF_H_
#define _UAPI_LINUX_NETCONF_H_
#include <linux/types.h>
#include <linux/netlink.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct netconfmsg {
 __u8 ncm_family;
};
enum {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 NETCONFA_UNSPEC,
 NETCONFA_IFINDEX,
 NETCONFA_FORWARDING,
 NETCONFA_RP_FILTER,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 NETCONFA_MC_FORWARDING,
 __NETCONFA_MAX
};
#define NETCONFA_MAX (__NETCONFA_MAX - 1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NETCONFA_IFINDEX_ALL -1
#define NETCONFA_IFINDEX_DEFAULT -2
#endif
