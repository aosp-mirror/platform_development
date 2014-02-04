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
#ifndef _IPX_H_
#define _IPX_H_
#include <linux/types.h>
#include <linux/sockios.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <linux/socket.h>
#define IPX_NODE_LEN 6
#define IPX_MTU 576
struct sockaddr_ipx {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __kernel_sa_family_t sipx_family;
 __be16 sipx_port;
 __be32 sipx_network;
 unsigned char sipx_node[IPX_NODE_LEN];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 sipx_type;
 unsigned char sipx_zero;
};
#define sipx_special sipx_port
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define sipx_action sipx_zero
#define IPX_DLTITF 0
#define IPX_CRTITF 1
struct ipx_route_definition {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __be32 ipx_network;
 __be32 ipx_router_network;
 unsigned char ipx_router_node[IPX_NODE_LEN];
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct ipx_interface_definition {
 __be32 ipx_network;
 unsigned char ipx_device[16];
 unsigned char ipx_dlink_type;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IPX_FRAME_NONE 0
#define IPX_FRAME_SNAP 1
#define IPX_FRAME_8022 2
#define IPX_FRAME_ETHERII 3
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IPX_FRAME_8023 4
#define IPX_FRAME_TR_8022 5
 unsigned char ipx_special;
#define IPX_SPECIAL_NONE 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IPX_PRIMARY 1
#define IPX_INTERNAL 2
 unsigned char ipx_node[IPX_NODE_LEN];
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct ipx_config_data {
 unsigned char ipxcfg_auto_select_primary;
 unsigned char ipxcfg_auto_create_interfaces;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct ipx_route_def {
 __be32 ipx_network;
 __be32 ipx_router_network;
#define IPX_ROUTE_NO_ROUTER 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char ipx_router_node[IPX_NODE_LEN];
 unsigned char ipx_device[16];
 unsigned short ipx_flags;
#define IPX_RT_SNAP 8
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IPX_RT_8022 4
#define IPX_RT_BLUEBOOK 2
#define IPX_RT_ROUTED 1
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SIOCAIPXITFCRT (SIOCPROTOPRIVATE)
#define SIOCAIPXPRISLT (SIOCPROTOPRIVATE + 1)
#define SIOCIPXCFGDATA (SIOCPROTOPRIVATE + 2)
#define SIOCIPXNCPCONN (SIOCPROTOPRIVATE + 3)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
