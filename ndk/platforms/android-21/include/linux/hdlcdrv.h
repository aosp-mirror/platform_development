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
#ifndef _UAPI_HDLCDRV_H
#define _UAPI_HDLCDRV_H
struct hdlcdrv_params {
 int iobase;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int irq;
 int dma;
 int dma2;
 int seriobase;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int pariobase;
 int midiiobase;
};
struct hdlcdrv_channel_params {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int tx_delay;
 int tx_tail;
 int slottime;
 int ppersist;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int fulldup;
};
struct hdlcdrv_old_channel_state {
 int ptt;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int dcd;
 int ptt_keyed;
};
struct hdlcdrv_channel_state {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int ptt;
 int dcd;
 int ptt_keyed;
 unsigned long tx_packets;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long tx_errors;
 unsigned long rx_packets;
 unsigned long rx_errors;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct hdlcdrv_ioctl {
 int cmd;
 union {
 struct hdlcdrv_params mp;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct hdlcdrv_channel_params cp;
 struct hdlcdrv_channel_state cs;
 struct hdlcdrv_old_channel_state ocs;
 unsigned int calibrate;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char bits;
 char modename[128];
 char drivername[32];
 } data;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define HDLCDRVCTL_GETMODEMPAR 0
#define HDLCDRVCTL_SETMODEMPAR 1
#define HDLCDRVCTL_MODEMPARMASK 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define HDLCDRVCTL_GETCHANNELPAR 10
#define HDLCDRVCTL_SETCHANNELPAR 11
#define HDLCDRVCTL_OLDGETSTAT 20
#define HDLCDRVCTL_CALIBRATE 21
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define HDLCDRVCTL_GETSTAT 22
#define HDLCDRVCTL_GETSAMPLES 30
#define HDLCDRVCTL_GETBITS 31
#define HDLCDRVCTL_GETMODE 40
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define HDLCDRVCTL_SETMODE 41
#define HDLCDRVCTL_MODELIST 42
#define HDLCDRVCTL_DRIVERNAME 43
#define HDLCDRV_PARMASK_IOBASE (1<<0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define HDLCDRV_PARMASK_IRQ (1<<1)
#define HDLCDRV_PARMASK_DMA (1<<2)
#define HDLCDRV_PARMASK_DMA2 (1<<3)
#define HDLCDRV_PARMASK_SERIOBASE (1<<4)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define HDLCDRV_PARMASK_PARIOBASE (1<<5)
#define HDLCDRV_PARMASK_MIDIIOBASE (1<<6)
#endif
