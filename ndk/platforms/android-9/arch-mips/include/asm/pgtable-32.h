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
#ifndef _ASM_PGTABLE_32_H
#define _ASM_PGTABLE_32_H
#include <asm/addrspace.h>
#include <asm/page.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <linux/linkage.h>
#include <asm/cachectl.h>
#include <asm/fixmap.h>
#include <asm-generic/pgtable-nopmd.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PGDIR_SHIFT (2 * PAGE_SHIFT + PTE_ORDER - PTE_T_LOG2)
#define PGDIR_SIZE (1UL << PGDIR_SHIFT)
#define PGDIR_MASK (~(PGDIR_SIZE-1))
#define __PGD_ORDER (32 - 3 * PAGE_SHIFT + PGD_T_LOG2 + PTE_T_LOG2)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PGD_ORDER (__PGD_ORDER >= 0 ? __PGD_ORDER : 0)
#define PUD_ORDER aieeee_attempt_to_allocate_pud
#define PMD_ORDER 1
#define PTE_ORDER 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PTRS_PER_PGD (USER_PTRS_PER_PGD * 2)
#define PTRS_PER_PTE ((PAGE_SIZE << PTE_ORDER) / sizeof(pte_t))
#define USER_PTRS_PER_PGD (0x80000000UL/PGDIR_SIZE)
#define FIRST_USER_ADDRESS 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VMALLOC_START MAP_BASE
#define PKMAP_BASE (0xfe000000UL)
#define VMALLOC_END (FIXADDR_START-2*PAGE_SIZE)
#define pte_ERROR(e)   printk("%s:%d: bad pte %08lx.\n", __FILE__, __LINE__, pte_val(e))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define pgd_ERROR(e)   printk("%s:%d: bad pgd %08lx.\n", __FILE__, __LINE__, pgd_val(e))
#define pmd_bad(pmd) (pmd_val(pmd) & ~PAGE_MASK)
#define pte_page(x) pfn_to_page(pte_pfn(x))
#define pte_pfn(x) ((unsigned long)((x).pte >> _PFN_SHIFT))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define pfn_pte(pfn, prot) __pte(((unsigned long long)(pfn) << _PFN_SHIFT) | pgprot_val(prot))
#define __pgd_offset(address) pgd_index(address)
#define __pud_offset(address) (((address) >> PUD_SHIFT) & (PTRS_PER_PUD-1))
#define __pmd_offset(address) (((address) >> PMD_SHIFT) & (PTRS_PER_PMD-1))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define pgd_offset_k(address) pgd_offset(&init_mm, address)
#define pgd_index(address) (((address) >> PGDIR_SHIFT) & (PTRS_PER_PGD-1))
#define pgd_offset(mm, addr) ((mm)->pgd + pgd_index(addr))
#define __pte_offset(address)   (((address) >> PAGE_SHIFT) & (PTRS_PER_PTE - 1))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define pte_offset(dir, address)   ((pte_t *) pmd_page_vaddr(*(dir)) + __pte_offset(address))
#define pte_offset_kernel(dir, address)   ((pte_t *) pmd_page_vaddr(*(dir)) + __pte_offset(address))
#define pte_offset_map(dir, address)   ((pte_t *)page_address(pmd_page(*(dir))) + __pte_offset(address))
#define pte_unmap(pte) ((void)(pte))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __swp_type(x) (((x).val >> 8) & 0x1f)
#define __swp_offset(x) ((x).val >> 13)
#define __swp_entry(type,offset)   ((swp_entry_t) { ((type) << 8) | ((offset) << 13) })
#define PTE_FILE_MAX_BITS 28
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define pte_to_pgoff(_pte) ((((_pte).pte >> 1) & 0x7) |   (((_pte).pte >> 2) & 0x8) |   (((_pte).pte >> 8) << 4))
#define pgoff_to_pte(off) ((pte_t) { (((off) & 0x7) << 1) |   (((off) & 0x8) << 2) |   (((off) >> 4) << 8) |   _PAGE_FILE })
#define __pte_to_swp_entry(pte) ((swp_entry_t) { pte_val(pte) })
#define __swp_entry_to_pte(x) ((pte_t) { (x).val })
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
