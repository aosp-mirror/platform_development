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
#ifndef __ASM_TLBFLUSH_H
#define __ASM_TLBFLUSH_H
#include <linux/mm.h>
#define flush_tlb_all() local_flush_tlb_all()
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define flush_tlb_mm(mm) local_flush_tlb_mm(mm)
#define flush_tlb_range(vma, vmaddr, end) local_flush_tlb_range(vma, vmaddr, end)
#define flush_tlb_kernel_range(vmaddr,end)   local_flush_tlb_kernel_range(vmaddr, end)
#define flush_tlb_page(vma, page) local_flush_tlb_page(vma, page)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define flush_tlb_one(vaddr) local_flush_tlb_one(vaddr)
#endif
