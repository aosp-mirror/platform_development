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
#ifndef _UAPI_AGP_H
#define _UAPI_AGP_H
#define AGPIOC_BASE 'A'
#define AGPIOC_INFO _IOR (AGPIOC_BASE, 0, struct agp_info*)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define AGPIOC_ACQUIRE _IO (AGPIOC_BASE, 1)
#define AGPIOC_RELEASE _IO (AGPIOC_BASE, 2)
#define AGPIOC_SETUP _IOW (AGPIOC_BASE, 3, struct agp_setup*)
#define AGPIOC_RESERVE _IOW (AGPIOC_BASE, 4, struct agp_region*)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define AGPIOC_PROTECT _IOW (AGPIOC_BASE, 5, struct agp_region*)
#define AGPIOC_ALLOCATE _IOWR(AGPIOC_BASE, 6, struct agp_allocate*)
#define AGPIOC_DEALLOCATE _IOW (AGPIOC_BASE, 7, int)
#define AGPIOC_BIND _IOW (AGPIOC_BASE, 8, struct agp_bind*)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define AGPIOC_UNBIND _IOW (AGPIOC_BASE, 9, struct agp_unbind*)
#define AGPIOC_CHIPSET_FLUSH _IO (AGPIOC_BASE, 10)
#define AGP_DEVICE "/dev/agpgart"
#ifndef TRUE
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define TRUE 1
#endif
#ifndef FALSE
#define FALSE 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#include <linux/types.h>
struct agp_version {
 __u16 major;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 minor;
};
typedef struct _agp_info {
 struct agp_version version;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 bridge_id;
 __u32 agp_mode;
 unsigned long aper_base;
 size_t aper_size;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 size_t pg_total;
 size_t pg_system;
 size_t pg_used;
} agp_info;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef struct _agp_setup {
 __u32 agp_mode;
} agp_setup;
typedef struct _agp_segment {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __kernel_off_t pg_start;
 __kernel_size_t pg_count;
 int prot;
} agp_segment;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef struct _agp_region {
 __kernel_pid_t pid;
 __kernel_size_t seg_count;
 struct _agp_segment *seg_list;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} agp_region;
typedef struct _agp_allocate {
 int key;
 __kernel_size_t pg_count;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 type;
 __u32 physical;
} agp_allocate;
typedef struct _agp_bind {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int key;
 __kernel_off_t pg_start;
} agp_bind;
typedef struct _agp_unbind {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int key;
 __u32 priority;
} agp_unbind;
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
