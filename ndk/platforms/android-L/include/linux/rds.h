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
#ifndef _LINUX_RDS_H
#define _LINUX_RDS_H
#include <linux/types.h>
#define RDS_IB_ABI_VERSION 0x301
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define RDS_CANCEL_SENT_TO 1
#define RDS_GET_MR 2
#define RDS_FREE_MR 3
#define RDS_RECVERR 5
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define RDS_CONG_MONITOR 6
#define RDS_GET_MR_FOR_DEST 7
#define RDS_CMSG_RDMA_ARGS 1
#define RDS_CMSG_RDMA_DEST 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define RDS_CMSG_RDMA_MAP 3
#define RDS_CMSG_RDMA_STATUS 4
#define RDS_CMSG_CONG_UPDATE 5
#define RDS_CMSG_ATOMIC_FADD 6
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define RDS_CMSG_ATOMIC_CSWP 7
#define RDS_CMSG_MASKED_ATOMIC_FADD 8
#define RDS_CMSG_MASKED_ATOMIC_CSWP 9
#define RDS_INFO_FIRST 10000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define RDS_INFO_COUNTERS 10000
#define RDS_INFO_CONNECTIONS 10001
#define RDS_INFO_SEND_MESSAGES 10003
#define RDS_INFO_RETRANS_MESSAGES 10004
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define RDS_INFO_RECV_MESSAGES 10005
#define RDS_INFO_SOCKETS 10006
#define RDS_INFO_TCP_SOCKETS 10007
#define RDS_INFO_IB_CONNECTIONS 10008
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define RDS_INFO_CONNECTION_STATS 10009
#define RDS_INFO_IWARP_CONNECTIONS 10010
#define RDS_INFO_LAST 10010
struct rds_info_counter {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint8_t name[32];
 uint64_t value;
} __attribute__((packed));
#define RDS_INFO_CONNECTION_FLAG_SENDING 0x01
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define RDS_INFO_CONNECTION_FLAG_CONNECTING 0x02
#define RDS_INFO_CONNECTION_FLAG_CONNECTED 0x04
#define TRANSNAMSIZ 16
struct rds_info_connection {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint64_t next_tx_seq;
 uint64_t next_rx_seq;
 __be32 laddr;
 __be32 faddr;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint8_t transport[TRANSNAMSIZ];
 uint8_t flags;
} __attribute__((packed));
#define RDS_INFO_MESSAGE_FLAG_ACK 0x01
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define RDS_INFO_MESSAGE_FLAG_FAST_ACK 0x02
struct rds_info_message {
 uint64_t seq;
 uint32_t len;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __be32 laddr;
 __be32 faddr;
 __be16 lport;
 __be16 fport;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint8_t flags;
} __attribute__((packed));
struct rds_info_socket {
 uint32_t sndbuf;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __be32 bound_addr;
 __be32 connected_addr;
 __be16 bound_port;
 __be16 connected_port;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint32_t rcvbuf;
 uint64_t inum;
} __attribute__((packed));
struct rds_info_tcp_socket {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __be32 local_addr;
 __be16 local_port;
 __be32 peer_addr;
 __be16 peer_port;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint64_t hdr_rem;
 uint64_t data_rem;
 uint32_t last_sent_nxt;
 uint32_t last_expected_una;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint32_t last_seen_una;
} __attribute__((packed));
#define RDS_IB_GID_LEN 16
struct rds_info_rdma_connection {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __be32 src_addr;
 __be32 dst_addr;
 uint8_t src_gid[RDS_IB_GID_LEN];
 uint8_t dst_gid[RDS_IB_GID_LEN];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint32_t max_send_wr;
 uint32_t max_recv_wr;
 uint32_t max_send_sge;
 uint32_t rdma_mr_max;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint32_t rdma_mr_size;
};
#define RDS_CONG_MONITOR_SIZE 64
#define RDS_CONG_MONITOR_BIT(port) (((unsigned int) port) % RDS_CONG_MONITOR_SIZE)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define RDS_CONG_MONITOR_MASK(port) (1ULL << RDS_CONG_MONITOR_BIT(port))
typedef uint64_t rds_rdma_cookie_t;
struct rds_iovec {
 uint64_t addr;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint64_t bytes;
};
struct rds_get_mr_args {
 struct rds_iovec vec;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint64_t cookie_addr;
 uint64_t flags;
};
struct rds_get_mr_for_dest_args {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct sockaddr_storage dest_addr;
 struct rds_iovec vec;
 uint64_t cookie_addr;
 uint64_t flags;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct rds_free_mr_args {
 rds_rdma_cookie_t cookie;
 uint64_t flags;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct rds_rdma_args {
 rds_rdma_cookie_t cookie;
 struct rds_iovec remote_vec;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint64_t local_vec_addr;
 uint64_t nr_local;
 uint64_t flags;
 uint64_t user_token;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct rds_atomic_args {
 rds_rdma_cookie_t cookie;
 uint64_t local_addr;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint64_t remote_addr;
 union {
 struct {
 uint64_t compare;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint64_t swap;
 } cswp;
 struct {
 uint64_t add;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 } fadd;
 struct {
 uint64_t compare;
 uint64_t swap;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint64_t compare_mask;
 uint64_t swap_mask;
 } m_cswp;
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint64_t add;
 uint64_t nocarry_mask;
 } m_fadd;
 };
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint64_t flags;
 uint64_t user_token;
};
struct rds_rdma_notify {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint64_t user_token;
 int32_t status;
};
#define RDS_RDMA_SUCCESS 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define RDS_RDMA_REMOTE_ERROR 1
#define RDS_RDMA_CANCELED 2
#define RDS_RDMA_DROPPED 3
#define RDS_RDMA_OTHER_ERROR 4
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define RDS_RDMA_READWRITE 0x0001
#define RDS_RDMA_FENCE 0x0002
#define RDS_RDMA_INVALIDATE 0x0004
#define RDS_RDMA_USE_ONCE 0x0008
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define RDS_RDMA_DONTWAIT 0x0010
#define RDS_RDMA_NOTIFY_ME 0x0020
#define RDS_RDMA_SILENT 0x0040
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
