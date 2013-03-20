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

#include <portability.h>
#include <unistd.h>
#include <stdarg.h>
#include <signal.h>
#include <signal_portable.h>
#include <time.h>
#include <errno.h>
#include <errno_portable.h>
#include <eventfd_portable.h>
#include <filefd_portable.h>
#include <inotify_portable.h>
#include <timerfd_portable.h>
#include <asm/unistd-portable.h>
#include <asm/unistd.h>

#define PORTABLE_TAG "syscall_portable"
#include <log_portable.h>

#if __NR_gettimeofday_portable == __NR_gettimeofday
#error "Bad build environment"
#endif

/*
 * Minimal syscall support for LTP testing.
 * These are the system calls that LTP references explicitly.
 * Not all of them are exported via bionic header so use #ifdef.
 *
 * TODO:
 *    Add existing portable system calls currently redirected from
 *    experimental Bionic linker code so that calls to them via
 *    syscall() are also processed. For example, LTP only calls open()
 *    directly and never does a syscall(__NR_open, ...).
 */


extern int REAL(syscall)(int, ...);

#define MAXARGS 8

int WRAP(syscall)(int portable_number, ...)
{
    va_list ap;
    int native_number, ret;
    int i, nargs, args[MAXARGS];

    ALOGV(" ");
    ALOGV("%s(portable_number:%d, ...) {", __func__, portable_number);

    switch (portable_number) {
#ifdef __NR_add_key_portable
    case __NR_add_key_portable: native_number = __NR_add_key; break;
#endif

#ifdef __NR_cacheflush_portable
    case __NR_cacheflush_portable: {
        long start, end, flags;

        va_start(ap, portable_number);
        start = va_arg(ap, long);
        end = va_arg(ap, long);
        flags = va_arg(ap, long);
        va_end(ap);

        ret = cacheflush(start, end, flags);
        goto done;
    }
#endif

#ifdef __NR_capget_portable
    case __NR_capget_portable: native_number = __NR_capget; break;
#endif

#ifdef __NR_capset_portable
    case __NR_capset_portable: native_number = __NR_capset; break;
#endif

#ifdef __NR_clock_getres_portable
    case __NR_clock_getres_portable: native_number = __NR_clock_getres; break;
#endif

#ifdef __NR_clock_nanosleep
    case __NR_clock_nanosleep_portable: native_number = __NR_clock_nanosleep; break;
#endif

#ifdef __NR_dup3_portable
    case __NR_dup3_portable: native_number = __NR_dup3; break;
#endif

#ifdef __NR_epoll_create_portable
    case __NR_epoll_create_portable: native_number = __NR_epoll_create; break;
#endif

#ifdef __NR_epoll_create1_portable
    case __NR_epoll_create1_portable: native_number = __NR_epoll_create1; break;
#endif

#ifdef __NR_eventfd_portable
    /*
     * Prior to 2.6.27 we only had this system call,
     * which didn't have a flags argument. The kernel
     * just provides a zero for flags when this system
     * call number is used.
     */
    case __NR_eventfd_portable: {
        unsigned int initval;                        /* 64-bit counter initial value */
        int flags = 0;

        va_start(ap, portable_number);

        initval  = va_arg(ap, int);

        va_end(ap);

        ret = WRAP(eventfd)(initval, flags);      /* Android uses __NR_eventfd2 in eventfd() */
        goto done;
    }
#endif

#ifdef __NR_eventfd2_portable
    /*
     * Starting with Linux 2.6.27 a flags argument was added.
     * Both Bionic and glibc implement the eventfd() now with
     * the additional flags argument.
     */
    case __NR_eventfd2_portable: {
        unsigned int initval;                        /* 64-bit counter initial value */
        int flags;

        va_start(ap, portable_number);

        initval  = va_arg(ap, int);
        flags = va_arg(ap, int);

        va_end(ap);

        ret = WRAP(eventfd)(initval, flags);      /* Android uses __NR_eventfd2 in eventfd() */
        goto done;
    }
#endif

#ifdef __NR_exit_group_portable
    case __NR_exit_group_portable: native_number = __NR_exit_group; break;
#endif

#ifdef __NR_faccessat_portable
    case __NR_faccessat_portable: native_number = __NR_faccessat; break;
#endif

#ifdef __NR_fallocate_portable
    case __NR_fallocate_portable: native_number = __NR_fallocate; break;
#endif

#ifdef __NR_fchmodat_portable
    case __NR_fchmodat_portable: native_number = __NR_fchmodat; break;
#endif

#ifdef __NR_fchownat_portable
    case __NR_fchownat_portable: native_number = __NR_fchownat; break;
#endif

#ifdef __NR_fstatat64_portable
    case __NR_fstatat64_portable: native_number = __NR_fstatat64; break;
#endif

#ifdef __NR_futimesat_portable
    case __NR_futimesat_portable: native_number = __NR_futimesat; break;
#endif

#ifdef __NR_getegid_portable
    case __NR_getegid_portable: native_number = __NR_getegid; break;
#endif

#ifdef __NR_geteuid_portable
    case __NR_geteuid_portable: native_number = __NR_geteuid; break;
#endif

#ifdef __NR_getgid_portable
    case __NR_getgid_portable: native_number = __NR_getgid; break;
#endif

#ifdef __NR_get_mempolicy_portable
    case __NR_get_mempolicy_portable: native_number = __NR_get_mempolicy; break;
#endif

#ifdef __NR_get_robust_list_portable
    case __NR_get_robust_list_portable: native_number = __NR_get_robust_list; break;
#endif

#ifdef __NR_gettid_portable
    case __NR_gettid_portable: native_number = __NR_gettid; break;
#endif

#ifdef __NR_gettimeofday_portable
    case __NR_gettimeofday_portable: native_number = __NR_gettimeofday; break;
#endif

#ifdef __NR_getuid_portable
    case __NR_getuid_portable: native_number = __NR_getuid; break;
#endif

#ifdef __NR_inotify_init_portable
    case __NR_inotify_init_portable: native_number = __NR_inotify_init; break;
#endif

#ifdef __NR_inotify_add_watch_portable
    case __NR_inotify_add_watch_portable: native_number = __NR_inotify_add_watch; break;
#endif

#ifdef __NR_inotify_init1_portable
    case __NR_inotify_init1_portable: {
        int portable_flags;

        va_start(ap, portable_number);
        portable_flags = va_arg(ap, int);
        va_end(ap);

        ret = WRAP(inotify_init1)(portable_flags);
        goto done;
    }
#endif

#ifdef __NR_keyctl_portable
    case __NR_keyctl_portable: native_number = __NR_keyctl; break;
#endif

#ifdef __NR_linkat
    case __NR_linkat_portable: native_number = __NR_linkat; break;
#endif

#ifdef __NR_mbind_portable
    case __NR_mbind_portable: native_number = __NR_mbind; break;
#endif

#ifdef __NR_mkdirat_portable
    case __NR_mkdirat_portable: native_number = __NR_mkdirat; break;
#endif

#ifdef __NR_mknodat_portable
    case __NR_mknodat_portable: native_number = __NR_mknodat; break;
#endif

#ifdef __NR_openat_portable
    case __NR_openat_portable: native_number = __NR_openat; break;
#endif

#ifdef __NR_pipe2_portable
    case __NR_pipe2_portable: {
        int *pipefd_ptr;
        int portable_flags;

        va_start(ap, portable_number);
        pipefd_ptr = va_arg(ap, int *);
        portable_flags = va_arg(ap, int);
        va_end(ap);

        ret = WRAP(pipe2)(pipefd_ptr, portable_flags);
        goto done;
    }
#endif

#ifdef __NR_readahead_portable
    case __NR_readahead_portable: native_number = __NR_readahead; break;
#endif

#ifdef __NR_readlinkat_portable
    case __NR_readlinkat_portable: native_number = __NR_readlinkat; break;
#endif

#ifdef __NR_renameat_portable
    case __NR_renameat_portable: native_number = __NR_renameat; break;
#endif

#ifdef __NR_rt_sigaction_portable
    case __NR_rt_sigaction_portable: {
        int sig;
        struct sigaction_portable *act;
        struct sigaction_portable *oact;
        size_t sigsetsize;

        va_start(ap, portable_number);
        sig = va_arg(ap, int);
        act = va_arg(ap, struct sigaction_portable *);
        oact = va_arg(ap, struct sigaction_portable *);
        sigsetsize = va_arg(ap, size_t);
        va_end(ap);
        return WRAP(__rt_sigaction)(sig, act, oact, sigsetsize);
    }
#endif

#ifdef __NR_rt_sigprocmask_portable
    case __NR_rt_sigprocmask_portable: {
        int how;
        const sigset_portable_t *set;
        sigset_portable_t *oset;
        size_t sigsetsize;

        va_start(ap, portable_number);
        how = va_arg(ap, int);
        set = va_arg(ap, sigset_portable_t *);
        oset = va_arg(ap, sigset_portable_t *);
        sigsetsize = va_arg(ap, size_t);
        va_end(ap);

        ret = WRAP(__rt_sigprocmask)(how, set, oset, sigsetsize);
        goto done;
    }
#endif

#ifdef __NR_rt_sigtimedwait_portable
    case __NR_rt_sigtimedwait_portable: {
        const sigset_portable_t *set;
        siginfo_portable_t *info;
        const struct timespec *timeout;
        size_t sigsetsize;

        va_start(ap, portable_number);
        set = va_arg(ap, sigset_portable_t *);
        info = va_arg(ap, siginfo_portable_t *);
        timeout = va_arg(ap, struct timespec *);
        sigsetsize = va_arg(ap, size_t);
        va_end(ap);

        ret = WRAP(__rt_sigtimedwait)(set, info, timeout, sigsetsize);
        goto done;
    }
#endif

#ifdef __NR_rt_sigqueueinfo_portable
    case __NR_rt_sigqueueinfo_portable: {
        pid_t pid;
        int sig;
        siginfo_portable_t *uinfo;

        va_start(ap, portable_number);
        pid = va_arg(ap, pid_t);
        sig = va_arg(ap, int);
        uinfo = va_arg(ap, siginfo_portable_t *);
        va_end(ap);

        ret = WRAP(rt_sigqueueinfo)(pid, sig, uinfo);
        goto done;
    }
#endif

#ifdef __NR_setgid_portable
    case __NR_setgid_portable: native_number = __NR_setgid; break;
#endif

#ifdef __NR_set_mempolicy_portable
    case __NR_set_mempolicy_portable: native_number = __NR_set_mempolicy; break;
#endif

#ifdef __NR_set_robust_list_portable
    case __NR_set_robust_list_portable: native_number = __NR_set_robust_list; break;
#endif

#ifdef __NR_set_tid_address_portable
    case __NR_set_tid_address_portable: native_number = __NR_set_tid_address; break;
#endif

#ifdef __NR_sgetmask_portable
    case __NR_sgetmask_portable: native_number = __NR_sgetmask; break;
#endif

#ifdef __NR_signalfd4_portable
    case __NR_signalfd4_portable: {
        int fd;
        sigset_portable_t *portable_sigmask;
        int sigsetsize;
        int flags;

        va_start(ap, portable_number);

        fd = va_arg(ap, int);
        portable_sigmask = va_arg(ap, sigset_portable_t *);
        sigsetsize = va_arg(ap, int);
        flags = va_arg(ap, int);

        va_end(ap);

        ret = do_signalfd4_portable(fd, (const sigset_portable_t *) portable_sigmask, sigsetsize,
                                    flags);
        goto done;
    }
#endif

#ifdef __NR_socketcall_portable
    case __NR_socketcall_portable: native_number = __NR_socketcall; break;
#endif

#ifdef __NR_splice_portable
    case __NR_splice_portable: native_number = __NR_splice; break;
#endif

/* REMIND - DOUBLE CHECK THIS ONE */
#ifdef __NR_ssetmask_portable
    case __NR_ssetmask_portable: native_number = __NR_ssetmask; break;
#endif

#ifdef __NR_swapoff_portable
    case __NR_swapoff_portable: native_number = __NR_swapoff; break;
#endif

#ifdef __NR_swapon_portable
    case __NR_swapon_portable: native_number = __NR_swapon; break;
#endif

#ifdef __NR_symlinkat_portable
    case __NR_symlinkat_portable: native_number = __NR_symlinkat; break;
#endif

/*
 * ARM uses the new, version 2, form of sync_file_range() which
 * doesn't waste 32 bits between the 32 bit arg and the 64 bit arg.
 * It does this by moving the last 32 bit arg and placing it with
 * the 1st 32 bit arg.
 *
 * Here's the trivial mapping function in the kernel ARM code:
 *
 *   sync_file_range2(int fd, unsigned int flags, loff_t offset, loff_t nbytes) {
 *       return sys_sync_file_range(fd, offset, nbytes, flags);
 *   }
 *
 * For portability we have to do a similar mapping for the native/MIPS system
 * call but have to provide the alignment padding expected by the sync_file_range()
 * system call. We avoid alignment issues while using varargs by avoiding the use
 * of 64 bit args.
 */
#if defined( __NR_arm_sync_file_range_portable)
    case __NR_arm_sync_file_range_portable: native_number = __NR_sync_file_range; {
        int fd;
        int flags;
        int offset_low, offset_high;
        int nbytes_low, nbytes_high;
        int align_fill = 0;


        va_start(ap, portable_number);
        fd = va_arg(ap, int);
        flags = va_arg(ap, int);
        offset_low = va_arg(ap, int);
        offset_high = va_arg(ap, int);
        nbytes_low = va_arg(ap, int);
        nbytes_high = va_arg(ap, int);
        va_end(ap);

        ALOGV("%s: Calling syscall(native_number:%d:'sync_file_range', fd:%d, "
              "align_fill:0x%x, offset_low:0x%x, offset_high:0x%x, "
              "nbytes_low:0x%x, nbytes_high:0x%x, flags:0x%x);", __func__,
              native_number, fd, align_fill, offset_low, offset_high,
              nbytes_low, nbytes_high, flags);

        ret = REAL(syscall)(native_number, fd, align_fill, offset_low, offset_high,
                      nbytes_low, nbytes_high, flags);

        goto done;
    }
#endif


#ifdef __NR__sysctl_portable
    case __NR__sysctl_portable: native_number = __NR__sysctl; break;
#endif

#ifdef __NR_sysfs_portable
    case __NR_sysfs_portable: native_number = __NR_sysfs; break;
#endif

#ifdef __NR_syslog_portable
    case __NR_syslog_portable: native_number = __NR_syslog; break;
#endif

#ifdef __NR_tee_portable
    case __NR_tee_portable: native_number = __NR_tee; break;
#endif

#ifdef __NR_timer_create_portable
    case __NR_timer_create_portable: {
        clockid_t clockid;
        struct sigevent *evp;
        timer_t *timerid;

        va_start(ap, portable_number);
        clockid = va_arg(ap, clockid_t);
        evp = va_arg(ap, struct sigevent *);
        timerid = va_arg(ap, timer_t *);
        va_end(ap);

        ret = WRAP(timer_create)(clockid, evp, timerid);
        goto done;
    }
#endif

#ifdef __NR_timerfd_create_portable
    case __NR_timerfd_create_portable: {
        int clockid;
        int flags;

        va_start(ap, portable_number);
        clockid = va_arg(ap, int);              /* clockid is portable */
        flags = va_arg(ap, int);                /* flags need to be mapped */
        va_end(ap);

        ret = WRAP(timerfd_create)(clockid, flags);
        goto done;
    }
#endif

#ifdef __NR_timerfd_gettime_portable
    case __NR_timerfd_gettime_portable: native_number = __NR_timerfd_gettime; break;
#endif

#ifdef __NR_timerfd_settime_portable
    case __NR_timerfd_settime_portable: native_number = __NR_timerfd_settime; break;
#endif

#ifdef __NR_timer_getoverrun_portable
    case __NR_timer_getoverrun_portable: native_number = __NR_timer_getoverrun; break;
#endif

#ifdef __NR_timer_gettime_portable
    case __NR_timer_gettime_portable: native_number = __NR_timer_gettime; break;
#endif

#ifdef __NR_timer_settime_portable
    case __NR_timer_settime_portable: native_number = __NR_timer_settime; break;
#endif

#ifdef __NR_rt_tgsigqueueinfo_portable
    case __NR_rt_tgsigqueueinfo_portable: {
        pid_t tgid;
        pid_t pid;
        int sig;
        siginfo_portable_t *uinfo;

        va_start(ap, portable_number);
        tgid = va_arg(ap, pid_t);
        pid = va_arg(ap, pid_t);
        sig = va_arg(ap, int);
        uinfo = va_arg(ap, siginfo_portable_t *);
        va_end(ap);

        ret = WRAP(rt_tgsigqueueinfo)(tgid, pid, sig, uinfo);
        goto done;
    }
#endif

#ifdef __NR_tkill_portable
    case __NR_tkill_portable: {
        int tid, sig;

        va_start(ap, portable_number);
        tid = va_arg(ap, int);
        sig = va_arg(ap, int);
        va_end(ap);

        ret = WRAP(tkill)(tid, sig);
        goto done;
    }
#endif

#ifdef __NR_uname_portable
    case __NR_uname_portable: native_number = __NR_uname; break;
#endif

#ifdef __NR_vmsplice_portable
    case __NR_vmsplice_portable: native_number = __NR_vmsplice; break;
#endif

    default:
        ALOGV("%s(portable_number:%d,  ...): case default; native_number = -1; "
              "[ERROR: ADD MISSING SYSTEM CALL]", __func__, portable_number);

        native_number = -1;
        break;
    }

    ALOGV("%s: native_number = %d", __func__, native_number);

    if (native_number <= 0) {
        ALOGV("%s: native_number:%d <= 0; ret = -1; [ERROR: FIX SYSTEM CALL]", __func__,
                   native_number);

        *REAL(__errno)() = ENOSYS;
        ret = -1;
        goto done;
    }

    /*
     * Get the argument list
     * This is pretty crappy:
     *   It assumes that the portable and native arguments are compatible
     *   It assumes that no more than MAXARGS arguments are passed
     *
     * Possible changes:
     *  o include the argument count for each mapped system call
     *  o map the syscall into the equivalent library call:
     *    eg syscall(__NR_gettimeofday_portable, struct timeval *tv, struct timezone *tz) =>
     *       gettimeofday(struct timeval *tv, struct timezone *tz)
     *
     * second option is probably best as it allows argument remapping to take place if needed
     *
     */
    va_start(ap, portable_number);
    /* For now assume all syscalls take MAXARGS arguments. */
    nargs = MAXARGS;
    for (i = 0; i < nargs; i++)
        args[i] = va_arg(ap, int);
    va_end(ap);

    ALOGV("%s: Calling syscall(%d, %d, %d, %d, %d, %d, %d, %d, %d);", __func__,
          native_number, args[0], args[1], args[2], args[3], args[4],
          args[5], args[6], args[7]);

    ret = REAL(syscall)(native_number, args[0], args[1], args[2], args[3],
                  args[4], args[5], args[6], args[7]);

done:
    if (ret == -1) {
        ALOGV("%s: ret == -1; errno:%d;", __func__, *REAL(__errno)());
    }
    ALOGV("%s: return(ret:%d); }", __func__, ret);
    return ret;
}
