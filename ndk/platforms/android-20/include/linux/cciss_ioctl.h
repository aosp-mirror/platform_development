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
#ifndef _UAPICCISS_IOCTLH
#define _UAPICCISS_IOCTLH
#include <linux/types.h>
#include <linux/ioctl.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <linux/cciss_defs.h>
#define CCISS_IOC_MAGIC 'B'
typedef struct _cciss_pci_info_struct
{
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char bus;
 unsigned char dev_fn;
 unsigned short domain;
 __u32 board_id;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} cciss_pci_info_struct;
typedef struct _cciss_coalint_struct
{
 __u32 delay;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 count;
} cciss_coalint_struct;
typedef char NodeName_type[16];
typedef __u32 Heartbeat_type;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CISS_PARSCSIU2 0x0001
#define CISS_PARCSCIU3 0x0002
#define CISS_FIBRE1G 0x0100
#define CISS_FIBRE2G 0x0200
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef __u32 BusTypes_type;
typedef char FirmwareVer_type[4];
typedef __u32 DriverVer_type;
#define MAX_KMALLOC_SIZE 128000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef struct _IOCTL_Command_struct {
 LUNAddr_struct LUN_info;
 RequestBlock_struct Request;
 ErrorInfo_struct error_info;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 WORD buf_size;
 BYTE __user *buf;
} IOCTL_Command_struct;
typedef struct _BIG_IOCTL_Command_struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 LUNAddr_struct LUN_info;
 RequestBlock_struct Request;
 ErrorInfo_struct error_info;
 DWORD malloc_size;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 DWORD buf_size;
 BYTE __user *buf;
} BIG_IOCTL_Command_struct;
typedef struct _LogvolInfo_struct{
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 LunID;
 int num_opens;
 int num_parts;
} LogvolInfo_struct;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CCISS_GETPCIINFO _IOR(CCISS_IOC_MAGIC, 1, cciss_pci_info_struct)
#define CCISS_GETINTINFO _IOR(CCISS_IOC_MAGIC, 2, cciss_coalint_struct)
#define CCISS_SETINTINFO _IOW(CCISS_IOC_MAGIC, 3, cciss_coalint_struct)
#define CCISS_GETNODENAME _IOR(CCISS_IOC_MAGIC, 4, NodeName_type)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CCISS_SETNODENAME _IOW(CCISS_IOC_MAGIC, 5, NodeName_type)
#define CCISS_GETHEARTBEAT _IOR(CCISS_IOC_MAGIC, 6, Heartbeat_type)
#define CCISS_GETBUSTYPES _IOR(CCISS_IOC_MAGIC, 7, BusTypes_type)
#define CCISS_GETFIRMVER _IOR(CCISS_IOC_MAGIC, 8, FirmwareVer_type)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CCISS_GETDRIVVER _IOR(CCISS_IOC_MAGIC, 9, DriverVer_type)
#define CCISS_REVALIDVOLS _IO(CCISS_IOC_MAGIC, 10)
#define CCISS_PASSTHRU _IOWR(CCISS_IOC_MAGIC, 11, IOCTL_Command_struct)
#define CCISS_DEREGDISK _IO(CCISS_IOC_MAGIC, 12)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CCISS_REGNEWDISK _IOW(CCISS_IOC_MAGIC, 13, int)
#define CCISS_REGNEWD _IO(CCISS_IOC_MAGIC, 14)
#define CCISS_RESCANDISK _IO(CCISS_IOC_MAGIC, 16)
#define CCISS_GETLUNINFO _IOR(CCISS_IOC_MAGIC, 17, LogvolInfo_struct)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CCISS_BIG_PASSTHRU _IOWR(CCISS_IOC_MAGIC, 18, BIG_IOCTL_Command_struct)
#endif
