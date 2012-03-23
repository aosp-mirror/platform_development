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
#ifndef __ASM_IO_APIC_H
#define __ASM_IO_APIC_H
#include <asm/types.h>
#include <asm/mpspec.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <asm/apicdef.h>
union IO_APIC_reg_00 {
 u32 raw;
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u32 __reserved_2 : 14,
 LTS : 1,
 delivery_type : 1,
 __reserved_1 : 8,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ID : 8;
 } __attribute__ ((packed)) bits;
};
union IO_APIC_reg_01 {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u32 raw;
 struct {
 u32 version : 8,
 __reserved_2 : 7,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 PRQ : 1,
 entries : 8,
 __reserved_1 : 8;
 } __attribute__ ((packed)) bits;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
union IO_APIC_reg_02 {
 u32 raw;
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u32 __reserved_2 : 24,
 arbitration : 4,
 __reserved_1 : 4;
 } __attribute__ ((packed)) bits;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
union IO_APIC_reg_03 {
 u32 raw;
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u32 boot_DT : 1,
 __reserved_1 : 31;
 } __attribute__ ((packed)) bits;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum ioapic_irq_destination_types {
 dest_Fixed = 0,
 dest_LowestPrio = 1,
 dest_SMI = 2,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 dest__reserved_1 = 3,
 dest_NMI = 4,
 dest_INIT = 5,
 dest__reserved_2 = 6,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 dest_ExtINT = 7
};
struct IO_APIC_route_entry {
 __u32 vector : 8,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 delivery_mode : 3,
 dest_mode : 1,
 delivery_status : 1,
 polarity : 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 irr : 1,
 trigger : 1,
 mask : 1,
 __reserved_2 : 15;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 union { struct { __u32
 __reserved_1 : 24,
 physical_dest : 4,
 __reserved_2 : 4;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 } physical;
 struct { __u32
 __reserved_1 : 24,
 logical_dest : 8;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 } logical;
 } dest;
} __attribute__ ((packed));
#define io_apic_assign_pci_irqs 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
