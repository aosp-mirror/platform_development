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
#ifndef _SGI_PI1_H
#define _SGI_PI1_H
struct pi1_regs {
 u8 _data[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 volatile u8 data;
 u8 _ctrl[3];
 volatile u8 ctrl;
#define PI1_CTRL_STROBE_N 0x01
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PI1_CTRL_AFD_N 0x02
#define PI1_CTRL_INIT_N 0x04
#define PI1_CTRL_SLIN_N 0x08
#define PI1_CTRL_IRQ_ENA 0x10
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PI1_CTRL_DIR 0x20
#define PI1_CTRL_SEL 0x40
 u8 _status[3];
 volatile u8 status;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PI1_STAT_DEVID 0x03  
#define PI1_STAT_NOINK 0x04  
#define PI1_STAT_ERROR 0x08
#define PI1_STAT_ONLINE 0x10
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PI1_STAT_PE 0x20
#define PI1_STAT_ACK 0x40
#define PI1_STAT_BUSY 0x80
 u8 _dmactrl[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 volatile u8 dmactrl;
#define PI1_DMACTRL_FIFO_EMPTY 0x01  
#define PI1_DMACTRL_ABORT 0x02  
#define PI1_DMACTRL_STDMODE 0x00  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PI1_DMACTRL_SGIMODE 0x04  
#define PI1_DMACTRL_RICOHMODE 0x08  
#define PI1_DMACTRL_HPMODE 0x0c  
#define PI1_DMACTRL_BLKMODE 0x10  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PI1_DMACTRL_FIFO_CLEAR 0x20  
#define PI1_DMACTRL_READ 0x40  
#define PI1_DMACTRL_RUN 0x80  
 u8 _intstat[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 volatile u8 intstat;
#define PI1_INTSTAT_ACK 0x04
#define PI1_INTSTAT_FEMPTY 0x08
#define PI1_INTSTAT_NOINK 0x10
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PI1_INTSTAT_ONLINE 0x20
#define PI1_INTSTAT_ERR 0x40
#define PI1_INTSTAT_PE 0x80
 u8 _intmask[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 volatile u8 intmask;
#define PI1_INTMASK_ACK 0x04
#define PI1_INTMASK_FIFO_EMPTY 0x08
#define PI1_INTMASK_NOINK 0x10
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PI1_INTMASK_ONLINE 0x20
#define PI1_INTMASK_ERR 0x40
#define PI1_INTMASK_PE 0x80
 u8 _timer1[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 volatile u8 timer1;
#define PI1_TIME1 0x27
 u8 _timer2[3];
 volatile u8 timer2;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PI1_TIME2 0x13
 u8 _timer3[3];
 volatile u8 timer3;
#define PI1_TIME3 0x10
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u8 _timer4[3];
 volatile u8 timer4;
#define PI1_TIME4 0x00
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
