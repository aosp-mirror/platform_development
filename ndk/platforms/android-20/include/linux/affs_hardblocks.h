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
#ifndef AFFS_HARDBLOCKS_H
#define AFFS_HARDBLOCKS_H
#include <linux/types.h>
struct RigidDiskBlock {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 rdb_ID;
 __be32 rdb_SummedLongs;
 __s32 rdb_ChkSum;
 __u32 rdb_HostID;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __be32 rdb_BlockBytes;
 __u32 rdb_Flags;
 __u32 rdb_BadBlockList;
 __be32 rdb_PartitionList;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 rdb_FileSysHeaderList;
 __u32 rdb_DriveInit;
 __u32 rdb_Reserved1[6];
 __u32 rdb_Cylinders;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 rdb_Sectors;
 __u32 rdb_Heads;
 __u32 rdb_Interleave;
 __u32 rdb_Park;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 rdb_Reserved2[3];
 __u32 rdb_WritePreComp;
 __u32 rdb_ReducedWrite;
 __u32 rdb_StepRate;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 rdb_Reserved3[5];
 __u32 rdb_RDBBlocksLo;
 __u32 rdb_RDBBlocksHi;
 __u32 rdb_LoCylinder;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 rdb_HiCylinder;
 __u32 rdb_CylBlocks;
 __u32 rdb_AutoParkSeconds;
 __u32 rdb_HighRDSKBlock;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 rdb_Reserved4;
 char rdb_DiskVendor[8];
 char rdb_DiskProduct[16];
 char rdb_DiskRevision[4];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char rdb_ControllerVendor[8];
 char rdb_ControllerProduct[16];
 char rdb_ControllerRevision[4];
 __u32 rdb_Reserved5[10];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define IDNAME_RIGIDDISK 0x5244534B
struct PartitionBlock {
 __be32 pb_ID;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __be32 pb_SummedLongs;
 __s32 pb_ChkSum;
 __u32 pb_HostID;
 __be32 pb_Next;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 pb_Flags;
 __u32 pb_Reserved1[2];
 __u32 pb_DevFlags;
 __u8 pb_DriveName[32];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 pb_Reserved2[15];
 __be32 pb_Environment[17];
 __u32 pb_EReserved[15];
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IDNAME_PARTITION 0x50415254
#define RDB_ALLOCATION_LIMIT 16
#endif
