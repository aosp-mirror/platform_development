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
#ifndef _UAPI__LINUX_MROUTE_H
#define _UAPI__LINUX_MROUTE_H
#include <linux/sockios.h>
#include <linux/types.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MRT_BASE 200
#define MRT_INIT (MRT_BASE)
#define MRT_DONE (MRT_BASE+1)
#define MRT_ADD_VIF (MRT_BASE+2)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MRT_DEL_VIF (MRT_BASE+3)
#define MRT_ADD_MFC (MRT_BASE+4)
#define MRT_DEL_MFC (MRT_BASE+5)
#define MRT_VERSION (MRT_BASE+6)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MRT_ASSERT (MRT_BASE+7)
#define MRT_PIM (MRT_BASE+8)
#define MRT_TABLE (MRT_BASE+9)
#define MRT_ADD_MFC_PROXY (MRT_BASE+10)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MRT_DEL_MFC_PROXY (MRT_BASE+11)
#define MRT_MAX (MRT_BASE+11)
#define SIOCGETVIFCNT SIOCPROTOPRIVATE
#define SIOCGETSGCNT (SIOCPROTOPRIVATE+1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SIOCGETRPF (SIOCPROTOPRIVATE+2)
#define MAXVIFS 32
typedef unsigned long vifbitmap_t;
typedef unsigned short vifi_t;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define ALL_VIFS ((vifi_t)(-1))
#define VIFM_SET(n,m) ((m)|=(1<<(n)))
#define VIFM_CLR(n,m) ((m)&=~(1<<(n)))
#define VIFM_ISSET(n,m) ((m)&(1<<(n)))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIFM_CLRALL(m) ((m)=0)
#define VIFM_COPY(mfrom,mto) ((mto)=(mfrom))
#define VIFM_SAME(m1,m2) ((m1)==(m2))
struct vifctl {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 vifi_t vifc_vifi;
 unsigned char vifc_flags;
 unsigned char vifc_threshold;
 unsigned int vifc_rate_limit;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 union {
 struct in_addr vifc_lcl_addr;
 int vifc_lcl_ifindex;
 };
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct in_addr vifc_rmt_addr;
};
#define VIFF_TUNNEL 0x1
#define VIFF_SRCRT 0x2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIFF_REGISTER 0x4
#define VIFF_USE_IFINDEX 0x8
struct mfcctl {
 struct in_addr mfcc_origin;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct in_addr mfcc_mcastgrp;
 vifi_t mfcc_parent;
 unsigned char mfcc_ttls[MAXVIFS];
 unsigned int mfcc_pkt_cnt;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int mfcc_byte_cnt;
 unsigned int mfcc_wrong_if;
 int mfcc_expire;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct sioc_sg_req {
 struct in_addr src;
 struct in_addr grp;
 unsigned long pktcnt;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long bytecnt;
 unsigned long wrong_if;
};
struct sioc_vif_req {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 vifi_t vifi;
 unsigned long icount;
 unsigned long ocount;
 unsigned long ibytes;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long obytes;
};
struct igmpmsg {
 __u32 unused1,unused2;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char im_msgtype;
 unsigned char im_mbz;
 unsigned char im_vif;
 unsigned char unused3;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct in_addr im_src,im_dst;
};
#define MFC_ASSERT_THRESH (3*HZ)
#define IGMPMSG_NOCACHE 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IGMPMSG_WRONGVIF 2
#define IGMPMSG_WHOLEPKT 3
#endif
