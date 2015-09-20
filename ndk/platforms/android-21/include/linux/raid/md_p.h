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
#ifndef _MD_P_H
#define _MD_P_H
#include <linux/types.h>
#include <asm/byteorder.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_RESERVED_BYTES (64 * 1024)
#define MD_RESERVED_SECTORS (MD_RESERVED_BYTES / 512)
#define MD_NEW_SIZE_SECTORS(x) ((x & ~(MD_RESERVED_SECTORS - 1)) - MD_RESERVED_SECTORS)
#define MD_SB_BYTES 4096
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_SB_WORDS (MD_SB_BYTES / 4)
#define MD_SB_SECTORS (MD_SB_BYTES / 512)
#define MD_SB_GENERIC_OFFSET 0
#define MD_SB_PERSONALITY_OFFSET 64
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_SB_DISKS_OFFSET 128
#define MD_SB_DESCRIPTOR_OFFSET 992
#define MD_SB_GENERIC_CONSTANT_WORDS 32
#define MD_SB_GENERIC_STATE_WORDS 32
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_SB_GENERIC_WORDS (MD_SB_GENERIC_CONSTANT_WORDS + MD_SB_GENERIC_STATE_WORDS)
#define MD_SB_PERSONALITY_WORDS 64
#define MD_SB_DESCRIPTOR_WORDS 32
#define MD_SB_DISKS 27
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_SB_DISKS_WORDS (MD_SB_DISKS*MD_SB_DESCRIPTOR_WORDS)
#define MD_SB_RESERVED_WORDS (1024 - MD_SB_GENERIC_WORDS - MD_SB_PERSONALITY_WORDS - MD_SB_DISKS_WORDS - MD_SB_DESCRIPTOR_WORDS)
#define MD_SB_EQUAL_WORDS (MD_SB_GENERIC_WORDS + MD_SB_PERSONALITY_WORDS + MD_SB_DISKS_WORDS)
#define MD_DISK_FAULTY 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_DISK_ACTIVE 1
#define MD_DISK_SYNC 2
#define MD_DISK_REMOVED 3
#define MD_DISK_WRITEMOSTLY 9
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef struct mdp_device_descriptor_s {
 __u32 number;
 __u32 major;
 __u32 minor;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 raid_disk;
 __u32 state;
 __u32 reserved[MD_SB_DESCRIPTOR_WORDS - 5];
} mdp_disk_t;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_SB_MAGIC 0xa92b4efc
#define MD_SB_CLEAN 0
#define MD_SB_ERRORS 1
#define MD_SB_BITMAP_PRESENT 8
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef struct mdp_superblock_s {
 __u32 md_magic;
 __u32 major_version;
 __u32 minor_version;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 patch_version;
 __u32 gvalid_words;
 __u32 set_uuid0;
 __u32 ctime;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 level;
 __u32 size;
 __u32 nr_disks;
 __u32 raid_disks;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 md_minor;
 __u32 not_persistent;
 __u32 set_uuid1;
 __u32 set_uuid2;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 set_uuid3;
 __u32 gstate_creserved[MD_SB_GENERIC_CONSTANT_WORDS - 16];
 __u32 utime;
 __u32 state;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 active_disks;
 __u32 working_disks;
 __u32 failed_disks;
 __u32 spare_disks;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 sb_csum;
#if defined(__BYTE_ORDER) ? __BYTE_ORDER == __BIG_ENDIAN : defined(__BIG_ENDIAN)
 __u32 events_hi;
 __u32 events_lo;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 cp_events_hi;
 __u32 cp_events_lo;
#elif defined(__BYTE_ORDER) ? __BYTE_ORDER == __LITTLE_ENDIAN : defined(__LITTLE_ENDIAN)
 __u32 events_lo;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 events_hi;
 __u32 cp_events_lo;
 __u32 cp_events_hi;
#else
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#error unspecified endianness
#endif
 __u32 recovery_cp;
 __u64 reshape_position;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 new_level;
 __u32 delta_disks;
 __u32 new_layout;
 __u32 new_chunk;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 gstate_sreserved[MD_SB_GENERIC_STATE_WORDS - 18];
 __u32 layout;
 __u32 chunk_size;
 __u32 root_pv;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 root_block;
 __u32 pstate_reserved[MD_SB_PERSONALITY_WORDS - 4];
 mdp_disk_t disks[MD_SB_DISKS];
 __u32 reserved[MD_SB_RESERVED_WORDS];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mdp_disk_t this_disk;
} mdp_super_t;
#define MD_SUPERBLOCK_1_TIME_SEC_MASK ((1ULL<<40) - 1)
struct mdp_superblock_1 {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le32 magic;
 __le32 major_version;
 __le32 feature_map;
 __le32 pad0;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 set_uuid[16];
 char set_name[32];
 __le64 ctime;
 __le32 level;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le32 layout;
 __le64 size;
 __le32 chunksize;
 __le32 raid_disks;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le32 bitmap_offset;
 __le32 new_level;
 __le64 reshape_position;
 __le32 delta_disks;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le32 new_layout;
 __le32 new_chunk;
 __le32 new_offset;
 __le64 data_offset;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le64 data_size;
 __le64 super_offset;
 __le64 recovery_offset;
 __le32 dev_number;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le32 cnt_corrected_read;
 __u8 device_uuid[16];
 __u8 devflags;
#define WriteMostly1 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 bblog_shift;
 __le16 bblog_size;
 __le32 bblog_offset;
 __le64 utime;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le64 events;
 __le64 resync_offset;
 __le32 sb_csum;
 __le32 max_dev;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 pad3[64-32];
 __le16 dev_roles[0];
};
#define MD_FEATURE_BITMAP_OFFSET 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_FEATURE_RECOVERY_OFFSET 2
#define MD_FEATURE_RESHAPE_ACTIVE 4
#define MD_FEATURE_BAD_BLOCKS 8
#define MD_FEATURE_REPLACEMENT 16
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_FEATURE_RESHAPE_BACKWARDS 32
#define MD_FEATURE_NEW_OFFSET 64
#define MD_FEATURE_RECOVERY_BITMAP 128
#define MD_FEATURE_ALL (MD_FEATURE_BITMAP_OFFSET   |MD_FEATURE_RECOVERY_OFFSET   |MD_FEATURE_RESHAPE_ACTIVE   |MD_FEATURE_BAD_BLOCKS   |MD_FEATURE_REPLACEMENT   |MD_FEATURE_RESHAPE_BACKWARDS   |MD_FEATURE_NEW_OFFSET   |MD_FEATURE_RECOVERY_BITMAP   )
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
