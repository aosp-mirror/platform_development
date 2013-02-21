/*
 * Copyright 2012, The Android Open Source Project
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

#include <stdarg.h>
#include <stdlib.h>
#include <signal.h>
#include <signal_portable.h>
#include <portability.h>
#include <stdio.h>
#include <errno.h>
#include <errno_portable.h>

#define PORTABLE_TAG "signal_portable"
#include <log_portable.h>


#if SIGBUS_PORTABLE == SIGBUS
#error Bad build environment
#endif

/*
 * The next five hidden functions are not exposed in the
 * libportable shared object. They are used here and other
 * functions, like waitpid(), which need to map signal numbers.
 */
__hidden char *map_portable_signum_to_name(int portable_signum)
{
    char *name;

    switch(portable_signum) {
    case SIGHUP_PORTABLE:       name = "SIGHUP_PORTABLE:1";             break;
    case SIGINT_PORTABLE:       name = "SIGINT_PORTABLE:2";             break;
    case SIGQUIT_PORTABLE:      name = "SIGQUIT_PORTABLE:3";            break;
    case SIGILL_PORTABLE:       name = "SIGILL_PORTABLE:4";             break;
    case SIGTRAP_PORTABLE:      name = "SIGTRAP_PORTABLE:5";            break;
    case SIGABRT_PORTABLE:      name = "SIGABRT_PORTABLE:6";            break;
    case SIGBUS_PORTABLE:       name = "SIGBUS_PORTABLE:7";             break;
    case SIGFPE_PORTABLE:       name = "SIGFPE_PORTABLE:8";             break;
    case SIGKILL_PORTABLE:      name = "SIGKILL_PORTABLE:9";            break;
    case SIGUSR1_PORTABLE:      name = "SIGUSR1_PORTABLE:10";           break;
    case SIGSEGV_PORTABLE:      name = "SIGSEGV_PORTABLE:11";           break;
    case SIGUSR2_PORTABLE:      name = "SIGUSR2_PORTABLE:12";           break;
    case SIGPIPE_PORTABLE:      name = "SIGPIPE_PORTABLE:13";           break;
    case SIGALRM_PORTABLE:      name = "SIGALRM_PORTABLE:14";           break;
    case SIGTERM_PORTABLE:      name = "SIGTERM_PORTABLE:15";           break;
    case SIGSTKFLT_PORTABLE:    name = "SIGSTKFLT_PORTABLE:16";         break;
    case SIGCHLD_PORTABLE:      name = "SIGCHLD_PORTABLE:17";           break;
    case SIGCONT_PORTABLE:      name = "SIGCONT_PORTABLE:18";           break;
    case SIGSTOP_PORTABLE:      name = "SIGSTOP_PORTABLE:19";           break;
    case SIGTSTP_PORTABLE:      name = "SIGTSTP_PORTABLE:20";           break;
    case SIGTTIN_PORTABLE:      name = "SIGTTIN_PORTABLE:21";           break;
    case SIGTTOU_PORTABLE:      name = "SIGTTOU_PORTABLE:22";           break;
    case SIGURG_PORTABLE:       name = "SIGURG_PORTABLE:23";            break;
    case SIGXCPU_PORTABLE:      name = "SIGXCPU_PORTABLE:24";           break;
    case SIGXFSZ_PORTABLE:      name = "SIGXFSZ_PORTABLE:25";           break;
    case SIGVTALRM_PORTABLE:    name = "SIGVTALRM_PORTABLE:26";         break;
    case SIGPROF_PORTABLE:      name = "SIGPROF_PORTABLE:27";           break;
    case SIGWINCH_PORTABLE:     name = "SIGWINCH_PORTABLE:28";          break;
    case SIGIO_PORTABLE:        name = "SIGIO_PORTABLE:29";             break;
    case SIGPWR_PORTABLE:       name = "SIGPWR_PORTABLE:30";            break;
    case SIGSYS_PORTABLE:       name = "SIGSYS_PORTABLE:31";            break;
    case SIGRTMIN_PORTABLE:     name = "SIGRTMIN_PORTABLE:32";          break;
    default:                    name = "<<UNKNOWN>>";                   break;
    }
    return name;
}


__hidden char *map_mips_signum_to_name(int mips_signum)
{
    char *name;

    switch(mips_signum) {
    case SIGHUP:        name = "SIGHUP:1";      break;
    case SIGINT:        name = "SIGINT:2";      break;
    case SIGQUIT:       name = "SIGQUIT:3";     break;
    case SIGILL:        name = "SIGILL:4";      break;
    case SIGTRAP:       name = "SIGTRAP:5";     break;
    case SIGIOT:        name = "SIGIOT:6";      break;
    case SIGEMT:        name = "SIGEMT:7";      break;
    case SIGFPE:        name = "SIGFPE:8";      break;
    case SIGKILL:       name = "SIGKILL:9";     break;
    case SIGBUS:        name = "SIGBUS:10";     break;
    case SIGSEGV:       name = "SIGSEGV:11";    break;
    case SIGSYS:        name = "SIGSYS:12";     break;
    case SIGPIPE:       name = "SIGPIPE:13";    break;
    case SIGALRM:       name = "SIGALRM:14";    break;
    case SIGTERM:       name = "SIGTERM:15";    break;
    case SIGUSR1:       name = "SIGUSR1:16";    break;
    case SIGUSR2:       name = "SIGUSR2:17";    break;
    case SIGCHLD:       name = "SIGCHLD:18";    break;
    case SIGPWR:        name = "SIGPWR:19";     break;
    case SIGWINCH:      name = "SIGWINCH:20";   break;
    case SIGURG:        name = "SIGURG:21";     break;
    case SIGIO:         name = "SIGIO:22";      break;
    case SIGSTOP:       name = "SIGSTOP:23";    break;
    case SIGTSTP:       name = "SIGTSTP:24";    break;
    case SIGCONT:       name = "SIGCONT:25";    break;
    case SIGTTIN:       name = "SIGTTIN:26";    break;
    case SIGTTOU:       name = "SIGTTOU:27";    break;
    case SIGVTALRM:     name = "SIGVTALRM:28";  break;
    case SIGPROF:       name = "SIGPROF:29";    break;
    case SIGXCPU:       name = "SIGXCPU:30";    break;
    case SIGXFSZ:       name = "SIGXFSZ:31";    break;

    case SIGRTMIN:      name = "SIGRTMIN:32";   break;
    default:            name = "<<UNKNOWN>>";   break;
    }
    return name;
}


