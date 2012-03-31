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
#ifndef __ASM_CMPXCHG_H
#define __ASM_CMPXCHG_H
#include <linux/irqflags.h>
#define __HAVE_ARCH_CMPXCHG 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __cmpxchg_asm(ld, st, m, old, new)  ({   __typeof(*(m)) __ret;     if (cpu_has_llsc && R10000_LLSC_WAR) {   __asm__ __volatile__(   "	.set	push				\n"   "	.set	noat				\n"   "	.set	mips3				\n"   "1:	" ld "	%0, %2		# __cmpxchg_asm	\n"   "	bne	%0, %z3, 2f			\n"   "	.set	mips0				\n"   "	move	$1, %z4				\n"   "	.set	mips3				\n"   "	" st "	$1, %1				\n"   "	beqzl	$1, 1b				\n"   "2:						\n"   "	.set	pop				\n"   : "=&r" (__ret), "=R" (*m)   : "R" (*m), "Jr" (old), "Jr" (new)   : "memory");   } else if (cpu_has_llsc) {   __asm__ __volatile__(   "	.set	push				\n"   "	.set	noat				\n"   "	.set	mips3				\n"   "1:	" ld "	%0, %2		# __cmpxchg_asm	\n"   "	bne	%0, %z3, 2f			\n"   "	.set	mips0				\n"   "	move	$1, %z4				\n"   "	.set	mips3				\n"   "	" st "	$1, %1				\n"   "	beqz	$1, 3f				\n"   "2:						\n"   "	.subsection 2				\n"   "3:	b	1b				\n"   "	.previous				\n"   "	.set	pop				\n"   : "=&r" (__ret), "=R" (*m)   : "R" (*m), "Jr" (old), "Jr" (new)   : "memory");   } else {   unsigned long __flags;     raw_local_irq_save(__flags);   __ret = *m;   if (__ret == old)   *m = new;   raw_local_irq_restore(__flags);   }     __ret;  })
#define __cmpxchg(ptr, old, new, barrier)  ({   __typeof__(ptr) __ptr = (ptr);   __typeof__(*(ptr)) __old = (old);   __typeof__(*(ptr)) __new = (new);   __typeof__(*(ptr)) __res = 0;     barrier;     switch (sizeof(*(__ptr))) {   case 4:   __res = __cmpxchg_asm("ll", "sc", __ptr, __old, __new);   break;   case 8:   if (sizeof(long) == 8) {   __res = __cmpxchg_asm("lld", "scd", __ptr,   __old, __new);   break;   }   default:   __cmpxchg_called_with_bad_pointer();   break;   }     barrier;     __res;  })
#define cmpxchg(ptr, old, new) __cmpxchg(ptr, old, new, smp_llsc_mb())
#define cmpxchg_local(ptr, old, new) __cmpxchg(ptr, old, new, )
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define cmpxchg64(ptr, o, n)   ({   BUILD_BUG_ON(sizeof(*(ptr)) != 8);   cmpxchg((ptr), (o), (n));   })
#include <asm-generic/cmpxchg-local.h>
#define cmpxchg64_local(ptr, o, n) __cmpxchg64_local_generic((ptr), (o), (n))
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
