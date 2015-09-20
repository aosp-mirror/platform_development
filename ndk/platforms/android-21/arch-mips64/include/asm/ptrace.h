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
#ifndef _UAPI_ASM_PTRACE_H
#define _UAPI_ASM_PTRACE_H
#define FPR_BASE 32
#define PC 64
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CAUSE 65
#define BADVADDR 66
#define MMHI 67
#define MMLO 68
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FPC_CSR 69
#define FPC_EIR 70
#define DSP_BASE 71
#define DSP_CONTROL 77
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define ACX 78
struct pt_regs {
 unsigned long regs[32];
 unsigned long cp0_status;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long hi;
 unsigned long lo;
 unsigned long cp0_badvaddr;
 unsigned long cp0_cause;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long cp0_epc;
} __attribute__ ((aligned (8)));
#define PTRACE_GETREGS 12
#define PTRACE_SETREGS 13
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PTRACE_GETFPREGS 14
#define PTRACE_SETFPREGS 15
#define PTRACE_OLDSETOPTIONS 21
#define PTRACE_GET_THREAD_AREA 25
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PTRACE_SET_THREAD_AREA 26
#define PTRACE_PEEKTEXT_3264 0xc0
#define PTRACE_PEEKDATA_3264 0xc1
#define PTRACE_POKETEXT_3264 0xc2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PTRACE_POKEDATA_3264 0xc3
#define PTRACE_GET_THREAD_AREA_3264 0xc4
enum pt_watch_style {
 pt_watch_style_mips32,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 pt_watch_style_mips64
};
struct mips32_watch_regs {
 unsigned int watchlo[8];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned short watchhi[8];
 unsigned short watch_masks[8];
 unsigned int num_valid;
} __attribute__((aligned(8)));
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct mips64_watch_regs {
 unsigned long long watchlo[8];
 unsigned short watchhi[8];
 unsigned short watch_masks[8];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int num_valid;
} __attribute__((aligned(8)));
struct pt_watch_regs {
 enum pt_watch_style style;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 union {
 struct mips32_watch_regs mips32;
 struct mips64_watch_regs mips64;
 };
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define PTRACE_GET_WATCH_REGS 0xd0
#define PTRACE_SET_WATCH_REGS 0xd1
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
