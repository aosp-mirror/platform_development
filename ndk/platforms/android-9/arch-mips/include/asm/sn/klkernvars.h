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
#ifndef __ASM_SN_KLKERNVARS_H
#define __ASM_SN_KLKERNVARS_H
#define KV_MAGIC_OFFSET 0x0
#define KV_RO_NASID_OFFSET 0x4
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KV_RW_NASID_OFFSET 0x6
#define KV_MAGIC 0x5f4b565f
#ifndef __ASSEMBLY__
#include <asm/sn/types.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef struct kern_vars_s {
 int kv_magic;
 nasid_t kv_ro_nasid;
 nasid_t kv_rw_nasid;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long kv_ro_baseaddr;
 unsigned long kv_rw_baseaddr;
} kern_vars_t;
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