/*
 * Maps a signal number from portable to native.
 */
__hidden int signum_pton(int portable_signum)
{
    int mips_signum = -1;

    switch(portable_signum) {
    case SIGHUP_PORTABLE:               /* 1 */
        return SIGHUP;

    case SIGINT_PORTABLE:               /* 2 */
        return SIGINT;

    case SIGQUIT_PORTABLE:              /* 3 */
        return SIGQUIT;

    case SIGILL_PORTABLE:               /* 4 */
        return SIGILL;

    case SIGTRAP_PORTABLE:              /* 5 */
        return SIGTRAP;

    case SIGABRT_PORTABLE:              /* 6 */
        return SIGABRT;

    case SIGBUS_PORTABLE:               /* 7 --> 10 */
        return SIGBUS;

    case SIGFPE_PORTABLE:               /* 8 */
        return SIGFPE;

    case SIGKILL_PORTABLE:              /* 9 */
        return SIGKILL;

    case SIGUSR1_PORTABLE:              /* 10 --> 16 */
        return SIGUSR1;

    case SIGSEGV_PORTABLE:              /* 11 */
        return SIGSEGV;

    case SIGUSR2_PORTABLE:              /* 12 --> 17 */
        return SIGUSR2;

    case SIGPIPE_PORTABLE:              /* 13 */
        return SIGPIPE;

    case SIGALRM_PORTABLE:              /* 14 */
        return SIGALRM;

    case SIGTERM_PORTABLE:              /* 15 */
        return SIGTERM;

    case SIGSTKFLT_PORTABLE:            /* 16 --> 7 */
        return SIGEMT;                  /* No native SIGSTKFLT exist  ...
                                           ... mapping it to SIGEMT. */

    case SIGCHLD_PORTABLE:              /* 17 --> 18 */
        return SIGCHLD;

    case SIGCONT_PORTABLE:
        return SIGCONT;                 /* 18 --> 25 */

    case SIGSTOP_PORTABLE:              /* 19 --> 23 */
        return SIGSTOP;

    case SIGTSTP_PORTABLE:              /* 20 --> 24 */
        return SIGTSTP;

    case SIGTTIN_PORTABLE:              /* 21 --> 26 */
        return SIGTTIN;

    case SIGTTOU_PORTABLE:              /* 22 --> 27 */
        return SIGTTOU;

    case SIGURG_PORTABLE:               /* 23 --> 21 */
        return SIGURG;

    case SIGXCPU_PORTABLE:              /* 24 --> 30 */
        return SIGXCPU;

    case SIGXFSZ_PORTABLE:              /* 25 --> 31 */
        return SIGXFSZ;

    case SIGVTALRM_PORTABLE:            /* 26 --> 28 */
        return SIGVTALRM;

    case SIGPROF_PORTABLE:              /* 27 --> 29 */
        return SIGPROF;

    case SIGWINCH_PORTABLE:             /* 28 --> 20 */
        return SIGWINCH;

    case SIGIO_PORTABLE:                /* 29 --> 22 */
        return SIGIO;

    case SIGPWR_PORTABLE:               /* 30 --> 19 */
        return SIGPWR;

    case SIGSYS_PORTABLE:               /* 31 --> 12 */
        return SIGSYS;

    case SIGRTMIN_PORTABLE:             /* 32 */
        return SIGRTMIN;

    default:
        ALOGE("%s: switch default: NOTE portable_signum:%d Not supported. Just a Test?",
              __func__,                 portable_signum);
        /*
         * User could be LTP testing with bogus signal numbers,
         * if so we mimic the test.
         *
         * If the signal is just outside the PORTABLE range
         * we use a signal just outside the MIPS range.
         */
        if (portable_signum < 0) {
            mips_signum = portable_signum;
        } else if (portable_signum > NSIG_PORTABLE) {
            mips_signum = (portable_signum - NSIG_PORTABLE) +  NSIG;
        } else {
            ALOGE("%s: 0 <= portable_signum:%d <= NSIG_PORTABLE:%d; Not supported, return(0);",
                  __func__, portable_signum,      NSIG_PORTABLE);

            mips_signum = 0;
        }
        break;
    }
    ALOGV("%s(portable_signum:%d): return(mips_signum:%d);", __func__,
              portable_signum,            mips_signum);

    return mips_signum;
}


