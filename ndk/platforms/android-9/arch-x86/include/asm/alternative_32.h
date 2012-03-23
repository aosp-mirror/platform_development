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
#ifndef _I386_ALTERNATIVE_H
#define _I386_ALTERNATIVE_H
#include <asm/types.h>
#include <linux/stddef.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <linux/types.h>
struct alt_instr {
 u8 *instr;
 u8 *replacement;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u8 cpuid;
 u8 instrlen;
 u8 replacementlen;
 u8 pad;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct module;
#define alternative(oldinstr, newinstr, feature)   asm volatile ("661:\n\t" oldinstr "\n662:\n"   ".section .altinstructions,\"a\"\n"   "  .align 4\n"   "  .long 661b\n"     "  .long 663f\n"     "  .byte %c0\n"     "  .byte 662b-661b\n"     "  .byte 664f-663f\n"     ".previous\n"   ".section .altinstr_replacement,\"ax\"\n"   "663:\n\t" newinstr "\n664:\n"    ".previous" :: "i" (feature) : "memory")
#define alternative_input(oldinstr, newinstr, feature, input...)   asm volatile ("661:\n\t" oldinstr "\n662:\n"   ".section .altinstructions,\"a\"\n"   "  .align 4\n"   "  .long 661b\n"     "  .long 663f\n"     "  .byte %c0\n"     "  .byte 662b-661b\n"     "  .byte 664f-663f\n"     ".previous\n"   ".section .altinstr_replacement,\"ax\"\n"   "663:\n\t" newinstr "\n664:\n"    ".previous" :: "i" (feature), ##input)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define alternative_io(oldinstr, newinstr, feature, output, input...)   asm volatile ("661:\n\t" oldinstr "\n662:\n"   ".section .altinstructions,\"a\"\n"   "  .align 4\n"   "  .long 661b\n"     "  .long 663f\n"     "  .byte %c[feat]\n"     "  .byte 662b-661b\n"     "  .byte 664f-663f\n"     ".previous\n"   ".section .altinstr_replacement,\"ax\"\n"   "663:\n\t" newinstr "\n664:\n"     ".previous" : output : [feat] "i" (feature), ##input)
#define ASM_OUTPUT2(a, b) a, b
#define LOCK_PREFIX ""
struct paravirt_patch_site;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __parainstructions NULL
#define __parainstructions_end NULL
#endif
