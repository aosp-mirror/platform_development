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
#ifndef _ASM_I8259_H
#define _ASM_I8259_H
#include <linux/compiler.h>
#include <linux/spinlock.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <asm/io.h>
#include <irq.h>
#define PIC_MASTER_CMD 0x20
#define PIC_MASTER_IMR 0x21
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PIC_MASTER_ISR PIC_MASTER_CMD
#define PIC_MASTER_POLL PIC_MASTER_ISR
#define PIC_MASTER_OCW3 PIC_MASTER_ISR
#define PIC_SLAVE_CMD 0xa0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PIC_SLAVE_IMR 0xa1
#define PIC_CASCADE_IR 2
#define MASTER_ICW4_DEFAULT 0x01
#define SLAVE_ICW4_DEFAULT 0x01
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PIC_ICW4_AEOI 2
#endif
