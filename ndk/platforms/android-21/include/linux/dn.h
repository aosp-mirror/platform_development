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
#include <linux/ioctl.h>
#include <linux/types.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <linux/if_ether.h>
#define DNPROTO_NSP 2
#define DNPROTO_ROU 3
#define DNPROTO_NML 4
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DNPROTO_EVL 5
#define DNPROTO_EVR 6
#define DNPROTO_NSPT 7
#define DN_ADDL 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DN_MAXADDL 2
#define DN_MAXOPTL 16
#define DN_MAXOBJL 16
#define DN_MAXACCL 40
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DN_MAXALIASL 128
#define DN_MAXNODEL 256
#define DNBUFSIZE 65023
#define SO_CONDATA 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SO_CONACCESS 2
#define SO_PROXYUSR 3
#define SO_LINKINFO 7
#define DSO_CONDATA 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DSO_DISDATA 10
#define DSO_CONACCESS 2
#define DSO_ACCEPTMODE 4
#define DSO_CONACCEPT 5
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DSO_CONREJECT 6
#define DSO_LINKINFO 7
#define DSO_STREAM 8
#define DSO_SEQPACKET 9
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DSO_MAXWINDOW 11
#define DSO_NODELAY 12
#define DSO_CORK 13
#define DSO_SERVICES 14
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DSO_INFO 15
#define DSO_MAX 15
#define LL_INACTIVE 0
#define LL_CONNECTING 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LL_RUNNING 2
#define LL_DISCONNECTING 3
#define ACC_IMMED 0
#define ACC_DEFER 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SDF_WILD 1
#define SDF_PROXY 2
#define SDF_UICPROXY 4
struct dn_naddr {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le16 a_len;
 __u8 a_addr[DN_MAXADDL];
};
struct sockaddr_dn {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 sdn_family;
 __u8 sdn_flags;
 __u8 sdn_objnum;
 __le16 sdn_objnamel;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 sdn_objname[DN_MAXOBJL];
 struct dn_naddr sdn_add;
};
#define sdn_nodeaddrl sdn_add.a_len
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define sdn_nodeaddr sdn_add.a_addr
struct optdata_dn {
 __le16 opt_status;
#define opt_sts opt_status
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le16 opt_optl;
 __u8 opt_data[16];
};
struct accessdata_dn {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 acc_accl;
 __u8 acc_acc[DN_MAXACCL];
 __u8 acc_passl;
 __u8 acc_pass[DN_MAXACCL];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 acc_userl;
 __u8 acc_user[DN_MAXACCL];
};
struct linkinfo_dn {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 idn_segsize;
 __u8 idn_linkstate;
};
union etheraddress {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 dne_addr[ETH_ALEN];
 struct {
 __u8 dne_hiord[4];
 __u8 dne_nodeaddr[2];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 } dne_remote;
};
struct dn_addr {
 __le16 dna_family;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 union etheraddress dna_netaddr;
};
#define DECNET_IOCTL_BASE 0x89
#define SIOCSNETADDR _IOW(DECNET_IOCTL_BASE, 0xe0, struct dn_naddr)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SIOCGNETADDR _IOR(DECNET_IOCTL_BASE, 0xe1, struct dn_naddr)
#define OSIOCSNETADDR _IOW(DECNET_IOCTL_BASE, 0xe0, int)
#define OSIOCGNETADDR _IOR(DECNET_IOCTL_BASE, 0xe1, int)
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
