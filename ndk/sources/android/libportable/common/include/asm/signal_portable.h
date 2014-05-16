/****************************************************************************
  Derived from gdk/platforms/android-14/arch-arm/usr/include/asm/signal.h
 ****************************************************************************
 ***
 ***   This header was ORIGINALLY automatically generated from a Linux kernel
 ***   header of the same name, to make information necessary for userspace to
 ***   call into the kernel available to libc.  It contains only constants,
 ***   structures, and macros generated from the original header, and thus,
 ***   contains no copyrightable information.
 ***
 ****************************************************************************
 ****************************************************************************/
#ifndef _ASMARM_SIGNAL_PORTABLE_H
#define _ASMARM_SIGNAL_PORTABLE_H

struct siginfo;                         /* TODO: Change to siginfo_portable */

#define NSIG_PORTABLE 64
typedef unsigned long sigset_portable_t;

#define SIGHUP_PORTABLE 1
#define SIGINT_PORTABLE 2
#define SIGQUIT_PORTABLE 3
#define SIGILL_PORTABLE 4
#define SIGTRAP_PORTABLE 5
#define SIGABRT_PORTABLE 6
#define SIGIOT_PORTABLE 6
#define SIGBUS_PORTABLE 7
#define SIGFPE_PORTABLE 8
#define SIGKILL_PORTABLE 9
#define SIGUSR1_PORTABLE 10
#define SIGSEGV_PORTABLE 11
#define SIGUSR2_PORTABLE 12
#define SIGPIPE_PORTABLE 13
#define SIGALRM_PORTABLE 14
#define SIGTERM_PORTABLE 15
#define SIGSTKFLT_PORTABLE 16
#define SIGCHLD_PORTABLE 17
#define SIGCONT_PORTABLE 18
#define SIGSTOP_PORTABLE 19
#define SIGTSTP_PORTABLE 20
#define SIGTTIN_PORTABLE 21
#define SIGTTOU_PORTABLE 22
#define SIGURG_PORTABLE 23
#define SIGXCPU_PORTABLE 24
#define SIGXFSZ_PORTABLE 25
#define SIGVTALRM_PORTABLE 26
#define SIGPROF_PORTABLE 27
#define SIGWINCH_PORTABLE 28
#define SIGIO_PORTABLE 29
#define SIGPOLL_PORTABLE SIGIO

#define SIGPWR_PORTABLE 30
#define SIGSYS_PORTABLE 31
#define SIGUNUSED_PORTABLE 31

#define SIGSWI_PORTABLE 32
#define SIGRTMIN_PORTABLE 32

#define SIGRT_1_PORTABLE (SIGRTMIN_PORTABLE + 1)
#define SIGRT_2_PORTABLE (SIGRTMIN_PORTABLE + 2)
#define SIGRT_3_PORTABLE (SIGRTMIN_PORTABLE + 3)
#define SIGRT_4_PORTABLE (SIGRTMIN_PORTABLE + 4)
#define SIGRT_5_PORTABLE (SIGRTMIN_PORTABLE + 5)
#define SIGRT_5_PORTABLE (SIGRTMIN_PORTABLE + 5)
#define SIGRT_6_PORTABLE (SIGRTMIN_PORTABLE + 6)
#define SIGRT_7_PORTABLE (SIGRTMIN_PORTABLE + 7)
#define SIGRT_8_PORTABLE (SIGRTMIN_PORTABLE + 8)
#define SIGRT_9_PORTABLE (SIGRTMIN_PORTABLE + 9)
#define SIGRT_10_PORTABLE (SIGRTMIN_PORTABLE + 10)
#define SIGRT_11_PORTABLE (SIGRTMIN_PORTABLE + 11)
#define SIGRT_12_PORTABLE (SIGRTMIN_PORTABLE + 12)
#define SIGRT_13_PORTABLE (SIGRTMIN_PORTABLE + 13)
#define SIGRT_14_PORTABLE (SIGRTMIN_PORTABLE + 14)
#define SIGRT_15_PORTABLE (SIGRTMIN_PORTABLE + 15)
#define SIGRT_15_PORTABLE (SIGRTMIN_PORTABLE + 15)
#define SIGRT_16_PORTABLE (SIGRTMIN_PORTABLE + 16)
#define SIGRT_17_PORTABLE (SIGRTMIN_PORTABLE + 17)
#define SIGRT_18_PORTABLE (SIGRTMIN_PORTABLE + 18)
#define SIGRT_19_PORTABLE (SIGRTMIN_PORTABLE + 19)
#define SIGRT_20_PORTABLE (SIGRTMIN_PORTABLE + 20)
#define SIGRT_20_PORTABLE (SIGRTMIN_PORTABLE + 20)
#define SIGRT_21_PORTABLE (SIGRTMIN_PORTABLE + 21)
#define SIGRT_22_PORTABLE (SIGRTMIN_PORTABLE + 22)
#define SIGRT_23_PORTABLE (SIGRTMIN_PORTABLE + 23)
#define SIGRT_24_PORTABLE (SIGRTMIN_PORTABLE + 24)
#define SIGRT_25_PORTABLE (SIGRTMIN_PORTABLE + 25)
#define SIGRT_25_PORTABLE (SIGRTMIN_PORTABLE + 25)
#define SIGRT_26_PORTABLE (SIGRTMIN_PORTABLE + 26)
#define SIGRT_27_PORTABLE (SIGRTMIN_PORTABLE + 27)
#define SIGRT_28_PORTABLE (SIGRTMIN_PORTABLE + 28)
#define SIGRT_29_PORTABLE (SIGRTMIN_PORTABLE + 29)
#define SIGRT_30_PORTABLE (SIGRTMIN_PORTABLE + 30)
#define SIGRT_31_PORTABLE (SIGRTMIN_PORTABLE + 31)
#define SIGRT_32_PORTABLE (SIGRTMIN_PORTABLE + 32)

