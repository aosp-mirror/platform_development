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
#ifndef __ASM_SPINLOCK_H
#define __ASM_SPINLOCK_H
#include <asm/atomic.h>
#include <asm/rwlock.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <asm/page.h>
#include <asm/processor.h>
#include <linux/compiler.h>
#define CLI_STRING "cli"
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define STI_STRING "sti"
#define CLI_STI_CLOBBERS
#define CLI_STI_INPUT_ARGS
#define _raw_spin_relax(lock) cpu_relax()
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define _raw_read_relax(lock) cpu_relax()
#define _raw_write_relax(lock) cpu_relax()
#endif
