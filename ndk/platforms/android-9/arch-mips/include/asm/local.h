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
#ifndef _ARCH_MIPS_LOCAL_H
#define _ARCH_MIPS_LOCAL_H
#include <linux/percpu.h>
#include <linux/bitops.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <asm/atomic.h>
#include <asm/cmpxchg.h>
#include <asm/war.h>
typedef struct
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
{
 atomic_long_t a;
} local_t;
#define LOCAL_INIT(i) { ATOMIC_LONG_INIT(i) }
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define local_read(l) atomic_long_read(&(l)->a)
#define local_set(l, i) atomic_long_set(&(l)->a, (i))
#define local_add(i, l) atomic_long_add((i), (&(l)->a))
#define local_sub(i, l) atomic_long_sub((i), (&(l)->a))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define local_inc(l) atomic_long_inc(&(l)->a)
#define local_dec(l) atomic_long_dec(&(l)->a)
#define local_cmpxchg(l, o, n)   ((long)cmpxchg_local(&((l)->a.counter), (o), (n)))
#define local_xchg(l, n) (xchg_local(&((l)->a.counter), (n)))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define local_add_unless(l, a, u)  ({   long c, old;   c = local_read(l);   while (c != (u) && (old = local_cmpxchg((l), c, c + (a))) != c)   c = old;   c != (u);  })
#define local_inc_not_zero(l) local_add_unless((l), 1, 0)
#define local_dec_return(l) local_sub_return(1, (l))
#define local_inc_return(l) local_add_return(1, (l))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define local_sub_and_test(i, l) (local_sub_return((i), (l)) == 0)
#define local_inc_and_test(l) (local_inc_return(l) == 0)
#define local_dec_and_test(l) (local_sub_return(1, (l)) == 0)
#define local_add_negative(i, l) (local_add_return(i, (l)) < 0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __local_inc(l) ((l)->a.counter++)
#define __local_dec(l) ((l)->a.counter++)
#define __local_add(i, l) ((l)->a.counter+=(i))
#define __local_sub(i, l) ((l)->a.counter-=(i))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define cpu_local_wrap_v(l)   ({ local_t res__;   preempt_disable();   res__ = (l);   preempt_enable();   res__; })
#define cpu_local_wrap(l)   ({ preempt_disable();   l;   preempt_enable(); })  
#define cpu_local_read(l) cpu_local_wrap_v(local_read(&__get_cpu_var(l)))
#define cpu_local_set(l, i) cpu_local_wrap(local_set(&__get_cpu_var(l), (i)))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define cpu_local_inc(l) cpu_local_wrap(local_inc(&__get_cpu_var(l)))
#define cpu_local_dec(l) cpu_local_wrap(local_dec(&__get_cpu_var(l)))
#define cpu_local_add(i, l) cpu_local_wrap(local_add((i), &__get_cpu_var(l)))
#define cpu_local_sub(i, l) cpu_local_wrap(local_sub((i), &__get_cpu_var(l)))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __cpu_local_inc(l) cpu_local_inc(l)
#define __cpu_local_dec(l) cpu_local_dec(l)
#define __cpu_local_add(i, l) cpu_local_add((i), (l))
#define __cpu_local_sub(i, l) cpu_local_sub((i), (l))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
