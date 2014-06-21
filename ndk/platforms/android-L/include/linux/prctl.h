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
#ifndef _LINUX_PRCTL_H
#define _LINUX_PRCTL_H
#define PR_SET_PDEATHSIG 1
#define PR_GET_PDEATHSIG 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_GET_DUMPABLE 3
#define PR_SET_DUMPABLE 4
#define PR_GET_UNALIGN 5
#define PR_SET_UNALIGN 6
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_UNALIGN_NOPRINT 1
#define PR_UNALIGN_SIGBUS 2
#define PR_GET_KEEPCAPS 7
#define PR_SET_KEEPCAPS 8
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_GET_FPEMU 9
#define PR_SET_FPEMU 10
#define PR_FPEMU_NOPRINT 1
#define PR_FPEMU_SIGFPE 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_GET_FPEXC 11
#define PR_SET_FPEXC 12
#define PR_FP_EXC_SW_ENABLE 0x80
#define PR_FP_EXC_DIV 0x010000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_FP_EXC_OVF 0x020000
#define PR_FP_EXC_UND 0x040000
#define PR_FP_EXC_RES 0x080000
#define PR_FP_EXC_INV 0x100000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_FP_EXC_DISABLED 0
#define PR_FP_EXC_NONRECOV 1
#define PR_FP_EXC_ASYNC 2
#define PR_FP_EXC_PRECISE 3
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_GET_TIMING 13
#define PR_SET_TIMING 14
#define PR_TIMING_STATISTICAL 0
#define PR_TIMING_TIMESTAMP 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_SET_NAME 15
#define PR_GET_NAME 16
#define PR_GET_ENDIAN 19
#define PR_SET_ENDIAN 20
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_ENDIAN_BIG 0
#define PR_ENDIAN_LITTLE 1
#define PR_ENDIAN_PPC_LITTLE 2
#define PR_GET_SECCOMP 21
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_SET_SECCOMP 22
#define PR_CAPBSET_READ 23
#define PR_CAPBSET_DROP 24
#define PR_GET_TSC 25
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_SET_TSC 26
#define PR_TSC_ENABLE 1
#define PR_TSC_SIGSEGV 2
#define PR_GET_SECUREBITS 27
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_SET_SECUREBITS 28
#define PR_SET_TIMERSLACK 29
#define PR_GET_TIMERSLACK 30
#define PR_TASK_PERF_EVENTS_DISABLE 31
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_TASK_PERF_EVENTS_ENABLE 32
#define PR_MCE_KILL 33
#define PR_MCE_KILL_CLEAR 0
#define PR_MCE_KILL_SET 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_MCE_KILL_LATE 0
#define PR_MCE_KILL_EARLY 1
#define PR_MCE_KILL_DEFAULT 2
#define PR_MCE_KILL_GET 34
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_SET_MM 35
#define PR_SET_MM_START_CODE 1
#define PR_SET_MM_END_CODE 2
#define PR_SET_MM_START_DATA 3
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_SET_MM_END_DATA 4
#define PR_SET_MM_START_STACK 5
#define PR_SET_MM_START_BRK 6
#define PR_SET_MM_BRK 7
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_SET_MM_ARG_START 8
#define PR_SET_MM_ARG_END 9
#define PR_SET_MM_ENV_START 10
#define PR_SET_MM_ENV_END 11
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_SET_MM_AUXV 12
#define PR_SET_MM_EXE_FILE 13
#define PR_SET_PTRACER 0x59616d61
#define PR_SET_PTRACER_ANY ((unsigned long)-1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_SET_CHILD_SUBREAPER 36
#define PR_GET_CHILD_SUBREAPER 37
#define PR_SET_NO_NEW_PRIVS 38
#define PR_GET_NO_NEW_PRIVS 39
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PR_GET_TID_ADDRESS 40
#define PR_SET_TIMERSLACK_PID 41
#define PR_SET_VMA 0x53564d41
#define PR_SET_VMA_ANON_NAME 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
