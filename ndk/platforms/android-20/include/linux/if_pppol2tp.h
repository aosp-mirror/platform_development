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
#ifndef _UAPI__LINUX_IF_PPPOL2TP_H
#define _UAPI__LINUX_IF_PPPOL2TP_H
#include <linux/types.h>
struct pppol2tp_addr {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __kernel_pid_t pid;
 int fd;
 struct sockaddr_in addr;
 __u16 s_tunnel, s_session;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 d_tunnel, d_session;
};
struct pppol2tpin6_addr {
 __kernel_pid_t pid;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int fd;
 __u16 s_tunnel, s_session;
 __u16 d_tunnel, d_session;
 struct sockaddr_in6 addr;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct pppol2tpv3_addr {
 __kernel_pid_t pid;
 int fd;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct sockaddr_in addr;
 __u32 s_tunnel, s_session;
 __u32 d_tunnel, d_session;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct pppol2tpv3in6_addr {
 __kernel_pid_t pid;
 int fd;
 __u32 s_tunnel, s_session;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 d_tunnel, d_session;
 struct sockaddr_in6 addr;
};
enum {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 PPPOL2TP_SO_DEBUG = 1,
 PPPOL2TP_SO_RECVSEQ = 2,
 PPPOL2TP_SO_SENDSEQ = 3,
 PPPOL2TP_SO_LNSMODE = 4,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 PPPOL2TP_SO_REORDERTO = 5,
};
enum {
 PPPOL2TP_MSG_DEBUG = (1 << 0),
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 PPPOL2TP_MSG_CONTROL = (1 << 1),
 PPPOL2TP_MSG_SEQ = (1 << 2),
 PPPOL2TP_MSG_DATA = (1 << 3),
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
