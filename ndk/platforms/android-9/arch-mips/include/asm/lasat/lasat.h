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
#ifndef _LASAT_H
#define _LASAT_H
#ifndef _LANGUAGE_ASSEMBLY
enum lasat_mtdparts {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 LASAT_MTD_BOOTLOADER,
 LASAT_MTD_SERVICE,
 LASAT_MTD_NORMAL,
 LASAT_MTD_CONFIG,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 LASAT_MTD_FS,
 LASAT_MTD_LAST
};
#include <linux/types.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LASAT_EEPROM_VERSION 7
struct lasat_eeprom_struct {
 unsigned int version;
 unsigned int cfg[3];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char hwaddr[6];
 unsigned char print_partno[12];
 unsigned char term0;
 unsigned char print_serial[14];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char term1;
 unsigned char prod_partno[12];
 unsigned char term2;
 unsigned char prod_serial[14];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char term3;
 unsigned char passwd_hash[16];
 unsigned char pwdnull;
 unsigned char vendid;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char ts_ref;
 unsigned char ts_signoff;
 unsigned char reserved[11];
 unsigned char debugaccess;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned short prid;
 unsigned int serviceflag;
 unsigned int ipaddr;
 unsigned int netmask;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int crc32;
};
struct lasat_eeprom_struct_pre7 {
 unsigned int version;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int flags[3];
 unsigned char hwaddr0[6];
 unsigned char hwaddr1[6];
 unsigned char print_partno[9];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char term0;
 unsigned char print_serial[14];
 unsigned char term1;
 unsigned char prod_partno[9];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char term2;
 unsigned char prod_serial[14];
 unsigned char term3;
 unsigned char passwd_hash[24];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char pwdnull;
 unsigned char vendor;
 unsigned char ts_ref;
 unsigned char ts_signoff;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char reserved[6];
 unsigned int writecount;
 unsigned int ipaddr;
 unsigned int netmask;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int crc32;
};
#define LASAT_W0_DSCTYPE(v) (((v)) & 0xf)
#define LASAT_W0_BMID(v) (((v) >> 0x04) & 0xf)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LASAT_W0_CPUTYPE(v) (((v) >> 0x08) & 0xf)
#define LASAT_W0_BUSSPEED(v) (((v) >> 0x0c) & 0xf)
#define LASAT_W0_CPUCLK(v) (((v) >> 0x10) & 0xf)
#define LASAT_W0_SDRAMBANKSZ(v) (((v) >> 0x14) & 0xf)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LASAT_W0_SDRAMBANKS(v) (((v) >> 0x18) & 0xf)
#define LASAT_W0_L2CACHE(v) (((v) >> 0x1c) & 0xf)
#define LASAT_W1_EDHAC(v) (((v)) & 0xf)
#define LASAT_W1_HIFN(v) (((v) >> 0x04) & 0x1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LASAT_W1_ISDN(v) (((v) >> 0x05) & 0x1)
#define LASAT_W1_IDE(v) (((v) >> 0x06) & 0x1)
#define LASAT_W1_HDLC(v) (((v) >> 0x07) & 0x1)
#define LASAT_W1_USVERSION(v) (((v) >> 0x08) & 0x1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LASAT_W1_4MACS(v) (((v) >> 0x09) & 0x1)
#define LASAT_W1_EXTSERIAL(v) (((v) >> 0x0a) & 0x1)
#define LASAT_W1_FLASHSIZE(v) (((v) >> 0x0c) & 0xf)
#define LASAT_W1_PCISLOTS(v) (((v) >> 0x10) & 0xf)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LASAT_W1_PCI1OPT(v) (((v) >> 0x14) & 0xf)
#define LASAT_W1_PCI2OPT(v) (((v) >> 0x18) & 0xf)
#define LASAT_W1_PCI3OPT(v) (((v) >> 0x1c) & 0xf)
#define LASAT_BMID_MASQUERADE2 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LASAT_BMID_MASQUERADEPRO 1
#define LASAT_BMID_SAFEPIPE25 2
#define LASAT_BMID_SAFEPIPE50 3
#define LASAT_BMID_SAFEPIPE100 4
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LASAT_BMID_SAFEPIPE5000 5
#define LASAT_BMID_SAFEPIPE7000 6
#define LASAT_BMID_SAFEPIPE1000 7
#define LASAT_BMID_UNKNOWN 0xf
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LASAT_MAX_BMID_NAMES 9  
#define LASAT_HAS_EDHAC (1 << 0)
#define LASAT_EDHAC_FAST (1 << 1)
#define LASAT_HAS_EADI (1 << 2)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LASAT_HAS_HIFN (1 << 3)
#define LASAT_HAS_ISDN (1 << 4)
#define LASAT_HAS_LEASEDLINE_IF (1 << 5)
#define LASAT_HAS_HDC (1 << 6)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LASAT_PRID_MASQUERADE2 0
#define LASAT_PRID_MASQUERADEPRO 1
#define LASAT_PRID_SAFEPIPE25 2
#define LASAT_PRID_SAFEPIPE50 3
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LASAT_PRID_SAFEPIPE100 4
#define LASAT_PRID_SAFEPIPE5000 5
#define LASAT_PRID_SAFEPIPE7000 6
#define LASAT_PRID_SAFEPIPE30 7
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LASAT_PRID_SAFEPIPE5100 8
#define LASAT_PRID_SAFEPIPE7100 9
#define LASAT_PRID_SAFEPIPE1110 10
#define LASAT_PRID_SAFEPIPE3020 11
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LASAT_PRID_SAFEPIPE3030 12
#define LASAT_PRID_SAFEPIPE5020 13
#define LASAT_PRID_SAFEPIPE5030 14
#define LASAT_PRID_SAFEPIPE1120 15
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LASAT_PRID_SAFEPIPE1130 16
#define LASAT_PRID_SAFEPIPE6010 17
#define LASAT_PRID_SAFEPIPE6110 18
#define LASAT_PRID_SAFEPIPE6210 19
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LASAT_PRID_SAFEPIPE1020 20
#define LASAT_PRID_SAFEPIPE1040 21
#define LASAT_PRID_SAFEPIPE1060 22
struct lasat_info {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int li_cpu_hz;
 unsigned int li_bus_hz;
 unsigned int li_bmid;
 unsigned int li_memsize;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int li_flash_size;
 unsigned int li_prid;
 unsigned char li_bmstr[16];
 unsigned char li_namestr[32];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char li_typestr[16];
 unsigned int li_flash_base;
 unsigned long li_flashpart_base[LASAT_MTD_LAST];
 unsigned long li_flashpart_size[LASAT_MTD_LAST];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct lasat_eeprom_struct li_eeprom_info;
 unsigned int li_eeprom_upgrade_version;
 unsigned int li_debugaccess;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define N_MACHTYPES 2
#include <linux/delay.h>
#define LASAT_100_DIVIDER 20
#define LASAT_200_DIVIDER 8
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IS_LASAT_200() (current_cpu_data.cputype == CPU_R5000)
#endif
#define LASAT_SERVICEMODE_MAGIC_1 0xdeadbeef
#define LASAT_SERVICEMODE_MAGIC_2 0xfedeabba
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LASAT_GT_BASE (KSEG1ADDR(0x14000000))
#define Vrc5074_PHYS_BASE 0x1fa00000
#define Vrc5074_BASE (KSEG1ADDR(Vrc5074_PHYS_BASE))
#define PCI_WINDOW1 0x1a000000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
