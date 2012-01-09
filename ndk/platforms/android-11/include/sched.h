/*
 * Copyright (C) 2008 The Android Open Source Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
#ifndef _SCHED_H_
#define _SCHED_H_

#include <sys/cdefs.h>
#include <sys/time.h>

__BEGIN_DECLS

#define SCHED_NORMAL            0
#define SCHED_OTHER             0
#define SCHED_FIFO              1
#define SCHED_RR                2

struct sched_param {
    int sched_priority;
};

extern int sched_setscheduler(pid_t, int, const struct sched_param *);
extern int sched_getscheduler(pid_t);
extern int sched_yield(void);
extern int sched_get_priority_max(int policy);
extern int sched_get_priority_min(int policy);
extern int sched_setparam(pid_t, const struct sched_param *);
extern int sched_getparam(pid_t, struct sched_param *);
extern int sched_rr_get_interval(pid_t pid, struct timespec *tp);

#define CLONE_VM             0x00000100
#define CLONE_FS             0x00000200
#define CLONE_FILES          0x00000400
#define CLONE_SIGHAND        0x00000800
#define CLONE_PTRACE         0x00002000
#define CLONE_VFORK          0x00004000
#define CLONE_PARENT         0x00008000
#define CLONE_THREAD         0x00010000
#define CLONE_NEWNS          0x00020000
#define CLONE_SYSVSEM        0x00040000
#define CLONE_SETTLS         0x00080000
#define CLONE_PARENT_SETTID  0x00100000
#define CLONE_CHILD_CLEARTID 0x00200000
#define CLONE_DETACHED       0x00400000
#define CLONE_UNTRACED       0x00800000
#define CLONE_CHILD_SETTID   0x01000000
#define CLONE_STOPPED        0x02000000

#ifdef _GNU_SOURCE
extern int clone(int (*fn)(void *), void *child_stack, int flags, void*  arg, ...);
#endif

/* Support for cpu thread affinity */
#ifdef _GNU_SOURCE

extern int sched_getcpu(void);


/* Our implementation supports up to 32 independent CPUs, which is also
 * the maximum supported by the kernel at the moment. GLibc uses 1024 by
 * default.
 *
 * If you want to use more than that, you should use CPU_ALLOC() / CPU_FREE()
 * and the CPU_XXX_S() macro variants.
 */
#define CPU_SETSIZE   32

#define __CPU_BITTYPE    unsigned long int  /* mandated by the kernel  */
#define __CPU_BITSHIFT   5                  /* should be log2(BITTYPE) */
#define __CPU_BITS       (1 << __CPU_BITSHIFT)
#define __CPU_ELT(x)     ((x) >> __CPU_BITSHIFT)
#define __CPU_MASK(x)    ((__CPU_BITTYPE)1 << ((x) & (__CPU_BITS-1)))

typedef struct {
    __CPU_BITTYPE  __bits[ CPU_SETSIZE / __CPU_BITS ];
} cpu_set_t;

extern int sched_setaffinity(pid_t pid, size_t setsize, const cpu_set_t* set);

extern int sched_getaffinity(pid_t pid, size_t setsize, cpu_set_t* set);

/* Provide optimized implementation for 32-bit cpu_set_t */
#if CPU_SETSIZE == __CPU_BITS

#  define CPU_ZERO(set_)   \
    do{ \
        (set_)->__bits[0] = 0; \
    }while(0)

#  define CPU_SET(cpu_,set_) \
    do {\
        size_t __cpu = (cpu_); \
        if (__cpu < CPU_SETSIZE) \
            (set_)->__bits[0] |= __CPU_MASK(__cpu); \
    }while (0)

#  define CPU_CLR(cpu_,set_) \
    do {\
        size_t __cpu = (cpu_); \
        if (__cpu < CPU_SETSIZE) \
            (set_)->__bits[0] &= ~__CPU_MASK(__cpu); \
    }while (0)

#  define CPU_ISSET(cpu_, set_) \
    (__extension__({\
        size_t  __cpu = (cpu_); \
        (cpu_ < CPU_SETSIZE) \
            ? ((set_)->__bits[0] & __CPU_MASK(__cpu)) != 0 \
            : 0; \
    }))

#  define CPU_EQUAL(set1_, set2_) \
    ((set1_)->__bits[0] == (set2_)->__bits[0])

#  define __CPU_OP(dst_, set1_, set2_, op_) \
    do { \
        (dst_)->__bits[0] = (set1_)->__bits[0] op_ (set2_)->__bits[0]; \
    } while (0)

