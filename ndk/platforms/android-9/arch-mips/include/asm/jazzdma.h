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
#ifndef _ASM_JAZZDMA_H
#define _ASM_JAZZDMA_H
#define VDMA_PAGESIZE 4096
#define VDMA_PGTBL_ENTRIES 4096
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VDMA_PGTBL_SIZE (sizeof(VDMA_PGTBL_ENTRY) * VDMA_PGTBL_ENTRIES)
#define VDMA_PAGE_EMPTY 0xff000000
#define VDMA_PAGE(a) ((unsigned int)(a) >> 12)
#define VDMA_OFFSET(a) ((unsigned int)(a) & (VDMA_PAGESIZE-1))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VDMA_ERROR 0xffffffff
typedef volatile struct VDMA_PGTBL_ENTRY {
 unsigned int frame;
 unsigned int owner;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} VDMA_PGTBL_ENTRY;
#define JAZZ_R4030_CHNL_MODE 0xE0000100  
#define JAZZ_R4030_CHNL_ENABLE 0xE0000108  
#define JAZZ_R4030_CHNL_COUNT 0xE0000110  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define JAZZ_R4030_CHNL_ADDR 0xE0000118  
#define R4030_CHNL_ENABLE (1<<0)
#define R4030_CHNL_WRITE (1<<1)
#define R4030_TC_INTR (1<<8)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define R4030_MEM_INTR (1<<9)
#define R4030_ADDR_INTR (1<<10)
#define R4030_MODE_ATIME_40 (0)  
#define R4030_MODE_ATIME_80 (1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define R4030_MODE_ATIME_120 (2)
#define R4030_MODE_ATIME_160 (3)
#define R4030_MODE_ATIME_200 (4)
#define R4030_MODE_ATIME_240 (5)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define R4030_MODE_ATIME_280 (6)
#define R4030_MODE_ATIME_320 (7)
#define R4030_MODE_WIDTH_8 (1<<3)  
#define R4030_MODE_WIDTH_16 (2<<3)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define R4030_MODE_WIDTH_32 (3<<3)
#define R4030_MODE_INTR_EN (1<<5)
#define R4030_MODE_BURST (1<<6)  
#define R4030_MODE_FAST_ACK (1<<7)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
