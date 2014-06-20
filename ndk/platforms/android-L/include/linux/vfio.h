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
#ifndef _UAPIVFIO_H
#define _UAPIVFIO_H
#include <linux/types.h>
#include <linux/ioctl.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VFIO_API_VERSION 0
#define VFIO_TYPE1_IOMMU 1
#define VFIO_TYPE (';')
#define VFIO_BASE 100
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VFIO_GET_API_VERSION _IO(VFIO_TYPE, VFIO_BASE + 0)
#define VFIO_CHECK_EXTENSION _IO(VFIO_TYPE, VFIO_BASE + 1)
#define VFIO_SET_IOMMU _IO(VFIO_TYPE, VFIO_BASE + 2)
struct vfio_group_status {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 argsz;
 __u32 flags;
#define VFIO_GROUP_FLAGS_VIABLE (1 << 0)
#define VFIO_GROUP_FLAGS_CONTAINER_SET (1 << 1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define VFIO_GROUP_GET_STATUS _IO(VFIO_TYPE, VFIO_BASE + 3)
#define VFIO_GROUP_SET_CONTAINER _IO(VFIO_TYPE, VFIO_BASE + 4)
#define VFIO_GROUP_UNSET_CONTAINER _IO(VFIO_TYPE, VFIO_BASE + 5)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VFIO_GROUP_GET_DEVICE_FD _IO(VFIO_TYPE, VFIO_BASE + 6)
struct vfio_device_info {
 __u32 argsz;
 __u32 flags;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VFIO_DEVICE_FLAGS_RESET (1 << 0)
#define VFIO_DEVICE_FLAGS_PCI (1 << 1)
 __u32 num_regions;
 __u32 num_irqs;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define VFIO_DEVICE_GET_INFO _IO(VFIO_TYPE, VFIO_BASE + 7)
struct vfio_region_info {
 __u32 argsz;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 flags;
#define VFIO_REGION_INFO_FLAG_READ (1 << 0)
#define VFIO_REGION_INFO_FLAG_WRITE (1 << 1)
#define VFIO_REGION_INFO_FLAG_MMAP (1 << 2)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 index;
 __u32 resv;
 __u64 size;
 __u64 offset;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define VFIO_DEVICE_GET_REGION_INFO _IO(VFIO_TYPE, VFIO_BASE + 8)
struct vfio_irq_info {
 __u32 argsz;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 flags;
#define VFIO_IRQ_INFO_EVENTFD (1 << 0)
#define VFIO_IRQ_INFO_MASKABLE (1 << 1)
#define VFIO_IRQ_INFO_AUTOMASKED (1 << 2)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VFIO_IRQ_INFO_NORESIZE (1 << 3)
 __u32 index;
 __u32 count;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VFIO_DEVICE_GET_IRQ_INFO _IO(VFIO_TYPE, VFIO_BASE + 9)
struct vfio_irq_set {
 __u32 argsz;
 __u32 flags;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VFIO_IRQ_SET_DATA_NONE (1 << 0)
#define VFIO_IRQ_SET_DATA_BOOL (1 << 1)
#define VFIO_IRQ_SET_DATA_EVENTFD (1 << 2)
#define VFIO_IRQ_SET_ACTION_MASK (1 << 3)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VFIO_IRQ_SET_ACTION_UNMASK (1 << 4)
#define VFIO_IRQ_SET_ACTION_TRIGGER (1 << 5)
 __u32 index;
 __u32 start;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 count;
 __u8 data[];
};
#define VFIO_DEVICE_SET_IRQS _IO(VFIO_TYPE, VFIO_BASE + 10)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VFIO_IRQ_SET_DATA_TYPE_MASK (VFIO_IRQ_SET_DATA_NONE |   VFIO_IRQ_SET_DATA_BOOL |   VFIO_IRQ_SET_DATA_EVENTFD)
#define VFIO_IRQ_SET_ACTION_TYPE_MASK (VFIO_IRQ_SET_ACTION_MASK |   VFIO_IRQ_SET_ACTION_UNMASK |   VFIO_IRQ_SET_ACTION_TRIGGER)
#define VFIO_DEVICE_RESET _IO(VFIO_TYPE, VFIO_BASE + 11)
enum {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 VFIO_PCI_BAR0_REGION_INDEX,
 VFIO_PCI_BAR1_REGION_INDEX,
 VFIO_PCI_BAR2_REGION_INDEX,
 VFIO_PCI_BAR3_REGION_INDEX,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 VFIO_PCI_BAR4_REGION_INDEX,
 VFIO_PCI_BAR5_REGION_INDEX,
 VFIO_PCI_ROM_REGION_INDEX,
 VFIO_PCI_CONFIG_REGION_INDEX,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 VFIO_PCI_VGA_REGION_INDEX,
 VFIO_PCI_NUM_REGIONS
};
enum {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 VFIO_PCI_INTX_IRQ_INDEX,
 VFIO_PCI_MSI_IRQ_INDEX,
 VFIO_PCI_MSIX_IRQ_INDEX,
 VFIO_PCI_ERR_IRQ_INDEX,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 VFIO_PCI_NUM_IRQS
};
struct vfio_iommu_type1_info {
 __u32 argsz;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 flags;
#define VFIO_IOMMU_INFO_PGSIZES (1 << 0)
 __u64 iova_pgsizes;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VFIO_IOMMU_GET_INFO _IO(VFIO_TYPE, VFIO_BASE + 12)
struct vfio_iommu_type1_dma_map {
 __u32 argsz;
 __u32 flags;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VFIO_DMA_MAP_FLAG_READ (1 << 0)
#define VFIO_DMA_MAP_FLAG_WRITE (1 << 1)
 __u64 vaddr;
 __u64 iova;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u64 size;
};
#define VFIO_IOMMU_MAP_DMA _IO(VFIO_TYPE, VFIO_BASE + 13)
struct vfio_iommu_type1_dma_unmap {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 argsz;
 __u32 flags;
 __u64 iova;
 __u64 size;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define VFIO_IOMMU_UNMAP_DMA _IO(VFIO_TYPE, VFIO_BASE + 14)
#endif
