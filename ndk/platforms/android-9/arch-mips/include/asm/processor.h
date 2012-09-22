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
#ifndef _ASM_PROCESSOR_H
#define _ASM_PROCESSOR_H
#include <linux/cpumask.h>
#include <linux/threads.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <asm/cachectl.h>
#include <asm/cpu.h>
#include <asm/cpu-info.h>
#include <asm/mipsregs.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <asm/prefetch.h>
#include <asm/system.h>
#define current_text_addr() ({ __label__ _l; _l: &&_l;})
#define TASK_SIZE 0x7fff8000UL
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define STACK_TOP TASK_SIZE
#define TASK_UNMAPPED_BASE ((TASK_SIZE / 3) & ~(PAGE_SIZE))
#define NUM_FPU_REGS 32
typedef __u64 fpureg_t;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct mips_fpu_struct {
 fpureg_t fpr[NUM_FPU_REGS];
 unsigned int fcr31;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NUM_DSP_REGS 6
typedef __u32 dspreg_t;
struct mips_dsp_state {
 dspreg_t dspr[NUM_DSP_REGS];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int dspcontrol;
};
#define INIT_CPUMASK {   {0,}  }
typedef struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long seg;
} mm_segment_t;
#define ARCH_MIN_TASKALIGN 8
struct mips_abi;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct thread_struct {
 unsigned long reg16;
 unsigned long reg17, reg18, reg19, reg20, reg21, reg22, reg23;
 unsigned long reg29, reg30, reg31;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long cp0_status;
 struct mips_fpu_struct fpu;
 struct mips_dsp_state dsp;
 unsigned long cp0_badvaddr;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long cp0_baduaddr;
 unsigned long error_code;
 unsigned long trap_no;
 unsigned long irix_trampoline;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long irix_oldctx;
 struct mips_abi *abi;
};
#define FPAFF_INIT
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define INIT_THREAD {       .reg16 = 0,   .reg17 = 0,   .reg18 = 0,   .reg19 = 0,   .reg20 = 0,   .reg21 = 0,   .reg22 = 0,   .reg23 = 0,   .reg29 = 0,   .reg30 = 0,   .reg31 = 0,       .cp0_status = 0,       .fpu = {   .fpr = {0,},   .fcr31 = 0,   },       FPAFF_INIT       .dsp = {   .dspr = {0, },   .dspcontrol = 0,   },       .cp0_badvaddr = 0,   .cp0_baduaddr = 0,   .error_code = 0,   .trap_no = 0,   .irix_trampoline = 0,   .irix_oldctx = 0,  }
struct task_struct;
#define release_thread(thread) do { } while(0)
#define prepare_to_copy(tsk) do { } while (0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __KSTK_TOS(tsk) ((unsigned long)task_stack_page(tsk) + THREAD_SIZE - 32)
#define task_pt_regs(tsk) ((struct pt_regs *)__KSTK_TOS(tsk) - 1)
#define KSTK_EIP(tsk) (task_pt_regs(tsk)->cp0_epc)
#define KSTK_ESP(tsk) (task_pt_regs(tsk)->regs[29])
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KSTK_STATUS(tsk) (task_pt_regs(tsk)->cp0_status)
#define cpu_relax() barrier()
#define return_address() ({__asm__ __volatile__("":::"$31");__builtin_return_address(0);})
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
