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
#define MD_RESERVED_BYTES (64 * 1024)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_RESERVED_SECTORS (MD_RESERVED_BYTES / 512)
#define MD_NEW_SIZE_SECTORS(x) ((x & ~(MD_RESERVED_SECTORS - 1)) - MD_RESERVED_SECTORS)
#define MD_SB_BYTES 4096
#define MD_SB_WORDS (MD_SB_BYTES / 4)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_SB_SECTORS (MD_SB_BYTES / 512)
#define MD_SB_GENERIC_OFFSET 0
#define MD_SB_PERSONALITY_OFFSET 64
#define MD_SB_DISKS_OFFSET 128
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_SB_DESCRIPTOR_OFFSET 992
#define MD_SB_GENERIC_CONSTANT_WORDS 32
#define MD_SB_GENERIC_STATE_WORDS 32
#define MD_SB_GENERIC_WORDS (MD_SB_GENERIC_CONSTANT_WORDS + MD_SB_GENERIC_STATE_WORDS)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_SB_PERSONALITY_WORDS 64
#define MD_SB_DESCRIPTOR_WORDS 32
#define MD_SB_DISKS 27
#define MD_SB_DISKS_WORDS (MD_SB_DISKS*MD_SB_DESCRIPTOR_WORDS)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_SB_RESERVED_WORDS (1024 - MD_SB_GENERIC_WORDS - MD_SB_PERSONALITY_WORDS - MD_SB_DISKS_WORDS - MD_SB_DESCRIPTOR_WORDS)
#define MD_SB_EQUAL_WORDS (MD_SB_GENERIC_WORDS + MD_SB_PERSONALITY_WORDS + MD_SB_DISKS_WORDS)
#define MD_DISK_FAULTY 0
#define MD_DISK_ACTIVE 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_DISK_SYNC 2
#define MD_DISK_REMOVED 3
#define MD_DISK_WRITEMOSTLY 9
typedef struct mdp_device_descriptor_s {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 number;
 __u32 major;
 __u32 minor;
 __u32 raid_disk;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 state;
 __u32 reserved[MD_SB_DESCRIPTOR_WORDS - 5];
} mdp_disk_t;
#define MD_SB_MAGIC 0xa92b4efc
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_SB_CLEAN 0
#define MD_SB_ERRORS 1
#define MD_SB_BITMAP_PRESENT 8
typedef struct mdp_superblock_s {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 md_magic;
 __u32 major_version;
 __u32 minor_version;
 __u32 patch_version;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 gvalid_words;
 __u32 set_uuid0;
 __u32 ctime;
 __u32 level;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 size;
 __u32 nr_disks;
 __u32 raid_disks;
 __u32 md_minor;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 not_persistent;
 __u32 set_uuid1;
 __u32 set_uuid2;
 __u32 set_uuid3;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 gstate_creserved[MD_SB_GENERIC_CONSTANT_WORDS - 16];
 __u32 utime;
 __u32 state;
 __u32 active_disks;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 working_disks;
 __u32 failed_disks;
 __u32 spare_disks;
 __u32 sb_csum;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#if defined(__BYTE_ORDER) ? __BYTE_ORDER == __BIG_ENDIAN : defined(__BIG_ENDIAN)
 __u32 events_hi;
 __u32 events_lo;
 __u32 cp_events_hi;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 cp_events_lo;
#elif defined(__BYTE_ORDER) ? __BYTE_ORDER == __LITTLE_ENDIAN : defined(__LITTLE_ENDIAN)
 __u32 events_lo;
 __u32 events_hi;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 cp_events_lo;
 __u32 cp_events_hi;
#else
#error unspecified endianness
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
 __u32 recovery_cp;
 __u64 reshape_position;
 __u32 new_level;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 delta_disks;
 __u32 new_layout;
 __u32 new_chunk;
 __u32 gstate_sreserved[MD_SB_GENERIC_STATE_WORDS - 18];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 layout;
 __u32 chunk_size;
 __u32 root_pv;
 __u32 root_block;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 pstate_reserved[MD_SB_PERSONALITY_WORDS - 4];
 mdp_disk_t disks[MD_SB_DISKS];
 __u32 reserved[MD_SB_RESERVED_WORDS];
 mdp_disk_t this_disk;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} mdp_super_t;
#define MD_SUPERBLOCK_1_TIME_SEC_MASK ((1ULL<<40) - 1)
struct mdp_superblock_1 {
 __le32 magic;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le32 major_version;
 __le32 feature_map;
 __le32 pad0;
 __u8 set_uuid[16];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char set_name[32];
 __le64 ctime;
 __le32 level;
 __le32 layout;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le64 size;
 __le32 chunksize;
 __le32 raid_disks;
 __le32 bitmap_offset;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le32 new_level;
 __le64 reshape_position;
 __le32 delta_disks;
 __le32 new_layout;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le32 new_chunk;
 __le32 new_offset;
 __le64 data_offset;
 __le64 data_size;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le64 super_offset;
 __le64 recovery_offset;
 __le32 dev_number;
 __le32 cnt_corrected_read;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 device_uuid[16];
 __u8 devflags;
#define WriteMostly1 1
 __u8 bblog_shift;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le16 bblog_size;
 __le32 bblog_offset;
 __le64 utime;
 __le64 events;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le64 resync_offset;
 __le32 sb_csum;
 __le32 max_dev;
 __u8 pad3[64-32];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __le16 dev_roles[0];
};
#define MD_FEATURE_BITMAP_OFFSET 1
#define MD_FEATURE_RECOVERY_OFFSET 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_FEATURE_RESHAPE_ACTIVE 4
#define MD_FEATURE_BAD_BLOCKS 8
#define MD_FEATURE_REPLACEMENT 16
#define MD_FEATURE_RESHAPE_BACKWARDS 32
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MD_FEATURE_NEW_OFFSET 64
#define MD_FEATURE_ALL (MD_FEATURE_BITMAP_OFFSET   |MD_FEATURE_RECOVERY_OFFSET   |MD_FEATURE_RESHAPE_ACTIVE   |MD_FEATURE_BAD_BLOCKS   |MD_FEATURE_REPLACEMENT   |MD_FEATURE_RESHAPE_BACKWARDS   |MD_FEATURE_NEW_OFFSET   )
#endif
