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
#ifndef _UAPI_NFNETLINK_H
#define _UAPI_NFNETLINK_H
#include <linux/types.h>
#include <linux/netfilter/nfnetlink_compat.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum nfnetlink_groups {
 NFNLGRP_NONE,
#define NFNLGRP_NONE NFNLGRP_NONE
 NFNLGRP_CONNTRACK_NEW,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NFNLGRP_CONNTRACK_NEW NFNLGRP_CONNTRACK_NEW
 NFNLGRP_CONNTRACK_UPDATE,
#define NFNLGRP_CONNTRACK_UPDATE NFNLGRP_CONNTRACK_UPDATE
 NFNLGRP_CONNTRACK_DESTROY,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NFNLGRP_CONNTRACK_DESTROY NFNLGRP_CONNTRACK_DESTROY
 NFNLGRP_CONNTRACK_EXP_NEW,
#define NFNLGRP_CONNTRACK_EXP_NEW NFNLGRP_CONNTRACK_EXP_NEW
 NFNLGRP_CONNTRACK_EXP_UPDATE,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NFNLGRP_CONNTRACK_EXP_UPDATE NFNLGRP_CONNTRACK_EXP_UPDATE
 NFNLGRP_CONNTRACK_EXP_DESTROY,
#define NFNLGRP_CONNTRACK_EXP_DESTROY NFNLGRP_CONNTRACK_EXP_DESTROY
 __NFNLGRP_MAX,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define NFNLGRP_MAX (__NFNLGRP_MAX - 1)
struct nfgenmsg {
 __u8 nfgen_family;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 version;
 __be16 res_id;
};
#define NFNETLINK_V0 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NFNL_SUBSYS_ID(x) ((x & 0xff00) >> 8)
#define NFNL_MSG_TYPE(x) (x & 0x00ff)
#define NFNL_SUBSYS_NONE 0
#define NFNL_SUBSYS_CTNETLINK 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NFNL_SUBSYS_CTNETLINK_EXP 2
#define NFNL_SUBSYS_QUEUE 3
#define NFNL_SUBSYS_ULOG 4
#define NFNL_SUBSYS_OSF 5
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NFNL_SUBSYS_IPSET 6
#define NFNL_SUBSYS_ACCT 7
#define NFNL_SUBSYS_CTNETLINK_TIMEOUT 8
#define NFNL_SUBSYS_CTHELPER 9
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NFNL_SUBSYS_COUNT 10
#endif