/*
 * Maps a signal number from native to portable.
 */
__hidden int signum_ntop(int mips_signum)
{
    int portable_ssignum = -1;

    switch(mips_signum) {
    case SIGHUP:                        /* 1 */
        return SIGHUP_PORTABLE;

    case SIGINT:                        /* 2 */
        return SIGINT_PORTABLE;

    case SIGQUIT:                       /* 3 */
        return SIGQUIT_PORTABLE;

    case SIGILL:                        /* 4 */
        return SIGILL_PORTABLE;

    case SIGTRAP:                       /* 5 */
        return SIGTRAP_PORTABLE;

    case SIGABRT:                       /* 6 */
        return SIGABRT_PORTABLE;

    case SIGBUS:                        /* 7 <-- 10 */
        return SIGBUS_PORTABLE;

    case SIGFPE:                        /* 8 */
        return SIGFPE_PORTABLE;

    case SIGKILL:                       /* 9 */
        return SIGKILL_PORTABLE;

    case SIGUSR1:                       /* 10 <-- 16 */
        return SIGUSR1_PORTABLE;

    case SIGSEGV:                       /* 11 */
        return SIGSEGV_PORTABLE;

    case SIGUSR2:                       /* 12 <-- 17 */
        return SIGUSR2_PORTABLE;

    case SIGPIPE:                       /* 13 */
        return SIGPIPE_PORTABLE;

    case SIGALRM:                       /* 14 */
        return SIGALRM_PORTABLE;

    case SIGTERM_PORTABLE:              /* 15 */
        return SIGTERM;

    case SIGEMT:                        /* 16 <--- 7 */
        return SIGSTKFLT_PORTABLE;      /* No native SIGSTKFLT exist ...
                                           ... reverse mapping SIGEMT ...
                                           ...  back to SIGSTKFLT. */

    case SIGCHLD:                       /* 17 <-- 18 */
        return SIGCHLD_PORTABLE;

    case SIGCONT:                       /* 18 <-- 15 */
        return SIGCONT_PORTABLE;

    case SIGSTOP:                       /* 19 <-- 23 */
        return SIGSTOP_PORTABLE;

    case SIGTSTP:                       /* 20 <-- 24 */
        return SIGTSTP_PORTABLE;

    case SIGTTIN:                       /* 21 <-- 26 */
        return SIGTTIN_PORTABLE;

    case SIGTTOU:                       /* 22 <-- 27 */
        return SIGTTOU_PORTABLE;

    case SIGURG:                        /* 23 <-- 21 */
        return SIGURG_PORTABLE;

    case SIGXCPU:                       /* 24 <-- 30 */
        return SIGXCPU_PORTABLE;

    case SIGXFSZ:                       /* 25 <-- 31 */
        return SIGXFSZ_PORTABLE;

    case SIGVTALRM:                     /* 26 <-- 28 */
        return SIGVTALRM_PORTABLE;

    case SIGPROF:                       /* 27 <-- 29 */
        return SIGPROF_PORTABLE;

    case SIGWINCH:                      /* 28 <-- 20 */
        return SIGWINCH_PORTABLE;

    case SIGIO:                         /* 29 <-- 22 */
        return SIGIO_PORTABLE;

    case SIGPWR:                        /* 30 <-- 19 */
        return SIGPWR_PORTABLE;

    case SIGSYS:                        /* 31 <-- 12 */
        return SIGSYS_PORTABLE;

    case SIGRTMIN_PORTABLE:             /* 32 */
        return SIGRTMIN;

    default:
        ALOGE("%s: switch default: mips_signum:%d Not supported! return(0);", __func__,
                                   mips_signum);
#if 0
        LOG_FATAL("%s: mips_signum:%d is not portable;", __func__, mips_signum);
#endif
        return 0;
    }
    return portable_ssignum;
}


/*
 * Array of signal handlers as the portable users expects they
 * they have been registered in the kernel. Problem is we need
 * to have our own handler to map the MIPS signal number to a
 * portable signal number.
 */
sig3handler_portable_t mips_portable_sighandler[NSIG_PORTABLE] = { NULL };

static void mips_sigaction_handler(int mips_signum, siginfo_t *sip, void *ucp)
{
    int portable_signum;
    char *portable_signame;
    char *mips_signame = map_mips_signum_to_name(mips_signum);
    sig3handler_portable_t portable_sighandler;
    siginfo_portable_t portable_si;
    siginfo_portable_t *portable_sip;

    ALOGV(" ");
    ALOGV("%s(mips_signum:%d:'%s', sip:%p, ucp:%p) {", __func__,
              mips_signum,
              mips_signame,        sip,    ucp);

    portable_signum = signum_ntop(mips_signum);
    portable_signame = map_portable_signum_to_name(portable_signum);
    portable_sighandler = mips_portable_sighandler[portable_signum];

    ASSERT(portable_sighandler != NULL);
    ASSERT(portable_sighandler != (sig3handler_portable_t) SIG_DFL);
    ASSERT(portable_sighandler != (sig3handler_portable_t) SIG_IGN);
    ASSERT(portable_sighandler != (sig3handler_portable_t) SIG_ERR);

    if (sip == NULL) {
        portable_sip = NULL;
    } else {
        int portable_si_errno;
        int portable_si_signo;
        char *mips_si_signame;
        char *portable_si_signame;

        /*
         * Map Structure members from MIPS to Portable.
         */
        portable_si_signo = signum_ntop(sip->si_signo);
        portable_si_signame = map_portable_signum_to_name(portable_si_signo);
        portable_si_errno = ntop_errno(sip->si_errno);

        mips_si_signame = map_mips_signum_to_name(sip->si_signo);

        /*
         * Deal with siginfo structure being a bit different.
         * Default to the same structure members.
         */
        ASSERT(sizeof(siginfo_portable_t) == sizeof(siginfo_t));
        memcpy(&portable_si, sip, sizeof(portable_si));
        portable_si.si_signo = portable_si_signo;
        portable_si.si_code = sip->si_code;           /* code and errno are swapped and ... */
        portable_si.si_errno = portable_si_errno;     /* ... errno needs to be translated. */

        portable_sip = &portable_si;
    } /* if sip */

    portable_sighandler(portable_signum, portable_sip, ucp);

    ALOGV("%s: return; }", __func__);
}


