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
#ifndef __ASM_MIPS_DEC_IOASIC_ADDRS_H
#define __ASM_MIPS_DEC_IOASIC_ADDRS_H
#define IOASIC_SLOT_SIZE 0x00040000
#define IOASIC_SYS_ROM (0*IOASIC_SLOT_SIZE)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IOASIC_IOCTL (1*IOASIC_SLOT_SIZE)  
#define IOASIC_ESAR (2*IOASIC_SLOT_SIZE)  
#define IOASIC_LANCE (3*IOASIC_SLOT_SIZE)  
#define IOASIC_SCC0 (4*IOASIC_SLOT_SIZE)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IOASIC_VDAC_HI (5*IOASIC_SLOT_SIZE)  
#define IOASIC_SCC1 (6*IOASIC_SLOT_SIZE)  
#define IOASIC_VDAC_LO (7*IOASIC_SLOT_SIZE)  
#define IOASIC_TOY (8*IOASIC_SLOT_SIZE)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IOASIC_ISDN (9*IOASIC_SLOT_SIZE)  
#define IOASIC_ERRADDR (9*IOASIC_SLOT_SIZE)  
#define IOASIC_CHKSYN (10*IOASIC_SLOT_SIZE)  
#define IOASIC_ACC_BUS (10*IOASIC_SLOT_SIZE)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IOASIC_MCR (11*IOASIC_SLOT_SIZE)  
#define IOASIC_FLOPPY (11*IOASIC_SLOT_SIZE)  
#define IOASIC_SCSI (12*IOASIC_SLOT_SIZE)  
#define IOASIC_FDC_DMA (13*IOASIC_SLOT_SIZE)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IOASIC_SCSI_DMA (14*IOASIC_SLOT_SIZE)  
#define IOASIC_RES_15 (15*IOASIC_SLOT_SIZE)  
#define IO_REG_SCSI_DMA_P 0x00  
#define IO_REG_SCSI_DMA_BP 0x10  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IO_REG_LANCE_DMA_P 0x20  
#define IO_REG_SCC0A_T_DMA_P 0x30  
#define IO_REG_SCC0A_R_DMA_P 0x40  
#define IO_REG_SCC1A_T_DMA_P 0x50  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IO_REG_SCC1A_R_DMA_P 0x60  
#define IO_REG_AB_T_DMA_P 0x50  
#define IO_REG_AB_R_DMA_P 0x60  
#define IO_REG_FLOPPY_DMA_P 0x70  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IO_REG_ISDN_T_DMA_P 0x80  
#define IO_REG_ISDN_T_DMA_BP 0x90  
#define IO_REG_ISDN_R_DMA_P 0xa0  
#define IO_REG_ISDN_R_DMA_BP 0xb0  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IO_REG_DATA_0 0xc0  
#define IO_REG_DATA_1 0xd0  
#define IO_REG_DATA_2 0xe0  
#define IO_REG_DATA_3 0xf0  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IO_REG_SSR 0x100  
#define IO_REG_SIR 0x110  
#define IO_REG_SIMR 0x120  
#define IO_REG_SAR 0x130  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IO_REG_ISDN_T_DATA 0x140  
#define IO_REG_ISDN_R_DATA 0x150  
#define IO_REG_LANCE_SLOT 0x160  
#define IO_REG_SCSI_SLOT 0x170  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IO_REG_SCC0A_SLOT 0x180  
#define IO_REG_SCC1A_SLOT 0x190  
#define IO_REG_AB_SLOT 0x190  
#define IO_REG_FLOPPY_SLOT 0x1a0  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IO_REG_SCSI_SCR 0x1b0  
#define IO_REG_SCSI_SDR0 0x1c0  
#define IO_REG_SCSI_SDR1 0x1d0  
#define IO_REG_FCTR 0x1e0  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IO_REG_RES_31 0x1f0  
#define IO_SSR_SCC0A_TX_DMA_EN (1<<31)  
#define IO_SSR_SCC0A_RX_DMA_EN (1<<30)  
#define IO_SSR_RES_27 (1<<27)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IO_SSR_RES_26 (1<<26)  
#define IO_SSR_RES_25 (1<<25)  
#define IO_SSR_RES_24 (1<<24)  
#define IO_SSR_RES_23 (1<<23)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IO_SSR_SCSI_DMA_DIR (1<<18)  
#define IO_SSR_SCSI_DMA_EN (1<<17)  
#define IO_SSR_LANCE_DMA_EN (1<<16)  
#define IO_SSR_SCC1A_TX_DMA_EN (1<<29)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IO_SSR_SCC1A_RX_DMA_EN (1<<28)  
#define IO_SSR_RES_22 (1<<22)  
#define IO_SSR_RES_21 (1<<21)  
#define IO_SSR_RES_20 (1<<20)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IO_SSR_RES_19 (1<<19)  
#define IO_SSR_AB_TX_DMA_EN (1<<29)  
#define IO_SSR_AB_RX_DMA_EN (1<<28)  
#define IO_SSR_FLOPPY_DMA_DIR (1<<22)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IO_SSR_FLOPPY_DMA_EN (1<<21)  
#define IO_SSR_ISDN_TX_DMA_EN (1<<20)  
#define IO_SSR_ISDN_RX_DMA_EN (1<<19)  
#define KN0X_IO_SSR_DIAGDN (1<<15)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KN0X_IO_SSR_SCC_RST (1<<11)  
#define KN0X_IO_SSR_RTC_RST (1<<10)  
#define KN0X_IO_SSR_ASC_RST (1<<9)  
#define KN0X_IO_SSR_LANCE_RST (1<<8)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
