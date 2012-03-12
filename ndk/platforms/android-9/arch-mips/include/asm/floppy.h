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
#ifndef _ASM_FLOPPY_H
#define _ASM_FLOPPY_H
#include <linux/dma-mapping.h>
#define MAX_BUFFER_SECTORS 24
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FLOPPY0_TYPE fd_drive_type(0)
#define FLOPPY1_TYPE fd_drive_type(1)
#define FDC1 fd_getfdaddr1();
#define N_FDC 1  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define N_DRIVE 8
#define CROSS_64KB(a, s) ((unsigned long)(a)/K_64 != ((unsigned long)(a) + (s) - 1) / K_64)
#define EXTRA_FLOPPY_PARAMS
#include <floppy.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
