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
#ifndef _UAPI_ASM_GENERIC_SIGINFO_H
#define _UAPI_ASM_GENERIC_SIGINFO_H
#include <linux/compiler.h>
#include <linux/types.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef union sigval {
 int sival_int;
 void __user *sival_ptr;
} sigval_t;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef __ARCH_SI_PREAMBLE_SIZE
#define __ARCH_SI_PREAMBLE_SIZE (3 * sizeof(int))
#endif
#define SI_MAX_SIZE 128
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef SI_PAD_SIZE
#define SI_PAD_SIZE ((SI_MAX_SIZE - __ARCH_SI_PREAMBLE_SIZE) / sizeof(int))
#endif
#ifndef __ARCH_SI_UID_T
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __ARCH_SI_UID_T __kernel_uid32_t
#endif
#ifndef __ARCH_SI_BAND_T
#define __ARCH_SI_BAND_T long
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#ifndef __ARCH_SI_CLOCK_T
#define __ARCH_SI_CLOCK_T __kernel_clock_t
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef __ARCH_SI_ATTRIBUTES
#define __ARCH_SI_ATTRIBUTES
#endif
#ifndef HAVE_ARCH_SIGINFO_T
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef struct siginfo {
 int si_signo;
 int si_errno;
 int si_code;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 union {
 int _pad[SI_PAD_SIZE];
 struct {
 __kernel_pid_t _pid;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __ARCH_SI_UID_T _uid;
 } _kill;
 struct {
 __kernel_timer_t _tid;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int _overrun;
 char _pad[sizeof( __ARCH_SI_UID_T) - sizeof(int)];
 sigval_t _sigval;
 int _sys_private;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 } _timer;
 struct {
 __kernel_pid_t _pid;
 __ARCH_SI_UID_T _uid;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 sigval_t _sigval;
 } _rt;
 struct {
 __kernel_pid_t _pid;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __ARCH_SI_UID_T _uid;
 int _status;
 __ARCH_SI_CLOCK_T _utime;
 __ARCH_SI_CLOCK_T _stime;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 } _sigchld;
 struct {
 void __user *_addr;
#ifdef __ARCH_SI_TRAPNO
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int _trapno;
#endif
 short _addr_lsb;
 } _sigfault;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct {
 __ARCH_SI_BAND_T _band;
 int _fd;
 } _sigpoll;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct {
 void __user *_call_addr;
 int _syscall;
 unsigned int _arch;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 } _sigsys;
 } _sifields;
} __ARCH_SI_ATTRIBUTES siginfo_t;
#define __ARCH_SIGSYS
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#define si_pid _sifields._kill._pid
#define si_uid _sifields._kill._uid
#define si_tid _sifields._timer._tid
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define si_overrun _sifields._timer._overrun
#define si_sys_private _sifields._timer._sys_private
#define si_status _sifields._sigchld._status
#define si_utime _sifields._sigchld._utime
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define si_stime _sifields._sigchld._stime
#define si_value _sifields._rt._sigval
#define si_int _sifields._rt._sigval.sival_int
#define si_ptr _sifields._rt._sigval.sival_ptr
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define si_addr _sifields._sigfault._addr
#ifdef __ARCH_SI_TRAPNO
#define si_trapno _sifields._sigfault._trapno
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define si_addr_lsb _sifields._sigfault._addr_lsb
#define si_band _sifields._sigpoll._band
#define si_fd _sifields._sigpoll._fd
#ifdef __ARCH_SIGSYS
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define si_call_addr _sifields._sigsys._call_addr
#define si_syscall _sifields._sigsys._syscall
#define si_arch _sifields._sigsys._arch
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __SI_KILL 0
#define __SI_TIMER 0
#define __SI_POLL 0
#define __SI_FAULT 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __SI_CHLD 0
#define __SI_RT 0
#define __SI_MESGQ 0
#define __SI_SYS 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __SI_CODE(T,N) (N)
#define SI_USER 0
#define SI_KERNEL 0x80
#define SI_QUEUE -1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SI_TIMER __SI_CODE(__SI_TIMER,-2)
#define SI_MESGQ __SI_CODE(__SI_MESGQ,-3)
#define SI_ASYNCIO -4
#define SI_SIGIO -5
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SI_TKILL -6
#define SI_DETHREAD -7
#define SI_FROMUSER(siptr) ((siptr)->si_code <= 0)
#define SI_FROMKERNEL(siptr) ((siptr)->si_code > 0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define ILL_ILLOPC (__SI_FAULT|1)
#define ILL_ILLOPN (__SI_FAULT|2)
#define ILL_ILLADR (__SI_FAULT|3)
#define ILL_ILLTRP (__SI_FAULT|4)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define ILL_PRVOPC (__SI_FAULT|5)
#define ILL_PRVREG (__SI_FAULT|6)
#define ILL_COPROC (__SI_FAULT|7)
#define ILL_BADSTK (__SI_FAULT|8)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NSIGILL 8
#define FPE_INTDIV (__SI_FAULT|1)
#define FPE_INTOVF (__SI_FAULT|2)
#define FPE_FLTDIV (__SI_FAULT|3)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FPE_FLTOVF (__SI_FAULT|4)
#define FPE_FLTUND (__SI_FAULT|5)
#define FPE_FLTRES (__SI_FAULT|6)
#define FPE_FLTINV (__SI_FAULT|7)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FPE_FLTSUB (__SI_FAULT|8)
#define NSIGFPE 8
#define SEGV_MAPERR (__SI_FAULT|1)
#define SEGV_ACCERR (__SI_FAULT|2)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NSIGSEGV 2
#define BUS_ADRALN (__SI_FAULT|1)
#define BUS_ADRERR (__SI_FAULT|2)
#define BUS_OBJERR (__SI_FAULT|3)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define BUS_MCEERR_AR (__SI_FAULT|4)
#define BUS_MCEERR_AO (__SI_FAULT|5)
#define NSIGBUS 5
#define TRAP_BRKPT (__SI_FAULT|1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define TRAP_TRACE (__SI_FAULT|2)
#define TRAP_BRANCH (__SI_FAULT|3)
#define TRAP_HWBKPT (__SI_FAULT|4)
#define NSIGTRAP 4
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CLD_EXITED (__SI_CHLD|1)
#define CLD_KILLED (__SI_CHLD|2)
#define CLD_DUMPED (__SI_CHLD|3)
#define CLD_TRAPPED (__SI_CHLD|4)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CLD_STOPPED (__SI_CHLD|5)
#define CLD_CONTINUED (__SI_CHLD|6)
#define NSIGCHLD 6
#define POLL_IN (__SI_POLL|1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define POLL_OUT (__SI_POLL|2)
#define POLL_MSG (__SI_POLL|3)
#define POLL_ERR (__SI_POLL|4)
#define POLL_PRI (__SI_POLL|5)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define POLL_HUP (__SI_POLL|6)
#define NSIGPOLL 6
#define SYS_SECCOMP (__SI_SYS|1)
#define NSIGSYS 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SIGEV_SIGNAL 0
#define SIGEV_NONE 1
#define SIGEV_THREAD 2
#define SIGEV_THREAD_ID 4
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef __ARCH_SIGEV_PREAMBLE_SIZE
#define __ARCH_SIGEV_PREAMBLE_SIZE (sizeof(int) * 2 + sizeof(sigval_t))
#endif
#define SIGEV_MAX_SIZE 64
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SIGEV_PAD_SIZE ((SIGEV_MAX_SIZE - __ARCH_SIGEV_PREAMBLE_SIZE)   / sizeof(int))
typedef struct sigevent {
 sigval_t sigev_value;
 int sigev_signo;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int sigev_notify;
 union {
 int _pad[SIGEV_PAD_SIZE];
 int _tid;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct {
 void (*_function)(sigval_t);
 void *_attribute;
 } _sigev_thread;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 } _sigev_un;
} sigevent_t;
#define sigev_notify_function _sigev_un._sigev_thread._function
#define sigev_notify_attributes _sigev_un._sigev_thread._attribute
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define sigev_notify_thread_id _sigev_un._tid
#endif
