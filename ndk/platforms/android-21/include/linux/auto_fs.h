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
#ifndef _UAPI_LINUX_AUTO_FS_H
#define _UAPI_LINUX_AUTO_FS_H
#include <linux/types.h>
#include <sys/ioctl.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define AUTOFS_PROTO_VERSION 3
#define AUTOFS_MAX_PROTO_VERSION AUTOFS_PROTO_VERSION
#define AUTOFS_MIN_PROTO_VERSION AUTOFS_PROTO_VERSION
#if defined(__ia64__) || defined(__alpha__)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef unsigned long autofs_wqt_t;
#else
typedef unsigned int autofs_wqt_t;
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define autofs_ptype_missing 0
#define autofs_ptype_expire 1
struct autofs_packet_hdr {
 int proto_version;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int type;
};
struct autofs_packet_missing {
 struct autofs_packet_hdr hdr;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 autofs_wqt_t wait_queue_token;
 int len;
 char name[NAME_MAX+1];
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct autofs_packet_expire {
 struct autofs_packet_hdr hdr;
 int len;
 char name[NAME_MAX+1];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define AUTOFS_IOC_READY _IO(0x93,0x60)
#define AUTOFS_IOC_FAIL _IO(0x93,0x61)
#define AUTOFS_IOC_CATATONIC _IO(0x93,0x62)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define AUTOFS_IOC_PROTOVER _IOR(0x93,0x63,int)
#define AUTOFS_IOC_SETTIMEOUT32 _IOWR(0x93,0x64,compat_ulong_t)
#define AUTOFS_IOC_SETTIMEOUT _IOWR(0x93,0x64,unsigned long)
#define AUTOFS_IOC_EXPIRE _IOR(0x93,0x65,struct autofs_packet_expire)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
