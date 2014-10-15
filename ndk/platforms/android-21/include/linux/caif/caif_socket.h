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
#ifndef _LINUX_CAIF_SOCKET_H
#define _LINUX_CAIF_SOCKET_H
#include <linux/types.h>
#include <linux/socket.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum caif_link_selector {
 CAIF_LINK_HIGH_BANDW,
 CAIF_LINK_LOW_LATENCY
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum caif_channel_priority {
 CAIF_PRIO_MIN = 0x01,
 CAIF_PRIO_LOW = 0x04,
 CAIF_PRIO_NORMAL = 0x0f,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 CAIF_PRIO_HIGH = 0x14,
 CAIF_PRIO_MAX = 0x1F
};
enum caif_protocol_type {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 CAIFPROTO_AT,
 CAIFPROTO_DATAGRAM,
 CAIFPROTO_DATAGRAM_LOOP,
 CAIFPROTO_UTIL,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 CAIFPROTO_RFM,
 CAIFPROTO_DEBUG,
 _CAIFPROTO_MAX
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CAIFPROTO_MAX _CAIFPROTO_MAX
enum caif_at_type {
 CAIF_ATTYPE_PLAIN = 2
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum caif_debug_type {
 CAIF_DEBUG_TRACE_INTERACTIVE = 0,
 CAIF_DEBUG_TRACE,
 CAIF_DEBUG_INTERACTIVE,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
enum caif_debug_service {
 CAIF_RADIO_DEBUG_SERVICE = 1,
 CAIF_APP_DEBUG_SERVICE
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct sockaddr_caif {
 __kernel_sa_family_t family;
 union {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct {
 __u8 type;
 } at;
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char service[16];
 } util;
 union {
 __u32 connection_id;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 nsapi;
 } dgm;
 struct {
 __u32 connection_id;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char volume[16];
 } rfm;
 struct {
 __u8 type;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 service;
 } dbg;
 } u;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum caif_socket_opts {
 CAIFSO_LINK_SELECT = 127,
 CAIFSO_REQ_PARAM = 128,
 CAIFSO_RSP_PARAM = 129,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#endif
