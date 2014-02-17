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
#ifndef _IP6T_REJECT_H
#define _IP6T_REJECT_H
#include <linux/types.h>
enum ip6t_reject_with {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 IP6T_ICMP6_NO_ROUTE,
 IP6T_ICMP6_ADM_PROHIBITED,
 IP6T_ICMP6_NOT_NEIGHBOUR,
 IP6T_ICMP6_ADDR_UNREACH,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 IP6T_ICMP6_PORT_UNREACH,
 IP6T_ICMP6_ECHOREPLY,
 IP6T_TCP_RESET
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct ip6t_reject_info {
 __u32 with;
};
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
