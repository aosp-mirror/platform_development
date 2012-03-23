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
#ifndef _ASM_REQUIRED_FEATURES_H
#define _ASM_REQUIRED_FEATURES_H 1
#define NEED_FPU (1<<(X86_FEATURE_FPU & 31))
#define NEED_PAE 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NEED_CX8 0
#define NEED_CMOV 0
#define NEED_3DNOW 0
#define NEED_PSE 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NEED_MSR 0
#define NEED_PGE 0
#define NEED_FXSR 0
#define NEED_XMM 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NEED_XMM2 0
#define NEED_LM 0
#define REQUIRED_MASK0 (NEED_FPU|NEED_PSE|NEED_MSR|NEED_PAE|  NEED_CX8|NEED_PGE|NEED_FXSR|NEED_CMOV|  NEED_XMM|NEED_XMM2)
#define SSE_MASK (NEED_XMM|NEED_XMM2)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define REQUIRED_MASK1 (NEED_LM|NEED_3DNOW)
#define REQUIRED_MASK2 0
#define REQUIRED_MASK3 0
#define REQUIRED_MASK4 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define REQUIRED_MASK5 0
#define REQUIRED_MASK6 0
#define REQUIRED_MASK7 0
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
