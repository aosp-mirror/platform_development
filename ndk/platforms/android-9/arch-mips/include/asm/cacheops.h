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
#ifndef __ASM_CACHEOPS_H
#define __ASM_CACHEOPS_H
#define Index_Invalidate_I 0x00
#define Index_Writeback_Inv_D 0x01
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define Index_Load_Tag_I 0x04
#define Index_Load_Tag_D 0x05
#define Index_Store_Tag_I 0x08
#define Index_Store_Tag_D 0x09
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define Hit_Invalidate_I 0x10
#define Hit_Invalidate_D 0x11
#define Hit_Writeback_Inv_D 0x15
#define Create_Dirty_Excl_D 0x0d
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define Fill 0x14
#define Hit_Writeback_I 0x18
#define Hit_Writeback_D 0x19
#define Index_Invalidate_SI 0x02
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define Index_Writeback_Inv_SD 0x03
#define Index_Load_Tag_SI 0x06
#define Index_Load_Tag_SD 0x07
#define Index_Store_Tag_SI 0x0A
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define Index_Store_Tag_SD 0x0B
#define Create_Dirty_Excl_SD 0x0f
#define Hit_Invalidate_SI 0x12
#define Hit_Invalidate_SD 0x13
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define Hit_Writeback_Inv_SD 0x17
#define Hit_Writeback_SD 0x1b
#define Hit_Set_Virtual_SI 0x1e
#define Hit_Set_Virtual_SD 0x1f
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define R5K_Page_Invalidate_S 0x17
#define Page_Invalidate_T 0x16
#define Index_Writeback_Inv_S 0x03
#define Index_Load_Tag_S 0x07
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define Index_Store_Tag_S 0x0B
#define Hit_Invalidate_S 0x13
#define Cache_Barrier 0x14
#define Hit_Writeback_Inv_S 0x17
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define Index_Load_Data_I 0x18
#define Index_Load_Data_D 0x19
#define Index_Load_Data_S 0x1b
#define Index_Store_Data_I 0x1c
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define Index_Store_Data_D 0x1d
#define Index_Store_Data_S 0x1f
#endif