static void mips_sighandler(int mips_signum)
{
    int portable_signum;
    char *portable_signame;
    char *mips_signame = map_mips_signum_to_name(mips_signum);
    sig3handler_portable_t portable_sighandler;

    ALOGV(" ");
    ALOGV("%s(mips_signum:%d:'%s') {", __func__, mips_signum, mips_signame);

    mips_sigaction_handler(mips_signum, NULL, NULL);

    ALOGV("%s: return; }", __func__);
}


static sighandler_t sighandler_pton(sighandler_portable_t portable_handler, int sigaction)
{
    sighandler_t mips_handler;

    ALOGV("%s(portable_handler:%p, sigaction:%d) {", __func__,
              portable_handler,    sigaction);

    switch((int) portable_handler) {
    case (int) SIG_DFL:
    case (int) SIG_IGN:
    case (int) SIG_ERR:
        mips_handler = portable_handler;
        break;

    default:
        if (sigaction)
            mips_handler = (sighandler_t) mips_sighandler;
        else
            mips_handler = (sighandler_t) mips_sigaction_handler;
        break;
    }

    ALOGV("%s: return(mips_handler:%p); }", __func__, mips_handler);
    return mips_handler;
}


/*
 * This function maps the signal number and calls one of the low level mips signal()
 * functions implemented in libc/unistd/signal.c:
 *              sysv_signal()
 *              bsd_signal()
 *
 * The last 2 parameters to this static function, mips_signal_fn*, specify which of
 * these functions to call.  We intercept the above to functions, as well as signal(),
 * functions below.
 *
 * In addition, we intercept the signal_handler with our own handlers that map the
 * signal number from the MIPS convention to the PORTABLE/ARM convention.
 */
static sighandler_portable_t
do_signal_portable(int portable_signum, sighandler_portable_t portable_handler,
                   __sighandler_t (mips_signal_fn)(int, __sighandler_t))
{
    char *portable_signame = map_portable_signum_to_name(portable_signum);
    int mips_signum;
    sighandler_t mips_handler;
    sighandler_portable_t rv;
    sighandler_portable_t prev_portable_handler;

    ALOGV("%s(portable_signum:%d:%s, portable_handler:%p,  mips_signal_fn:%p) {", __func__,
              portable_signum,
              portable_signame,      portable_handler,     mips_signal_fn);

    mips_signum = signum_pton(portable_signum);

    if ((portable_signum != 0) && ((mips_signum <= 0) || (mips_signum > NSIG))) {
        /*
         * Invalid request; Let the kernel generate the proper return value and set errno.
         */
        mips_handler = sighandler_pton(portable_handler, 0);
        rv = mips_signal_fn(mips_signum, mips_handler);
    } else {
        /*
         * We have a usable signal number, redirect it to our signal handler
         * if a portable handler was provided so we can convert the signal number.
         * Save our current mapped signal handler for likely return.
         */
        prev_portable_handler = (sighandler_portable_t) mips_portable_sighandler[portable_signum];

        mips_handler = sighandler_pton(portable_handler, 0);
        if (mips_handler != portable_handler) {
            mips_portable_sighandler[portable_signum] = (sig3handler_portable_t) portable_handler;
        }
        rv = mips_signal_fn(mips_signum, mips_handler);

        if ((rv == (sighandler_portable_t) mips_sighandler) ||
            (rv == (sighandler_portable_t) mips_sigaction_handler)) {

            rv = (sighandler_t) prev_portable_handler;
        }
    }

    ALOGV("%s: return(rv:%p); }", __func__, rv);
    return rv;
}


/*
 * signal() can't be called directly, due to an in-line function in signal.h which
 * redirects the call to bsd_signal(). _signal() is a static function; not to be called
 * directly. This function isn't actually needed.
 */
sighandler_portable_t signal_portable(int portable_signum, sighandler_portable_t handler)
{
    sighandler_portable_t rv;

    ALOGV(" ");
    ALOGV("%s(portable_signum:%d, handler:%p) {", __func__,
              portable_signum,    handler);

    /* bsd does a SA_RESTART */
    rv = do_signal_portable(portable_signum, handler, bsd_signal);

    ALOGV("%s: return(ret:%p); }", __func__, rv);
    return rv;
}


