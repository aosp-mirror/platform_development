/*	$NetBSD: elf_machdep.h,v 1.4 2010/03/18 08:28:33 cegger Exp $	*/

#if !defined __i386__

#define	ELF32_MACHDEP_ENDIANNESS	ELFDATA2LSB
#define	ELF32_MACHDEP_ID_CASES						\
		case EM_386:						\
			break;

#define	ELF64_MACHDEP_ENDIANNESS	ELFDATA2LSB
#define	ELF64_MACHDEP_ID_CASES						\
		case EM_X86_64:						\
			break;

#define	ELF32_MACHDEP_ID	EM_386
#define	ELF64_MACHDEP_ID	EM_X86_64

#define ARCH_ELFSIZE		64	/* MD native binary size */

/* x86-64 relocations */

#define R_X86_64_NONE		0
#define R_X86_64_64		1
#define R_X86_64_PC32		2
#define R_X86_64_GOT32		3
#define R_X86_64_PLT32		4
#define R_X86_64_COPY		5
#define R_X86_64_GLOB_DAT	6
#define R_X86_64_JUMP_SLOT	7
#define R_X86_64_RELATIVE	8
#define R_X86_64_GOTPCREL	9
#define R_X86_64_32		10
#define R_X86_64_32S		11
#define R_X86_64_16		12
#define R_X86_64_PC16		13
#define R_X86_64_8		14
#define R_X86_64_PC8		15

/* TLS relocations */
#define R_X86_64_DTPMOD64	16
#define R_X86_64_DTPOFF64	17
#define R_X86_64_TPOFF64	18
#define R_X86_64_TLSGD		19
#define R_X86_64_TLSLD		20
#define R_X86_64_DTPOFF32	21
#define R_X86_64_GOTTPOFF	22
#define R_X86_64_TPOFF32	23

#define	R_TYPE(name)	__CONCAT(R_X86_64_,name)

#else	/*	!__i386__	*/

#include <i386/elf_machdep.h>

#endif	/*	!__i386__	*/
