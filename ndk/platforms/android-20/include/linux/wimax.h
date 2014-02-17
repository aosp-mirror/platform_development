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
#ifndef __LINUX__WIMAX_H__
#define __LINUX__WIMAX_H__
#include <linux/types.h>
enum {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 WIMAX_GNL_VERSION = 01,
 WIMAX_GNL_ATTR_INVALID = 0x00,
 WIMAX_GNL_ATTR_MAX = 10,
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum {
 WIMAX_GNL_OP_MSG_FROM_USER,
 WIMAX_GNL_OP_MSG_TO_USER,
 WIMAX_GNL_OP_RFKILL,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 WIMAX_GNL_OP_RESET,
 WIMAX_GNL_RE_STATE_CHANGE,
 WIMAX_GNL_OP_STATE_GET,
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum {
 WIMAX_GNL_MSG_IFIDX = 1,
 WIMAX_GNL_MSG_PIPE_NAME,
 WIMAX_GNL_MSG_DATA,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
enum wimax_rf_state {
 WIMAX_RF_OFF = 0,
 WIMAX_RF_ON = 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 WIMAX_RF_QUERY = 2,
};
enum {
 WIMAX_GNL_RFKILL_IFIDX = 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 WIMAX_GNL_RFKILL_STATE,
};
enum {
 WIMAX_GNL_RESET_IFIDX = 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
enum {
 WIMAX_GNL_STGET_IFIDX = 1,
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum {
 WIMAX_GNL_STCH_IFIDX = 1,
 WIMAX_GNL_STCH_STATE_OLD,
 WIMAX_GNL_STCH_STATE_NEW,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
 enum wimax_st {
 __WIMAX_ST_NULL = 0,
 WIMAX_ST_DOWN,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __WIMAX_ST_QUIESCING,
 WIMAX_ST_UNINITIALIZED,
 WIMAX_ST_RADIO_OFF,
 WIMAX_ST_READY,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 WIMAX_ST_SCANNING,
 WIMAX_ST_CONNECTING,
 WIMAX_ST_CONNECTED,
 __WIMAX_ST_INVALID
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#endif
