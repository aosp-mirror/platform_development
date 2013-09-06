#ifndef __ASM_ELF_H
#define __ASM_ELF_H

#include <asm/ptrace.h>
#include <asm/user.h>

typedef unsigned long elf_greg_t;
typedef unsigned long elf_freg_t[3];


#define ELF_NGREG (sizeof (struct pt_regs) / sizeof(elf_greg_t))
typedef elf_greg_t elf_gregset_t[ELF_NGREG];

typedef struct user_fp elf_fpregset_t;

#define ELF_CLASS ELFCLASS32
#define ELF_DATA ELFDATA2LSB

#define USE_ELF_CORE_DUMP
#define ELF_EXEC_PAGESIZE 4096

#define ELF_ET_DYN_BASE (2 * TASK_SIZE / 3)


#define ELF_HWCAP (elf_hwcap)

#define ELF_PLATFORM_SIZE 8

#define ELF_PLATFORM (elf_platform)

#endif
