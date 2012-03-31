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
#ifndef _ASM_DIV64_H
#define _ASM_DIV64_H
#include <linux/types.h>
#if _MIPS_SZLONG == 32
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <asm/compiler.h>
#define do_div64_32(res, high, low, base) ({   unsigned long __quot32, __mod32;   unsigned long __cf, __tmp, __tmp2, __i;     __asm__(".set	push\n\t"   ".set	noat\n\t"   ".set	noreorder\n\t"   "move	%2, $0\n\t"   "move	%3, $0\n\t"   "b	1f\n\t"   " li	%4, 0x21\n"   "0:\n\t"   "sll	$1, %0, 0x1\n\t"   "srl	%3, %0, 0x1f\n\t"   "or	%0, $1, %5\n\t"   "sll	%1, %1, 0x1\n\t"   "sll	%2, %2, 0x1\n"   "1:\n\t"   "bnez	%3, 2f\n\t"   " sltu	%5, %0, %z6\n\t"   "bnez	%5, 3f\n"   "2:\n\t"   " addiu	%4, %4, -1\n\t"   "subu	%0, %0, %z6\n\t"   "addiu	%2, %2, 1\n"   "3:\n\t"   "bnez	%4, 0b\n\t"   " srl	%5, %1, 0x1f\n\t"   ".set	pop"   : "=&r" (__mod32), "=&r" (__tmp),   "=&r" (__quot32), "=&r" (__cf),   "=&r" (__i), "=&r" (__tmp2)   : "Jr" (base), "0" (high), "1" (low));     (res) = __quot32;   __mod32; })
#define do_div(n, base) ({   unsigned long long __quot;   unsigned long __mod;   unsigned long long __div;   unsigned long __upper, __low, __high, __base;     __div = (n);   __base = (base);     __high = __div >> 32;   __low = __div;   __upper = __high;     if (__high)   __asm__("divu	$0, %z2, %z3"   : "=h" (__upper), "=l" (__high)   : "Jr" (__high), "Jr" (__base)   : GCC_REG_ACCUM);     __mod = do_div64_32(__low, __upper, __low, __base);     __quot = __high;   __quot = __quot << 32 | __low;   (n) = __quot;   __mod; })
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#if _MIPS_SZLONG == 64
#define do_div(n, base) ({   unsigned long __quot;   unsigned int __mod;   unsigned long __div;   unsigned int __base;     __div = (n);   __base = (base);     __mod = __div % __base;   __quot = __div / __base;     (n) = __quot;   __mod; })
#endif
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
