/*************************************************************************
    Copyright (C) 2002,2003,2004,2005 Wei Qin
    See file COPYING for more information.

    This program is free software; you can redistribute it and/or modify    
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*************************************************************************/

#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <assert.h>
#include "read_elf.h"

#define SwapHalf(a) (((a & 0x00ff) << 8) | ((a & 0xff00) >> 8))
#define SwapWord(a) (((a & 0xff000000) >> 24) | ((a & 0x00ff0000) >> 8) | ((a & 0x0000ff00) << 8) | ((a & 0x000000ff) << 24))
#define SwapAddr(a) SwapWord(a)
#define SwapOff(a) SwapWord(a)
#define SwapSection(a) SwapHalf(a)

int LittleEndian()
{
  Elf32_Word a = 0x01020304;
  return *(char *) &a == 0x04;
}

void SwapElfHeader(Elf32_Ehdr *hdr)
{
  hdr->e_type = SwapHalf(hdr->e_type);
  hdr->e_machine = SwapHalf(hdr->e_machine);
  hdr->e_version = SwapWord(hdr->e_version);
  hdr->e_entry = SwapAddr(hdr->e_entry);
  hdr->e_phoff = SwapOff(hdr->e_phoff);
  hdr->e_shoff = SwapOff(hdr->e_shoff);
  hdr->e_flags = SwapWord(hdr->e_flags);
  hdr->e_ehsize = SwapHalf(hdr->e_ehsize);
  hdr->e_phentsize = SwapHalf(hdr->e_phentsize);
  hdr->e_phnum = SwapHalf(hdr->e_phnum);
  hdr->e_shentsize = SwapHalf(hdr->e_shentsize);
  hdr->e_shnum = SwapHalf(hdr->e_shnum);
  hdr->e_shstrndx = SwapHalf(hdr->e_shstrndx);
}

void SwapSectionHeader(Elf32_Shdr *shdr)
{
  shdr->sh_name = SwapWord(shdr->sh_name);
  shdr->sh_type = SwapWord(shdr->sh_type);
  shdr->sh_flags = SwapWord(shdr->sh_flags);
  shdr->sh_addr = SwapAddr(shdr->sh_addr);
  shdr->sh_offset = SwapOff(shdr->sh_offset);
  shdr->sh_size = SwapWord(shdr->sh_size);
  shdr->sh_link = SwapWord(shdr->sh_link);
  shdr->sh_info = SwapWord(shdr->sh_info);
  shdr->sh_addralign = SwapWord(shdr->sh_addralign);
  shdr->sh_entsize = SwapWord(shdr->sh_entsize);
}

void SwapElfSymbol(Elf32_Sym *sym)
{
    sym->st_name = SwapWord(sym->st_name);
    sym->st_value = SwapAddr(sym->st_value);
    sym->st_size = SwapWord(sym->st_size);
    sym->st_shndx = SwapSection(sym->st_shndx);
}

void AdjustElfHeader(Elf32_Ehdr *hdr)
{
  switch(hdr->e_ident[EI_DATA])
  {
    case ELFDATA2LSB:
      if (!LittleEndian())
        SwapElfHeader(hdr);
      break;
    case ELFDATA2MSB:
      if (LittleEndian())
        SwapElfHeader(hdr);
      break;
  }
}

void AdjustSectionHeader(Elf32_Ehdr *hdr, Elf32_Shdr *shdr)
{
  switch(hdr->e_ident[EI_DATA])
  {
    case ELFDATA2LSB:
      if (!LittleEndian())
        SwapSectionHeader(shdr);
      break;
    case ELFDATA2MSB:
      if (LittleEndian())
        SwapSectionHeader(shdr);
      break;
  }
}

void AdjustElfSymbols(Elf32_Ehdr *hdr, Elf32_Sym *elf_symbols, int num_entries)
{
    if (hdr->e_ident[EI_DATA] == ELFDATA2LSB && LittleEndian())
        return;
    if (hdr->e_ident[EI_DATA] == ELFDATA2MSB && !LittleEndian())
        return;
    for (int ii = 0; ii < num_entries; ++ii) {
        SwapElfSymbol(&elf_symbols[ii]);
    }
}

Elf32_Ehdr *ReadElfHeader(FILE *fobj)
{
  Elf32_Ehdr *hdr = new Elf32_Ehdr;
  int rval = fread(hdr, sizeof(Elf32_Ehdr), 1, fobj);
  if (rval != 1) {
    delete hdr;
    return NULL;
  }
  if (hdr->e_ident[EI_MAG0] != 0x7f || hdr->e_ident[EI_MAG1] != 'E' ||
      hdr->e_ident[EI_MAG2] != 'L' || hdr->e_ident[EI_MAG3] != 'F') {
    delete hdr;
    return NULL;
  }
  AdjustElfHeader(hdr);
  return hdr;
}

Elf32_Shdr *ReadSectionHeaders(Elf32_Ehdr *hdr, FILE *f)
{
  int i;
  unsigned long sz = hdr->e_shnum * hdr->e_shentsize;
  assert(sizeof(Elf32_Shdr) == hdr->e_shentsize);
  Elf32_Shdr *shdr = new Elf32_Shdr[hdr->e_shnum];

  if (fseek(f, hdr->e_shoff, SEEK_SET) != 0)
  {
    delete[] shdr;
    return NULL;
  }
  if (fread(shdr, sz, 1, f) != 1)
  {
    delete[] shdr;
    return NULL;
  }

  for(i = 0; i < hdr->e_shnum; i++)
    AdjustSectionHeader(hdr, shdr + i);

  return shdr;
}


char *ReadStringTable(Elf32_Ehdr *hdr, Elf32_Shdr *shdr_table, FILE *f)
{
  Elf32_Shdr *shdr = shdr_table + hdr->e_shstrndx;
  char *string_table;

  string_table = new char[shdr->sh_size];
  fseek(f, shdr->sh_offset, SEEK_SET);
  fread(string_table, shdr->sh_size, 1, f);

  return string_table;
}

int ReadSection(Elf32_Shdr *shdr, void *buffer, FILE *f)
{
  if (fseek(f, shdr->sh_offset, SEEK_SET) != 0)
    return -1;
  if (fread(buffer, shdr->sh_size, 1, f) != 1)
    return -1;
  return 0;
}

char *GetSymbolName(Elf32_Half index, char *string_table)
{
  return string_table + index;
}

Elf32_Shdr *FindSymbolTableSection(Elf32_Ehdr *hdr,
                                   Elf32_Shdr *shdr,
                                   char *string_table)
{
  for(int ii = 0; ii < hdr->e_shnum; ii++) {
    if (shdr[ii].sh_type == SHT_SYMTAB &&
       strcmp(GetSymbolName(shdr[ii].sh_name, string_table),
              ".symtab") == 0)
    {
      return &shdr[ii];
    }
  }
  return NULL;
}

Elf32_Shdr *FindSymbolStringTableSection(Elf32_Ehdr *hdr,
                                         Elf32_Shdr *shdr,
                                         char *string_table)
{
  for(int ii = 0; ii < hdr->e_shnum; ii++) {
    if (shdr[ii].sh_type == SHT_STRTAB &&
       strcmp(GetSymbolName(shdr[ii].sh_name, string_table),
              ".strtab") == 0)
    {
      return &shdr[ii];
    }
  }
  return NULL;
}