sighandler_portable_t sysv_signal_portable(int portable_signum, sighandler_portable_t handler)
{
    sighandler_portable_t rv;

    ALOGV(" ");
    ALOGV("%s(portable_signum:%d, handler:%p) {", __func__,
              portable_signum,    handler);

    /* sysv does a SA_RESETHAND */
    rv = do_signal_portable(portable_signum, handler, sysv_signal);

    ALOGV("%s: return(ret:%p); }", __func__, rv);
    return rv;
}


/*
 * NOTE:
 *    handler is either the Bionic
 *      bsd_signal() signal handler
 * or
 *      the sysv_signal() signal handler.
 */
sighandler_portable_t bsd_signal_portable(int portable_signum, sighandler_portable_t handler)
{
    sighandler_portable_t rv;

    ALOGV(" ");
    ALOGV("%s(portable_signum:%d, handler:%p) {", __func__,
              portable_signum,    handler);

    /* bsd does a SA_RESTART */
    rv = do_signal_portable(portable_signum, handler, bsd_signal);

    ALOGV("%s: return(ret:%p); }", __func__, rv);
    return rv;
}


static int do_kill(int id, int portable_signum, int (*fn)(int, int))
{
    char *portable_signame = map_portable_signum_to_name(portable_signum);
    int mips_signum;
    int rv;

    ALOGV("%s(id:%d, portable_signum:%d:'%s', fn:%p) {", __func__,
              id,    portable_signum,
                     portable_signame,        fn);

    mips_signum = signum_pton(portable_signum);

    rv =  fn(id, mips_signum);

    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}


int killpg_portable(int pgrp, int portable_signum)
{
    int rv;

    ALOGV(" ");
    ALOGV("%s(pgrp:%d, portable_signum:%d) {", __func__,
              pgrp,    portable_signum);

    rv = do_kill(pgrp, portable_signum, killpg);

    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}


int kill_portable(pid_t pid, int portable_signum)
{
    int rv;

    ALOGV(" ");
    ALOGV("%s(pid:%d, portable_signum:%d) {", __func__,
              pid,    portable_signum);

    rv = do_kill(pid, portable_signum, kill);

    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}


int tkill_portable(int tid, int portable_signum)
{
    extern int tkill(int, int);
    int rv;

    ALOGV(" ");
    ALOGV("%s(tid:%d, portable_signum:%d) {", __func__,
              tid,    portable_signum);

    rv = do_kill(tid, portable_signum, tkill);

    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}


/* tgkill is not exported from android-14 libc.so */
#if 0
int tgkill_portable(int tgid, int tid, int portable_signum)
{
    extern int tgkill(int, int, int);
    char *portable_signame = map_portable_signum_to_name(portable_signum);
    int mips_signum;
    int rv;

    ALOGV("%s(tgid:%d, tid:%d, portable_signum:%d:'%s') {", __func__,
              tgid,    tid,    portable_signum, portable_signame);

    mips_signum = signum_pton(portable_signum);

    if ((portable_signum != 0) && (mips_signum == 0))
        rv = 0;
    else
        rv = tgkill(tgid, tid, mips_signum);

    ALOGV("%s: return rv:%d; }", __func__, rv);
    return rv;
}
#endif


int raise_portable(int portable_signum)
{
    char *portable_signame = map_portable_signum_to_name(portable_signum);
    int mips_signum = signum_pton(portable_signum);
    int rv;

    ALOGV("%s(portable_signum:%d:'%s') {", __func__, portable_signum, portable_signame);

    if ((portable_signum != 0) && (mips_signum == 0))
        rv = 0;
    else
        rv = raise(mips_signum);

    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}


void sigset_pton(sigset_portable_t *portable_sigset, sigset_t *mips_sigset)
{
    int portable_signum;

    ASSERT(mips_sigset != NULL);

    ALOGV("%s(portable_sigset:%p, mips_sigset:%p) {", __func__,
              portable_sigset,    mips_sigset);

    sigemptyset(mips_sigset);
    if (invalid_pointer((void *)portable_sigset)) {
        ALOGE("%s: portable_sigset:%p is not valid; returning empty set.", __func__,
                   portable_sigset);
        goto done;
    }

    for(portable_signum = 1; portable_signum <= NSIG_PORTABLE; portable_signum++) {

        if (sigismember_portable(portable_sigset, portable_signum)) {
            char *portable_signame = map_portable_signum_to_name(portable_signum);
            int mips_signum = signum_pton(portable_signum);
            char *mips_signame;

            if (mips_signum != 0) {
                int err;

                mips_signame = map_mips_signum_to_name(mips_signum);
                ALOGV("%s: sigaddset(mips_sigset:%p, mips_signum:%d:'%s');", __func__,
                                     mips_sigset,    mips_signum,
                                                     mips_signame);

                err = sigaddset(mips_sigset, mips_signum);
                if (err == -1) {
                    PERROR("sigaddset");
                }
            }
        }
    }

done:
    ALOGV("%s: return; }", __func__);
    return;
}


