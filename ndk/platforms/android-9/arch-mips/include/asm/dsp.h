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
#ifndef _ASM_DSP_H
#define _ASM_DSP_H
#include <asm/cpu.h>
#include <asm/cpu-features.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <asm/hazards.h>
#include <asm/mipsregs.h>
#define DSP_DEFAULT 0x00000000
#define DSP_MASK 0x3ff
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define __enable_dsp_hazard()  do {   asm("_ehb");  } while (0)
#define __save_dsp(tsk)  do {   tsk->thread.dsp.dspr[0] = mfhi1();   tsk->thread.dsp.dspr[1] = mflo1();   tsk->thread.dsp.dspr[2] = mfhi2();   tsk->thread.dsp.dspr[3] = mflo2();   tsk->thread.dsp.dspr[4] = mfhi3();   tsk->thread.dsp.dspr[5] = mflo3();   tsk->thread.dsp.dspcontrol = rddsp(DSP_MASK);  } while (0)
#define save_dsp(tsk)  do {   if (cpu_has_dsp)   __save_dsp(tsk);  } while (0)
#define __restore_dsp(tsk)  do {   mthi1(tsk->thread.dsp.dspr[0]);   mtlo1(tsk->thread.dsp.dspr[1]);   mthi2(tsk->thread.dsp.dspr[2]);   mtlo2(tsk->thread.dsp.dspr[3]);   mthi3(tsk->thread.dsp.dspr[4]);   mtlo3(tsk->thread.dsp.dspr[5]);   wrdsp(tsk->thread.dsp.dspcontrol, DSP_MASK);  } while (0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define restore_dsp(tsk)  do {   if (cpu_has_dsp)   __restore_dsp(tsk);  } while (0)
#define __get_dsp_regs(tsk)  ({   if (tsk == current)   __save_dsp(current);     tsk->thread.dsp.dspr;  })
#endif
