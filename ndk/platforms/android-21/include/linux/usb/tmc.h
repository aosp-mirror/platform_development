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
#ifndef __LINUX_USB_TMC_H
#define __LINUX_USB_TMC_H
#define USBTMC_STATUS_SUCCESS 0x01
#define USBTMC_STATUS_PENDING 0x02
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBTMC_STATUS_FAILED 0x80
#define USBTMC_STATUS_TRANSFER_NOT_IN_PROGRESS 0x81
#define USBTMC_STATUS_SPLIT_NOT_IN_PROGRESS 0x82
#define USBTMC_STATUS_SPLIT_IN_PROGRESS 0x83
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBTMC_REQUEST_INITIATE_ABORT_BULK_OUT 1
#define USBTMC_REQUEST_CHECK_ABORT_BULK_OUT_STATUS 2
#define USBTMC_REQUEST_INITIATE_ABORT_BULK_IN 3
#define USBTMC_REQUEST_CHECK_ABORT_BULK_IN_STATUS 4
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBTMC_REQUEST_INITIATE_CLEAR 5
#define USBTMC_REQUEST_CHECK_CLEAR_STATUS 6
#define USBTMC_REQUEST_GET_CAPABILITIES 7
#define USBTMC_REQUEST_INDICATOR_PULSE 64
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBTMC_IOC_NR 91
#define USBTMC_IOCTL_INDICATOR_PULSE _IO(USBTMC_IOC_NR, 1)
#define USBTMC_IOCTL_CLEAR _IO(USBTMC_IOC_NR, 2)
#define USBTMC_IOCTL_ABORT_BULK_OUT _IO(USBTMC_IOC_NR, 3)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define USBTMC_IOCTL_ABORT_BULK_IN _IO(USBTMC_IOC_NR, 4)
#define USBTMC_IOCTL_CLEAR_OUT_HALT _IO(USBTMC_IOC_NR, 6)
#define USBTMC_IOCTL_CLEAR_IN_HALT _IO(USBTMC_IOC_NR, 7)
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
