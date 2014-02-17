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
#ifndef __LINUX_CAPI_H__
#define __LINUX_CAPI_H__
#include <linux/types.h>
#include <linux/ioctl.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <linux/kernelcapi.h>
typedef struct capi_register_params {
 __u32 level3cnt;
 __u32 datablkcnt;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 datablklen;
} capi_register_params;
#define CAPI_REGISTER _IOW('C',0x01,struct capi_register_params)
#define CAPI_MANUFACTURER_LEN 64
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CAPI_GET_MANUFACTURER _IOWR('C',0x06,int)
typedef struct capi_version {
 __u32 majorversion;
 __u32 minorversion;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 majormanuversion;
 __u32 minormanuversion;
} capi_version;
#define CAPI_GET_VERSION _IOWR('C',0x07,struct capi_version)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CAPI_SERIAL_LEN 8
#define CAPI_GET_SERIAL _IOWR('C',0x08,int)
typedef struct capi_profile {
 __u16 ncontroller;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 nbchannel;
 __u32 goptions;
 __u32 support1;
 __u32 support2;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 support3;
 __u32 reserved[6];
 __u32 manu[5];
} capi_profile;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CAPI_GET_PROFILE _IOWR('C',0x09,struct capi_profile)
typedef struct capi_manufacturer_cmd {
 unsigned long cmd;
 void __user *data;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} capi_manufacturer_cmd;
#define CAPI_MANUFACTURER_CMD _IOWR('C',0x20, struct capi_manufacturer_cmd)
#define CAPI_GET_ERRCODE _IOR('C',0x21, __u16)
#define CAPI_INSTALLED _IOR('C',0x22, __u16)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef union capi_ioctl_struct {
 __u32 contr;
 capi_register_params rparams;
 __u8 manufacturer[CAPI_MANUFACTURER_LEN];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 capi_version version;
 __u8 serial[CAPI_SERIAL_LEN];
 capi_profile profile;
 capi_manufacturer_cmd cmd;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 errcode;
} capi_ioctl_struct;
#define CAPIFLAG_HIGHJACKING 0x0001
#define CAPI_GET_FLAGS _IOR('C',0x23, unsigned)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CAPI_SET_FLAGS _IOR('C',0x24, unsigned)
#define CAPI_CLR_FLAGS _IOR('C',0x25, unsigned)
#define CAPI_NCCI_OPENCOUNT _IOR('C',0x26, unsigned)
#define CAPI_NCCI_GETUNIT _IOR('C',0x27, unsigned)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
