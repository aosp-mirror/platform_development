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
#include <signal.h>
#include <time.h>
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

static __inline__ int WRAP(sigismember)(sigset_portable_t *set, int signum)
{
    unsigned long *local_set = (unsigned long *)set;
    signum--;
    return (int)((local_set[signum/LONG_BIT] >> (signum%LONG_BIT)) & 1);
}


static __inline__ int WRAP(sigaddset)(sigset_portable_t *set, int signum)
{
    unsigned long *local_set = (unsigned long *)set;
    signum--;
    local_set[signum/LONG_BIT] |= 1UL << (signum%LONG_BIT);
    return 0;
}


static __inline__ int WRAP(sigdelset)(sigset_portable_t *set, int signum)
{
    unsigned long *local_set = (unsigned long *)set;
    signum--;
    local_set[signum/LONG_BIT] &= ~(1UL << (signum%LONG_BIT));
    return 0;
}


static __inline__ int WRAP(sigemptyset)(sigset_portable_t *set)
{
    memset(set, 0, sizeof *set);
    return 0;
}

static __inline__ int WRAP(sigfillset)(sigset_portable_t *set)
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
static __inline__ __sighandler_portable_t WRAP(signal)(int s, sighandler_portable_t f)
{
    return bsd_signal(s,f);
}
#endif

/* the portable mapped syscall itself */
extern __sighandler_portable_t WRAP(__signal)(int, __sighandler_portable_t);

extern int WRAP(sigprocmask)(int, const sigset_portable_t *, sigset_portable_t *);

extern int WRAP(sigaction)(int, const struct sigaction_portable *,
                              struct sigaction_portable *);

extern int WRAP(sigpending)(sigset_portable_t *);
extern int WRAP(sigsuspend)(const sigset_portable_t *);
extern int WRAP(sigwait)(const sigset_portable_t *set, int *sig);
extern int WRAP(siginterrupt)(int  sig, int  flag);

extern int WRAP(raise)(int);
extern int WRAP(kill)(pid_t, int);
extern int WRAP(killpg)(int pgrp, int sig);
extern int WRAP(tkill)(int tid, int portable_signum);
extern int WRAP(sigaltstack)(const portable_stack_t *ss, portable_stack_t *oss);
extern int WRAP(timer_create)(clockid_t, struct sigevent *, timer_t *);

#if 0
extern int WRAP(signalfd)(int fd, const sigset_portable_t *portable_sigmask, int flags);
#endif

extern __hidden int do_signalfd4_portable(int fd, const sigset_portable_t *portable_sigmask,
                                          int portable_sigsetsize, int flags);

extern __hidden int read_signalfd_mapper(int fd, void *buf, size_t count);
extern __hidden char *map_portable_signum_to_name(int portable_signum);
extern __hidden char *map_mips_signum_to_name(int mips_signum);
extern __hidden int signum_pton(int portable_signum);
extern __hidden int signum_ntop(int mips_signum);

typedef int (*sigmask_fn)(int, const sigset_t *, sigset_t *);
typedef int (*rt_sigmask_fn)(int, const sigset_t *, sigset_t *, size_t);
typedef int (*sigaction_fn)(int, const struct sigaction *, struct sigaction *);
typedef int (*rt_sigaction_fn)(int, const struct sigaction *, struct sigaction *, size_t);


extern __hidden int do_sigmask(int portable_how, const sigset_portable_t *portable_sigset,
                               sigset_portable_t *portable_oldset, sigmask_fn fn,
                               rt_sigmask_fn rt_fn);


/* These functions are called from syscall.c and experimental Bionic linker. */
extern int WRAP(__rt_sigaction)(int portable_signum,
                                const struct sigaction_portable *act,
                                struct sigaction_portable *oldact,
                                size_t sigsetsize);

extern int WRAP(__rt_sigprocmask)(int portable_how,
                                  const sigset_portable_t *portable_sigset,
                                  sigset_portable_t *portable_oldset,
                                  size_t sigsetsize);

extern int WRAP(__rt_sigtimedwait)(const sigset_portable_t *portable_sigset,
                                   siginfo_portable_t *portable_siginfo,
                                   const struct timespec *timeout,
                                   size_t portable_sigsetsize);


/* These functions are only called from syscall.c; not experimental Bionic linker. */
extern __hidden int WRAP(rt_sigqueueinfo)(pid_t pid, int sig, siginfo_portable_t *uinfo);

extern __hidden int WRAP(rt_tgsigqueueinfo)(pid_t tgid, pid_t pid, int sig,
                                               siginfo_portable_t *uinfo);


/* Called by clone when memory and signal handlers aren't compatable. */
extern __hidden void signal_disable_mapping(void);

__END_DECLS

#endif /* _SIGNAL_PORTABLE_H_ */
