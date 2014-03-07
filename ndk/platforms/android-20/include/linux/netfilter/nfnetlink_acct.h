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
#ifndef _UAPI_NFNL_ACCT_H_
#define _UAPI_NFNL_ACCT_H_
#ifndef NFACCT_NAME_MAX
#define NFACCT_NAME_MAX 32
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
enum nfnl_acct_msg_types {
 NFNL_MSG_ACCT_NEW,
 NFNL_MSG_ACCT_GET,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 NFNL_MSG_ACCT_GET_CTRZERO,
 NFNL_MSG_ACCT_DEL,
 NFNL_MSG_ACCT_MAX
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum nfnl_acct_type {
 NFACCT_UNSPEC,
 NFACCT_NAME,
 NFACCT_PKTS,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 NFACCT_BYTES,
 NFACCT_USE,
 __NFACCT_MAX
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NFACCT_MAX (__NFACCT_MAX - 1)
#endif
