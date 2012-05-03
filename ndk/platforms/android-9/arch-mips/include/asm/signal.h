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
#ifndef _ASM_SIGNAL_H
#define _ASM_SIGNAL_H
#include <linux/types.h>
#define _NSIG 128
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NSIG 128
#define _NSIG_BPW (sizeof(unsigned long) * 8)
#define _NSIG_WORDS (_NSIG / _NSIG_BPW)
typedef struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long sig[_NSIG_WORDS];
} sigset_t;
typedef unsigned long old_sigset_t;
#define SIGHUP 1  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SIGINT 2  
#define SIGQUIT 3  
#define SIGILL 4  
#define SIGTRAP 5  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SIGIOT 6  
#define SIGABRT SIGIOT  
#define SIGEMT 7
#define SIGFPE 8  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SIGKILL 9  
#define SIGBUS 10  
#define SIGSEGV 11  
#define SIGSYS 12
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SIGPIPE 13  
#define SIGALRM 14  
#define SIGTERM 15  
#define SIGUSR1 16  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SIGUSR2 17  
#define SIGCHLD 18  
#define SIGCLD SIGCHLD  
#define SIGPWR 19  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SIGWINCH 20  
#define SIGURG 21  
#define SIGIO 22  
#define SIGPOLL SIGIO  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SIGSTOP 23  
#define SIGTSTP 24  
#define SIGCONT 25  
#define SIGTTIN 26  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SIGTTOU 27  
#define SIGVTALRM 28  
#define SIGPROF 29  
#define SIGXCPU 30  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SIGXFSZ 31  
#define SIGRTMIN 32
#define SIGRTMAX _NSIG
#define SA_ONSTACK 0x08000000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SA_RESETHAND 0x80000000
#define SA_RESTART 0x10000000
#define SA_SIGINFO 0x00000008
#define SA_NODEFER 0x40000000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SA_NOCLDWAIT 0x00010000
#define SA_NOCLDSTOP 0x00000001
#define SA_NOMASK SA_NODEFER
#define SA_ONESHOT SA_RESETHAND
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SA_RESTORER 0x04000000  
#define SS_ONSTACK 1
#define SS_DISABLE 2
#define MINSIGSTKSZ 2048
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SIGSTKSZ 8192
struct siginfo;
#define SIG_BLOCK 1  
#define SIG_UNBLOCK 2  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SIG_SETMASK 3  
#include <asm-generic/signal.h>
struct sigaction {
 unsigned int sa_flags;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 union {
 __sighandler_t sa_handler;
 void (*sa_sigaction) (int, struct siginfo *, void *);
 } __sigaction_handler;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define sa_handler __sigaction_handler.sa_handler
#define sa_sigaction __sigaction_handler.sa_sigaction
 sigset_t sa_mask;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct k_sigaction {
 struct sigaction sa;
};
typedef struct sigaltstack {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 void *ss_sp;
 size_t ss_size;
 int ss_flags;
} stack_t;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