void
sigset_ntop(const sigset_t *const_mips_sigset, sigset_portable_t *portable_sigset)
{
    int mips_signum;
    sigset_t *mips_sigset = (sigset_t *) const_mips_sigset;

    ALOGV("%s(const_mips_sigset:%p, portable_sigset:%p) {", __func__,
              const_mips_sigset,    portable_sigset);

    ASSERT(mips_sigset != NULL);

    if (invalid_pointer((void *)portable_sigset)) {
        ALOGE("%s: portable_sigset:%p is not Valid; can't return sigset", __func__,
                   portable_sigset);
        goto done;
    }
    sigemptyset_portable(portable_sigset);

    for(mips_signum = 1; mips_signum <= NSIG; mips_signum++) {
        if (sigismember(mips_sigset, mips_signum)) {
            int portable_signum = signum_ntop(mips_signum);

            if (portable_signum != 0)
                sigaddset_portable(portable_sigset, portable_signum);
        }
    }

done:
    ALOGV("%s: return; }", __func__);
    return;
}


static int sigaction_flags_pton(int portable_flags)
{
    int mips_flags = 0;

    if (portable_flags & SA_NOCLDSTOP_PORTABLE) {
        mips_flags |= SA_NOCLDSTOP;
    }
    if (portable_flags & SA_NOCLDWAIT_PORTABLE) {
        mips_flags |= SA_NOCLDWAIT;
    }
    if (portable_flags & SA_SIGINFO_PORTABLE) {
        mips_flags |= SA_SIGINFO;
    }
    if (portable_flags & SA_THIRTYTWO_PORTABLE) {
        ALOGV("%s: SA_THIRTYTWO_PORTABLE isn't SUPPORTED.", __func__);
    }
    if (portable_flags & SA_RESTORER_PORTABLE) {
        mips_flags |= SA_RESTORER;
    }
    if (portable_flags & SA_ONSTACK_PORTABLE) {
        mips_flags |= SA_ONSTACK;
    }
    if (portable_flags & SA_RESTART_PORTABLE) {
        mips_flags |= SA_RESTART;
    }
    if (portable_flags & SA_NODEFER_PORTABLE) {
        mips_flags |= SA_NODEFER;
    }
    if (portable_flags & SA_RESETHAND_PORTABLE) {
        mips_flags |= SA_RESETHAND;
    }

    ALOGV("%s(portable_flags:0x%x) return(mips_flags:0x%x);", __func__,
              portable_flags,             mips_flags);

    return mips_flags;
}


int sigaction_flags_ntop(int mips_flags)
{
    int portable_flags = 0;

    if (mips_flags & SA_NOCLDSTOP)      portable_flags |= SA_NOCLDSTOP_PORTABLE;
    if (mips_flags & SA_NOCLDWAIT)      portable_flags |= SA_NOCLDWAIT_PORTABLE;
    if (mips_flags & SA_SIGINFO)        portable_flags |= SA_SIGINFO_PORTABLE;
#ifdef SA_THIRTYTWO
    if (mips_flags & SA_THIRTYTWO)      portable_flags |= SA_THIRTYTWO_PORTABLE;
#endif
    if (mips_flags & SA_RESTORER)       portable_flags |= SA_RESTORER_PORTABLE;
    if (mips_flags & SA_ONSTACK)        portable_flags |= SA_ONSTACK_PORTABLE;
    if (mips_flags & SA_RESTART)        portable_flags |= SA_RESTART_PORTABLE;
    if (mips_flags & SA_NODEFER)        portable_flags |= SA_NODEFER_PORTABLE;
    if (mips_flags & SA_RESETHAND)      portable_flags |= SA_RESETHAND_PORTABLE;

    ALOGV("%s(mips_flags:0x%x) return(portable_flags:0x%x);", __func__,
              mips_flags,             portable_flags);

    return portable_flags;
}


/*
 * Called by portable/ARM code, which we map and do MIPS system calls.
 *
 * The incoming system call used a Portable/ARM sigaction structure:
 * ------------------------------------------------------------------
 *   struct sigaction_portable {
 *     union {
 *       __sighandler_portable_t        _sa_handler;
 *       __sigaction_handler_portable_t _sa_sigaction;
 *     } _u;
 *     sigset_portable_t sa_mask;
 *     unsigned long sa_flags;
 *     void (*sa_restorer)(void);
 * };
 *
 * A similar, but different, structure is used in the MIPS/Native system call:
 * ---------------------------------------------------------------------------
 *    struct sigaction {
 *      unsigned int sa_flags;
 *      union {
 *        __sighandler_t                  sa_handler;
 *        __sigaction_handler_portable_t _sa_sigaction;
 *      } __u;
 *      sigset_t sa_mask;
 *  };
 *
 * This sigaction structure needs to be mapped before the MIPS systems call as well as after for
 * returning the old/previous sigaction. Also, like signal_portable() above, we need to maintain
 * a table of signal handlers that our intercepting handler can call after it converts the signal
 * numbers.
 */
static int do_sigaction_portable(int portable_signum, const struct sigaction_portable *act,
                                 struct sigaction_portable *oldact)
{
    int mips_signum;
    char *mips_signame;
    struct sigaction mips_act;
    struct sigaction mips_oldact;
    sighandler_t mips_handler;
    sighandler_portable_t portable_handler;
    sig3handler_portable_t prev_portable_handler;
    char *portable_signame = map_portable_signum_to_name(portable_signum);
    int rv;

    ALOGV("%s(portable_signum:%d:'%s', act:%p, oldact:%p) {", __func__,
              portable_signum,
              portable_signame,        act,    oldact);

    mips_signum = signum_pton(portable_signum);
    mips_signame = map_mips_signum_to_name(mips_signum);

