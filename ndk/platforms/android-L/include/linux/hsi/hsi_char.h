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
#ifndef __HSI_CHAR_H
#define __HSI_CHAR_H
#define HSI_CHAR_MAGIC 'k'
#define HSC_IOW(num, dtype) _IOW(HSI_CHAR_MAGIC, num, dtype)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define HSC_IOR(num, dtype) _IOR(HSI_CHAR_MAGIC, num, dtype)
#define HSC_IOWR(num, dtype) _IOWR(HSI_CHAR_MAGIC, num, dtype)
#define HSC_IO(num) _IO(HSI_CHAR_MAGIC, num)
#define HSC_RESET HSC_IO(16)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define HSC_SET_PM HSC_IO(17)
#define HSC_SEND_BREAK HSC_IO(18)
#define HSC_SET_RX HSC_IOW(19, struct hsc_rx_config)
#define HSC_GET_RX HSC_IOW(20, struct hsc_rx_config)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define HSC_SET_TX HSC_IOW(21, struct hsc_tx_config)
#define HSC_GET_TX HSC_IOW(22, struct hsc_tx_config)
#define HSC_PM_DISABLE 0
#define HSC_PM_ENABLE 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define HSC_MODE_STREAM 1
#define HSC_MODE_FRAME 2
#define HSC_FLOW_SYNC 0
#define HSC_ARB_RR 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define HSC_ARB_PRIO 1
struct hsc_rx_config {
 uint32_t mode;
 uint32_t flow;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint32_t channels;
};
struct hsc_tx_config {
 uint32_t mode;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 uint32_t channels;
 uint32_t speed;
 uint32_t arb_mode;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
