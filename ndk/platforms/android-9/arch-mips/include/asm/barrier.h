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
#ifndef __ASM_BARRIER_H
#define __ASM_BARRIER_H
#define read_barrier_depends() do { } while(0)
#define smp_read_barrier_depends() do { } while(0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __sync() do { } while(0)
#define __fast_iob()   __asm__ __volatile__(   ".set	push\n\t"   ".set	noreorder\n\t"   "lw	$0,%0\n\t"   "nop\n\t"   ".set	pop"   :     : "m" (*(int *)CKSEG1)   : "memory")
#define fast_wmb() __sync()
#define fast_rmb() __sync()
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define fast_mb() __sync()
#define fast_iob()   do {   __sync();   __fast_iob();   } while (0)
#define wmb() fast_wmb()
#define rmb() fast_rmb()
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define mb() fast_mb()
#define iob() fast_iob()
#define __WEAK_ORDERING_MB "		\n"
#define __WEAK_LLSC_MB "		\n"
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define smp_mb() __asm__ __volatile__(__WEAK_ORDERING_MB : : :"memory")
#define smp_rmb() __asm__ __volatile__(__WEAK_ORDERING_MB : : :"memory")
#define smp_wmb() __asm__ __volatile__(__WEAK_ORDERING_MB : : :"memory")
#define set_mb(var, value)   do { var = value; smp_mb(); } while (0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define smp_llsc_mb() __asm__ __volatile__(__WEAK_LLSC_MB : : :"memory")
#define smp_llsc_rmb() __asm__ __volatile__(__WEAK_LLSC_MB : : :"memory")
#define smp_llsc_wmb() __asm__ __volatile__(__WEAK_LLSC_MB : : :"memory")
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