    if ((portable_signum != 0) && (mips_signum == 0)) {
        /* We got a portable signum that we can't map; Ignore the request */
        rv = 0;
        goto done;
    }
    if (portable_signum >= 0 && portable_signum < NSIG_PORTABLE)
        prev_portable_handler = mips_portable_sighandler[portable_signum];
    else
        prev_portable_handler = NULL;

    memset(&mips_act, 0, sizeof(mips_act));

    if (invalid_pointer((void *)act)) {
        rv = sigaction(mips_signum, (struct sigaction *)act, &mips_oldact);
    } else {
        /*
         * Make the MIPS version of sigaction, which has no sa_restorer function pointer.
         * Also the handler will be called with a pointer to a to a sigcontext structure
         * which is totally non-portable.
         */
        sigset_pton(((sigset_portable_t *)&act->sa_mask),
                                    ((sigset_t *) &mips_act.sa_mask));

        mips_act.sa_flags = sigaction_flags_pton(act->sa_flags);

        if (mips_act.sa_flags & SA_SIGINFO) {
            /*
             * Providing the three argument version of a signal handler.
             */
            if (portable_signum >= 0 && portable_signum < NSIG_PORTABLE) {
                mips_portable_sighandler[portable_signum] =
                                (sig3handler_portable_t) act->sa_sigaction_portable;

                mips_act.sa_sigaction = mips_sigaction_handler;
            }
            else {
                mips_act.sa_sigaction = act->sa_sigaction_portable;
            }
        } else {
            /*
             * Providing the classic single argument version of a signal handler.
             */
            portable_handler = act->sa_handler_portable;
            if ((portable_signum < 0) || (portable_signum > NSIG_PORTABLE)) {
                /*
                 * Let the kernel generate the proper return value and set errno.
                 */
                mips_act.sa_handler = (sighandler_t) portable_handler;
            } else {
                mips_handler = sighandler_pton(portable_handler, 1);
                if (mips_handler != portable_handler) {
                    mips_portable_sighandler[portable_signum] =
                                                       (sig3handler_portable_t) portable_handler;
                }
                mips_act.sa_handler = mips_handler;
            }
        }
        rv = sigaction(mips_signum, &mips_act, &mips_oldact);
    }

    if (oldact) {
        if (mips_oldact.sa_sigaction == (__sigaction_handler_portable_t) mips_sigaction_handler ||
            mips_oldact.sa_sigaction == (__sigaction_handler_portable_t) mips_sighandler) {

            oldact->sa_sigaction_portable =
                                           (__sigaction_handler_portable_t) prev_portable_handler;
        } else {
            oldact->sa_sigaction_portable =
                                        (__sigaction_handler_portable_t) mips_oldact.sa_sigaction;
        }
        sigset_ntop((sigset_t *) &(mips_oldact.sa_mask),
                                    (sigset_portable_t *) &(oldact->sa_mask));

        oldact->sa_flags = sigaction_flags_ntop(mips_oldact.sa_flags);
        oldact->sa_restorer = NULL;
    }

done:
    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}


int sigaction_portable(int portable_signum, const struct sigaction_portable *act,
                       struct sigaction_portable *oldact)
{
    int rv;

    ALOGV(" ");
    ALOGV("%s(portable_signum:%d, act:%p, oldact:%p) {", __func__,
              portable_signum,    act,    oldact);

    rv = do_sigaction_portable(portable_signum, act, oldact);

    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}


#if 0
/*
 * So far it appears that signalfd() isn't supported by bionic
 * the kernel trap numbers are available.
 */
int signalfd_portable(int fd, const sigset_t *portable_sigmask, int flags)
{
    sigset_t mips_sigmask;

    sigset_pton(portable_sigmask, &mips_sigmask);

    return signalfd(fd, &mips_sigmask, flags);
}
#endif


int sigsuspend_portable(const sigset_portable_t *portable_sigmask)
{
    int rv;
    sigset_t mips_sigmask;

    ALOGV("%s(portable_sigmask:%p) {", __func__, portable_sigmask);

    if (invalid_pointer((void *)portable_sigmask)) {
        errno = EFAULT;
        rv = -1;
    } else {
        sigset_pton((sigset_portable_t *)portable_sigmask, &mips_sigmask);
        rv = sigsuspend(&mips_sigmask);
    }

    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}


int sigpending_portable(sigset_portable_t *portable_sigset)
{
    int rv;
    sigset_t mips_sigset;

    ALOGV("%s(portable_sigset:%p) {", __func__,
              portable_sigset);

    if (invalid_pointer((void *)portable_sigset)) {
        errno = EFAULT;
        rv = -1;
    } else {
        rv = sigpending(&mips_sigset);
        sigset_ntop(&mips_sigset, portable_sigset);
    }

    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}


int sigwait_portable(const sigset_portable_t *portable_sigset, int *ptr_to_portable_sig)
{
    int rv;
    sigset_t mips_sigset;
    int mips_sig;
    int portable_sig;

    ALOGV("%s(portable_sigset:%p, ptr_to_portable_sig:%p) {", __func__,
              portable_sigset,    ptr_to_portable_sig);

    if (invalid_pointer((void *)portable_sigset)) {
        errno = EFAULT;
        rv = -1;
    } else {
        sigset_pton((sigset_portable_t *)portable_sigset, &mips_sigset);

        rv = sigwait(&mips_sigset, &mips_sig);

        portable_sig = signum_ntop(mips_sig);
        *ptr_to_portable_sig = portable_sig;
    }
    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}


