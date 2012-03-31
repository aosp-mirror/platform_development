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
#ifndef _ASM_SGIARCS_H
#define _ASM_SGIARCS_H
#include <asm/types.h>
#include <asm/fw/arc/types.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PROM_ESUCCESS 0x00
#define PROM_E2BIG 0x01
#define PROM_EACCESS 0x02
#define PROM_EAGAIN 0x03
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PROM_EBADF 0x04
#define PROM_EBUSY 0x05
#define PROM_EFAULT 0x06
#define PROM_EINVAL 0x07
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PROM_EIO 0x08
#define PROM_EISDIR 0x09
#define PROM_EMFILE 0x0a
#define PROM_EMLINK 0x0b
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PROM_ENAMETOOLONG 0x0c
#define PROM_ENODEV 0x0d
#define PROM_ENOENT 0x0e
#define PROM_ENOEXEC 0x0f
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PROM_ENOMEM 0x10
#define PROM_ENOSPC 0x11
#define PROM_ENOTDIR 0x12
#define PROM_ENOTTY 0x13
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PROM_ENXIO 0x14
#define PROM_EROFS 0x15
#define PROM_EADDRNOTAVAIL 0x1f
#define PROM_ETIMEDOUT 0x20
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PROM_ECONNABORTED 0x21
#define PROM_ENOCONNECT 0x22
enum linux_devclass {
 system, processor, cache, adapter, controller, peripheral, memory
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
enum linux_devtypes {
 Arc, Cpu, Fpu,
 picache, pdcache,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 sicache, sdcache, sccache,
 memdev, eisa_adapter, tc_adapter, scsi_adapter, dti_adapter,
 multifunc_adapter, dsk_controller, tp_controller, cdrom_controller,
 worm_controller, serial_controller, net_controller, disp_controller,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 parallel_controller, ptr_controller, kbd_controller, audio_controller,
 misc_controller, disk_peripheral, flpy_peripheral, tp_peripheral,
 modem_peripheral, monitor_peripheral, printer_peripheral,
 ptr_peripheral, kbd_peripheral, term_peripheral, line_peripheral,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 net_peripheral, misc_peripheral, anon
};
enum linux_identifier {
 bogus, ronly, removable, consin, consout, input, output
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct linux_component {
 enum linux_devclass class;
 enum linux_devtypes type;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 enum linux_identifier iflags;
 USHORT vers;
 USHORT rev;
 ULONG key;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ULONG amask;
 ULONG cdsize;
 ULONG ilen;
 _PULONG iname;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
typedef struct linux_component pcomponent;
struct linux_sysid {
 char vend[8], prod[8];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
enum arcs_memtypes {
 arcs_eblock,
 arcs_rvpage,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 arcs_fcontig,
 arcs_free,
 arcs_bmem,
 arcs_prog,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 arcs_atmp,
 arcs_aperm,
};
enum arc_memtypes {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 arc_eblock,
 arc_rvpage,
 arc_free,
 arc_bmem,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 arc_prog,
 arc_atmp,
 arc_aperm,
 arc_fcontig,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
union linux_memtypes {
 enum arcs_memtypes arcs;
 enum arc_memtypes arc;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct linux_mdesc {
 union linux_memtypes type;
 ULONG base;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ULONG pages;
};
struct linux_tinfo {
 unsigned short yr;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned short mnth;
 unsigned short day;
 unsigned short hr;
 unsigned short min;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned short sec;
 unsigned short msec;
};
struct linux_vdirent {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ULONG namelen;
 unsigned char attr;
 char fname[32];
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum linux_omode {
 rdonly, wronly, rdwr, wronly_creat, rdwr_creat,
 wronly_ssede, rdwr_ssede, dirent, dirent_creat
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum linux_seekmode {
 absolute, relative
};
enum linux_mountops {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 media_load, media_unload
};
struct linux_bigint {
#ifdef __MIPSEL__
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u32 lo;
 s32 hi;
#else
 s32 hi;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u32 lo;
#endif
};
struct linux_finfo {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct linux_bigint begin;
 struct linux_bigint end;
 struct linux_bigint cur;
 enum linux_devtypes dtype;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long namelen;
 unsigned char attr;
 char name[32];
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct linux_romvec {
 LONG load;
 LONG invoke;
 LONG exec;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 LONG halt;
 LONG pdown;
 LONG restart;
 LONG reboot;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 LONG imode;
 LONG _unused1;
 LONG next_component;
 LONG child_component;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 LONG parent_component;
 LONG component_data;
 LONG child_add;
 LONG comp_del;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 LONG component_by_path;
 LONG cfg_save;
 LONG get_sysid;
 LONG get_mdesc;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 LONG _unused2;
 LONG get_tinfo;
 LONG get_rtime;
 LONG get_vdirent;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 LONG open;
 LONG close;
 LONG read;
 LONG get_rstatus;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 LONG write;
 LONG seek;
 LONG mount;
 LONG get_evar;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 LONG set_evar;
 LONG get_finfo;
 LONG set_finfo;
 LONG cache_flush;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 LONG TestUnicodeCharacter;
 LONG GetDisplayStatus;
};
typedef struct _SYSTEM_PARAMETER_BLOCK {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ULONG magic;
#define PROMBLOCK_MAGIC 0x53435241
 ULONG len;
 USHORT ver;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 USHORT rev;
 _PLONG rs_block;
 _PLONG dbg_block;
 _PLONG gevect;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 _PLONG utlbvect;
 ULONG rveclen;
 _PVOID romvec;
 ULONG pveclen;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 _PVOID pvector;
 ULONG adap_cnt;
 ULONG adap_typ0;
 ULONG adap_vcnt0;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 _PVOID adap_vector;
 ULONG adap_typ1;
 ULONG adap_vcnt1;
 _PVOID adap_vector1;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} SYSTEM_PARAMETER_BLOCK, *PSYSTEM_PARAMETER_BLOCK;
#define PROMBLOCK ((PSYSTEM_PARAMETER_BLOCK) (int)0xA0001000)
#define ROMVECTOR ((struct linux_romvec *) (long)(PROMBLOCK)->romvec)
union linux_cache_key {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct param {
#ifdef __MIPSEL__
 unsigned short size;
 unsigned char lsize;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char bsize;
#else
 unsigned char bsize;
 unsigned char lsize;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned short size;
#endif
 } info;
 unsigned long allinfo;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct linux_cdata {
 char *name;
 int mlen;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 enum linux_devtypes type;
};
#define SGIPROM_STDIN 0
#define SGIPROM_STDOUT 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGIPROM_ROFILE 0x01  
#define SGIPROM_HFILE 0x02  
#define SGIPROM_SFILE 0x04  
#define SGIPROM_AFILE 0x08  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGIPROM_DFILE 0x10  
#define SGIPROM_DELFILE 0x20  
struct sgi_partition {
 unsigned char flag;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGIPART_UNUSED 0x00
#define SGIPART_ACTIVE 0x80
 unsigned char shead, ssect, scyl;
 unsigned char systype;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char ehead, esect, ecyl;
 unsigned char rsect0, rsect1, rsect2, rsect3;
 unsigned char tsect0, tsect1, tsect2, tsect3;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SGIBBLOCK_MAGIC 0xaa55
#define SGIBBLOCK_MAXPART 0x0004
struct sgi_bootblock {
 unsigned char _unused[446];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct sgi_partition partitions[SGIBBLOCK_MAXPART];
 unsigned short magic;
};
struct sgi_bparm_block {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned short bytes_sect;
 unsigned char sect_clust;
 unsigned short sect_resv;
 unsigned char nfats;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned short nroot_dirents;
 unsigned short sect_volume;
 unsigned char media_type;
 unsigned short sect_fat;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned short sect_track;
 unsigned short nheads;
 unsigned short nhsects;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct sgi_bsector {
 unsigned char jmpinfo[3];
 unsigned char manuf_name[8];
 struct sgi_bparm_block info;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define SMB_DEBUG_MAGIC 0xfeeddead
struct linux_smonblock {
 unsigned long magic;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 void (*handler)(void);
 unsigned long dtable_base;
 int (*printf)(const char *fmt, ...);
 unsigned long btable_base;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long mpflushreqs;
 unsigned long ntab;
 unsigned long stab;
 int smax;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#endif
