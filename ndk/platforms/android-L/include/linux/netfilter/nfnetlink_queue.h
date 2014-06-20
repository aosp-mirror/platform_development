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
#ifndef _NFNETLINK_QUEUE_H
#define _NFNETLINK_QUEUE_H
#include <linux/types.h>
#include <linux/netfilter/nfnetlink.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum nfqnl_msg_types {
 NFQNL_MSG_PACKET,
 NFQNL_MSG_VERDICT,
 NFQNL_MSG_CONFIG,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 NFQNL_MSG_VERDICT_BATCH,
 NFQNL_MSG_MAX
};
struct nfqnl_msg_packet_hdr {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __be32 packet_id;
 __be16 hw_protocol;
 __u8 hook;
} __attribute__ ((packed));
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct nfqnl_msg_packet_hw {
 __be16 hw_addrlen;
 __u16 _pad;
 __u8 hw_addr[8];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct nfqnl_msg_packet_timestamp {
 __aligned_be64 sec;
 __aligned_be64 usec;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
enum nfqnl_attr_type {
 NFQA_UNSPEC,
 NFQA_PACKET_HDR,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 NFQA_VERDICT_HDR,
 NFQA_MARK,
 NFQA_TIMESTAMP,
 NFQA_IFINDEX_INDEV,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 NFQA_IFINDEX_OUTDEV,
 NFQA_IFINDEX_PHYSINDEV,
 NFQA_IFINDEX_PHYSOUTDEV,
 NFQA_HWADDR,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 NFQA_PAYLOAD,
 NFQA_CT,
 NFQA_CT_INFO,
 NFQA_CAP_LEN,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 NFQA_SKB_INFO,
 __NFQA_MAX
};
#define NFQA_MAX (__NFQA_MAX - 1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct nfqnl_msg_verdict_hdr {
 __be32 verdict;
 __be32 id;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum nfqnl_msg_config_cmds {
 NFQNL_CFG_CMD_NONE,
 NFQNL_CFG_CMD_BIND,
 NFQNL_CFG_CMD_UNBIND,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 NFQNL_CFG_CMD_PF_BIND,
 NFQNL_CFG_CMD_PF_UNBIND,
};
struct nfqnl_msg_config_cmd {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 command;
 __u8 _pad;
 __be16 pf;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum nfqnl_config_mode {
 NFQNL_COPY_NONE,
 NFQNL_COPY_META,
 NFQNL_COPY_PACKET,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct nfqnl_msg_config_params {
 __be32 copy_range;
 __u8 copy_mode;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} __attribute__ ((packed));
enum nfqnl_attr_config {
 NFQA_CFG_UNSPEC,
 NFQA_CFG_CMD,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 NFQA_CFG_PARAMS,
 NFQA_CFG_QUEUE_MAXLEN,
 NFQA_CFG_MASK,
 NFQA_CFG_FLAGS,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __NFQA_CFG_MAX
};
#define NFQA_CFG_MAX (__NFQA_CFG_MAX-1)
#define NFQA_CFG_F_FAIL_OPEN (1 << 0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NFQA_CFG_F_CONNTRACK (1 << 1)
#define NFQA_CFG_F_GSO (1 << 2)
#define NFQA_CFG_F_MAX (1 << 3)
#define NFQA_SKB_CSUMNOTREADY (1 << 0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NFQA_SKB_GSO (1 << 1)
#endif
