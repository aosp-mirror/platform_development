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
#ifndef _ASM_BOOTINFO_H
#define _ASM_BOOTINFO_H
#include <linux/types.h>
#include <asm/setup.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MACH_UNKNOWN 0  
#define MACH_DSUNKNOWN 0
#define MACH_DS23100 1  
#define MACH_DS5100 2  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MACH_DS5000_200 3  
#define MACH_DS5000_1XX 4  
#define MACH_DS5000_XX 5  
#define MACH_DS5000_2X0 6  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MACH_DS5400 7  
#define MACH_DS5500 8  
#define MACH_DS5800 9  
#define MACH_DS5900 10  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MACH_MSP4200_EVAL 0  
#define MACH_MSP4200_GW 1  
#define MACH_MSP4200_FPGA 2  
#define MACH_MSP7120_EVAL 3  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MACH_MSP7120_GW 4  
#define MACH_MSP7120_FPGA 5  
#define MACH_MSP_OTHER 255  
#define MACH_MIKROTIK_RB532 0  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MACH_MIKROTIK_RB532A 1  
#define CL_SIZE COMMAND_LINE_SIZE
#define BOOT_MEM_MAP_MAX 32
#define BOOT_MEM_RAM 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define BOOT_MEM_ROM_DATA 2
#define BOOT_MEM_RESERVED 3
struct boot_mem_map {
 int nr_map;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct boot_mem_map_entry {
 phys_t addr;
 phys_t size;
 long type;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 } map[BOOT_MEM_MAP_MAX];
};
#endif
