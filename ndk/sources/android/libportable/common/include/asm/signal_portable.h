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

#define NSIG_PORTABLE 32
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

#define SIGRTMIN_PORTABLE 32
#define SIGRTMAX_PORTABLE _NSIG_PORTABLE
#define SIGSWI_PORTABLE 32

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
