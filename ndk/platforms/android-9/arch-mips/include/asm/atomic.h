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
#ifndef _ASM_ATOMIC_H
#define _ASM_ATOMIC_H
#include <linux/irqflags.h>
#include <asm/barrier.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <asm/cpu-features.h>
#include <asm/war.h>
#include <asm/system.h>
typedef struct { volatile int counter; } atomic_t;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define ATOMIC_INIT(i) { (i) }
#define atomic_read(v) ((v)->counter)
#define atomic_set(v, i) ((v)->counter = (i))
#define atomic_cmpxchg(v, o, n) (cmpxchg(&((v)->counter), (o), (n)))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define atomic_xchg(v, new) (xchg(&((v)->counter), (new)))
#define atomic_inc_not_zero(v) atomic_add_unless((v), 1, 0)
#define atomic_dec_return(v) atomic_sub_return(1, (v))
#define atomic_inc_return(v) atomic_add_return(1, (v))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define atomic_sub_and_test(i, v) (atomic_sub_return((i), (v)) == 0)
#define atomic_inc_and_test(v) (atomic_inc_return(v) == 0)
#define atomic_dec_and_test(v) (atomic_sub_return(1, (v)) == 0)
#define atomic_dec_if_positive(v) atomic_sub_if_positive(1, v)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define atomic_inc(v) atomic_add(1, (v))
#define atomic_dec(v) atomic_sub(1, (v))
#define atomic_add_negative(i, v) (atomic_add_return(i, (v)) < 0)
#define smp_mb__before_atomic_dec() smp_llsc_mb()
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define smp_mb__after_atomic_dec() smp_llsc_mb()
#define smp_mb__before_atomic_inc() smp_llsc_mb()
#define smp_mb__after_atomic_inc() smp_llsc_mb()
#include <asm-generic/atomic.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
