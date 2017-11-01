#!/usr/bin/env python3

from __future__ import print_function

import argparse
import collections
import struct
import sys


Elf_Hdr = collections.namedtuple(
        'Elf_Hdr',
        'ei_class ei_data ei_version ei_osabi e_type e_machine e_version '
        'e_entry e_phoff e_shoff e_flags e_ehsize e_phentsize e_phnum '
        'e_shentsize e_shnum e_shstridx')


Elf_Shdr = collections.namedtuple(
        'Elf_Shdr',
        'sh_name sh_type sh_flags sh_addr sh_offset sh_size sh_link sh_info '
        'sh_addralign sh_entsize')


Elf_Dyn = collections.namedtuple('Elf_Dyn', 'd_tag d_val')


class Elf_Sym(collections.namedtuple(
    'ELF_Sym', 'st_name st_value st_size st_info st_other st_shndx')):

    STB_LOCAL = 0
    STB_GLOBAL = 1
    STB_WEAK = 2

    SHN_UNDEF = 0

    @property
    def st_bind(self):
        return (self.st_info >> 4)

    @property
    def is_local(self):
        return self.st_bind == Elf_Sym.STB_LOCAL

    @property
    def is_global(self):
        return self.st_bind == Elf_Sym.STB_GLOBAL

    @property
    def is_weak(self):
        return self.st_bind == Elf_Sym.STB_WEAK

    @property
    def is_undef(self):
        return self.st_shndx == Elf_Sym.SHN_UNDEF


class ELFError(ValueError):
    pass


# ELF file format constants.
ELF_MAGIC = b'\x7fELF'

EI_CLASS = 4
EI_DATA = 5

ELFCLASS32 = 1
ELFCLASS64 = 2
ELFDATA2LSB = 1
ELFDATA2MSB = 2

DT_NULL = 0
DT_NEEDED = 1


if sys.version_info >= (3, 0):
    def _extract_buf_byte(buf, offset):
        return buf[offset]
else:
    def _extract_buf_byte(buf, offset):
        return ord(buf[offset])


def _extract_zero_terminated_buf_slice(buf, offset):
    """Extract a zero-terminated buffer slice from the given offset"""
    end = offset
    try:
        while _extract_buf_byte(buf, end) != 0:
            end += 1
    except IndexError:
        pass
    return buf[offset:end]


if sys.version_info >= (3, 0):
    def _extract_zero_terminated_str(buf, offset):
        buf_slice = _extract_zero_terminated_buf_slice(buf, offset)
        return buf_slice.decode('utf-8')
else:
    def _extract_zero_terminated_str(buf, offset):
        return _extract_zero_terminated_buf_slice(buf, offset)


