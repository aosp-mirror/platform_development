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
#ifndef _ASM_IRQ_H
#define _ASM_IRQ_H
#include <linux/linkage.h>
#include <asm/mipsmtregs.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <irq.h>
#define irq_canonicalize(irq) (irq)  
#define IRQ_AFFINITY_HOOK(irq) do { } while (0)
#define __DO_IRQ_SMTC_HOOK(irq)  do {   IRQ_AFFINITY_HOOK(irq);  } while (0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __NO_AFFINITY_IRQ_SMTC_HOOK(irq) do { } while (0)
#define do_IRQ(irq)  do {   irq_enter();   __DO_IRQ_SMTC_HOOK(irq);   generic_handle_irq(irq);   irq_exit();  } while (0)
#define CP0_LEGACY_COMPARE_IRQ 7
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
