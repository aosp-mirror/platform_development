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
#ifndef _LINUX_VIRTIO_NET_H
#define _LINUX_VIRTIO_NET_H
#include <linux/types.h>
#include <linux/virtio_ids.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <linux/virtio_config.h>
#include <linux/if_ether.h>
#define VIRTIO_NET_F_CSUM 0
#define VIRTIO_NET_F_GUEST_CSUM 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIRTIO_NET_F_MAC 5
#define VIRTIO_NET_F_GSO 6
#define VIRTIO_NET_F_GUEST_TSO4 7
#define VIRTIO_NET_F_GUEST_TSO6 8
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIRTIO_NET_F_GUEST_ECN 9
#define VIRTIO_NET_F_GUEST_UFO 10
#define VIRTIO_NET_F_HOST_TSO4 11
#define VIRTIO_NET_F_HOST_TSO6 12
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIRTIO_NET_F_HOST_ECN 13
#define VIRTIO_NET_F_HOST_UFO 14
#define VIRTIO_NET_F_MRG_RXBUF 15
#define VIRTIO_NET_F_STATUS 16
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIRTIO_NET_F_CTRL_VQ 17
#define VIRTIO_NET_F_CTRL_RX 18
#define VIRTIO_NET_F_CTRL_VLAN 19
#define VIRTIO_NET_F_CTRL_RX_EXTRA 20
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIRTIO_NET_F_GUEST_ANNOUNCE 21
#define VIRTIO_NET_F_MQ 22
#define VIRTIO_NET_F_CTRL_MAC_ADDR 23
#define VIRTIO_NET_S_LINK_UP 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIRTIO_NET_S_ANNOUNCE 2
struct virtio_net_config {
 __u8 mac[6];
 __u16 status;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 max_virtqueue_pairs;
} __attribute__((packed));
struct virtio_net_hdr {
#define VIRTIO_NET_HDR_F_NEEDS_CSUM 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIRTIO_NET_HDR_F_DATA_VALID 2
 __u8 flags;
#define VIRTIO_NET_HDR_GSO_NONE 0
#define VIRTIO_NET_HDR_GSO_TCPV4 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIRTIO_NET_HDR_GSO_UDP 3
#define VIRTIO_NET_HDR_GSO_TCPV6 4
#define VIRTIO_NET_HDR_GSO_ECN 0x80
 __u8 gso_type;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 hdr_len;
 __u16 gso_size;
 __u16 csum_start;
 __u16 csum_offset;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct virtio_net_hdr_mrg_rxbuf {
 struct virtio_net_hdr hdr;
 __u16 num_buffers;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct virtio_net_ctrl_hdr {
 __u8 class;
 __u8 cmd;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} __attribute__((packed));
typedef __u8 virtio_net_ctrl_ack;
#define VIRTIO_NET_OK 0
#define VIRTIO_NET_ERR 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIRTIO_NET_CTRL_RX 0
#define VIRTIO_NET_CTRL_RX_PROMISC 0
#define VIRTIO_NET_CTRL_RX_ALLMULTI 1
#define VIRTIO_NET_CTRL_RX_ALLUNI 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIRTIO_NET_CTRL_RX_NOMULTI 3
#define VIRTIO_NET_CTRL_RX_NOUNI 4
#define VIRTIO_NET_CTRL_RX_NOBCAST 5
struct virtio_net_ctrl_mac {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 entries;
 __u8 macs[][ETH_ALEN];
} __attribute__((packed));
#define VIRTIO_NET_CTRL_MAC 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIRTIO_NET_CTRL_MAC_TABLE_SET 0
#define VIRTIO_NET_CTRL_MAC_ADDR_SET 1
#define VIRTIO_NET_CTRL_VLAN 2
#define VIRTIO_NET_CTRL_VLAN_ADD 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIRTIO_NET_CTRL_VLAN_DEL 1
#define VIRTIO_NET_CTRL_ANNOUNCE 3
#define VIRTIO_NET_CTRL_ANNOUNCE_ACK 0
struct virtio_net_ctrl_mq {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 virtqueue_pairs;
};
#define VIRTIO_NET_CTRL_MQ 4
#define VIRTIO_NET_CTRL_MQ_VQ_PAIRS_SET 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIRTIO_NET_CTRL_MQ_VQ_PAIRS_MIN 1
#define VIRTIO_NET_CTRL_MQ_VQ_PAIRS_MAX 0x8000
#endif
