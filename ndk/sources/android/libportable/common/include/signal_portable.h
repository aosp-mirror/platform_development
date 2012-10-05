/*
 * Derived from gdk/platforms/android-14/arch-arm/usr/include/signal.h
 *
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
#ifndef _SIGNAL_PORTABLE_H_
#define _SIGNAL_PORTABLE_H_

#include <portability.h>
#include <sys/cdefs.h>
#include <limits.h>             /* For LONG_BIT */
#include <string.h>             /* For memset() */
#include <sys/types.h>
#include <asm/signal_portable.h>
#include <asm/sigcontext_portable.h>

#define __ARCH_SI_UID_T __kernel_uid32_t
#include <asm/siginfo_portable.h>
#undef __ARCH_SI_UID_T

__BEGIN_DECLS

typedef int sig_atomic_t;

#if 0
/* _NSIG is used by the SIGRTMAX definition under <asm/signal.h>, however
 * its definition is part of a #if __KERNEL__ .. #endif block in the original
 * kernel headers and is thus not part of our cleaned-up versions.
 *
 * Looking at the current kernel sources, it is defined as 64 for all
 * architectures except for the 'mips' one which set it to 128.
 */
#ifndef _NSIG_PORTABLE
#  define _NSIG_PORTABLE  64
#endif
#endif

extern const char * const sys_siglist[];
extern const char * const sys_signame[];

static __inline__ int sigismember_portable(sigset_portable_t *set, int signum)
{
    unsigned long *local_set = (unsigned long *)set;
    signum--;
    return (int)((local_set[signum/LONG_BIT] >> (signum%LONG_BIT)) & 1);
}


static __inline__ int sigaddset_portable(sigset_portable_t *set, int signum)
{
    unsigned long *local_set = (unsigned long *)set;
    signum--;
    local_set[signum/LONG_BIT] |= 1UL << (signum%LONG_BIT);
    return 0;
}


static __inline__ int sigdelset_portable(sigset_portable_t *set, int signum)
{
    unsigned long *local_set = (unsigned long *)set;
    signum--;
    local_set[signum/LONG_BIT] &= ~(1UL << (signum%LONG_BIT));
    return 0;
}


static __inline__ int sigemptyset_portable(sigset_portable_t *set)
{
    memset(set, 0, sizeof *set);
    return 0;
}

static __inline__ int sigfillset_portable(sigset_portable_t *set)
{
    memset(set, ~0, sizeof *set);
    return 0;
}

/* compatibility types */
typedef void            (*sig_portable_t)(int);
typedef sig_portable_t   sighandler_portable_t;

/* Extended compatibility types, for processing a siginfo_t argument */
typedef void            (*sig3_portable_t)(int, siginfo_portable_t *, void *);
typedef sig3_portable_t   sig3handler_portable_t;

/* differentiater between sysv and bsd behaviour 8*/
extern __sighandler_t sysv_signal(int, __sighandler_portable_t);
extern __sighandler_t bsd_signal(int, __sighandler_portable_t);

#if 0
/* the default is bsd */
static __inline__ __sighandler_portable_t signal_portable(int s, sighandler_portable_t f)
{
    return bsd_signal(s,f);
}
#endif

/* the portable mapped syscall itself */
extern __sighandler_portable_t __signal_portable(int, __sighandler_portable_t);

extern int sigprocmask_portable(int, const sigset_portable_t *, sigset_portable_t *);
extern int sigaction_portable(int, const struct sigaction_portable *, struct sigaction_portable *);

extern int sigpending_portable(sigset_portable_t *);
extern int sigsuspend_portable(const sigset_portable_t *);
extern int sigwait_portable(const sigset_portable_t *set, int *sig);
extern int siginterrupt_portable(int  sig, int  flag);

extern int raise_portable(int);
extern int kill_portable(pid_t, int);
extern int killpg_portable(int pgrp, int sig);
extern int sigaltstack_portable(const portable_stack_t *ss, portable_stack_t *oss);

extern __hidden char *map_portable_signum_to_name(int portable_signum);
extern __hidden char *map_mips_signum_to_name(int mips_signum);
extern __hidden int map_portable_signum_to_mips(int portable_signum);
extern __hidden int map_mips_signum_to_portable(int mips_signum);
typedef int (*sigmask_fn)(int, const sigset_t *, sigset_t *);
extern __hidden int sigmask_helper(int portable_how, const sigset_portable_t *portable_sigset, sigset_portable_t *portable_oldset, sigmask_fn fn, char *fname);

__END_DECLS

#endif /* _SIGNAL_PORTABLE_H_ */
