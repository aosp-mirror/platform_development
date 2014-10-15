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
#ifndef __PACKET_DIAG_H__
#define __PACKET_DIAG_H__
#include <linux/types.h>
struct packet_diag_req {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 sdiag_family;
 __u8 sdiag_protocol;
 __u16 pad;
 __u32 pdiag_ino;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 pdiag_show;
 __u32 pdiag_cookie[2];
};
#define PACKET_SHOW_INFO 0x00000001
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PACKET_SHOW_MCLIST 0x00000002
#define PACKET_SHOW_RING_CFG 0x00000004
#define PACKET_SHOW_FANOUT 0x00000008
#define PACKET_SHOW_MEMINFO 0x00000010
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PACKET_SHOW_FILTER 0x00000020
struct packet_diag_msg {
 __u8 pdiag_family;
 __u8 pdiag_type;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 pdiag_num;
 __u32 pdiag_ino;
 __u32 pdiag_cookie[2];
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum {
 PACKET_DIAG_INFO,
 PACKET_DIAG_MCLIST,
 PACKET_DIAG_RX_RING,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 PACKET_DIAG_TX_RING,
 PACKET_DIAG_FANOUT,
 PACKET_DIAG_UID,
 PACKET_DIAG_MEMINFO,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 PACKET_DIAG_FILTER,
 __PACKET_DIAG_MAX,
};
#define PACKET_DIAG_MAX (__PACKET_DIAG_MAX - 1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct packet_diag_info {
 __u32 pdi_index;
 __u32 pdi_version;
 __u32 pdi_reserve;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 pdi_copy_thresh;
 __u32 pdi_tstamp;
 __u32 pdi_flags;
#define PDI_RUNNING 0x1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PDI_AUXDATA 0x2
#define PDI_ORIGDEV 0x4
#define PDI_VNETHDR 0x8
#define PDI_LOSS 0x10
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct packet_diag_mclist {
 __u32 pdmc_index;
 __u32 pdmc_count;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 pdmc_type;
 __u16 pdmc_alen;
 __u8 pdmc_addr[MAX_ADDR_LEN];
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct packet_diag_ring {
 __u32 pdr_block_size;
 __u32 pdr_block_nr;
 __u32 pdr_frame_size;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 pdr_frame_nr;
 __u32 pdr_retire_tmo;
 __u32 pdr_sizeof_priv;
 __u32 pdr_features;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#endif