#  define CPU_COUNT(set_)  __builtin_popcountl((set_)->__bits[0])

#else /* CPU_SETSIZE != __CPU_BITS */

#  define CPU_ZERO(set_)          CPU_ZERO_S(sizeof(cpu_set_t), set_)
#  define CPU_SET(cpu_,set_)      CPU_SET_S(cpu_,sizeof(cpu_set_t),set_)
#  define CPU_CLR(cpu_,set_)      CPU_CLR_S(cpu_,sizeof(cpu_set_t),set_)
#  define CPU_ISSET(cpu_,set_)    CPU_ISSET_S(cpu_,sizeof(cpu_set_t),set_)
#  define CPU_COUNT(set_)         CPU_COUNT_S(sizeof(cpu_set_t),set_)
#  define CPU_EQUAL(set1_,set2_)  CPU_EQUAL_S(sizeof(cpu_set_t),set1_,set2_)

#  define __CPU_OP(dst_,set1_,set2_,op_)  __CPU_OP_S(sizeof(cpu_set_t),dst_,set1_,set2_,op_)

#endif /* CPU_SETSIZE != __CPU_BITS */

#define CPU_AND(set1_,set2_)   __CPU_OP(set1_,set2_,&)
#define CPU_OR(set1_,set2_)    __CPU_OP(set1_,set2_,|)
#define CPU_XOR(set1_,set2_)   __CPU_OP(set1_,set2_,^)

/* Support for dynamically-allocated cpu_set_t */

#define CPU_ALLOC_SIZE(count) \
    __CPU_ELT((count) + (__CPU_BITS-1))*sizeof(__CPU_BITTYPE)

#define CPU_ALLOC(count)   __sched_cpualloc((count));
#define CPU_FREE(set)      __sched_cpufree((set))

extern cpu_set_t* __sched_cpualloc(size_t count);
extern void       __sched_cpufree(cpu_set_t* set);

#define CPU_ZERO_S(setsize_,set_)  \
    do { \
        size_t __nn = 0; \
        size_t __nn_max = (setsize_)/sizeof(__CPU_BITTYPE); \
        for (; __nn < __nn_max; __nn++) \
            (set_)->__bits[__nn] = 0; \
    } while (0)

#define CPU_SET_S(cpu_,setsize_,set_) \
    do { \
        size_t __cpu = (cpu_); \
        if (__cpu < 8*(setsize_)) \
            (set_)->__bits[__CPU_ELT(__cpu)] |= __CPU_MASK(__cpu); \
    } while (0)

#define CPU_CLR_S(cpu_,setsize_,set_) \
    do { \
        size_t __cpu = (cpu_); \
        if (__cpu < 8*(setsize_)) \
            (set_)->__bits[__CPU_ELT(__cpu)] &= ~__CPU_MASK(__cpu); \
    } while (0)

#define CPU_ISSET_S(cpu_, setsize_, set_) \
    (__extension__ ({ \
        size_t __cpu = (cpu_); \
        (__cpu < 8*(setsize_)) \
          ? ((set_)->__bits[__CPU_ELT(__cpu)] & __CPU_MASK(__cpu)) != 0 \
          : 0; \
    }))

#define CPU_EQUAL_S(setsize_, set1_, set2_) \
    (__extension__ ({ \
        __const __CPU_BITTYPE* __src1 = (set1_)->__bits; \
        __const __CPU_BITTYPE* __src2 = (set2_)->__bits; \
        size_t __nn = 0, __nn_max = (setsize_)/sizeof(__CPU_BITTYPE); \
        for (; __nn < __nn_max; __nn++) { \
            if (__src1[__nn] != __src2[__nn]) \
                break; \
        } \
        __nn == __nn_max; \
    }))

#define __CPU_OP_S(setsize_, dstset_, srcset1_, srcset2_, op) \
    do { \
        cpu_set_t* __dst = (dstset); \
        const __CPU_BITTYPE* __src1 = (srcset1)->__bits; \
        const __CPU_BITTYPE* __src2 = (srcset2)->__bits; \
        size_t __nn = 0, __nn_max = (setsize_)/sizeof(__CPU_BITTYPE); \
        for (; __nn < __nn_max; __nn++) \
            (__dst)->__bits[__nn] = __src1[__nn] op __src2[__nn]; \
    } while (0)

#define CPU_COUNT_S(setsize_, set_) \
    __sched_cpucount((setsize_), (set_))

extern int __sched_cpucount(size_t setsize, cpu_set_t* set);

#endif /* _GNU_SOURCE */

__END_DECLS

#endif /* _SCHED_H_ */
