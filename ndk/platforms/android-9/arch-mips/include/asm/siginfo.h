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
#ifndef _ASM_SIGINFO_H
#define _ASM_SIGINFO_H
#define __ARCH_SIGEV_PREAMBLE_SIZE (sizeof(long) + 2*sizeof(int))
#undef __ARCH_SI_TRAPNO  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define HAVE_ARCH_SIGINFO_T
#define HAVE_ARCH_COPY_SIGINFO
struct siginfo;
#define __ARCH_SI_PREAMBLE_SIZE (3 * sizeof(int))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <asm-generic/siginfo.h>
typedef struct siginfo {
 int si_signo;
 int si_code;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int si_errno;
 int __pad0[SI_MAX_SIZE / sizeof(int) - SI_PAD_SIZE - 3];
 union {
 int _pad[SI_PAD_SIZE];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct {
 pid_t _pid;
 __ARCH_SI_UID_T _uid;
 } _kill;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct {
 timer_t _tid;
 int _overrun;
 char _pad[sizeof( __ARCH_SI_UID_T) - sizeof(int)];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 sigval_t _sigval;
 int _sys_private;
 } _timer;
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 pid_t _pid;
 __ARCH_SI_UID_T _uid;
 sigval_t _sigval;
 } _rt;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct {
 pid_t _pid;
 __ARCH_SI_UID_T _uid;
 int _status;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 clock_t _utime;
 clock_t _stime;
 } _sigchld;
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 pid_t _pid;
 clock_t _utime;
 int _status;
 clock_t _stime;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 } _irix_sigchld;
 struct {
 void __user *_addr;
#ifdef __ARCH_SI_TRAPNO
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int _trapno;
#endif
 } _sigfault;
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __ARCH_SI_BAND_T _band;
 int _fd;
 } _sigpoll;
 } _sifields;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} siginfo_t;
#undef SI_ASYNCIO
#undef SI_TIMER
#undef SI_MESGQ
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SI_ASYNCIO -2  
#define SI_TIMER __SI_CODE(__SI_TIMER, -3)  
#define SI_MESGQ __SI_CODE(__SI_MESGQ, -4)  
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
