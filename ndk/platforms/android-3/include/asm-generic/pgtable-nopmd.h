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
#ifndef _PGTABLE_NOPMD_H
#define _PGTABLE_NOPMD_H
#ifndef __ASSEMBLY__
#include <asm-generic/pgtable-nopud.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct mm_struct;
#define __PAGETABLE_PMD_FOLDED
typedef struct { pud_t pud; } pmd_t;
#define PMD_SHIFT PUD_SHIFT
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PTRS_PER_PMD 1
#define PMD_SIZE (1UL << PMD_SHIFT)
#define PMD_MASK (~(PMD_SIZE-1))
#define pmd_ERROR(pmd) (pud_ERROR((pmd).pud))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define pud_populate(mm, pmd, pte) do { } while (0)
#define set_pud(pudptr, pudval) set_pmd((pmd_t *)(pudptr), (pmd_t) { pudval })
#define pmd_val(x) (pud_val((x).pud))
#define __pmd(x) ((pmd_t) { __pud(x) } )
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define pud_page(pud) (pmd_page((pmd_t){ pud }))
#define pud_page_vaddr(pud) (pmd_page_vaddr((pmd_t){ pud }))
#define pmd_alloc_one(mm, address) NULL
#define __pmd_free_tlb(tlb, x, a) do { } while (0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#undef pmd_addr_end
#define pmd_addr_end(addr, end) (end)
#endif
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
