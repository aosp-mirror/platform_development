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
#ifndef _LINUX_ION_H
#define _LINUX_ION_H
#include <linux/types.h>
struct ion_handle;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum ion_heap_type {
 ION_HEAP_TYPE_SYSTEM,
 ION_HEAP_TYPE_SYSTEM_CONTIG,
 ION_HEAP_TYPE_CARVEOUT,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ION_HEAP_TYPE_CUSTOM,
 ION_NUM_HEAPS = 16,
};
#define ION_HEAP_SYSTEM_MASK (1 << ION_HEAP_TYPE_SYSTEM)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define ION_HEAP_SYSTEM_CONTIG_MASK (1 << ION_HEAP_TYPE_SYSTEM_CONTIG)
#define ION_HEAP_CARVEOUT_MASK (1 << ION_HEAP_TYPE_CARVEOUT)
#define ION_FLAG_CACHED 1
#define ION_FLAG_CACHED_NEEDS_SYNC 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct ion_allocation_data {
 size_t len;
 size_t align;
 unsigned int heap_mask;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int flags;
 struct ion_handle *handle;
};
struct ion_fd_data {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct ion_handle *handle;
 int fd;
};
struct ion_handle_data {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct ion_handle *handle;
};
struct ion_custom_data {
 unsigned int cmd;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long arg;
};
#define ION_IOC_MAGIC 'I'
#define ION_IOC_ALLOC _IOWR(ION_IOC_MAGIC, 0,   struct ion_allocation_data)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define ION_IOC_FREE _IOWR(ION_IOC_MAGIC, 1, struct ion_handle_data)
#define ION_IOC_MAP _IOWR(ION_IOC_MAGIC, 2, struct ion_fd_data)
#define ION_IOC_SHARE _IOWR(ION_IOC_MAGIC, 4, struct ion_fd_data)
#define ION_IOC_IMPORT _IOWR(ION_IOC_MAGIC, 5, struct ion_fd_data)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define ION_IOC_SYNC _IOWR(ION_IOC_MAGIC, 7, struct ion_fd_data)
#define ION_IOC_CUSTOM _IOWR(ION_IOC_MAGIC, 6, struct ion_custom_data)
#endif