def _replace_dt_needed_buf_internal(buf, dt_needed_name):
    # Check ELF ident.
    if len(buf) < 8:
        raise ELFError('bad ident')

    if buf[0:4] != ELF_MAGIC:
        raise ELFError('bad magic')

    ei_class = _extract_buf_byte(buf, EI_CLASS)
    if ei_class not in (ELFCLASS32, ELFCLASS64):
        raise ELFError('unknown word size')
    is_32bit = ei_class == ELFCLASS32

    ei_data = _extract_buf_byte(buf, EI_DATA)
    if ei_data not in (ELFDATA2LSB, ELFDATA2MSB):
        raise ELFError('unknown endianness')

    # ELF structure definitions.
    endian_fmt = '<' if ei_data == ELFDATA2LSB else '>'

    if is_32bit:
        elf_hdr_fmt = endian_fmt + '4x4B8xHHLLLLLHHHHHH'
        elf_shdr_fmt = endian_fmt + 'LLLLLLLLLL'
        elf_dyn_fmt = endian_fmt + 'lL'
        elf_sym_fmt = endian_fmt + 'LLLBBH'
    else:
        elf_hdr_fmt = endian_fmt + '4x4B8xHHLQQQLHHHHHH'
        elf_shdr_fmt = endian_fmt + 'LLQQQQLLQQ'
        elf_dyn_fmt = endian_fmt + 'QQ'
        elf_sym_fmt = endian_fmt + 'LBBHQQ'

    def parse_struct(cls, fmt, offset, error_msg):
        try:
            return cls._make(struct.unpack_from(fmt, buf, offset))
        except struct.error:
            raise ELFError(error_msg)

    def parse_elf_hdr(offset):
        return parse_struct(Elf_Hdr, elf_hdr_fmt, offset, 'bad elf header')

    def parse_elf_shdr(offset):
        return parse_struct(Elf_Shdr, elf_shdr_fmt, offset,
                            'bad section header')

    def parse_elf_dyn(offset):
        return parse_struct(Elf_Dyn, elf_dyn_fmt, offset, 'bad .dynamic entry')

    def extract_str(offset):
        return _extract_zero_terminated_str(buf, offset)

    # Parse ELF header.
    header = parse_elf_hdr(0)

    # Check section header size.
    if header.e_shentsize == 0:
        raise ELFError('no section header')

    # Find .shstrtab section.
    shstrtab_shdr_off = \
            header.e_shoff + header.e_shstridx * header.e_shentsize
    shstrtab_shdr = parse_elf_shdr(shstrtab_shdr_off)
    shstrtab_off = shstrtab_shdr.sh_offset

    # Parse ELF section header.
    sections = dict()
    header_end = header.e_shoff + header.e_shnum * header.e_shentsize
    for shdr_off in range(header.e_shoff, header_end, header.e_shentsize):
        shdr = parse_elf_shdr(shdr_off)
        name = extract_str(shstrtab_off + shdr.sh_name)
        sections[name] = shdr

    # Find .dynamic and .dynstr section header.
    dynamic_shdr = sections.get('.dynamic')
    if not dynamic_shdr:
        raise ELFError('no .dynamic section')

    dynstr_shdr = sections.get('.dynstr')
    if not dynstr_shdr:
        raise ELFError('no .dynstr section')

    dynamic_off = dynamic_shdr.sh_offset
    dynstr_off = dynstr_shdr.sh_offset

    # Find DT_NULL entry.
    ent_size = dynamic_shdr.sh_entsize
    assert struct.calcsize(elf_dyn_fmt) == ent_size

    if dynamic_shdr.sh_size < ent_size:
        raise ELFError('.dynamic section is empty')

    dynamic_end = dynamic_off + dynamic_shdr.sh_size
    dt_null_off = dynamic_end - ent_size
    if parse_elf_dyn(dt_null_off).d_tag != DT_NULL:
        raise ELFError('.dynamic section is not ended with DT_NULL')
    dt_null_ent = buf[dt_null_off:dt_null_off + ent_size]

    # Build result buffer which replaces matching DT_NEEDED entries.
    res = buf[0:dynamic_off]
    for ent_off in range(dynamic_off, dynamic_end, ent_size):
        ent = parse_elf_dyn(ent_off)
        if ent.d_tag != DT_NEEDED or \
                extract_str(dynstr_off + ent.d_val) != dt_needed_name:
            res += buf[ent_off:ent_off + ent_size]
    for ent_off in range(len(res), dynamic_end, ent_size):
        res += dt_null_ent
    res += buf[dynamic_end:]
    return res


def replace_dt_needed_buf(buf, dt_needed_name):
    try:
        return _replace_dt_needed_buf_internal(buf, dt_needed_name)
    except IndexError:
        raise ELFError('bad offset')


def replace_dt_needed(input_path, output_path, dt_needed_name):
    with open(input_path, 'rb') as f:
        buf = f.read()

    buf = replace_dt_needed_buf(buf, dt_needed_name)

    with open(output_path, 'wb') as f:
        f.write(buf)


def main():
    parser = argparse.ArgumentParser(description='Remove DT_NEEDED entries')
    parser.add_argument('input', help='input ELF file')
    parser.add_argument('--output', '-o', required=True, help='output ELF file')
    parser.add_argument('--name', required=True, help='name')
    args = parser.parse_args()

    try:
        replace_dt_needed(args.input, args.output, args.name)
        return 0
    except (OSError, ELFError) as e:
        print('error:', e, file=sys.stderr)

    return 1

if __name__ == '__main__':
    sys.exit(main())
