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

#include <unistd.h>
#include <stdarg.h>
#include <signal.h>
#include <signal_portable.h>
#include <time.h>
#include <errno.h>
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
 */


extern int syscall(int, ...);

#define MAXARGS 6

int syscall_portable(int portable_number, ...)
{
    va_list ap;
    int native_number, ret;
    int i, nargs, args[MAXARGS];

    switch (portable_number) {
#ifdef __NR_add_key_portable
    case __NR_add_key_portable: native_number = __NR_add_key; break;
#endif
#ifdef __NR_cacheflush_portable
    case __NR_cacheflush_portable:
    {
        long start, end, flags;
        va_start(ap, portable_number);
        start = va_arg(ap, long);
        end = va_arg(ap, long);
        flags = va_arg(ap, long);
        va_end(ap);
        return cacheflush(start, end, flags);
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
#ifdef __NR_dup3_portable
    case __NR_dup3_portable: native_number = __NR_dup3; break;
#endif
#ifdef __NR_epoll_create1_portable
    case __NR_epoll_create1_portable: native_number = __NR_epoll_create1; break;
#endif
#ifdef __NR_eventfd_portable
    case __NR_eventfd_portable: native_number = __NR_eventfd; break;
#endif
#ifdef __NR_eventfd2_portable
    case __NR_eventfd2_portable: native_number = __NR_eventfd2; break;
#endif
#ifdef __NR_exit_group_portable
    case __NR_exit_group_portable: native_number = __NR_exit_group; break;
#endif
#ifdef __NR_fallocate_portable
    case __NR_fallocate_portable: native_number = __NR_fallocate; break;
#endif
#ifdef __NR_getegid_portable
    case __NR_getegid_portable: native_number = __NR_getegid; break;
#endif
#ifdef __NR_geteuid_portable
    case __NR_geteuid_portable: native_number = __NR_geteuid; break;
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
#ifdef __NR_inotify_init1_portable
    case __NR_inotify_init1_portable: native_number = __NR_inotify_init1; break;
#endif
#ifdef __NR_keyctl_portable
    case __NR_keyctl_portable: native_number = __NR_keyctl; break;
#endif
#ifdef __NR_mbind_portable
    case __NR_mbind_portable: native_number = __NR_mbind; break;
#endif
#ifdef __NR_pipe2_portable
    case __NR_pipe2_portable: native_number = __NR_pipe2; break;
#endif
#ifdef __NR_rt_sigaction_portable
    case __NR_rt_sigaction_portable: native_number = __NR_rt_sigaction; break;
#endif
#ifdef __NR_rt_sigprocmask_portable
    case __NR_rt_sigprocmask_portable: native_number = __NR_rt_sigprocmask; break;
#endif
#ifdef __NR_rt_sigtimedwait_portable
    case __NR_rt_sigtimedwait_portable: native_number = __NR_rt_sigtimedwait; break;
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
    case __NR_signalfd4_portable: native_number = __NR_signalfd4; break;
#endif
#ifdef __NR_socketcall_portable
    case __NR_socketcall_portable: native_number = __NR_socketcall; break;
#endif
#ifdef __NR_splice_portable
    case __NR_splice_portable: native_number = __NR_splice; break;
#endif
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
#ifdef __NR_sync_file_range2_portable
    case __NR_sync_file_range2_portable: native_number = __NR_sync_file_range2; break;
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
#ifdef __NR_timer_create_portable
    case __NR_timer_create_portable:
    {
        extern int timer_create_portable(clockid_t, struct sigevent *, timer_t *);
        clockid_t clockid;
        struct sigevent *evp;
        timer_t *timerid;

        va_start(ap, portable_number);
        clockid = va_arg(ap, clockid_t);
        evp = va_arg(ap, struct sigevent *);
        timerid = va_arg(ap, timer_t *);
        va_end(ap);

        return timer_create_portable(clockid, evp, timerid);
    }
#endif
#ifdef __NR_timerfd_create_portable
    case __NR_timerfd_create_portable: native_number = __NR_timerfd_create; break;
#endif
#ifdef __NR_timer_getoverrun_portable
    case __NR_timer_getoverrun_portable: native_number = __NR_timer_getoverrun; break;
#endif
#ifdef __NR_timer_gettime_portable
    case __NR_timer_gettime_portable: native_number = __NR_timer_gettime; break;
#endif
#ifdef __NR_tkill_portable
    case __NR_tkill_portable:
    {
        extern int tkill_portable(int, int);
        int tid, sig;

        va_start(ap, portable_number);
        tid = va_arg(ap, int);
        sig = va_arg(ap, int);
        va_end(ap);

        return tkill_portable(tid, sig);
    }
#endif
    default:
        native_number = -1;
        break;
    }

    ALOGV("%s(portable_number:%d, ...) { native_number = %d", __func__,
              portable_number,           native_number);

    if (native_number == -1) {
        errno = ENOSYS;
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

    ret = syscall(native_number, args[0], args[1], args[2], args[3], args[4], args[5]);

done:
    if (ret == -1) {
        ALOGE("%s: ret == -1; errno:%d;", __func__, errno);
    }
    ALOGV("%s: return(ret:%d); }", __func__, ret);
    return ret;
}
