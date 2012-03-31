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
#ifndef _ASM_SN_KLDIR_H
#define _ASM_SN_KLDIR_H
#define KLDIR_MAGIC 0x434d5f53505f5357
#ifdef __ASSEMBLY__
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KLDIR_OFF_MAGIC 0x00
#define KLDIR_OFF_OFFSET 0x08
#define KLDIR_OFF_POINTER 0x10
#define KLDIR_OFF_SIZE 0x18
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KLDIR_OFF_COUNT 0x20
#define KLDIR_OFF_STRIDE 0x28
#endif
#define SYMMON_STACK_SIZE 0x8000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifdef PROM
#define IP27_LAUNCH_OFFSET 0x2400
#define IP27_LAUNCH_SIZE 0x400
#define IP27_LAUNCH_COUNT 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IP27_LAUNCH_STRIDE 0x200
#define IP27_KLCONFIG_OFFSET 0x4000
#define IP27_KLCONFIG_SIZE 0xc000
#define IP27_KLCONFIG_COUNT 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IP27_KLCONFIG_STRIDE 0
#define IP27_NMI_OFFSET 0x3000
#define IP27_NMI_SIZE 0x40
#define IP27_NMI_COUNT 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IP27_NMI_STRIDE 0x40
#define IP27_PI_ERROR_OFFSET 0x12000
#define IP27_PI_ERROR_SIZE 0x4000
#define IP27_PI_ERROR_COUNT 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IP27_PI_ERROR_STRIDE 0
#define IP27_SYMMON_STK_OFFSET 0x25000
#define IP27_SYMMON_STK_SIZE 0xe000
#define IP27_SYMMON_STK_COUNT 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IP27_SYMMON_STK_STRIDE 0x7000
#define IP27_FREEMEM_OFFSET 0x19000
#define IP27_FREEMEM_SIZE -1
#define IP27_FREEMEM_COUNT 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IP27_FREEMEM_STRIDE 0
#endif
#define IO6_GDA_OFFSET 0x11000
#define IO6_GDA_SIZE 0x400
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IO6_GDA_COUNT 1
#define IO6_GDA_STRIDE 0
#define IP27_NMI_KREGS_OFFSET 0x11400
#define IP27_NMI_KREGS_CPU_SIZE 0x200
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IP27_NMI_EFRAME_OFFSET 0x11800
#define IP27_NMI_EFRAME_SIZE 0x200
#define KLDIR_ENT_SIZE 0x40
#define KLDIR_MAX_ENTRIES (0x400 / 0x40)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef __ASSEMBLY__
typedef struct kldir_ent_s {
 u64 magic;
 off_t offset;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long pointer;
 size_t size;
 u64 count;
 size_t stride;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char rsvd[16];
} kldir_ent_t;
#endif
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
