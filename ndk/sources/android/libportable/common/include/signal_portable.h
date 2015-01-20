/*
 * Copyright 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _SIGNAL_PORTABLE_H_
#define _SIGNAL_PORTABLE_H_

#if (__mips__)

#include <portability.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>

struct stack_t_portable {
  void *ss_sp;
  int ss_flags;
  size_t ss_size;
};

static inline void stack_t_pton(const struct stack_t_portable *ptr_p, stack_t *ptr_n) {
  memset(ptr_n, '\0', sizeof(stack_t));
  ptr_n->ss_sp    = ptr_p->ss_sp;
  ptr_n->ss_flags = ptr_p->ss_flags;
  ptr_n->ss_size  = ptr_p->ss_size;
}

static inline void stack_t_ntop(const stack_t *ptr_n, struct stack_t_portable *ptr_p) {
  memset(ptr_p, '\0', sizeof(struct stack_t_portable));
  ptr_p->ss_sp    = ptr_n->ss_sp;
  ptr_p->ss_flags = ptr_n->ss_flags;
  ptr_p->ss_size  = ptr_n->ss_size;
}

int WRAP(sigaltstack)(const struct stack_t_portable *ss, struct stack_t_portable *oss) {
  stack_t ss_n, oss_n;
  if (ss != NULL) {
    stack_t_pton(ss, &ss_n);
    if (oss != NULL){
      int ret = REAL(sigaltstack)(&ss_n, &oss_n);
      stack_t_ntop(&oss_n, oss);
      return ret;
    }
    else
      return REAL(sigaltstack)(&ss_n, NULL);
  }
  else if (oss != NULL) {
    int ret = REAL(sigaltstack)(NULL, &oss_n);
    stack_t_ntop(&oss_n, oss);
    return ret;
  }
  else
    return REAL(sigaltstack)(NULL, NULL);
}

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
// unsupported in MIPS
#define SIGSTKFLT_PORTABLE 16
//
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
#define SIGPOLL_PORTABLE SIGIO_PORTABLE
#define SIGPWR_PORTABLE 30
#define SIGSYS_PORTABLE 31
#define SIGUNUSED_PORTABLE 31
// unsupported in MIPS
#define SIGSWI_PORTABLE 32
//

static inline int signo_pton(int signum_p) {
  switch(signum_p) {
    case SIGHUP_PORTABLE: return SIGHUP;
    case SIGINT_PORTABLE: return SIGINT;
    case SIGQUIT_PORTABLE: return SIGQUIT;
    case SIGILL_PORTABLE: return SIGILL;
    case SIGTRAP_PORTABLE: return SIGTRAP;
    case SIGABRT_PORTABLE: return SIGABRT;
    case SIGBUS_PORTABLE: return SIGBUS;
    case SIGFPE_PORTABLE: return SIGFPE;
    case SIGKILL_PORTABLE: return SIGKILL;
    case SIGUSR1_PORTABLE: return SIGUSR1;
    case SIGSEGV_PORTABLE: return SIGSEGV;
    case SIGUSR2_PORTABLE: return SIGUSR2;
    case SIGPIPE_PORTABLE: return SIGPIPE;
    case SIGALRM_PORTABLE: return SIGALRM;
    case SIGTERM_PORTABLE: return SIGTERM;
    case SIGCHLD_PORTABLE: return SIGCHLD;
    case SIGCONT_PORTABLE: return SIGCONT;
    case SIGSTOP_PORTABLE: return SIGSTOP;
    case SIGTSTP_PORTABLE: return SIGTSTP;
    case SIGTTIN_PORTABLE: return SIGTTIN;
    case SIGTTOU_PORTABLE: return SIGTTOU;
    case SIGURG_PORTABLE: return SIGURG;
    case SIGXCPU_PORTABLE: return SIGXCPU;
    case SIGXFSZ_PORTABLE: return SIGXFSZ;
    case SIGVTALRM_PORTABLE: return SIGVTALRM;
    case SIGPROF_PORTABLE: return SIGPROF;
    case SIGWINCH_PORTABLE: return SIGWINCH;
    case SIGIO_PORTABLE: return SIGIO;
    case SIGPWR_PORTABLE: return SIGPWR;
    case SIGSYS_PORTABLE: return SIGSYS;
    default:
      fprintf(stderr, "Unknown SIGNAL:%d\n", signum_p);
      abort();
  }
}

static inline int signo_ntop(int signum_n) {
  switch(signum_n) {
    case SIGHUP: return SIGHUP_PORTABLE;
    case SIGINT: return SIGINT_PORTABLE;
    case SIGQUIT: return SIGQUIT_PORTABLE;
    case SIGILL: return SIGILL_PORTABLE;
    case SIGTRAP: return SIGTRAP_PORTABLE;
    case SIGABRT: return SIGABRT_PORTABLE;
    case SIGBUS: return SIGBUS_PORTABLE;
    case SIGFPE: return SIGFPE_PORTABLE;
    case SIGKILL: return SIGKILL_PORTABLE;
    case SIGUSR1: return SIGUSR1_PORTABLE;
    case SIGSEGV: return SIGSEGV_PORTABLE;
    case SIGUSR2: return SIGUSR2_PORTABLE;
    case SIGPIPE: return SIGPIPE_PORTABLE;
    case SIGALRM: return SIGALRM_PORTABLE;
    case SIGTERM: return SIGTERM_PORTABLE;
    case SIGCHLD: return SIGCHLD_PORTABLE;
    case SIGCONT: return SIGCONT_PORTABLE;
    case SIGSTOP: return SIGSTOP_PORTABLE;
    case SIGTSTP: return SIGTSTP_PORTABLE;
    case SIGTTIN: return SIGTTIN_PORTABLE;
    case SIGTTOU: return SIGTTOU_PORTABLE;
    case SIGURG: return SIGURG_PORTABLE;
    case SIGXCPU: return SIGXCPU_PORTABLE;
    case SIGXFSZ: return SIGXFSZ_PORTABLE;
    case SIGVTALRM: return SIGVTALRM_PORTABLE;
    case SIGPROF: return SIGPROF_PORTABLE;
    case SIGWINCH: return SIGWINCH_PORTABLE;
    case SIGIO: return SIGIO_PORTABLE;
    case SIGPWR: return SIGPWR_PORTABLE;
    case SIGSYS: return SIGSYS_PORTABLE;
    default:
      fprintf(stderr, "Unknown SIGNAL:%d\n", signum_n);
      abort();
  }
}

#define SA_NOCLDSTOP_PORTABLE 0x00000001
#define SA_NOCLDWAIT_PORTABLE 0x00000002
#define SA_SIGINFO_PORTABLE 0x00000004
// unsupported in MIPS
#define SA_THIRTYTWO_PORTABLE 0x02000000
#define SA_RESTORER_PORTABLE 0x04000000
//
#define SA_ONSTACK_PORTABLE 0x08000000
#define SA_RESTART_PORTABLE 0x10000000
#define SA_NODEFER_PORTABLE 0x40000000
#define SA_RESETHAND_PORTABLE 0x80000000
#define SA_NOMASK_PORTABLE SA_NODEFER_PORTABLE
#define SA_ONESHOT_PORTABLE SA_RESETHAND_PORTABLE

static inline int sa_flags_pton(int sa_flags_p) {
  int sa_flags_n = 0;
  sa_flags_n |= (sa_flags_p & SA_NOCLDSTOP_PORTABLE) ? SA_NOCLDSTOP : 0;
  sa_flags_n |= (sa_flags_p & SA_NOCLDWAIT_PORTABLE) ? SA_NOCLDWAIT : 0;
  sa_flags_n |= (sa_flags_p & SA_SIGINFO_PORTABLE) ? SA_SIGINFO : 0;
  sa_flags_n |= (sa_flags_p & SA_ONSTACK_PORTABLE) ? SA_ONSTACK : 0;
  sa_flags_n |= (sa_flags_p & SA_RESTART_PORTABLE) ? SA_RESTART : 0;
  sa_flags_n |= (sa_flags_p & SA_NODEFER_PORTABLE) ? SA_NODEFER : 0;
  sa_flags_n |= (sa_flags_p & SA_RESETHAND_PORTABLE) ? SA_RESETHAND : 0;
  return sa_flags_n;
}

static inline int sa_flags_ntop(int sa_flags_n) {
  int sa_flags_p = 0;
  sa_flags_p |= (sa_flags_n & SA_NOCLDSTOP) ? SA_NOCLDSTOP_PORTABLE : 0;
  sa_flags_p |= (sa_flags_n & SA_NOCLDWAIT) ? SA_NOCLDWAIT_PORTABLE : 0;
  sa_flags_p |= (sa_flags_n & SA_SIGINFO) ? SA_SIGINFO_PORTABLE : 0;
  sa_flags_p |= (sa_flags_n & SA_ONSTACK) ? SA_ONSTACK_PORTABLE : 0;
  sa_flags_p |= (sa_flags_n & SA_RESTART) ? SA_RESTART_PORTABLE : 0;
  sa_flags_p |= (sa_flags_n & SA_NODEFER) ? SA_NODEFER_PORTABLE : 0;
  sa_flags_p |= (sa_flags_n & SA_RESETHAND) ? SA_RESETHAND_PORTABLE : 0;
  return sa_flags_p;
}

typedef unsigned long  sigset_t_portable;
struct sigaction_portable {
  union {
    __sighandler_t _sa_handler;
    void (*_sa_sigaction)(int, struct siginfo *, void *);
  } _u;
  sigset_t_portable sa_mask;
  unsigned long sa_flags;
  void (*sa_restorer)(void); // obsolete
};

static inline void sigset_t_pton(const sigset_t_portable *ptr_p, sigset_t *ptr_n) {
  memset(ptr_n, '\0', sizeof(sigset_t));
  ptr_n->sig[0] = *ptr_p;
}

static inline void sigset_t_ntop(const sigset_t *ptr_n, sigset_t_portable *ptr_p) {
  memset(ptr_p, '\0', sizeof(sigset_t_portable));
  *ptr_p = ptr_n->sig[0];
}

static inline void sigaction_pton(const struct sigaction_portable *ptr_p, struct sigaction *ptr_n) {
  memset(ptr_n, '\0', sizeof(struct sigaction));
  ptr_n->sa_sigaction = ptr_p->_u._sa_sigaction;
  sigset_t_pton(&ptr_p->sa_mask, &ptr_n->sa_mask);
  ptr_n->sa_flags       = sa_flags_pton(ptr_p->sa_flags);
}

static inline void sigaction_ntop(const struct sigaction *ptr_n, struct sigaction_portable *ptr_p) {
  memset(ptr_p, '\0', sizeof(struct sigaction_portable));
  ptr_p->_u._sa_sigaction = ptr_n->sa_sigaction;
  sigset_t_ntop(&ptr_n->sa_mask, &ptr_p->sa_mask);
  ptr_p->sa_flags      = sa_flags_ntop(ptr_n->sa_flags);
}

int WRAP(sigaction)(int signum, const struct sigaction_portable *act, struct sigaction_portable *oldact) {
  struct sigaction act_n, oldact_n;
  int signum_n = signo_pton(signum);

  if (act != NULL) {
    sigaction_pton(act, &act_n);
    if (oldact != NULL) {
      int ret = REAL(sigaction)(signum_n, &act_n, &oldact_n);
      sigaction_ntop(&oldact_n, oldact);
      return ret;
    }
    else
      return REAL(sigaction)(signum_n, &act_n, NULL);
  }
  else if (oldact != NULL) {
    int ret = REAL(sigaction)(signum_n, NULL, &oldact_n);
    sigaction_ntop(&oldact_n, oldact);
    return ret;
  }
  else
    return REAL(sigaction)(signum_n, NULL, NULL);
}

int WRAP(sigaddset)(sigset_t_portable *set, int signum) {
  int signum_n = signo_pton(signum);
  sigset_t set_n;
  sigset_t_pton(set, &set_n);
  int ret = REAL(sigaddset)(&set_n, signum_n);
  sigset_t_ntop(&set_n, set);
  return ret;
}

int WRAP(sigdelset)(sigset_t_portable *set, int signum) {
  int signum_n = signo_pton(signum);
  sigset_t set_n;
  sigset_t_pton(set, &set_n);
  int ret = REAL(sigdelset)(&set_n, signum_n);
  sigset_t_ntop(&set_n, set);
  return ret;
}

int WRAP(sigemptyset)(sigset_t_portable *set){
  sigset_t set_n;
  sigset_t_pton(set, &set_n);
  int ret = REAL(sigemptyset)(&set_n);
  sigset_t_ntop(&set_n, set);
  return ret;
}

int WRAP(sigfillset)(sigset_t_portable *set){
  sigset_t set_n;
  sigset_t_pton(set, &set_n);
  int ret = REAL(sigfillset)(&set_n);
  sigset_t_ntop(&set_n, set);
  return ret;
}

int WRAP(sigismember)(const sigset_t_portable *set, int signum) {
  int signum_n = signo_pton(signum);
  sigset_t set_n;
  sigset_t_pton(set, &set_n);
  return REAL(sigismember)(&set_n, signum_n);
}

int WRAP(sigpending)(sigset_t_portable *set) {
  sigset_t set_n;
  sigset_t_pton(set, &set_n);
  int ret = REAL(sigpending)(&set_n);
  sigset_t_ntop(&set_n, set);
  return ret;
}

#define SIG_BLOCK_PORTABLE 0
#define SIG_UNBLOCK_PORTABLE 1
#define SIG_SETMASK_PORTABLE 2

int WRAP(sigprocmask)(int how, const sigset_t_portable *set, sigset_t_portable *oldset) {
  int how_n;
  switch(how) {
    case SIG_BLOCK_PORTABLE: how_n =  SIG_BLOCK; break;
    case SIG_UNBLOCK_PORTABLE: how_n = SIG_UNBLOCK; break;
    case SIG_SETMASK_PORTABLE: how_n = SIG_SETMASK; break;
    default:
      fprintf(stderr, "Unknown sigprocmask action:%d\n", how);
      abort();
  }
  sigset_t set_n, oldset_n;
  if (set != NULL) {
    sigset_t_pton(set, &set_n);
    if (oldset != NULL) {
      int ret = REAL(sigprocmask)(how_n, &set_n, &oldset_n);
      sigset_t_ntop(&oldset_n, oldset);
      return ret;
    }
    else
      return REAL(sigprocmask)(how_n, &set_n, NULL);
  }
  else if (oldset != NULL) {
    int ret = REAL(sigprocmask)(how_n, NULL, &oldset_n);
    sigset_t_ntop(&oldset_n, oldset);
    return ret;
  }
  else
    return REAL(sigprocmask)(how_n, NULL, NULL);
}

int WRAP(sigsuspend)(const sigset_t_portable *mask) {
  sigset_t mask_n;
  sigset_t_pton(mask, &mask_n);
  return REAL(sigsuspend)(&mask_n);
}

int WRAP(sigwait)(const sigset_t_portable *set, int *sig) {
  sigset_t set_n;
  sigset_t_pton(set, &set_n);
  int ret = REAL(sigwait)(&set_n, sig);
  *sig = signo_ntop(*sig);
  return ret;
}

int WRAP(kill)(pid_t pid, int sig) {
  int sig_n = signo_pton(sig);
  return REAL(kill)(pid, sig_n);
}

// sigset_t related function
#include <sys/select.h>
int WRAP(pselect)(int nfds, fd_set *readfds, fd_set *writefds, fd_set *exceptfds, const struct timespec *timeout, const sigset_t_portable *sigmask) {
  sigset_t sigmask_n;
  sigset_t_pton(sigmask, &sigmask_n);
  return  REAL(pselect)(nfds, readfds, writefds, exceptfds, timeout, sigmask_n);
}

#include <sys/signalfd.h>
int WRAP(signalfd)(int fd, const sigset_t_portable* mask, int flags) {
  sigset_t mask_n;
  sigset_t_pton(mask, &mask_n);
  return REAL(signalfd)(fd, mask_n, flags);
}

#include <poll.h>
int WRAP(ppoll)(struct pollfd *fds, nfds_t nfds, const struct timespec *timeout_ts, const sigset_t_portable *sigmask) {
  sigset_t sigmask_n;
  sigset_t_pton(sigmask, &sigmask_n);
  return REAL(ppoll)(fds, nfds, timeout_ts, sigmask_n);
}

#include <pthread.h>
int WRAP(pthread_sigmask)(int how, const sigset_t_portable *set, sigset_t_portable *oldset) {
  int how_n;
  switch(how) {
    case SIG_BLOCK_PORTABLE: how_n =  SIG_BLOCK; break;
    case SIG_UNBLOCK_PORTABLE: how_n = SIG_UNBLOCK; break;
    case SIG_SETMASK_PORTABLE: how_n = SIG_SETMASK; break;
    default:
      fprintf(stderr, "Unknown pthread_sigmask action:%d\n", how);
      abort();
  }
  sigset_t set_n, oldset_n;
  if (set != NULL) {
    sigset_t_pton(set, &set_n);
    if (oldset != NULL) {
      int ret = REAL(pthread_sigmask)(how_n, &set_n, &oldset_n);
      sigset_t_ntop(&oldset_n, oldset);
      return ret;
    }
    else
      return REAL(pthread_sigmask)(how_n, &set_n, NULL);
  }
  else if (oldset != NULL) {
    int ret = REAL(pthread_sigmask)(how_n, NULL, &oldset_n);
    sigset_t_ntop(&oldset_n, oldset);
    return ret;
  }
  else
    return REAL(pthread_sigmask)(how_n, NULL, NULL);
}

#include <sys/epoll.h>
int WRAP(epoll_pwait)(int fd, struct epoll_event* events, int max_events, int timeout, const sigset_t_portable* ss) {
  sigset_t ss_n;
  sigset_t_pton(ss, &ss_n);
  return REAL(epoll_pwait)(fd, events, max_events, timeout, ss_n);
}
#endif /* __mips__ */
#endif /* _SIGNAL_PORTABLE_H */
