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
#ifndef _ASM_SEGMENT_H
#define _ASM_SEGMENT_H
#define GDT_ENTRY_TLS_ENTRIES 3
#define GDT_ENTRY_TLS_MIN 6
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define GDT_ENTRY_TLS_MAX (GDT_ENTRY_TLS_MIN + GDT_ENTRY_TLS_ENTRIES - 1)
#define TLS_SIZE (GDT_ENTRY_TLS_ENTRIES * 8)
#define GDT_ENTRY_DEFAULT_USER_CS 14
#define __USER_CS (GDT_ENTRY_DEFAULT_USER_CS * 8 + 3)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define GDT_ENTRY_DEFAULT_USER_DS 15
#define __USER_DS (GDT_ENTRY_DEFAULT_USER_DS * 8 + 3)
#define GDT_ENTRY_KERNEL_BASE 12
#define GDT_ENTRY_KERNEL_CS (GDT_ENTRY_KERNEL_BASE + 0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __KERNEL_CS (GDT_ENTRY_KERNEL_CS * 8)
#define GDT_ENTRY_KERNEL_DS (GDT_ENTRY_KERNEL_BASE + 1)
#define __KERNEL_DS (GDT_ENTRY_KERNEL_DS * 8)
#define GDT_ENTRY_TSS (GDT_ENTRY_KERNEL_BASE + 4)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define GDT_ENTRY_LDT (GDT_ENTRY_KERNEL_BASE + 5)
#define GDT_ENTRY_PNPBIOS_BASE (GDT_ENTRY_KERNEL_BASE + 6)
#define GDT_ENTRY_APMBIOS_BASE (GDT_ENTRY_KERNEL_BASE + 11)
#define GDT_ENTRY_ESPFIX_SS (GDT_ENTRY_KERNEL_BASE + 14)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __ESPFIX_SS (GDT_ENTRY_ESPFIX_SS * 8)
#define GDT_ENTRY_PERCPU (GDT_ENTRY_KERNEL_BASE + 15)
#define __KERNEL_PERCPU 0
#define GDT_ENTRY_DOUBLEFAULT_TSS 31
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define GDT_ENTRIES 32
#define GDT_SIZE (GDT_ENTRIES * 8)
#define GDT_ENTRY_BOOT_CS 2
#define __BOOT_CS (GDT_ENTRY_BOOT_CS * 8)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define GDT_ENTRY_BOOT_DS (GDT_ENTRY_BOOT_CS + 1)
#define __BOOT_DS (GDT_ENTRY_BOOT_DS * 8)
#define GDT_ENTRY_PNPBIOS_CS32 (GDT_ENTRY_PNPBIOS_BASE + 0)
#define GDT_ENTRY_PNPBIOS_CS16 (GDT_ENTRY_PNPBIOS_BASE + 1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define GDT_ENTRY_PNPBIOS_DS (GDT_ENTRY_PNPBIOS_BASE + 2)
#define GDT_ENTRY_PNPBIOS_TS1 (GDT_ENTRY_PNPBIOS_BASE + 3)
#define GDT_ENTRY_PNPBIOS_TS2 (GDT_ENTRY_PNPBIOS_BASE + 4)
#define PNP_CS32 (GDT_ENTRY_PNPBIOS_CS32 * 8)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PNP_CS16 (GDT_ENTRY_PNPBIOS_CS16 * 8)  
#define PNP_DS (GDT_ENTRY_PNPBIOS_DS * 8)  
#define PNP_TS1 (GDT_ENTRY_PNPBIOS_TS1 * 8)  
#define PNP_TS2 (GDT_ENTRY_PNPBIOS_TS2 * 8)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IDT_ENTRIES 256
#define SEGMENT_RPL_MASK 0x3
#define SEGMENT_TI_MASK 0x4
#define USER_RPL 0x3
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SEGMENT_LDT 0x4
#define SEGMENT_GDT 0x0
#define get_kernel_rpl() 0
#define SEGMENT_IS_KERNEL_CODE(x) (((x) & 0xfc) == GDT_ENTRY_KERNEL_CS * 8)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SEGMENT_IS_FLAT_CODE(x) (((x) & 0xec) == GDT_ENTRY_KERNEL_CS * 8)
#define SEGMENT_IS_PNP_CODE(x) (((x) & 0xf4) == GDT_ENTRY_PNPBIOS_BASE * 8)
#endif
