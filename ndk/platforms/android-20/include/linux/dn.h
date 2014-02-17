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
#ifndef _LINUX_DN_H
#define _LINUX_DN_H
#include <linux/types.h>
#define DNPROTO_NSP 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DNPROTO_ROU 3
#define DNPROTO_NML 4
#define DNPROTO_EVL 5
#define DNPROTO_EVR 6
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DNPROTO_NSPT 7
#define DN_ADDL 2
#define DN_MAXADDL 2
#define DN_MAXOPTL 16
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DN_MAXOBJL 16
#define DN_MAXACCL 40
#define DN_MAXALIASL 128
#define DN_MAXNODEL 256
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DNBUFSIZE 65023
#define SO_CONDATA 1
#define SO_CONACCESS 2
#define SO_PROXYUSR 3
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SO_LINKINFO 7
#define DSO_CONDATA 1
#define DSO_DISDATA 10
#define DSO_CONACCESS 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DSO_ACCEPTMODE 4
#define DSO_CONACCEPT 5
#define DSO_CONREJECT 6
#define DSO_LINKINFO 7
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DSO_STREAM 8
#define DSO_SEQPACKET 9
#define DSO_MAXWINDOW 11
#define DSO_NODELAY 12
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DSO_CORK 13
#define DSO_SERVICES 14
#define DSO_INFO 15
#define DSO_MAX 15
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LL_INACTIVE 0
#define LL_CONNECTING 1
#define LL_RUNNING 2
#define LL_DISCONNECTING 3
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define ACC_IMMED 0
#define ACC_DEFER 1
#define SDF_WILD 1
#define SDF_PROXY 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SDF_UICPROXY 4
struct dn_naddr {
 __le16 a_len;
 __u8 a_addr[DN_MAXADDL];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct sockaddr_dn {
 __u16 sdn_family;
 __u8 sdn_flags;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 sdn_objnum;
 __le16 sdn_objnamel;
 __u8 sdn_objname[DN_MAXOBJL];
 struct dn_naddr sdn_add;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define sdn_nodeaddrl sdn_add.a_len
#define sdn_nodeaddr sdn_add.a_addr
struct optdata_dn {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le16 opt_status;
#define opt_sts opt_status
 __le16 opt_optl;
 __u8 opt_data[16];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct accessdata_dn {
 __u8 acc_accl;
 __u8 acc_acc[DN_MAXACCL];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 acc_passl;
 __u8 acc_pass[DN_MAXACCL];
 __u8 acc_userl;
 __u8 acc_user[DN_MAXACCL];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct linkinfo_dn {
 __u16 idn_segsize;
 __u8 idn_linkstate;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
union etheraddress {
 __u8 dne_addr[6];
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 dne_hiord[4];
 __u8 dne_nodeaddr[2];
 } dne_remote;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct dn_addr {
 __le16 dna_family;
 union etheraddress dna_netaddr;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DECNET_IOCTL_BASE 0x89
#define SIOCSNETADDR _IOW(DECNET_IOCTL_BASE, 0xe0, struct dn_naddr)
#define SIOCGNETADDR _IOR(DECNET_IOCTL_BASE, 0xe1, struct dn_naddr)
#define OSIOCSNETADDR _IOW(DECNET_IOCTL_BASE, 0xe0, int)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define OSIOCGNETADDR _IOR(DECNET_IOCTL_BASE, 0xe1, int)
#endif
