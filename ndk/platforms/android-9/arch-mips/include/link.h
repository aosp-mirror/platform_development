/* 
   For building unwind-dw2-fde-glibc.c for MIPS frame unwinding,
   we need to have <link.h> that defines struct dl_phdr_info,
   ELFW(type), and dl_iterate_phdr().
*/ 

#include <sys/types.h>
#include <elf.h>

struct dl_phdr_info
{
    Elf32_Addr dlpi_addr;
    const char *dlpi_name;
    const Elf32_Phdr *dlpi_phdr;
    Elf32_Half dlpi_phnum;
};

#if _MIPS_SZPTR == 32
#define ElfW(type)	Elf32_##type
#elif _MIPS_SZPTR == 64
#define ElfW(type)	Elf64_##type
#endif

int
dl_iterate_phdr(int (*cb)(struct dl_phdr_info *info, size_t size, void *data),
                void *data);
