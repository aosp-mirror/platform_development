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
#ifndef _ASM_ADDRSPACE_H
#define _ASM_ADDRSPACE_H
#include <spaces.h>
#ifdef __ASSEMBLY__
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define _ATYPE_
#define _ATYPE32_
#define _ATYPE64_
#define _CONST64_(x) x
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#else
#define _ATYPE_ __PTRDIFF_TYPE__
#define _ATYPE32_ int
#define _ATYPE64_ __s64
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define _CONST64_(x) x ## LL
#endif
#ifdef __ASSEMBLY__
#define _ACAST32_
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define _ACAST64_
#else
#define _ACAST32_ (_ATYPE_)(_ATYPE32_)  
#define _ACAST64_ (_ATYPE64_)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#define KSEGX(a) ((_ACAST32_ (a)) & 0xe0000000)
#define CPHYSADDR(a) ((_ACAST32_(a)) & 0x1fffffff)
#define XPHYSADDR(a) ((_ACAST64_(a)) &   _CONST64_(0x000000ffffffffff))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CKSEG0ADDR(a) (CPHYSADDR(a) | KSEG0)
#define CKSEG1ADDR(a) (CPHYSADDR(a) | KSEG1)
#define CKSEG2ADDR(a) (CPHYSADDR(a) | KSEG2)
#define CKSEG3ADDR(a) (CPHYSADDR(a) | KSEG3)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KSEG0ADDR(a) (CPHYSADDR(a) | KSEG0)
#define KSEG1ADDR(a) (CPHYSADDR(a) | KSEG1)
#define KSEG2ADDR(a) (CPHYSADDR(a) | KSEG2)
#define KSEG3ADDR(a) (CPHYSADDR(a) | KSEG3)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KUSEG 0x00000000
#define KSEG0 0x80000000
#define KSEG1 0xa0000000
#define KSEG2 0xc0000000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KSEG3 0xe0000000
#define CKUSEG 0x00000000
#define CKSEG0 0x80000000
#define CKSEG1 0xa0000000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CKSEG2 0xc0000000
#define CKSEG3 0xe0000000
#define K_CALG_COH_EXCL1_NOL2 0
#define K_CALG_COH_SHRL1_NOL2 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define K_CALG_UNCACHED 2
#define K_CALG_NONCOHERENT 3
#define K_CALG_COH_EXCL 4
#define K_CALG_COH_SHAREABLE 5
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define K_CALG_NOTUSED 6
#define K_CALG_UNCACHED_ACCEL 7
#define PHYS_TO_XKSEG_UNCACHED(p) PHYS_TO_XKPHYS(K_CALG_UNCACHED, (p))
#define PHYS_TO_XKSEG_CACHED(p) PHYS_TO_XKPHYS(K_CALG_COH_SHAREABLE, (p))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define XKPHYS_TO_PHYS(p) ((p) & TO_PHYS_MASK)
#define PHYS_TO_XKPHYS(cm, a) (_CONST64_(0x8000000000000000) |   (_CONST64_(cm) << 59) | (a))
#define TO_PHYS_MASK _CONST64_(0x07ffffffffffffff)  
#define COMPAT_K1BASE32 _CONST64_(0xffffffffa0000000)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PHYS_TO_COMPATK1(x) ((x) | COMPAT_K1BASE32)  
#define KDM_TO_PHYS(x) (_ACAST64_ (x) & TO_PHYS_MASK)
#define PHYS_TO_K0(x) (_ACAST64_ (x) | CAC_BASE)
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
