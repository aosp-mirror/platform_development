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
#ifndef __NEC_VR41XX_IRQ_H
#define __NEC_VR41XX_IRQ_H
#define MIPS_CPU_IRQ_BASE 0
#define MIPS_CPU_IRQ(x) (MIPS_CPU_IRQ_BASE + (x))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MIPS_SOFTINT0_IRQ MIPS_CPU_IRQ(0)
#define MIPS_SOFTINT1_IRQ MIPS_CPU_IRQ(1)
#define INT0_IRQ MIPS_CPU_IRQ(2)
#define INT1_IRQ MIPS_CPU_IRQ(3)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define INT2_IRQ MIPS_CPU_IRQ(4)
#define INT3_IRQ MIPS_CPU_IRQ(5)
#define INT4_IRQ MIPS_CPU_IRQ(6)
#define TIMER_IRQ MIPS_CPU_IRQ(7)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SYSINT1_IRQ_BASE 8
#define SYSINT1_IRQ(x) (SYSINT1_IRQ_BASE + (x))
#define BATTRY_IRQ SYSINT1_IRQ(0)
#define POWER_IRQ SYSINT1_IRQ(1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define RTCLONG1_IRQ SYSINT1_IRQ(2)
#define ELAPSEDTIME_IRQ SYSINT1_IRQ(3)
#define PIU_IRQ SYSINT1_IRQ(5)
#define AIU_IRQ SYSINT1_IRQ(6)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KIU_IRQ SYSINT1_IRQ(7)
#define GIUINT_IRQ SYSINT1_IRQ(8)
#define SIU_IRQ SYSINT1_IRQ(9)
#define BUSERR_IRQ SYSINT1_IRQ(10)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SOFTINT_IRQ SYSINT1_IRQ(11)
#define CLKRUN_IRQ SYSINT1_IRQ(12)
#define DOZEPIU_IRQ SYSINT1_IRQ(13)
#define SYSINT1_IRQ_LAST DOZEPIU_IRQ
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SYSINT2_IRQ_BASE 24
#define SYSINT2_IRQ(x) (SYSINT2_IRQ_BASE + (x))
#define RTCLONG2_IRQ SYSINT2_IRQ(0)
#define LED_IRQ SYSINT2_IRQ(1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define HSP_IRQ SYSINT2_IRQ(2)
#define TCLOCK_IRQ SYSINT2_IRQ(3)
#define FIR_IRQ SYSINT2_IRQ(4)
#define CEU_IRQ SYSINT2_IRQ(4)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define DSIU_IRQ SYSINT2_IRQ(5)
#define PCI_IRQ SYSINT2_IRQ(6)
#define SCU_IRQ SYSINT2_IRQ(7)
#define CSI_IRQ SYSINT2_IRQ(8)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define BCU_IRQ SYSINT2_IRQ(9)
#define ETHERNET_IRQ SYSINT2_IRQ(10)
#define SYSINT2_IRQ_LAST ETHERNET_IRQ
#define GIU_IRQ_BASE 40
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define GIU_IRQ(x) (GIU_IRQ_BASE + (x))  
#define GIU_IRQ_LAST GIU_IRQ(31)
#define VRC4173_IRQ_BASE 72
#define VRC4173_IRQ(x) (VRC4173_IRQ_BASE + (x))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VRC4173_USB_IRQ VRC4173_IRQ(0)
#define VRC4173_PCMCIA2_IRQ VRC4173_IRQ(1)
#define VRC4173_PCMCIA1_IRQ VRC4173_IRQ(2)
#define VRC4173_PS2CH2_IRQ VRC4173_IRQ(3)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VRC4173_PS2CH1_IRQ VRC4173_IRQ(4)
#define VRC4173_PIU_IRQ VRC4173_IRQ(5)
#define VRC4173_AIU_IRQ VRC4173_IRQ(6)
#define VRC4173_KIU_IRQ VRC4173_IRQ(7)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VRC4173_GIU_IRQ VRC4173_IRQ(8)
#define VRC4173_AC97_IRQ VRC4173_IRQ(9)
#define VRC4173_AC97INT1_IRQ VRC4173_IRQ(10)
#define VRC4173_DOZEPIU_IRQ VRC4173_IRQ(13)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VRC4173_IRQ_LAST VRC4173_DOZEPIU_IRQ
#endif