int siginterrupt_portable(int portable_signum, int flag)

{
    int rv;
    int mips_signum;

    ALOGV("%s(portable_signum:%d, flag:0x%x) {", __func__,
              portable_signum,    flag);

    mips_signum = signum_pton(portable_signum);

    if ((portable_signum != 0) && (mips_signum == 0)) {
        ALOGE("%s: Unsupported portable_signum:%d; Ignoring.", __func__,
                               portable_signum);
        rv = 0;
    } else {
        rv = siginterrupt(mips_signum, flag);
    }
    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}


__hidden int do_sigmask(int portable_how, const sigset_portable_t *portable_sigset,
                        sigset_portable_t *portable_oldset, sigmask_fn fn)
{
    int rv;
    int how;
    char *how_name;
    sigset_t mips_sigset, *mips_sigset_p;
    sigset_t mips_oldset, *mips_oldset_p;

    ALOGV("%s(portable_how:%d, portable_sigset:%p, portable_oldset:%p, fn:%p) {", __func__,
              portable_how,    portable_sigset,    portable_oldset,    fn);

    switch(portable_how) {
    case SIG_BLOCK_PORTABLE:    how = SIG_BLOCK;        how_name = "SIG_BLOCK";         break;
    case SIG_UNBLOCK_PORTABLE:  how = SIG_UNBLOCK;      how_name = "SIG_UNBLOCK";       break;
    case SIG_SETMASK_PORTABLE:  how = SIG_SETMASK;      how_name = "SIG_SETMASK";       break;

    default:
        ALOGE("%s: portable_how:%d NOT SUPPORTED!", __func__, portable_how);
        how = -1;
        break;
    }

    if (invalid_pointer((void *)portable_sigset)) {
        mips_sigset_p = (sigset_t *) portable_sigset;
    } else {
        mips_sigset_p = &mips_sigset;
        memset(mips_sigset_p, 0, sizeof(mips_sigset));
        sigemptyset(mips_sigset_p);
        sigset_pton((sigset_portable_t *)portable_sigset, &mips_sigset);
    }

    if (invalid_pointer((void *)portable_oldset)) {
        mips_oldset_p = (sigset_t *) portable_oldset;
    } else {
        mips_oldset_p = &mips_oldset;
        memset(mips_oldset_p, 0, sizeof(mips_oldset));
        sigemptyset(mips_oldset_p);
    }

    rv = fn(how, mips_sigset_p, mips_oldset_p);

    if (rv == 0 && !invalid_pointer(portable_oldset)) {
        /* Map returned mips_oldset to portable_oldset for return to caller */
        sigset_ntop(mips_oldset_p, portable_oldset);
    }

    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}


int sigprocmask_portable(int portable_how, const sigset_portable_t *portable_sigset,
                         sigset_portable_t *portable_oldset)
{
    int rv;

    ALOGV(" ");
    ALOGV("%s(portable_how:%d, portable_sigset:%p, portable_oldset:%p) {", __func__,
              portable_how,    portable_sigset,    portable_oldset);

    rv = do_sigmask(portable_how, portable_sigset, portable_oldset, sigprocmask);

    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}




/*
 * ss_flags and ss_size are located in different locations in stack_t structure:
 *
 * Incomming ARM/Portable stack_t:                  Outgoing MIPS stack_t:
 * -------------------------------              ----------------------------
 *    typedef struct sigaltstack {              typedef struct sigaltstack {
 *        void __user *ss_sp;                       void *ss_sp;
 *        int ss_flags;                             size_t ss_size;
 *        size_t ss_size;                           int ss_flags;
 *    } stack_t;
 *
 */
int sigaltstack_portable(const portable_stack_t *ss, portable_stack_t *oss)
{
    int rv;
    stack_t new_stack, *mips_ss;
    stack_t old_stack, *mips_oss;

    ALOGV(" ");
    ALOGV("%s(ss:%p, oss:%p) {", __func__, ss, oss);

    if (ss == NULL) {
        mips_ss = NULL;
    } else {
        if (invalid_pointer((void *)ss)) {
            ALOGE("%s: invalid_pointer(ss:%p): Let kernel set proper errno and set return value.",
                   __func__,           ss);

            mips_ss = (stack_t *) ss;
        } else {
            memset(&new_stack, 0, sizeof(stack_t));
            new_stack.ss_sp = ss->ss_sp;
            new_stack.ss_flags = ss->ss_flags;
            new_stack.ss_size = ss->ss_size;
            mips_ss = &new_stack;
        }
    }
    if (oss == NULL) {
        mips_oss = NULL;
    } else {
        if (invalid_pointer((void *)oss)) {
            ALOGE("%s: invalid_pointer(oss:%p): Let kernel set proper errno and return value.",
                   __func__,           oss);

            mips_oss = (stack_t *)oss;
        } else {
            memset(&old_stack, 0, sizeof(stack_t));
            mips_oss = &old_stack;
        }
    }

    rv = sigaltstack(mips_ss, mips_oss);

    if (!invalid_pointer(oss)) {
        oss->ss_sp = old_stack.ss_sp;
        oss->ss_flags = old_stack.ss_flags;
        oss->ss_size = old_stack.ss_size;
    }
    ALOGV("%s: return(rv:%d); }", __func__, rv);

    return rv;
}


