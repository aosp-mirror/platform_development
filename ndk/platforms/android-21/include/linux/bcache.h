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
#ifndef _LINUX_BCACHE_H
#define _LINUX_BCACHE_H
#include <asm/types.h>
#define BITMASK(name, type, field, offset, size)  static inline __u64 name(const type *k)  { return (k->field >> offset) & ~(~0ULL << size); }    static inline void SET_##name(type *k, __u64 v)  {   k->field &= ~(~(~0ULL << size) << offset);   k->field |= (v & ~(~0ULL << size)) << offset;  }
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct bkey {
 __u64 high;
 __u64 low;
 __u64 ptr[];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define KEY_FIELD(name, field, offset, size)   BITMASK(name, struct bkey, field, offset, size)
#define PTR_FIELD(name, offset, size)  static inline __u64 name(const struct bkey *k, unsigned i)  { return (k->ptr[i] >> offset) & ~(~0ULL << size); }    static inline void SET_##name(struct bkey *k, unsigned i, __u64 v)  {   k->ptr[i] &= ~(~(~0ULL << size) << offset);   k->ptr[i] |= (v & ~(~0ULL << size)) << offset;  }
#define KEY_SIZE_BITS 16
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KEY_MAX_U64S 8
#define KEY(inode, offset, size)  ((struct bkey) {   .high = (1ULL << 63) | ((__u64) (size) << 20) | (inode),   .low = (offset)  })
#define ZERO_KEY KEY(0, 0, 0)
#define MAX_KEY_INODE (~(~0 << 20))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MAX_KEY_OFFSET (~0ULL >> 1)
#define MAX_KEY KEY(MAX_KEY_INODE, MAX_KEY_OFFSET, 0)
#define KEY_START(k) (KEY_OFFSET(k) - KEY_SIZE(k))
#define START_KEY(k) KEY(KEY_INODE(k), KEY_START(k), 0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PTR_DEV_BITS 12
#define PTR_CHECK_DEV ((1 << PTR_DEV_BITS) - 1)
#define PTR(gen, offset, dev)   ((((__u64) dev) << 51) | ((__u64) offset) << 8 | gen)
#define bkey_copy(_dest, _src) memcpy(_dest, _src, bkey_bytes(_src))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define BKEY_PAD 8
#define BKEY_PADDED(key)   union { struct bkey key; __u64 key ## _pad[BKEY_PAD]; }
#define BCACHE_SB_VERSION_CDEV 0
#define BCACHE_SB_VERSION_BDEV 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define BCACHE_SB_VERSION_CDEV_WITH_UUID 3
#define BCACHE_SB_VERSION_BDEV_WITH_OFFSET 4
#define BCACHE_SB_MAX_VERSION 4
#define SB_SECTOR 8
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SB_SIZE 4096
#define SB_LABEL_SIZE 32
#define SB_JOURNAL_BUCKETS 256U
#define MAX_CACHES_PER_SET 8
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define BDEV_DATA_START_DEFAULT 16
struct cache_sb {
 __u64 csum;
 __u64 offset;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u64 version;
 __u8 magic[16];
 __u8 uuid[16];
 union {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 set_uuid[16];
 __u64 set_magic;
 };
 __u8 label[SB_LABEL_SIZE];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u64 flags;
 __u64 seq;
 __u64 pad[8];
 union {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct {
 __u64 nbuckets;
 __u16 block_size;
 __u16 bucket_size;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 nr_in_set;
 __u16 nr_this_dev;
 };
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u64 data_offset;
 };
 };
 __u32 last_mount;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 first_bucket;
 union {
 __u16 njournal_buckets;
 __u16 keys;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 };
 __u64 d[SB_JOURNAL_BUCKETS];
};
#define CACHE_REPLACEMENT_LRU 0U
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CACHE_REPLACEMENT_FIFO 1U
#define CACHE_REPLACEMENT_RANDOM 2U
#define CACHE_MODE_WRITETHROUGH 0U
#define CACHE_MODE_WRITEBACK 1U
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CACHE_MODE_WRITEAROUND 2U
#define CACHE_MODE_NONE 3U
#define BDEV_STATE_NONE 0U
#define BDEV_STATE_CLEAN 1U
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define BDEV_STATE_DIRTY 2U
#define BDEV_STATE_STALE 3U
#define JSET_MAGIC 0x245235c1a3625032ULL
#define PSET_MAGIC 0x6750e15f87337f91ULL
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define BSET_MAGIC 0x90135c78b99e07f5ULL
#define BCACHE_JSET_VERSION_UUIDv1 1
#define BCACHE_JSET_VERSION_UUID 1
#define BCACHE_JSET_VERSION 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct jset {
 __u64 csum;
 __u64 magic;
 __u64 seq;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 version;
 __u32 keys;
 __u64 last_seq;
 BKEY_PADDED(uuid_bucket);
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BKEY_PADDED(btree_root);
 __u16 btree_level;
 __u16 pad[3];
 __u64 prio_bucket[MAX_CACHES_PER_SET];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 union {
 struct bkey start[0];
 __u64 d[0];
 };
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct prio_set {
 __u64 csum;
 __u64 magic;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u64 seq;
 __u32 version;
 __u32 pad;
 __u64 next_bucket;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct bucket_disk {
 __u16 prio;
 __u8 gen;
 } __attribute((packed)) data[];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct uuid_entry {
 union {
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 uuid[16];
 __u8 label[32];
 __u32 first_reg;
 __u32 last_reg;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 invalidated;
 __u32 flags;
 __u64 sectors;
 };
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 pad[128];
 };
};
#define BCACHE_BSET_CSUM 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define BCACHE_BSET_VERSION 1
struct bset {
 __u64 csum;
 __u64 magic;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u64 seq;
 __u32 version;
 __u32 keys;
 union {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct bkey start[0];
 __u64 d[0];
 };
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct uuid_entry_v0 {
 __u8 uuid[16];
 __u8 label[32];
 __u32 first_reg;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 last_reg;
 __u32 invalidated;
 __u32 pad;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
