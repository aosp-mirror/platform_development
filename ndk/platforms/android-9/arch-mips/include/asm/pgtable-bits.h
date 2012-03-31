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
#ifndef _ASM_PGTABLE_BITS_H
#define _ASM_PGTABLE_BITS_H
#define _PAGE_PRESENT (1<<0)  
#define _PAGE_READ (1<<1)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define _PAGE_WRITE (1<<2)  
#define _PAGE_ACCESSED (1<<3)  
#define _PAGE_MODIFIED (1<<4)  
#define _PAGE_FILE (1<<4)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define _PAGE_R4KBUG (1<<5)  
#define _PAGE_GLOBAL (1<<6)
#define _PAGE_VALID (1<<7)
#define _PAGE_SILENT_READ (1<<7)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define _PAGE_DIRTY (1<<8)  
#define _PAGE_SILENT_WRITE (1<<8)
#define _CACHE_SHIFT 9
#define _CACHE_MASK (7<<9)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define _CACHE_CACHABLE_NO_WA (0<<_CACHE_SHIFT)  
#define _CACHE_CACHABLE_WA (1<<_CACHE_SHIFT)  
#define _CACHE_UNCACHED (2<<_CACHE_SHIFT)  
#define _CACHE_CACHABLE_NONCOHERENT (3<<_CACHE_SHIFT)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define _CACHE_CACHABLE_CE (4<<_CACHE_SHIFT)  
#define _CACHE_CACHABLE_COW (5<<_CACHE_SHIFT)  
#define _CACHE_CACHABLE_COHERENT (5<<_CACHE_SHIFT)  
#define _CACHE_CACHABLE_CUW (6<<_CACHE_SHIFT)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define _CACHE_UNCACHED_ACCELERATED (7<<_CACHE_SHIFT)  
#define __READABLE (_PAGE_READ | _PAGE_SILENT_READ | _PAGE_ACCESSED)
#define __WRITEABLE (_PAGE_WRITE | _PAGE_SILENT_WRITE | _PAGE_MODIFIED)
#define _PAGE_CHG_MASK (PAGE_MASK | _PAGE_ACCESSED | _PAGE_MODIFIED | _CACHE_MASK)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
