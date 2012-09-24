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
#ifndef _ASM_PGTABLE_H
#define _ASM_PGTABLE_H
#include <asm/pgtable-32.h>
#include <asm/io.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <asm/pgtable-bits.h>
struct mm_struct;
struct vm_area_struct;
#define PAGE_NONE __pgprot(_PAGE_PRESENT | _CACHE_CACHABLE_NONCOHERENT)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PAGE_SHARED __pgprot(_PAGE_PRESENT | _PAGE_READ | _PAGE_WRITE |   _page_cachable_default)
#define PAGE_COPY __pgprot(_PAGE_PRESENT | _PAGE_READ |   _page_cachable_default)
#define PAGE_READONLY __pgprot(_PAGE_PRESENT | _PAGE_READ |   _page_cachable_default)
#define PAGE_KERNEL __pgprot(_PAGE_PRESENT | __READABLE | __WRITEABLE |   _PAGE_GLOBAL | _page_cachable_default)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PAGE_USERIO __pgprot(_PAGE_PRESENT | _PAGE_READ | _PAGE_WRITE |   _page_cachable_default)
#define PAGE_KERNEL_UNCACHED __pgprot(_PAGE_PRESENT | __READABLE |   __WRITEABLE | _PAGE_GLOBAL | _CACHE_UNCACHED)
#define __P000 __pgprot(0)
#define __P001 __pgprot(0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __P010 __pgprot(0)
#define __P011 __pgprot(0)
#define __P100 __pgprot(0)
#define __P101 __pgprot(0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __P110 __pgprot(0)
#define __P111 __pgprot(0)
#define __S000 __pgprot(0)
#define __S001 __pgprot(0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __S010 __pgprot(0)
#define __S011 __pgprot(0)
#define __S100 __pgprot(0)
#define __S101 __pgprot(0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __S110 __pgprot(0)
#define __S111 __pgprot(0)
#define ZERO_PAGE(vaddr)   (virt_to_page((void *)(empty_zero_page + (((unsigned long)(vaddr)) & zero_page_mask))))
#define pmd_phys(pmd) virt_to_phys((void *)pmd_val(pmd))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define pmd_page(pmd) (pfn_to_page(pmd_phys(pmd) >> PAGE_SHIFT))
#define pmd_page_vaddr(pmd) pmd_val(pmd)
#define pte_none(pte) (!(pte_val(pte) & ~_PAGE_GLOBAL))
#define pte_present(pte) (pte_val(pte) & _PAGE_PRESENT)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define set_pte_at(mm, addr, ptep, pteval) set_pte(ptep, pteval)
#define set_pmd(pmdptr, pmdval) do { *(pmdptr) = (pmdval); } while(0)
#define PGD_T_LOG2 (__builtin_ffs(sizeof(pgd_t)) - 1)
#define PMD_T_LOG2 (__builtin_ffs(sizeof(pmd_t)) - 1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PTE_T_LOG2 (__builtin_ffs(sizeof(pte_t)) - 1)
#define pgprot_noncached pgprot_noncached
#define mk_pte(page, pgprot) pfn_pte(page_to_pfn(page), (pgprot))
#define kern_addr_valid(addr) (1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define io_remap_pfn_range(vma, vaddr, pfn, size, prot)   remap_pfn_range(vma, vaddr, pfn, size, prot)
#include <asm-generic/pgtable.h>
#define HAVE_ARCH_UNMAPPED_AREA
#define pgtable_cache_init() do { } while (0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
