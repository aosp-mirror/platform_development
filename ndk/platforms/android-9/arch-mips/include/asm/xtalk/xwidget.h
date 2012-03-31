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
#ifndef _ASM_XTALK_XWIDGET_H
#define _ASM_XTALK_XWIDGET_H
#include <linux/types.h>
#include <asm/xtalk/xtalk.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define WIDGET_ID 0x04
#define WIDGET_STATUS 0x0c
#define WIDGET_ERR_UPPER_ADDR 0x14
#define WIDGET_ERR_LOWER_ADDR 0x1c
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define WIDGET_CONTROL 0x24
#define WIDGET_REQ_TIMEOUT 0x2c
#define WIDGET_INTDEST_UPPER_ADDR 0x34
#define WIDGET_INTDEST_LOWER_ADDR 0x3c
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define WIDGET_ERR_CMD_WORD 0x44
#define WIDGET_LLP_CFG 0x4c
#define WIDGET_TFLUSH 0x54
#define WIDGET_REV_NUM 0xf0000000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define WIDGET_PART_NUM 0x0ffff000
#define WIDGET_MFG_NUM 0x00000ffe
#define WIDGET_REV_NUM_SHFT 28
#define WIDGET_PART_NUM_SHFT 12
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define WIDGET_MFG_NUM_SHFT 1
#define XWIDGET_PART_NUM(widgetid) (((widgetid) & WIDGET_PART_NUM) >> WIDGET_PART_NUM_SHFT)
#define XWIDGET_REV_NUM(widgetid) (((widgetid) & WIDGET_REV_NUM) >> WIDGET_REV_NUM_SHFT)
#define XWIDGET_MFG_NUM(widgetid) (((widgetid) & WIDGET_MFG_NUM) >> WIDGET_MFG_NUM_SHFT)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define WIDGET_LLP_REC_CNT 0xff000000
#define WIDGET_LLP_TX_CNT 0x00ff0000
#define WIDGET_PENDING 0x0000001f
#define WIDGET_ERR_UPPER_ADDR_ONLY 0x0000ffff
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define WIDGET_F_BAD_PKT 0x00010000
#define WIDGET_LLP_XBAR_CRD 0x0000f000
#define WIDGET_LLP_XBAR_CRD_SHFT 12
#define WIDGET_CLR_RLLP_CNT 0x00000800
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define WIDGET_CLR_TLLP_CNT 0x00000400
#define WIDGET_SYS_END 0x00000200
#define WIDGET_MAX_TRANS 0x000001f0
#define WIDGET_WIDGET_ID 0x0000000f
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define WIDGET_INT_VECTOR 0xff000000
#define WIDGET_INT_VECTOR_SHFT 24
#define WIDGET_TARGET_ID 0x000f0000
#define WIDGET_TARGET_ID_SHFT 16
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define WIDGET_UPP_ADDR 0x0000ffff
#define WIDGET_DIDN 0xf0000000
#define WIDGET_SIDN 0x0f000000
#define WIDGET_PACTYP 0x00f00000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define WIDGET_TNUM 0x000f8000
#define WIDGET_COHERENT 0x00004000
#define WIDGET_DS 0x00003000
#define WIDGET_GBR 0x00000800
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define WIDGET_VBPM 0x00000400
#define WIDGET_ERROR 0x00000200
#define WIDGET_BARRIER 0x00000100
#define WIDGET_LLP_MAXRETRY 0x03ff0000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define WIDGET_LLP_MAXRETRY_SHFT 16
#define WIDGET_LLP_NULLTIMEOUT 0x0000fc00
#define WIDGET_LLP_NULLTIMEOUT_SHFT 10
#define WIDGET_LLP_MAXBURST 0x000003ff
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define WIDGET_LLP_MAXBURST_SHFT 0
#ifndef __ASSEMBLY__
typedef u32 widgetreg_t;
typedef volatile struct widget_cfg {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 widgetreg_t w_pad_0;
 widgetreg_t w_id;
 widgetreg_t w_pad_1;
 widgetreg_t w_status;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 widgetreg_t w_pad_2;
 widgetreg_t w_err_upper_addr;
 widgetreg_t w_pad_3;
 widgetreg_t w_err_lower_addr;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 widgetreg_t w_pad_4;
 widgetreg_t w_control;
 widgetreg_t w_pad_5;
 widgetreg_t w_req_timeout;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 widgetreg_t w_pad_6;
 widgetreg_t w_intdest_upper_addr;
 widgetreg_t w_pad_7;
 widgetreg_t w_intdest_lower_addr;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 widgetreg_t w_pad_8;
 widgetreg_t w_err_cmd_word;
 widgetreg_t w_pad_9;
 widgetreg_t w_llp_cfg;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 widgetreg_t w_pad_10;
 widgetreg_t w_tflush;
} widget_cfg_t;
typedef struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned didn:4;
 unsigned sidn:4;
 unsigned pactyp:4;
 unsigned tnum:5;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned ct:1;
 unsigned ds:2;
 unsigned gbr:1;
 unsigned vbpm:1;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned error:1;
 unsigned bo:1;
 unsigned other:8;
} w_err_cmd_word_f;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef union {
 widgetreg_t r;
 w_err_cmd_word_f f;
} w_err_cmd_word_u;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef struct xwidget_info_s *xwidget_info_t;
typedef struct xwidget_hwid_s {
 xwidget_part_num_t part_num;
 xwidget_rev_num_t rev_num;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 xwidget_mfg_num_t mfg_num;
} *xwidget_hwid_t;
#define XWIDGET_HARDWARE_ID_MATCH(hwid1, hwid2)   (((hwid1)->part_num == (hwid2)->part_num) &&   (((hwid1)->mfg_num == XWIDGET_MFG_NUM_NONE) ||   ((hwid2)->mfg_num == XWIDGET_MFG_NUM_NONE) ||   ((hwid1)->mfg_num == (hwid2)->mfg_num)))
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
