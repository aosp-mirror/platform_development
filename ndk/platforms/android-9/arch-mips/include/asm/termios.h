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
#ifndef _ASM_TERMIOS_H
#define _ASM_TERMIOS_H
#include <asm/termbits.h>
#include <asm/ioctls.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct sgttyb {
 char sg_ispeed;
 char sg_ospeed;
 char sg_erase;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char sg_kill;
 int sg_flags;
};
struct tchars {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char t_intrc;
 char t_quitc;
 char t_startc;
 char t_stopc;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char t_eofc;
 char t_brkc;
};
struct ltchars {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char t_suspc;
 char t_dsuspc;
 char t_rprntc;
 char t_flushc;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char t_werasc;
 char t_lnextc;
};
struct winsize {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned short ws_row;
 unsigned short ws_col;
 unsigned short ws_xpixel;
 unsigned short ws_ypixel;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define NCC 8
struct termio {
 unsigned short c_iflag;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned short c_oflag;
 unsigned short c_cflag;
 unsigned short c_lflag;
 char c_line;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char c_cc[NCCS];
};
#define TIOCM_LE 0x001  
#define TIOCM_DTR 0x002  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define TIOCM_RTS 0x004  
#define TIOCM_ST 0x010  
#define TIOCM_SR 0x020  
#define TIOCM_CTS 0x040  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define TIOCM_CAR 0x100  
#define TIOCM_CD TIOCM_CAR
#define TIOCM_RNG 0x200  
#define TIOCM_RI TIOCM_RNG
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define TIOCM_DSR 0x400  
#define TIOCM_OUT1 0x2000
#define TIOCM_OUT2 0x4000
#define TIOCM_LOOP 0x8000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define N_TTY 0
#define N_SLIP 1
#define N_MOUSE 2
#define N_PPP 3
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define N_STRIP 4
#define N_AX25 5
#define N_X25 6
#define N_6PACK 7
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define N_MASC 8
#define N_R3964 9
#define N_PROFIBUS_FDL 10
#define N_IRDA 11
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define N_SMSBLOCK 12
#define N_HDLC 13
#define N_SYNC_PPP 14
#define N_HCI 15
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
