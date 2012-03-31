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
#ifndef _SGI_IOC_H
#define _SGI_IOC_H
#include <linux/types.h>
#include <asm/sgi/pi1.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct sgioc_uart_regs {
 u8 _ctrl1[3];
 volatile u8 ctrl1;
 u8 _data1[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 volatile u8 data1;
 u8 _ctrl2[3];
 volatile u8 ctrl2;
 u8 _data2[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 volatile u8 data2;
};
struct sgioc_keyb_regs {
 u8 _data[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 volatile u8 data;
 u8 _command[3];
 volatile u8 command;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct sgint_regs {
 u8 _istat0[3];
 volatile u8 istat0;
#define SGINT_ISTAT0_FFULL 0x01
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGINT_ISTAT0_SCSI0 0x02
#define SGINT_ISTAT0_SCSI1 0x04
#define SGINT_ISTAT0_ENET 0x08
#define SGINT_ISTAT0_GFXDMA 0x10
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGINT_ISTAT0_PPORT 0x20
#define SGINT_ISTAT0_HPC2 0x40
#define SGINT_ISTAT0_LIO2 0x80
 u8 _imask0[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 volatile u8 imask0;
 u8 _istat1[3];
 volatile u8 istat1;
#define SGINT_ISTAT1_ISDNI 0x01
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGINT_ISTAT1_PWR 0x02
#define SGINT_ISTAT1_ISDNH 0x04
#define SGINT_ISTAT1_LIO3 0x08
#define SGINT_ISTAT1_HPC3 0x10
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGINT_ISTAT1_AFAIL 0x20
#define SGINT_ISTAT1_VIDEO 0x40
#define SGINT_ISTAT1_GIO2 0x80
 u8 _imask1[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 volatile u8 imask1;
 u8 _vmeistat[3];
 volatile u8 vmeistat;
 u8 _cmeimask0[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 volatile u8 cmeimask0;
 u8 _cmeimask1[3];
 volatile u8 cmeimask1;
 u8 _cmepol[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 volatile u8 cmepol;
 u8 _tclear[3];
 volatile u8 tclear;
 u8 _errstat[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 volatile u8 errstat;
 u32 _unused0[2];
 u8 _tcnt0[3];
 volatile u8 tcnt0;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u8 _tcnt1[3];
 volatile u8 tcnt1;
 u8 _tcnt2[3];
 volatile u8 tcnt2;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u8 _tcword[3];
 volatile u8 tcword;
#define SGINT_TCWORD_BCD 0x01  
#define SGINT_TCWORD_MMASK 0x0e  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGINT_TCWORD_MITC 0x00  
#define SGINT_TCWORD_MOS 0x02  
#define SGINT_TCWORD_MRGEN 0x04  
#define SGINT_TCWORD_MSWGEN 0x06  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGINT_TCWORD_MSWST 0x08  
#define SGINT_TCWORD_MHWST 0x0a  
#define SGINT_TCWORD_CMASK 0x30  
#define SGINT_TCWORD_CLAT 0x00  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGINT_TCWORD_CLSB 0x10  
#define SGINT_TCWORD_CMSB 0x20  
#define SGINT_TCWORD_CALL 0x30  
#define SGINT_TCWORD_CNT0 0x00  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGINT_TCWORD_CNT1 0x40  
#define SGINT_TCWORD_CNT2 0x80  
#define SGINT_TCWORD_CRBCK 0xc0  
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGINT_TIMER_CLOCK 1000000
#define SGINT_TCSAMP_COUNTER ((SGINT_TIMER_CLOCK / HZ) + 255)
struct sgioc_regs {
 struct pi1_regs pport;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u32 _unused0[2];
 struct sgioc_uart_regs uart;
 struct sgioc_keyb_regs kbdmouse;
 u8 _gcsel[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 volatile u8 gcsel;
 u8 _genctrl[3];
 volatile u8 genctrl;
 u8 _panel[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 volatile u8 panel;
#define SGIOC_PANEL_POWERON 0x01
#define SGIOC_PANEL_POWERINTR 0x02
#define SGIOC_PANEL_VOLDNINTR 0x10
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGIOC_PANEL_VOLDNHOLD 0x20
#define SGIOC_PANEL_VOLUPINTR 0x40
#define SGIOC_PANEL_VOLUPHOLD 0x80
 u32 _unused1;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u8 _sysid[3];
 volatile u8 sysid;
#define SGIOC_SYSID_FULLHOUSE 0x01
#define SGIOC_SYSID_BOARDREV(x) (((x) & 0x1e) >> 1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGIOC_SYSID_CHIPREV(x) (((x) & 0xe0) >> 5)
 u32 _unused2;
 u8 _read[3];
 volatile u8 read;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u32 _unused3;
 u8 _dmasel[3];
 volatile u8 dmasel;
#define SGIOC_DMASEL_SCLK10MHZ 0x00  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGIOC_DMASEL_ISDNB 0x01  
#define SGIOC_DMASEL_ISDNA 0x02  
#define SGIOC_DMASEL_PPORT 0x04  
#define SGIOC_DMASEL_SCLK667MHZ 0x10  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGIOC_DMASEL_SCLKEXT 0x20  
 u32 _unused4;
 u8 _reset[3];
 volatile u8 reset;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGIOC_RESET_PPORT 0x01  
#define SGIOC_RESET_KBDMOUSE 0x02  
#define SGIOC_RESET_EISA 0x04  
#define SGIOC_RESET_ISDN 0x08  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGIOC_RESET_LC0OFF 0x10  
#define SGIOC_RESET_LC1OFF 0x20  
 u32 _unused5;
 u8 _write[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 volatile u8 write;
#define SGIOC_WRITE_NTHRESH 0x01  
#define SGIOC_WRITE_TPSPEED 0x02  
#define SGIOC_WRITE_EPSEL 0x04  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGIOC_WRITE_EASEL 0x08  
#define SGIOC_WRITE_U1AMODE 0x10  
#define SGIOC_WRITE_U0AMODE 0x20  
#define SGIOC_WRITE_MLO 0x40  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGIOC_WRITE_MHI 0x80  
 u32 _unused6;
 struct sgint_regs int3;
 u32 _unused7[16];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 volatile u32 extio;
#define EXTIO_S0_IRQ_3 0x8000  
#define EXTIO_S0_IRQ_2 0x4000  
#define EXTIO_S0_IRQ_1 0x2000  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define EXTIO_S0_RETRACE 0x1000
#define EXTIO_SG_IRQ_3 0x0800  
#define EXTIO_SG_IRQ_2 0x0400  
#define EXTIO_SG_IRQ_1 0x0200  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define EXTIO_SG_RETRACE 0x0100
#define EXTIO_GIO_33MHZ 0x0080
#define EXTIO_EISA_BUSERR 0x0040
#define EXTIO_MC_BUSERR 0x0020
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define EXTIO_HPC3_BUSERR 0x0010
#define EXTIO_S0_STAT_1 0x0008
#define EXTIO_S0_STAT_0 0x0004
#define EXTIO_SG_STAT_1 0x0002
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define EXTIO_SG_STAT_0 0x0001
};
#endif
