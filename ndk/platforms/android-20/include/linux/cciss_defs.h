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
#ifndef CCISS_DEFS_H
#define CCISS_DEFS_H
#include <linux/types.h>
#define SENSEINFOBYTES 32
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CMD_SUCCESS 0x0000
#define CMD_TARGET_STATUS 0x0001
#define CMD_DATA_UNDERRUN 0x0002
#define CMD_DATA_OVERRUN 0x0003
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CMD_INVALID 0x0004
#define CMD_PROTOCOL_ERR 0x0005
#define CMD_HARDWARE_ERR 0x0006
#define CMD_CONNECTION_LOST 0x0007
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CMD_ABORTED 0x0008
#define CMD_ABORT_FAILED 0x0009
#define CMD_UNSOLICITED_ABORT 0x000A
#define CMD_TIMEOUT 0x000B
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CMD_UNABORTABLE 0x000C
#define XFER_NONE 0x00
#define XFER_WRITE 0x01
#define XFER_READ 0x02
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define XFER_RSVD 0x03
#define ATTR_UNTAGGED 0x00
#define ATTR_SIMPLE 0x04
#define ATTR_HEADOFQUEUE 0x05
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define ATTR_ORDERED 0x06
#define ATTR_ACA 0x07
#define TYPE_CMD 0x00
#define TYPE_MSG 0x01
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define BYTE __u8
#define WORD __u16
#define HWORD __u16
#define DWORD __u32
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CISS_MAX_LUN 1024
#define LEVEL2LUN 1
#define LEVEL3LUN 0
#pragma pack(1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef union _SCSI3Addr_struct {
 struct {
 BYTE Dev;
 BYTE Bus:6;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BYTE Mode:2;
 } PeripDev;
 struct {
 BYTE DevLSB;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BYTE DevMSB:6;
 BYTE Mode:2;
 } LogDev;
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BYTE Dev:5;
 BYTE Bus:3;
 BYTE Targ:6;
 BYTE Mode:2;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 } LogUnit;
} SCSI3Addr_struct;
typedef struct _PhysDevAddr_struct {
 DWORD TargetId:24;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 DWORD Bus:6;
 DWORD Mode:2;
 SCSI3Addr_struct Target[2];
} PhysDevAddr_struct;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef struct _LogDevAddr_struct {
 DWORD VolId:30;
 DWORD Mode:2;
 BYTE reserved[4];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} LogDevAddr_struct;
typedef union _LUNAddr_struct {
 BYTE LunAddrBytes[8];
 SCSI3Addr_struct SCSI3Lun[4];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 PhysDevAddr_struct PhysDev;
 LogDevAddr_struct LogDev;
} LUNAddr_struct;
typedef struct _RequestBlock_struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BYTE CDBLen;
 struct {
 BYTE Type:3;
 BYTE Attribute:3;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BYTE Direction:2;
 } Type;
 HWORD Timeout;
 BYTE CDB[16];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} RequestBlock_struct;
typedef union _MoreErrInfo_struct{
 struct {
 BYTE Reserved[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BYTE Type;
 DWORD ErrorInfo;
 } Common_Info;
 struct{
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BYTE Reserved[2];
 BYTE offense_size;
 BYTE offense_num;
 DWORD offense_value;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 } Invalid_Cmd;
} MoreErrInfo_struct;
typedef struct _ErrorInfo_struct {
 BYTE ScsiStatus;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BYTE SenseLen;
 HWORD CommandStatus;
 DWORD ResidualCnt;
 MoreErrInfo_struct MoreErrInfo;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BYTE SenseInfo[SENSEINFOBYTES];
} ErrorInfo_struct;
#pragma pack()
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
