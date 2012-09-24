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
#ifndef _ASM_MODULE_H
#define _ASM_MODULE_H
#include <linux/list.h>
#include <asm/uaccess.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct mod_arch_specific {
 struct list_head dbe_list;
 const struct exception_table_entry *dbe_start;
 const struct exception_table_entry *dbe_end;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
typedef uint8_t Elf64_Byte;
typedef struct {
 Elf64_Addr r_offset;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 Elf64_Word r_sym;
 Elf64_Byte r_ssym;
 Elf64_Byte r_type3;
 Elf64_Byte r_type2;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 Elf64_Byte r_type;
} Elf64_Mips_Rel;
typedef struct {
 Elf64_Addr r_offset;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 Elf64_Word r_sym;
 Elf64_Byte r_ssym;
 Elf64_Byte r_type3;
 Elf64_Byte r_type2;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 Elf64_Byte r_type;
 Elf64_Sxword r_addend;
} Elf64_Mips_Rela;
#define Elf_Shdr Elf32_Shdr
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define Elf_Sym Elf32_Sym
#define Elf_Ehdr Elf32_Ehdr
#define Elf_Addr Elf32_Addr
#define Elf_Mips_Rel Elf32_Rel
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define Elf_Mips_Rela Elf32_Rela
#define ELF_MIPS_R_SYM(rel) ELF32_R_SYM(rel.r_info)
#define ELF_MIPS_R_TYPE(rel) ELF32_R_TYPE(rel.r_info)
#error MODULE_PROC_FAMILY undefined for your processor configuration
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define MODULE_KERNEL_TYPE "32BIT "
#define MODULE_KERNEL_SMTC ""
#define MODULE_ARCH_VERMAGIC   MODULE_PROC_FAMILY MODULE_KERNEL_TYPE MODULE_KERNEL_SMTC
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