#define SIGRTMAX_PORTABLE NSIG_PORTABLE

/*
 * Define MIPS/Native Real Time Signal Names for debugging.
 * NOTE:
 *    Currently only defining the 32 RT signals that the
 *    lib-portable application can interact with. MIPS has
 *    an additional 63 signals.
 */

#ifndef __SIGRTMIN
#define __SIGRTMIN SIGRTMIN
#endif
#ifndef __SIGRTMAX
#define __SIGRTMAX SIGRTMAX
#endif

#define SIGRT_1 (__SIGRTMIN + 1)
#define SIGRT_2 (__SIGRTMIN + 2)
#define SIGRT_3 (__SIGRTMIN + 3)
#define SIGRT_4 (__SIGRTMIN + 4)
#define SIGRT_5 (__SIGRTMIN + 5)
#define SIGRT_5 (__SIGRTMIN + 5)
#define SIGRT_6 (__SIGRTMIN + 6)
#define SIGRT_7 (__SIGRTMIN + 7)
#define SIGRT_8 (__SIGRTMIN + 8)
#define SIGRT_9 (__SIGRTMIN + 9)
#define SIGRT_10 (__SIGRTMIN + 10)
#define SIGRT_11 (__SIGRTMIN + 11)
#define SIGRT_12 (__SIGRTMIN + 12)
#define SIGRT_13 (__SIGRTMIN + 13)
#define SIGRT_14 (__SIGRTMIN + 14)
#define SIGRT_15 (__SIGRTMIN + 15)
#define SIGRT_15 (__SIGRTMIN + 15)
#define SIGRT_16 (__SIGRTMIN + 16)
#define SIGRT_17 (__SIGRTMIN + 17)
#define SIGRT_18 (__SIGRTMIN + 18)
#define SIGRT_19 (__SIGRTMIN + 19)
#define SIGRT_20 (__SIGRTMIN + 20)
#define SIGRT_20 (__SIGRTMIN + 20)
#define SIGRT_21 (__SIGRTMIN + 21)
#define SIGRT_22 (__SIGRTMIN + 22)
#define SIGRT_23 (__SIGRTMIN + 23)
#define SIGRT_24 (__SIGRTMIN + 24)
#define SIGRT_25 (__SIGRTMIN + 25)
#define SIGRT_25 (__SIGRTMIN + 25)
#define SIGRT_26 (__SIGRTMIN + 26)
#define SIGRT_27 (__SIGRTMIN + 27)
#define SIGRT_28 (__SIGRTMIN + 28)
#define SIGRT_29 (__SIGRTMIN + 29)
#define SIGRT_30 (__SIGRTMIN + 30)
#define SIGRT_31 (__SIGRTMIN + 31)
#define SIGRT_32 (__SIGRTMIN + 32)
/*
 * NOTE: Native signals SIGRT_33 ... SIGRTMAX
 * can't be used by a lib-portable application.
 */

#define SA_NOCLDSTOP_PORTABLE   0x00000001
#define SA_NOCLDWAIT_PORTABLE   0x00000002
#define SA_SIGINFO_PORTABLE     0x00000004
#define SA_THIRTYTWO_PORTABLE   0x02000000
#define SA_RESTORER_PORTABLE    0x04000000
#define SA_ONSTACK_PORTABLE     0x08000000
#define SA_RESTART_PORTABLE     0x10000000
#define SA_NODEFER_PORTABLE     0x40000000
#define SA_RESETHAND_PORTABLE   0x80000000

#define SA_NOMASK_PORTSBLE      SA_NODEFER_PORTABLE
#define SA_ONESHOT_PORTABLE     SA_RESETHAND_PORABLE


#include <asm-generic/signal_portable.h>

typedef __signalfn_t __user *__sighandler_portable_t;
typedef void (*__sigaction_handler_portable_t)(int, struct siginfo *, void *);

struct sigaction_portable {
 union {
   __sighandler_portable_t        _sa_handler;
   __sigaction_handler_portable_t _sa_sigaction;
 } _u;
 sigset_portable_t sa_mask;
 unsigned long sa_flags;
 void (*sa_restorer)(void);
};

#define sa_handler_portable     _u._sa_handler
#define sa_sigaction_portable   _u._sa_sigaction

typedef struct sigaltstack_portable {
 void __user *ss_sp;
 int ss_flags;
 size_t ss_size;
} portable_stack_t;

#endif
