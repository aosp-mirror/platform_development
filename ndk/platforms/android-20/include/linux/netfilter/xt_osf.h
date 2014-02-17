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
#ifndef _XT_OSF_H
#define _XT_OSF_H
#include <linux/types.h>
#define MAXGENRELEN 32
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define XT_OSF_GENRE (1<<0)
#define XT_OSF_TTL (1<<1)
#define XT_OSF_LOG (1<<2)
#define XT_OSF_INVERT (1<<3)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define XT_OSF_LOGLEVEL_ALL 0
#define XT_OSF_LOGLEVEL_FIRST 1
#define XT_OSF_LOGLEVEL_ALL_KNOWN 2
#define XT_OSF_TTL_TRUE 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define XT_OSF_TTL_LESS 1
#define XT_OSF_TTL_NOCHECK 2
struct xt_osf_info {
 char genre[MAXGENRELEN];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 len;
 __u32 flags;
 __u32 loglevel;
 __u32 ttl;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct xt_osf_wc {
 __u32 wc;
 __u32 val;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct xt_osf_opt {
 __u16 kind, length;
 struct xt_osf_wc wc;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct xt_osf_user_finger {
 struct xt_osf_wc wss;
 __u8 ttl, df;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 ss, mss;
 __u16 opt_num;
 char genre[MAXGENRELEN];
 char version[MAXGENRELEN];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char subtype[MAXGENRELEN];
 struct xt_osf_opt opt[MAX_IPOPTLEN];
};
struct xt_osf_nlmsg {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct xt_osf_user_finger f;
 struct iphdr ip;
 struct tcphdr tcp;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum iana_options {
 OSFOPT_EOL = 0,
 OSFOPT_NOP,
 OSFOPT_MSS,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 OSFOPT_WSO,
 OSFOPT_SACKP,
 OSFOPT_SACK,
 OSFOPT_ECHO,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 OSFOPT_ECHOREPLY,
 OSFOPT_TS,
 OSFOPT_POCP,
 OSFOPT_POSP,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 OSFOPT_EMPTY = 255,
};
enum xt_osf_window_size_options {
 OSF_WSS_PLAIN = 0,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 OSF_WSS_MSS,
 OSF_WSS_MTU,
 OSF_WSS_MODULO,
 OSF_WSS_MAX,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
enum xt_osf_msg_types {
 OSF_MSG_ADD,
 OSF_MSG_REMOVE,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 OSF_MSG_MAX,
};
enum xt_osf_attr_type {
 OSF_ATTR_UNSPEC,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 OSF_ATTR_FINGER,
 OSF_ATTR_MAX,
};
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
