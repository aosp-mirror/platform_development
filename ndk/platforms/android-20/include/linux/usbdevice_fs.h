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
#ifndef _UAPI_LINUX_USBDEVICE_FS_H
#define _UAPI_LINUX_USBDEVICE_FS_H
#include <linux/types.h>
#include <linux/magic.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct usbdevfs_ctrltransfer {
 __u8 bRequestType;
 __u8 bRequest;
 __u16 wValue;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 wIndex;
 __u16 wLength;
 __u32 timeout;
 void __user *data;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct usbdevfs_bulktransfer {
 unsigned int ep;
 unsigned int len;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int timeout;
 void __user *data;
};
struct usbdevfs_setinterface {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int interface;
 unsigned int altsetting;
};
struct usbdevfs_disconnectsignal {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int signr;
 void __user *context;
};
#define USBDEVFS_MAXDRIVERNAME 255
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct usbdevfs_getdriver {
 unsigned int interface;
 char driver[USBDEVFS_MAXDRIVERNAME + 1];
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct usbdevfs_connectinfo {
 unsigned int devnum;
 unsigned char slow;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBDEVFS_URB_SHORT_NOT_OK 0x01
#define USBDEVFS_URB_ISO_ASAP 0x02
#define USBDEVFS_URB_BULK_CONTINUATION 0x04
#define USBDEVFS_URB_NO_FSBR 0x20
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBDEVFS_URB_ZERO_PACKET 0x40
#define USBDEVFS_URB_NO_INTERRUPT 0x80
#define USBDEVFS_URB_TYPE_ISO 0
#define USBDEVFS_URB_TYPE_INTERRUPT 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBDEVFS_URB_TYPE_CONTROL 2
#define USBDEVFS_URB_TYPE_BULK 3
struct usbdevfs_iso_packet_desc {
 unsigned int length;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int actual_length;
 unsigned int status;
};
struct usbdevfs_urb {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char type;
 unsigned char endpoint;
 int status;
 unsigned int flags;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 void __user *buffer;
 int buffer_length;
 int actual_length;
 int start_frame;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int number_of_packets;
 int error_count;
 unsigned int signr;
 void __user *usercontext;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct usbdevfs_iso_packet_desc iso_frame_desc[0];
};
struct usbdevfs_ioctl {
 int ifno;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int ioctl_code;
 void __user *data;
};
struct usbdevfs_hub_portinfo {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char nports;
 char port [127];
};
#define USBDEVFS_CAP_ZERO_PACKET 0x01
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBDEVFS_CAP_BULK_CONTINUATION 0x02
#define USBDEVFS_CAP_NO_PACKET_SIZE_LIM 0x04
#define USBDEVFS_CAP_BULK_SCATTER_GATHER 0x08
#define USBDEVFS_DISCONNECT_CLAIM_IF_DRIVER 0x01
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBDEVFS_DISCONNECT_CLAIM_EXCEPT_DRIVER 0x02
struct usbdevfs_disconnect_claim {
 unsigned int interface;
 unsigned int flags;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char driver[USBDEVFS_MAXDRIVERNAME + 1];
};
#define USBDEVFS_CONTROL _IOWR('U', 0, struct usbdevfs_ctrltransfer)
#define USBDEVFS_CONTROL32 _IOWR('U', 0, struct usbdevfs_ctrltransfer32)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBDEVFS_BULK _IOWR('U', 2, struct usbdevfs_bulktransfer)
#define USBDEVFS_BULK32 _IOWR('U', 2, struct usbdevfs_bulktransfer32)
#define USBDEVFS_RESETEP _IOR('U', 3, unsigned int)
#define USBDEVFS_SETINTERFACE _IOR('U', 4, struct usbdevfs_setinterface)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBDEVFS_SETCONFIGURATION _IOR('U', 5, unsigned int)
#define USBDEVFS_GETDRIVER _IOW('U', 8, struct usbdevfs_getdriver)
#define USBDEVFS_SUBMITURB _IOR('U', 10, struct usbdevfs_urb)
#define USBDEVFS_SUBMITURB32 _IOR('U', 10, struct usbdevfs_urb32)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBDEVFS_DISCARDURB _IO('U', 11)
#define USBDEVFS_REAPURB _IOW('U', 12, void *)
#define USBDEVFS_REAPURB32 _IOW('U', 12, __u32)
#define USBDEVFS_REAPURBNDELAY _IOW('U', 13, void *)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBDEVFS_REAPURBNDELAY32 _IOW('U', 13, __u32)
#define USBDEVFS_DISCSIGNAL _IOR('U', 14, struct usbdevfs_disconnectsignal)
#define USBDEVFS_DISCSIGNAL32 _IOR('U', 14, struct usbdevfs_disconnectsignal32)
#define USBDEVFS_CLAIMINTERFACE _IOR('U', 15, unsigned int)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBDEVFS_RELEASEINTERFACE _IOR('U', 16, unsigned int)
#define USBDEVFS_CONNECTINFO _IOW('U', 17, struct usbdevfs_connectinfo)
#define USBDEVFS_IOCTL _IOWR('U', 18, struct usbdevfs_ioctl)
#define USBDEVFS_IOCTL32 _IOWR('U', 18, struct usbdevfs_ioctl32)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBDEVFS_HUB_PORTINFO _IOR('U', 19, struct usbdevfs_hub_portinfo)
#define USBDEVFS_RESET _IO('U', 20)
#define USBDEVFS_CLEAR_HALT _IOR('U', 21, unsigned int)
#define USBDEVFS_DISCONNECT _IO('U', 22)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBDEVFS_CONNECT _IO('U', 23)
#define USBDEVFS_CLAIM_PORT _IOR('U', 24, unsigned int)
#define USBDEVFS_RELEASE_PORT _IOR('U', 25, unsigned int)
#define USBDEVFS_GET_CAPABILITIES _IOR('U', 26, __u32)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBDEVFS_DISCONNECT_CLAIM _IOR('U', 27, struct usbdevfs_disconnect_claim)
#endif
