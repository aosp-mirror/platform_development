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
#ifndef __ASM_MPSPEC_DEF_H
#define __ASM_MPSPEC_DEF_H
#define SMP_MAGIC_IDENT (('_'<<24)|('P'<<16)|('M'<<8)|'_')
#define MAX_MPC_ENTRY 1024
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MAX_APICS 256
struct intel_mp_floating
{
 char mpf_signature[4];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long mpf_physptr;
 unsigned char mpf_length;
 unsigned char mpf_specification;
 unsigned char mpf_checksum;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char mpf_feature1;
 unsigned char mpf_feature2;
 unsigned char mpf_feature3;
 unsigned char mpf_feature4;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char mpf_feature5;
};
struct mp_config_table
{
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char mpc_signature[4];
#define MPC_SIGNATURE "PCMP"
 unsigned short mpc_length;
 char mpc_spec;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char mpc_checksum;
 char mpc_oem[8];
 char mpc_productid[12];
 unsigned long mpc_oemptr;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned short mpc_oemsize;
 unsigned short mpc_oemcount;
 unsigned long mpc_lapic;
 unsigned long reserved;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define MP_PROCESSOR 0
#define MP_BUS 1
#define MP_IOAPIC 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MP_INTSRC 3
#define MP_LINTSRC 4
#define MP_TRANSLATION 192  
struct mpc_config_processor
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
{
 unsigned char mpc_type;
 unsigned char mpc_apicid;
 unsigned char mpc_apicver;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char mpc_cpuflag;
#define CPU_ENABLED 1  
#define CPU_BOOTPROCESSOR 2  
 unsigned long mpc_cpufeature;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CPU_STEPPING_MASK 0x0F
#define CPU_MODEL_MASK 0xF0
#define CPU_FAMILY_MASK 0xF00
 unsigned long mpc_featureflag;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long mpc_reserved[2];
};
struct mpc_config_bus
{
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char mpc_type;
 unsigned char mpc_busid;
 unsigned char mpc_bustype[6];
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define BUSTYPE_EISA "EISA"
#define BUSTYPE_ISA "ISA"
#define BUSTYPE_INTERN "INTERN"  
#define BUSTYPE_MCA "MCA"
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define BUSTYPE_VL "VL"  
#define BUSTYPE_PCI "PCI"
#define BUSTYPE_PCMCIA "PCMCIA"
#define BUSTYPE_CBUS "CBUS"
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define BUSTYPE_CBUSII "CBUSII"
#define BUSTYPE_FUTURE "FUTURE"
#define BUSTYPE_MBI "MBI"
#define BUSTYPE_MBII "MBII"
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define BUSTYPE_MPI "MPI"
#define BUSTYPE_MPSA "MPSA"
#define BUSTYPE_NUBUS "NUBUS"
#define BUSTYPE_TC "TC"
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define BUSTYPE_VME "VME"
#define BUSTYPE_XPRESS "XPRESS"
struct mpc_config_ioapic
{
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char mpc_type;
 unsigned char mpc_apicid;
 unsigned char mpc_apicver;
 unsigned char mpc_flags;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MPC_APIC_USABLE 0x01
 unsigned long mpc_apicaddr;
};
struct mpc_config_intsrc
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
{
 unsigned char mpc_type;
 unsigned char mpc_irqtype;
 unsigned short mpc_irqflag;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char mpc_srcbus;
 unsigned char mpc_srcbusirq;
 unsigned char mpc_dstapic;
 unsigned char mpc_dstirq;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
enum mp_irq_source_types {
 mp_INT = 0,
 mp_NMI = 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mp_SMI = 2,
 mp_ExtINT = 3
};
#define MP_IRQDIR_DEFAULT 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MP_IRQDIR_HIGH 1
#define MP_IRQDIR_LOW 3
struct mpc_config_lintsrc
{
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char mpc_type;
 unsigned char mpc_irqtype;
 unsigned short mpc_irqflag;
 unsigned char mpc_srcbusid;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char mpc_srcbusirq;
 unsigned char mpc_destapic;
#define MP_APIC_ALL 0xFF
 unsigned char mpc_destapiclint;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct mp_config_oemtable
{
 char oem_signature[4];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MPC_OEM_SIGNATURE "_OEM"
 unsigned short oem_length;
 char oem_rev;
 char oem_checksum;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char mpc_oem[8];
};
struct mpc_config_translation
{
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char mpc_type;
 unsigned char trans_len;
 unsigned char trans_type;
 unsigned char trans_quad;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char trans_global;
 unsigned char trans_local;
 unsigned short trans_reserved;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum mp_bustype {
 MP_BUS_ISA = 1,
 MP_BUS_EISA,
 MP_BUS_PCI,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 MP_BUS_MCA,
};
#endif
