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
#ifndef _UAPI__LINUX_FUNCTIONFS_H__
#define _UAPI__LINUX_FUNCTIONFS_H__
#include <linux/types.h>
#include <linux/ioctl.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <linux/usb/ch9.h>
enum {
 FUNCTIONFS_DESCRIPTORS_MAGIC = 1,
 FUNCTIONFS_STRINGS_MAGIC = 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct usb_endpoint_descriptor_no_audio {
 __u8 bLength;
 __u8 bDescriptorType;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 bEndpointAddress;
 __u8 bmAttributes;
 __le16 wMaxPacketSize;
 __u8 bInterval;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} __attribute__((packed));
struct usb_functionfs_descs_head {
 __le32 magic;
 __le32 length;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le32 fs_count;
 __le32 hs_count;
} __attribute__((packed));
struct usb_functionfs_strings_head {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le32 magic;
 __le32 length;
 __le32 str_count;
 __le32 lang_count;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} __attribute__((packed));
enum usb_functionfs_event_type {
 FUNCTIONFS_BIND,
 FUNCTIONFS_UNBIND,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 FUNCTIONFS_ENABLE,
 FUNCTIONFS_DISABLE,
 FUNCTIONFS_SETUP,
 FUNCTIONFS_SUSPEND,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 FUNCTIONFS_RESUME
};
struct usb_functionfs_event {
 union {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct usb_ctrlrequest setup;
 } __attribute__((packed)) u;
 __u8 type;
 __u8 _pad[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} __attribute__((packed));
#define FUNCTIONFS_FIFO_STATUS _IO('g', 1)
#define FUNCTIONFS_FIFO_FLUSH _IO('g', 2)
#define FUNCTIONFS_CLEAR_HALT _IO('g', 3)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FUNCTIONFS_INTERFACE_REVMAP _IO('g', 128)
#define FUNCTIONFS_ENDPOINT_REVMAP _IO('g', 129)
#endif
