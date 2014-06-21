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
#ifndef _LINUX_VIRTIO_PCI_H
#define _LINUX_VIRTIO_PCI_H
#include <linux/virtio_config.h>
#define VIRTIO_PCI_HOST_FEATURES 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIRTIO_PCI_GUEST_FEATURES 4
#define VIRTIO_PCI_QUEUE_PFN 8
#define VIRTIO_PCI_QUEUE_NUM 12
#define VIRTIO_PCI_QUEUE_SEL 14
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIRTIO_PCI_QUEUE_NOTIFY 16
#define VIRTIO_PCI_STATUS 18
#define VIRTIO_PCI_ISR 19
#define VIRTIO_PCI_ISR_CONFIG 0x2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIRTIO_MSI_CONFIG_VECTOR 20
#define VIRTIO_MSI_QUEUE_VECTOR 22
#define VIRTIO_MSI_NO_VECTOR 0xffff
#define VIRTIO_PCI_CONFIG(dev) ((dev)->msix_enabled ? 24 : 20)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIRTIO_PCI_ABI_VERSION 0
#define VIRTIO_PCI_QUEUE_ADDR_SHIFT 12
#define VIRTIO_PCI_VRING_ALIGN 4096
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
