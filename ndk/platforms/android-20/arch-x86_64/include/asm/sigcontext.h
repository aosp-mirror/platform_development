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
#ifndef _UAPI_ASM_X86_SIGCONTEXT_H
#define _UAPI_ASM_X86_SIGCONTEXT_H
#include <linux/compiler.h>
#include <linux/types.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FP_XSTATE_MAGIC1 0x46505853U
#define FP_XSTATE_MAGIC2 0x46505845U
#define FP_XSTATE_MAGIC2_SIZE sizeof(FP_XSTATE_MAGIC2)
struct _fpx_sw_bytes {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 magic1;
 __u32 extended_size;
 __u64 xstate_bv;
 __u32 xstate_size;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 padding[7];
};
#ifdef __i386__
struct _fpreg {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned short significand[4];
 unsigned short exponent;
};
struct _fpxreg {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned short significand[4];
 unsigned short exponent;
 unsigned short padding[3];
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct _xmmreg {
 unsigned long element[4];
};
struct _fpstate {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long cw;
 unsigned long sw;
 unsigned long tag;
 unsigned long ipoff;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long cssel;
 unsigned long dataoff;
 unsigned long datasel;
 struct _fpreg _st[8];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned short status;
 unsigned short magic;
 unsigned long _fxsr_env[6];
 unsigned long mxcsr;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long reserved;
 struct _fpxreg _fxsr_st[8];
 struct _xmmreg _xmm[8];
 unsigned long padding1[44];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 union {
 unsigned long padding2[12];
 struct _fpx_sw_bytes sw_reserved;
 };
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define X86_FXSR_MAGIC 0x0000
struct sigcontext {
 unsigned short gs, __gsh;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned short fs, __fsh;
 unsigned short es, __esh;
 unsigned short ds, __dsh;
 unsigned long edi;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long esi;
 unsigned long ebp;
 unsigned long esp;
 unsigned long ebx;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long edx;
 unsigned long ecx;
 unsigned long eax;
 unsigned long trapno;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long err;
 unsigned long eip;
 unsigned short cs, __csh;
 unsigned long eflags;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long esp_at_signal;
 unsigned short ss, __ssh;
 struct _fpstate __user *fpstate;
 unsigned long oldmask;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long cr2;
};
#else
struct _fpstate {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 cwd;
 __u16 swd;
 __u16 twd;
 __u16 fop;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u64 rip;
 __u64 rdp;
 __u32 mxcsr;
 __u32 mxcsr_mask;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 st_space[32];
 __u32 xmm_space[64];
 __u32 reserved2[12];
 union {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 reserved3[12];
 struct _fpx_sw_bytes sw_reserved;
 };
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct sigcontext {
 __u64 r8;
 __u64 r9;
 __u64 r10;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u64 r11;
 __u64 r12;
 __u64 r13;
 __u64 r14;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u64 r15;
 __u64 rdi;
 __u64 rsi;
 __u64 rbp;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u64 rbx;
 __u64 rdx;
 __u64 rax;
 __u64 rcx;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u64 rsp;
 __u64 rip;
 __u64 eflags;
 __u16 cs;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 gs;
 __u16 fs;
 __u16 __pad0;
 __u64 err;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u64 trapno;
 __u64 oldmask;
 __u64 cr2;
 struct _fpstate __user *fpstate;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifdef __ILP32__
 __u32 __fpstate_pad;
#endif
 __u64 reserved1[8];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#endif
struct _xsave_hdr {
 __u64 xstate_bv;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u64 reserved1[2];
 __u64 reserved2[5];
};
struct _ymmh_state {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 ymmh_space[64];
};
struct _xstate {
 struct _fpstate fpstate;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct _xsave_hdr xstate_hdr;
 struct _ymmh_state ymmh;
};
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
