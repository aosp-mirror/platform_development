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
#ifndef _ASM_USER_H
#define _ASM_USER_H
#include <asm/page.h>
#include <asm/reg.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define EF_SIZE 180
struct user {
 unsigned long regs[EF_SIZE /
 sizeof(unsigned long) + 64];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 size_t u_tsize;
 size_t u_dsize;
 size_t u_ssize;
 unsigned long start_code;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long start_data;
 unsigned long start_stack;
 long int signal;
 unsigned long u_ar0;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long magic;
 char u_comm[32];
};
#define NBPG PAGE_SIZE
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define UPAGES 1
#define HOST_TEXT_START_ADDR (u.start_code)
#define HOST_DATA_START_ADDR (u.start_data)
#define HOST_STACK_END_ADDR (u.start_stack + u.u_ssize * NBPG)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
