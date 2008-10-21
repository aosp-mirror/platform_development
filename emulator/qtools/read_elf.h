#ifndef READ_ELF_H
#define READ_ELF_H

#include <stdio.h>
#include <elf.h>

Elf32_Ehdr *ReadElfHeader(FILE *fobj);
Elf32_Shdr *ReadSectionHeaders(Elf32_Ehdr *hdr, FILE *fobj);
char *ReadStringTable(Elf32_Ehdr *hdr, Elf32_Shdr *shdr, FILE *fobj);
Elf32_Shdr *FindSymbolTableSection(Elf32_Ehdr *hdr,
                                   Elf32_Shdr *shdr,
                                   char *string_table);
Elf32_Shdr *FindSymbolStringTableSection(Elf32_Ehdr *hdr,
                                         Elf32_Shdr *shdr,
                                         char *string_table);
int ReadSection(Elf32_Shdr *shdr, void *buffer, FILE *f);
void AdjustElfSymbols(Elf32_Ehdr *hdr, Elf32_Sym *elf_symbols,
                      int num_entries);

#endif /* READ_ELF_H */
