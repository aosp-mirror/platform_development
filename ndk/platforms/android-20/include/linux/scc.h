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
#ifndef _UAPI_SCC_H
#define _UAPI_SCC_H
#define PA0HZP 0x00
#define EAGLE 0x01
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PC100 0x02
#define PRIMUS 0x04
#define DRSI 0x08
#define BAYCOM 0x10
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum SCC_ioctl_cmds {
 SIOCSCCRESERVED = SIOCDEVPRIVATE,
 SIOCSCCCFG,
 SIOCSCCINI,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 SIOCSCCCHANINI,
 SIOCSCCSMEM,
 SIOCSCCGKISS,
 SIOCSCCSKISS,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 SIOCSCCGSTAT,
 SIOCSCCCAL
};
enum L1_params {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 PARAM_DATA,
 PARAM_TXDELAY,
 PARAM_PERSIST,
 PARAM_SLOTTIME,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 PARAM_TXTAIL,
 PARAM_FULLDUP,
 PARAM_SOFTDCD,
 PARAM_MUTE,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 PARAM_DTR,
 PARAM_RTS,
 PARAM_SPEED,
 PARAM_ENDDELAY,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 PARAM_GROUP,
 PARAM_IDLE,
 PARAM_MIN,
 PARAM_MAXKEY,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 PARAM_WAIT,
 PARAM_MAXDEFER,
 PARAM_TX,
 PARAM_HWEVENT = 31,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 PARAM_RETURN = 255
};
enum FULLDUP_modes {
 KISS_DUPLEX_HALF,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 KISS_DUPLEX_FULL,
 KISS_DUPLEX_LINK,
 KISS_DUPLEX_OPTIMA
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define TIMER_OFF 65535U
#define NO_SUCH_PARAM 65534U
enum HWEVENT_opts {
 HWEV_DCD_ON,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 HWEV_DCD_OFF,
 HWEV_ALL_SENT
};
#define RXGROUP 0100
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define TXGROUP 0200
enum CLOCK_sources {
 CLK_DPLL,
 CLK_EXTERNAL,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 CLK_DIVIDER,
 CLK_BRG
};
enum TX_state {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 TXS_IDLE,
 TXS_BUSY,
 TXS_ACTIVE,
 TXS_NEWFRAME,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 TXS_IDLE2,
 TXS_WAIT,
 TXS_TIMEOUT
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef unsigned long io_port;
struct scc_stat {
 long rxints;
 long txints;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 long exints;
 long spints;
 long txframes;
 long rxframes;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 long rxerrs;
 long txerrs;
 unsigned int nospace;
 unsigned int rx_over;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int tx_under;
 unsigned int tx_state;
 int tx_queued;
 unsigned int maxqueue;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int bufsize;
};
struct scc_modem {
 long speed;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char clocksrc;
 char nrz;
};
struct scc_kiss_cmd {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int command;
 unsigned param;
};
struct scc_hw_config {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 io_port data_a;
 io_port ctrl_a;
 io_port data_b;
 io_port ctrl_b;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 io_port vector_latch;
 io_port special;
 int irq;
 long clock;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char option;
 char brand;
 char escc;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct scc_mem_config {
 unsigned int dummy;
 unsigned int bufsize;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct scc_calibrate {
 unsigned int time;
 unsigned char pattern;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
