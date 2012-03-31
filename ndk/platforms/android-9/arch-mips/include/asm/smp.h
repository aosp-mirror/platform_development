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
#ifndef __ASM_SMP_H
#define __ASM_SMP_H
#include <linux/bitops.h>
#include <linux/linkage.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <linux/threads.h>
#include <linux/cpumask.h>
#include <asm/atomic.h>
#include <asm/smp-ops.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define raw_smp_processor_id() (current_thread_info()->cpu)
#define cpu_number_map(cpu) __cpu_number_map[cpu]
#define cpu_logical_map(cpu) __cpu_logical_map[cpu]
#define NO_PROC_ID (-1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SMP_RESCHEDULE_YOURSELF 0x1  
#define SMP_CALL_FUNCTION 0x2
#define cpu_possible_map phys_cpu_present_map
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
