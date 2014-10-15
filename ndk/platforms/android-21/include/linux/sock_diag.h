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
#ifndef _UAPI__SOCK_DIAG_H__
#define _UAPI__SOCK_DIAG_H__
#include <linux/types.h>
#define SOCK_DIAG_BY_FAMILY 20
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct sock_diag_req {
 __u8 sdiag_family;
 __u8 sdiag_protocol;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum {
 SK_MEMINFO_RMEM_ALLOC,
 SK_MEMINFO_RCVBUF,
 SK_MEMINFO_WMEM_ALLOC,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 SK_MEMINFO_SNDBUF,
 SK_MEMINFO_FWD_ALLOC,
 SK_MEMINFO_WMEM_QUEUED,
 SK_MEMINFO_OPTMEM,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 SK_MEMINFO_BACKLOG,
 SK_MEMINFO_VARS,
};
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
