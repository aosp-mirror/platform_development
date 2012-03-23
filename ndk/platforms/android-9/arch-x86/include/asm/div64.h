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
#ifndef _ASM_X86_DIV64_H
#define _ASM_X86_DIV64_H
#include <linux/types.h>
#define do_div(n,base) ({   unsigned long __upper, __low, __high, __mod, __base;   __base = (base);   asm("":"=a" (__low), "=d" (__high):"A" (n));   __upper = __high;   if (__high) {   __upper = __high % (__base);   __high = __high / (__base);   }   asm("divl %2":"=a" (__low), "=d" (__mod):"rm" (__base), "0" (__low), "1" (__upper));   asm("":"=A" (n):"a" (__low),"d" (__high));   __mod;  })
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define div_long_long_rem(a,b,c) div_ll_X_l_rem(a,b,c)
#endif
