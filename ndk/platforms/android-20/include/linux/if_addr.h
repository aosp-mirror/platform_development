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
#ifndef __LINUX_IF_ADDR_H
#define __LINUX_IF_ADDR_H
#include <linux/types.h>
#include <linux/netlink.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct ifaddrmsg {
 __u8 ifa_family;
 __u8 ifa_prefixlen;
 __u8 ifa_flags;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 ifa_scope;
 __u32 ifa_index;
};
enum {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 IFA_UNSPEC,
 IFA_ADDRESS,
 IFA_LOCAL,
 IFA_LABEL,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 IFA_BROADCAST,
 IFA_ANYCAST,
 IFA_CACHEINFO,
 IFA_MULTICAST,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __IFA_MAX,
};
#define IFA_MAX (__IFA_MAX - 1)
#define IFA_F_SECONDARY 0x01
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IFA_F_TEMPORARY IFA_F_SECONDARY
#define IFA_F_NODAD 0x02
#define IFA_F_OPTIMISTIC 0x04
#define IFA_F_DADFAILED 0x08
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IFA_F_HOMEADDRESS 0x10
#define IFA_F_DEPRECATED 0x20
#define IFA_F_TENTATIVE 0x40
#define IFA_F_PERMANENT 0x80
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct ifa_cacheinfo {
 __u32 ifa_prefered;
 __u32 ifa_valid;
 __u32 cstamp;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 tstamp;
};
#define IFA_RTA(r) ((struct rtattr*)(((char*)(r)) + NLMSG_ALIGN(sizeof(struct ifaddrmsg))))
#define IFA_PAYLOAD(n) NLMSG_PAYLOAD(n,sizeof(struct ifaddrmsg))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
