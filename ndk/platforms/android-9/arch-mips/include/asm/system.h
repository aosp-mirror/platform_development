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
#ifndef _ASM_SYSTEM_H
#define _ASM_SYSTEM_H
#include <linux/types.h>
#include <linux/irqflags.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <asm/addrspace.h>
#include <asm/barrier.h>
#include <asm/cmpxchg.h>
#include <asm/cpu-features.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <asm/dsp.h>
#include <asm/war.h>
struct task_struct;
#define __mips_mt_fpaff_switch_to(prev) do { (void) (prev); } while (0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define switch_to(prev, next, last)  do {   __mips_mt_fpaff_switch_to(prev);   if (cpu_has_dsp)   __save_dsp(prev);   (last) = resume(prev, next, task_thread_info(next));  } while (0)
#define finish_arch_switch(prev)  do {   if (cpu_has_dsp)   __restore_dsp(current);   if (cpu_has_userlocal)   write_c0_userlocal(current_thread_info()->tp_value);  } while (0)
#define __xchg_u64 __xchg_u64_unsupported_on_32bit_kernels
#define xchg(ptr, x) ((__typeof__(*(ptr)))__xchg((unsigned long)(x), (ptr), sizeof(*(ptr))))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef void (*vi_handler_t)(void);
#define __ARCH_WANT_UNLOCKED_CTXSW
#endif
