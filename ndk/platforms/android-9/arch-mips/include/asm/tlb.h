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
#ifndef __ASM_TLB_H
#define __ASM_TLB_H
#define tlb_start_vma(tlb, vma)   do {   if (!tlb->fullmm)   flush_cache_range(vma, vma->vm_start, vma->vm_end);   } while (0)
#define tlb_end_vma(tlb, vma) do { } while (0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __tlb_remove_tlb_entry(tlb, ptep, address) do { } while (0)
#define tlb_flush(tlb) flush_tlb_mm((tlb)->mm)
#include <asm-generic/tlb.h>
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
