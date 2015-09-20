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
#ifndef __LINUX_KVM_MIPS_H
#define __LINUX_KVM_MIPS_H
#include <linux/types.h>
struct kvm_regs {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u64 gpr[32];
 __u64 hi;
 __u64 lo;
 __u64 pc;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct kvm_fpu {
 __u64 fpr[32];
 __u32 fir;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 fccr;
 __u32 fexr;
 __u32 fenr;
 __u32 fcsr;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 pad;
};
#define KVM_REG_MIPS_R0 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 0)
#define KVM_REG_MIPS_R1 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KVM_REG_MIPS_R2 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 2)
#define KVM_REG_MIPS_R3 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 3)
#define KVM_REG_MIPS_R4 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 4)
#define KVM_REG_MIPS_R5 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 5)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KVM_REG_MIPS_R6 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 6)
#define KVM_REG_MIPS_R7 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 7)
#define KVM_REG_MIPS_R8 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 8)
#define KVM_REG_MIPS_R9 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 9)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KVM_REG_MIPS_R10 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 10)
#define KVM_REG_MIPS_R11 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 11)
#define KVM_REG_MIPS_R12 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 12)
#define KVM_REG_MIPS_R13 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 13)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KVM_REG_MIPS_R14 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 14)
#define KVM_REG_MIPS_R15 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 15)
#define KVM_REG_MIPS_R16 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 16)
#define KVM_REG_MIPS_R17 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 17)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KVM_REG_MIPS_R18 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 18)
#define KVM_REG_MIPS_R19 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 19)
#define KVM_REG_MIPS_R20 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 20)
#define KVM_REG_MIPS_R21 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 21)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KVM_REG_MIPS_R22 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 22)
#define KVM_REG_MIPS_R23 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 23)
#define KVM_REG_MIPS_R24 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 24)
#define KVM_REG_MIPS_R25 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 25)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KVM_REG_MIPS_R26 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 26)
#define KVM_REG_MIPS_R27 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 27)
#define KVM_REG_MIPS_R28 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 28)
#define KVM_REG_MIPS_R29 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 29)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KVM_REG_MIPS_R30 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 30)
#define KVM_REG_MIPS_R31 (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 31)
#define KVM_REG_MIPS_HI (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 32)
#define KVM_REG_MIPS_LO (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 33)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KVM_REG_MIPS_PC (KVM_REG_MIPS | KVM_REG_SIZE_U64 | 34)
struct kvm_debug_exit_arch {
 __u64 epc;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct kvm_guest_debug_arch {
};
struct kvm_sync_regs {
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct kvm_sregs {
};
struct kvm_mips_interrupt {
 __u32 cpu;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 irq;
};
#endif
