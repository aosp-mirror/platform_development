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
#ifndef _ASM_SOCKET_H
#define _ASM_SOCKET_H
#include <asm/sockios.h>
#define SOL_SOCKET 0xffff
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SO_DEBUG 0x0001  
#define SO_REUSEADDR 0x0004  
#define SO_KEEPALIVE 0x0008  
#define SO_DONTROUTE 0x0010  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SO_BROADCAST 0x0020  
#define SO_LINGER 0x0080  
#define SO_OOBINLINE 0x0100  
#define SO_TYPE 0x1008  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SO_STYLE SO_TYPE  
#define SO_ERROR 0x1007  
#define SO_SNDBUF 0x1001  
#define SO_RCVBUF 0x1002  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SO_SNDLOWAT 0x1003  
#define SO_RCVLOWAT 0x1004  
#define SO_SNDTIMEO 0x1005  
#define SO_RCVTIMEO 0x1006  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SO_ACCEPTCONN 0x1009
#define SO_PROTOCOL 0x1028  
#define SO_DOMAIN 0x1029  
#define SO_NO_CHECK 11
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SO_PRIORITY 12
#define SO_BSDCOMPAT 14
#define SO_PASSCRED 17
#define SO_PEERCRED 18
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SO_SECURITY_AUTHENTICATION 22
#define SO_SECURITY_ENCRYPTION_TRANSPORT 23
#define SO_SECURITY_ENCRYPTION_NETWORK 24
#define SO_BINDTODEVICE 25
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SO_ATTACH_FILTER 26
#define SO_DETACH_FILTER 27
#define SO_PEERNAME 28
#define SO_TIMESTAMP 29
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SCM_TIMESTAMP SO_TIMESTAMP
#define SO_PEERSEC 30
#define SO_SNDBUFFORCE 31
#define SO_RCVBUFFORCE 33
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SO_PASSSEC 34
#define SO_TIMESTAMPNS 35
#define SCM_TIMESTAMPNS SO_TIMESTAMPNS
#define SO_MARK 36
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SO_TIMESTAMPING 37
#define SCM_TIMESTAMPING SO_TIMESTAMPING
#define SO_RXQ_OVFL 40
#define SO_WIFI_STATUS 41
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SCM_WIFI_STATUS SO_WIFI_STATUS
#endif
