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
#ifndef __ASM_JAZZ_H
#define __ASM_JAZZ_H
#define JAZZ_LOCAL_IO_SPACE 0xe0000000
#define PICA_ASIC_REVISION 0xe0000008
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PICA_LED 0xe000f000
#define LED_DOT 0x01
#define LED_SPACE 0x00
#define LED_0 0xfc
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LED_1 0x60
#define LED_2 0xda
#define LED_3 0xf2
#define LED_4 0x66
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LED_5 0xb6
#define LED_6 0xbe
#define LED_7 0xe0
#define LED_8 0xfe
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LED_9 0xf6
#define LED_A 0xee
#define LED_b 0x3e
#define LED_C 0x9c
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LED_d 0x7a
#define LED_E 0x9e
#define LED_F 0x8e
#ifndef __ASSEMBLY__
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#define JAZZ_ETHERNET_BASE 0xe0001000
#define JAZZ_SCSI_BASE 0xe0002000
#define JAZZ_KEYBOARD_ADDRESS 0xe0005000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define JAZZ_KEYBOARD_DATA 0xe0005000
#define JAZZ_KEYBOARD_COMMAND 0xe0005001
#ifndef __ASSEMBLY__
typedef struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char data;
 unsigned char command;
} jazz_keyboard_hardware;
#define jazz_kh ((keyboard_hardware *) JAZZ_KEYBOARD_ADDRESS)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef struct {
 unsigned char pad0[3];
 unsigned char data;
 unsigned char pad1[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char command;
} mips_keyboard_hardware;
#define keyboard_hardware jazz_keyboard_hardware
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MIPS_KEYBOARD_ADDRESS 0xb9005000
#define MIPS_KEYBOARD_DATA 0xb9005003
#define MIPS_KEYBOARD_COMMAND 0xb9005007
#define JAZZ_SERIAL1_BASE (unsigned int)0xe0006000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define JAZZ_SERIAL2_BASE (unsigned int)0xe0007000
#define JAZZ_PARALLEL_BASE (unsigned int)0xe0008000
#define JAZZ_DUMMY_DEVICE 0xe000d000
#define JAZZ_TIMER_INTERVAL 0xe0000228
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define JAZZ_TIMER_REGISTER 0xe0000230
#ifndef __ASSEMBLY__
#ifdef __MIPSEL__
typedef struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int bank2 : 3;
 unsigned int bank1 : 3;
 unsigned int mem_bus_width : 1;
 unsigned int reserved2 : 1;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int page_mode : 1;
 unsigned int reserved1 : 23;
} dram_configuration;
#else
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef struct {
 unsigned int reserved1 : 23;
 unsigned int page_mode : 1;
 unsigned int reserved2 : 1;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int mem_bus_width : 1;
 unsigned int bank1 : 3;
 unsigned int bank2 : 3;
} dram_configuration;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#endif
#define PICA_DRAM_CONFIG 0xe00fffe0
#define JAZZ_IO_IRQ_SOURCE 0xe0010000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define JAZZ_IO_IRQ_ENABLE 0xe0010002
#define JAZZ_IRQ_START 24
#define JAZZ_IRQ_END (24 + 9)
#define JAZZ_PARALLEL_IRQ (JAZZ_IRQ_START + 0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define JAZZ_FLOPPY_IRQ (JAZZ_IRQ_START + 1)
#define JAZZ_SOUND_IRQ (JAZZ_IRQ_START + 2)
#define JAZZ_VIDEO_IRQ (JAZZ_IRQ_START + 3)
#define JAZZ_ETHERNET_IRQ (JAZZ_IRQ_START + 4)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define JAZZ_SCSI_IRQ (JAZZ_IRQ_START + 5)
#define JAZZ_KEYBOARD_IRQ (JAZZ_IRQ_START + 6)
#define JAZZ_MOUSE_IRQ (JAZZ_IRQ_START + 7)
#define JAZZ_SERIAL1_IRQ (JAZZ_IRQ_START + 8)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define JAZZ_SERIAL2_IRQ (JAZZ_IRQ_START + 9)
#define JAZZ_TIMER_IRQ (MIPS_CPU_IRQ_BASE+6)
#define JAZZ_SCSI_DMA 0  
#define JAZZ_FLOPPY_DMA 1  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define JAZZ_AUDIOL_DMA 2  
#define JAZZ_AUDIOR_DMA 3  
#define JAZZ_R4030_CONFIG 0xE0000000  
#define JAZZ_R4030_REVISION 0xE0000008  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define JAZZ_R4030_INV_ADDR 0xE0000010  
#define JAZZ_R4030_TRSTBL_BASE 0xE0000018  
#define JAZZ_R4030_TRSTBL_LIM 0xE0000020  
#define JAZZ_R4030_TRSTBL_INV 0xE0000028  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define JAZZ_R4030_CACHE_MTNC 0xE0000030  
#define JAZZ_R4030_R_FAIL_ADDR 0xE0000038  
#define JAZZ_R4030_M_FAIL_ADDR 0xE0000040  
#define JAZZ_R4030_CACHE_PTAG 0xE0000048  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define JAZZ_R4030_CACHE_LTAG 0xE0000050  
#define JAZZ_R4030_CACHE_BMASK 0xE0000058  
#define JAZZ_R4030_CACHE_BWIN 0xE0000060  
#define JAZZ_R4030_REM_SPEED 0xE0000070  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define JAZZ_R4030_IRQ_ENABLE 0xE00000E8  
#define JAZZ_R4030_INVAL_ADDR 0xE0000010  
#define JAZZ_R4030_IRQ_SOURCE 0xE0000200  
#define JAZZ_R4030_I386_ERROR 0xE0000208  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define JAZZ_EISA_IRQ_ACK 0xE0000238  
#ifndef __ASSEMBLY__
#endif
#define JAZZ_FDC_BASE 0xe0003000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define JAZZ_RTC_BASE 0xe0004000
#define JAZZ_PORT_BASE 0xe2000000
#define JAZZ_EISA_BASE 0xe3000000
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
