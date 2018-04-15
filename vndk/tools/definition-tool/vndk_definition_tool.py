#!/usr/bin/env python3

from __future__ import print_function

import argparse
import codecs
import collections
import copy
import csv
import io
import itertools
import json
import os
import posixpath
import re
import shutil
import stat
import struct
import sys
import zipfile


#------------------------------------------------------------------------------
# Python 2 and 3 Compatibility Layer
#------------------------------------------------------------------------------

if sys.version_info >= (3, 0):
    from os import makedirs
    from mmap import ACCESS_READ, mmap

    def get_py3_bytes(buf):
        return buf

    create_chr = chr
    enumerate_bytes = enumerate
else:
    from mmap import ACCESS_READ, mmap

    def makedirs(path, exist_ok):
        if exist_ok and os.path.isdir(path):
            return
        return os.makedirs(path)

    class mmap(mmap):
        def __enter__(self):
            return self

        def __exit__(self, exc, value, tb):
            self.close()

        def __getitem__(self, key):
            res = super(mmap, self).__getitem__(key)
            if type(key) == int:
                return ord(res)
            return res

    class Py3Bytes(bytes):
        def __getitem__(self, key):
            res = super(Py3Bytes, self).__getitem__(key)
            if type(key) == int:
                return ord(res)
            return Py3Bytes(res)

    def get_py3_bytes(buf):
        return Py3Bytes(buf)

    create_chr = unichr

    def enumerate_bytes(iterable):
        for i, byte in enumerate(iterable):
            yield (i, ord(byte))

    FileNotFoundError = OSError

try:
    from sys import intern
except ImportError:
    pass


#------------------------------------------------------------------------------
# Modified UTF-8 Encoder and Decoder
#------------------------------------------------------------------------------

def encode_mutf8(input, errors='strict'):
    i = 0
    res = io.BytesIO()

    for i, char in enumerate(input):
        code = ord(char)
        if code == 0x00:
            res.write(b'\xc0\x80')
        elif code < 0x80:
            res.write(bytearray((code,)))
        elif code < 0x800:
            res.write(bytearray((0xc0 | (code >> 6), 0x80 | (code & 0x3f))))
        elif code < 0x10000:
            res.write(bytearray((0xe0 | (code >> 12),
                                 0x80 | ((code >> 6) & 0x3f),
                                 0x80 | (code & 0x3f))))
        elif code < 0x110000:
            code -= 0x10000
            code_hi = 0xd800 + (code >> 10)
            code_lo = 0xdc00 + (code & 0x3ff)
            res.write(bytearray((0xe0 | (code_hi >> 12),
                                 0x80 | ((code_hi >> 6) & 0x3f),
                                 0x80 | (code_hi & 0x3f),
                                 0xe0 | (code_lo >> 12),
                                 0x80 | ((code_lo >> 6) & 0x3f),
                                 0x80 | (code_lo & 0x3f))))
        else:
            raise UnicodeEncodeError('mutf-8', input, i, i + 1,
                                     'illegal code point')

    return (res.getvalue(), i)


def decode_mutf8(input, errors='strict'):
    res = io.StringIO()

    num_next = 0

    i = 0
    code = 0
    start = 0

    code_surrogate = None
    start_surrogate = None

    def raise_error(start, reason):
        raise UnicodeDecodeError('mutf-8', input, start, i + 1, reason)

    for i, byte in enumerate_bytes(input):
        if (byte & 0x80) == 0x00:
            if num_next > 0:
                raise_error(start, 'invalid continuation byte')
            num_next = 0
            code = byte
            start = i
        elif (byte & 0xc0) == 0x80:
            if num_next < 1:
                raise_error(start, 'invalid start byte')
            num_next -= 1
            code = (code << 6) | (byte & 0x3f)
        elif (byte & 0xe0) == 0xc0:
            if num_next > 0:
                raise_error(start, 'invalid continuation byte')
            num_next = 1
            code = byte & 0x1f
            start = i
        elif (byte & 0xf0) == 0xe0:
            if num_next > 0:
                raise_error(start, 'invalid continuation byte')
            num_next = 2
            code = byte & 0x0f
            start = i
        else:
            raise_error(i, 'invalid start byte')

        if num_next == 0:
            if code >= 0xd800 and code <= 0xdbff:  # High surrogate
                if code_surrogate is not None:
                    raise_error(start_surrogate, 'invalid high surrogate')
                code_surrogate = code
                start_surrogate = start
                continue

            if code >= 0xdc00 and code <= 0xdfff:  # Low surrogate
                if code_surrogate is None:
                    raise_error(start, 'invalid low surrogate')
                code = ((code_surrogate & 0x3f) << 10) | (code & 0x3f) + 0x10000
                code_surrogate = None
                start_surrogate = None
            elif code_surrogate is not None:
                if errors == 'ignore':
                    res.write(create_chr(code_surrogate))
                    code_surrogate = None
                    start_surrogate = None
                else:
                    raise_error(start_surrogate, 'illegal surrogate')

            res.write(create_chr(code))

    # Check the unexpected end of input
    if num_next > 0:
        raise_error(start, 'unexpected end')
    if code_surrogate is not None:
        raise_error(start_surrogate, 'unexpected end')

    return (res.getvalue(), i)


def probe_mutf8(name):
    if name == 'mutf-8':
        return codecs.CodecInfo(encode_mutf8, decode_mutf8)
    return None

codecs.register(probe_mutf8)


#------------------------------------------------------------------------------
# Collections
#------------------------------------------------------------------------------

def defaultnamedtuple(typename, field_names, default):
    """Create a namedtuple type with default values.

    This function creates a namedtuple type which will fill in default value
    when actual arguments to the constructor were omitted.

    >>> Point = defaultnamedtuple('Point', ['x', 'y'], 0)
    >>> Point()
    Point(x=0, y=0)
    >>> Point(1)
    Point(x=1, y=0)
    >>> Point(1, 2)
    Point(x=1, y=2)
    >>> Point(x=1, y=2)
    Point(x=1, y=2)
    >>> Point(y=2, x=1)
    Point(x=1, y=2)

    >>> PermSet = defaultnamedtuple('PermSet', 'allowed disallowed', set())
    >>> s = PermSet()
    >>> s
    PermSet(allowed=set(), disallowed=set())
    >>> s.allowed is not s.disallowed
    True
    >>> PermSet({1})
    PermSet(allowed={1}, disallowed=set())
    >>> PermSet({1}, {2})
    PermSet(allowed={1}, disallowed={2})
    """

    if isinstance(field_names, str):
        field_names = field_names.replace(',', ' ').split()
    field_names = list(map(str, field_names))
    num_fields = len(field_names)

    base_cls = collections.namedtuple(typename, field_names)
    def __new__(cls, *args, **kwargs):
        args = list(args)
        for i in range(len(args), num_fields):
            arg = kwargs.get(field_names[i])
            if arg:
                args.append(arg)
            else:
                args.append(copy.copy(default))
        return base_cls.__new__(cls, *args)
    return type(typename, (base_cls,), {'__new__': __new__})


def create_struct(name, fields):
    """Create a namedtuple with unpack_from() function.
    >>> Point = create_struct('Point', [('x', 'I'), ('y', 'I')])
    >>> pt = Point.unpack_from(b'\\x00\\x00\\x00\\x00\\x01\\x00\\x00\\x00', 0)
    >>> pt.x
    0
    >>> pt.y
    1
    """
    field_names = [name for name, ty in fields]
    cls = collections.namedtuple(name, field_names)
    cls.struct_fmt = ''.join(ty for name, ty in fields)
    cls.struct_size = struct.calcsize(cls.struct_fmt)
    def unpack_from(cls, buf, offset=0):
        unpacked = struct.unpack_from(cls.struct_fmt, buf, offset)
        return cls.__new__(cls, *unpacked)
    cls.unpack_from = classmethod(unpack_from)
    return cls


#------------------------------------------------------------------------------
# ELF Parser
#------------------------------------------------------------------------------

Elf_Hdr = collections.namedtuple(
        'Elf_Hdr',
        'ei_class ei_data ei_version ei_osabi e_type e_machine e_version '
        'e_entry e_phoff e_shoff e_flags e_ehsize e_phentsize e_phnum '
        'e_shentsize e_shnum e_shstridx')


Elf_Shdr = collections.namedtuple(
        'Elf_Shdr',
        'sh_name sh_type sh_flags sh_addr sh_offset sh_size sh_link sh_info '
        'sh_addralign sh_entsize')


Elf_Phdr = collections.namedtuple(
        'Elf_Phdr',
        'p_type p_offset p_vaddr p_paddr p_filesz p_memsz p_flags p_align')


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


class ELF(object):
    # ELF file format constants.
    ELF_MAGIC = b'\x7fELF'

    EI_CLASS = 4
    EI_DATA = 5

    ELFCLASSNONE = 0
    ELFCLASS32 = 1
    ELFCLASS64 = 2

    ELFDATANONE = 0
    ELFDATA2LSB = 1
    ELFDATA2MSB = 2

    PT_LOAD = 1

    PF_X = 1
    PF_W = 2
    PF_R = 4

    DT_NEEDED = 1
    DT_RPATH = 15
    DT_RUNPATH = 29

    _ELF_CLASS_NAMES = {
        ELFCLASS32: '32',
        ELFCLASS64: '64',
    }

    _ELF_DATA_NAMES = {
        ELFDATA2LSB: 'Little-Endian',
        ELFDATA2MSB: 'Big-Endian',
    }

    EM_NONE = 0
    EM_386 = 3
    EM_MIPS = 8
    EM_ARM = 40
    EM_X86_64 = 62
    EM_AARCH64 = 183

    def _create_elf_machines(d):
        elf_machine_ids = {}
        for key, value in d.items():
            if key.startswith('EM_'):
                elf_machine_ids[value] = key
        return elf_machine_ids

    ELF_MACHINES = _create_elf_machines(locals())

    del _create_elf_machines


    @staticmethod
    def _dict_find_key_by_value(d, dst):
        for key, value in d.items():
            if value == dst:
                return key
        raise KeyError(dst)

    @staticmethod
    def get_ei_class_from_name(name):
        return ELF._dict_find_key_by_value(ELF._ELF_CLASS_NAMES, name)

    @staticmethod
    def get_ei_data_from_name(name):
        return ELF._dict_find_key_by_value(ELF._ELF_DATA_NAMES, name)

    @staticmethod
    def get_e_machine_from_name(name):
        return ELF._dict_find_key_by_value(ELF.ELF_MACHINES, name)


    __slots__ = ('ei_class', 'ei_data', 'e_machine', 'dt_rpath', 'dt_runpath',
                 'dt_needed', 'exported_symbols', 'imported_symbols',
                 'file_size', 'ro_seg_file_size', 'ro_seg_mem_size',
                 'rw_seg_file_size', 'rw_seg_mem_size',)


    def __init__(self, ei_class=ELFCLASSNONE, ei_data=ELFDATANONE, e_machine=0,
                 dt_rpath=None, dt_runpath=None, dt_needed=None,
                 exported_symbols=None, imported_symbols=None,
                 file_size=0, ro_seg_file_size=0, ro_seg_mem_size=0,
                 rw_seg_file_size=0, rw_seg_mem_size=0):
        self.ei_class = ei_class
        self.ei_data = ei_data
        self.e_machine = e_machine
        self.dt_rpath = dt_rpath if dt_rpath is not None else []
        self.dt_runpath = dt_runpath if dt_runpath is not None else []
        self.dt_needed = dt_needed if dt_needed is not None else []
        self.exported_symbols = \
                exported_symbols if exported_symbols is not None else set()
        self.imported_symbols = \
                imported_symbols if imported_symbols is not None else set()
        self.file_size = file_size
        self.ro_seg_file_size = ro_seg_file_size
        self.ro_seg_mem_size = ro_seg_mem_size
        self.rw_seg_file_size = rw_seg_file_size
        self.rw_seg_mem_size = rw_seg_mem_size

    def __repr__(self):
        args = (a + '=' + repr(getattr(self, a)) for a in self.__slots__)
        return 'ELF(' + ', '.join(args) + ')'

    def __eq__(self, rhs):
        return all(getattr(self, a) == getattr(rhs, a) for a in self.__slots__)

    @property
    def elf_class_name(self):
        return self._ELF_CLASS_NAMES.get(self.ei_class, 'None')

    @property
    def elf_data_name(self):
        return self._ELF_DATA_NAMES.get(self.ei_data, 'None')

    @property
    def elf_machine_name(self):
        return self.ELF_MACHINES.get(self.e_machine, str(self.e_machine))

    @property
    def is_32bit(self):
        return self.ei_class == ELF.ELFCLASS32

    @property
    def is_64bit(self):
        return self.ei_class == ELF.ELFCLASS64

    @property
    def sorted_exported_symbols(self):
        return sorted(list(self.exported_symbols))

    @property
    def sorted_imported_symbols(self):
        return sorted(list(self.imported_symbols))

    def dump(self, file=None):
        """Print parsed ELF information to the file"""
        file = file if file is not None else sys.stdout

        print('EI_CLASS\t' + self.elf_class_name, file=file)
        print('EI_DATA\t\t' + self.elf_data_name, file=file)
        print('E_MACHINE\t' + self.elf_machine_name, file=file)
        print('FILE_SIZE\t' + str(self.file_size), file=file)
        print('RO_SEG_FILE_SIZE\t' + str(self.ro_seg_file_size), file=file)
        print('RO_SEG_MEM_SIZE\t' + str(self.ro_seg_mem_size), file=file)
        print('RW_SEG_FILE_SIZE\t' + str(self.rw_seg_file_size), file=file)
        print('RW_SEG_MEM_SIZE\t' + str(self.rw_seg_mem_size), file=file)
        for dt_rpath in self.dt_rpath:
            print('DT_RPATH\t' + dt_rpath, file=file)
        for dt_runpath in self.dt_runpath:
            print('DT_RUNPATH\t' + dt_runpath, file=file)
        for dt_needed in self.dt_needed:
            print('DT_NEEDED\t' + dt_needed, file=file)
        for symbol in self.sorted_exported_symbols:
            print('EXP_SYMBOL\t' + symbol, file=file)
        for symbol in self.sorted_imported_symbols:
            print('IMP_SYMBOL\t' + symbol, file=file)

    # Extract zero-terminated buffer slice.
    def _extract_zero_terminated_buf_slice(self, buf, offset):
        """Extract a zero-terminated buffer slice from the given offset"""
        end = buf.find(b'\0', offset)
        if end == -1:
            return buf[offset:]
        return buf[offset:end]

    # Extract c-style interned string from the buffer.
    if sys.version_info >= (3, 0):
        def _extract_zero_terminated_str(self, buf, offset):
            """Extract a c-style string from the given buffer and offset"""
            buf_slice = self._extract_zero_terminated_buf_slice(buf, offset)
            return intern(buf_slice.decode('utf-8'))
    else:
        def _extract_zero_terminated_str(self, buf, offset):
            """Extract a c-style string from the given buffer and offset"""
            return intern(self._extract_zero_terminated_buf_slice(buf, offset))

    def _parse_from_buf_internal(self, buf):
        """Parse ELF image resides in the buffer"""

        # Check ELF ident.
        if buf.size() < 8:
            raise ELFError('bad ident')

        if buf[0:4] != ELF.ELF_MAGIC:
            raise ELFError('bad magic')

        self.ei_class = buf[ELF.EI_CLASS]
        if self.ei_class not in (ELF.ELFCLASS32, ELF.ELFCLASS64):
            raise ELFError('unknown word size')

        self.ei_data = buf[ELF.EI_DATA]
        if self.ei_data not in (ELF.ELFDATA2LSB, ELF.ELFDATA2MSB):
            raise ELFError('unknown endianness')

        self.file_size = buf.size()

        # ELF structure definitions.
        endian_fmt = '<' if self.ei_data == ELF.ELFDATA2LSB else '>'

        if self.is_32bit:
            elf_hdr_fmt = endian_fmt + '4x4B8xHHLLLLLHHHHHH'
            elf_shdr_fmt = endian_fmt + 'LLLLLLLLLL'
            elf_phdr_fmt = endian_fmt + 'LLLLLLLL'
            elf_dyn_fmt = endian_fmt + 'lL'
            elf_sym_fmt = endian_fmt + 'LLLBBH'
        else:
            elf_hdr_fmt = endian_fmt + '4x4B8xHHLQQQLHHHHHH'
            elf_shdr_fmt = endian_fmt + 'LLQQQQLLQQ'
            elf_phdr_fmt = endian_fmt + 'LLQQQQQQ'
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

        if self.is_32bit:
            def parse_elf_phdr(offset):
                return parse_struct(Elf_Phdr, elf_phdr_fmt, offset,
                                    'bad program header')
        else:
            def parse_elf_phdr(offset):
                try:
                    p = struct.unpack_from(elf_phdr_fmt, buf, offset)
                    return Elf_Phdr(p[0], p[2], p[3], p[4], p[5], p[6], p[1],
                                    p[7])
                except struct.error:
                    raise ELFError('bad program header')

        def parse_elf_dyn(offset):
            return parse_struct(Elf_Dyn, elf_dyn_fmt, offset,
                                'bad .dynamic entry')

        if self.is_32bit:
            def parse_elf_sym(offset):
                return parse_struct(Elf_Sym, elf_sym_fmt, offset, 'bad elf sym')
        else:
            def parse_elf_sym(offset):
                try:
                    p = struct.unpack_from(elf_sym_fmt, buf, offset)
                    return Elf_Sym(p[0], p[4], p[5], p[1], p[2], p[3])
                except struct.error:
                    raise ELFError('bad elf sym')

        def extract_str(offset):
            return self._extract_zero_terminated_str(buf, offset)

        # Parse ELF header.
        header = parse_elf_hdr(0)
        self.e_machine = header.e_machine

        # Parse ELF program header and calculate segment size.
        if header.e_phentsize == 0:
            raise ELFError('no program header')

        ro_seg_file_size = 0
        ro_seg_mem_size = 0
        rw_seg_file_size = 0
        rw_seg_mem_size = 0

        assert struct.calcsize(elf_phdr_fmt) == header.e_phentsize
        seg_end = header.e_phoff + header.e_phnum * header.e_phentsize
        for phdr_off in range(header.e_phoff, seg_end, header.e_phentsize):
            phdr = parse_elf_phdr(phdr_off)
            if phdr.p_type != ELF.PT_LOAD:
                continue
            if phdr.p_flags & ELF.PF_W:
                rw_seg_file_size += phdr.p_filesz
                rw_seg_mem_size += phdr.p_memsz
            else:
                ro_seg_file_size += phdr.p_filesz
                ro_seg_mem_size += phdr.p_memsz

        self.ro_seg_file_size = ro_seg_file_size
        self.ro_seg_mem_size = ro_seg_mem_size
        self.rw_seg_file_size = rw_seg_file_size
        self.rw_seg_mem_size = rw_seg_mem_size

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

        # Parse entries in .dynamic section.
        assert struct.calcsize(elf_dyn_fmt) == dynamic_shdr.sh_entsize
        dynamic_end = dynamic_off + dynamic_shdr.sh_size
        for ent_off in range(dynamic_off, dynamic_end, dynamic_shdr.sh_entsize):
            ent = parse_elf_dyn(ent_off)
            if ent.d_tag == ELF.DT_NEEDED:
                self.dt_needed.append(extract_str(dynstr_off + ent.d_val))
            elif ent.d_tag == ELF.DT_RPATH:
                self.dt_rpath.extend(
                        extract_str(dynstr_off + ent.d_val).split(':'))
            elif ent.d_tag == ELF.DT_RUNPATH:
                self.dt_runpath.extend(
                        extract_str(dynstr_off + ent.d_val).split(':'))

        # Parse exported symbols in .dynsym section.
        dynsym_shdr = sections.get('.dynsym')
        if dynsym_shdr:
            exp_symbols = self.exported_symbols
            imp_symbols = self.imported_symbols

            dynsym_off = dynsym_shdr.sh_offset
            dynsym_end = dynsym_off + dynsym_shdr.sh_size
            dynsym_entsize = dynsym_shdr.sh_entsize

            # Skip first symbol entry (null symbol).
            dynsym_off += dynsym_entsize

            for ent_off in range(dynsym_off, dynsym_end, dynsym_entsize):
                ent = parse_elf_sym(ent_off)
                symbol_name = extract_str(dynstr_off + ent.st_name)
                if ent.is_undef:
                    imp_symbols.add(symbol_name)
                elif not ent.is_local:
                    exp_symbols.add(symbol_name)

    def _parse_from_buf(self, buf):
        """Parse ELF image resides in the buffer"""
        try:
            self._parse_from_buf_internal(buf)
        except IndexError:
            raise ELFError('bad offset')

    def _parse_from_file(self, path):
        """Parse ELF image from the file path"""
        with open(path, 'rb') as f:
            st = os.fstat(f.fileno())
            if not st.st_size:
                raise ELFError('empty file')
            with mmap(f.fileno(), st.st_size, access=ACCESS_READ) as image:
                self._parse_from_buf(image)

    def _parse_from_dump_lines(self, path, lines):
        patt = re.compile('^([A-Za-z_]+)\t+(.*)$')
        for line_no, line in enumerate(lines):
            match = patt.match(line)
            if not match:
                print('error: {}: {}: failed to parse'
                        .format(path, line_no + 1), file=sys.stderr)
                continue
            key = match.group(1)
            value = match.group(2)

            if key == 'EI_CLASS':
                self.ei_class = ELF.get_ei_class_from_name(value)
            elif key == 'EI_DATA':
                self.ei_data = ELF.get_ei_data_from_name(value)
            elif key == 'E_MACHINE':
                self.e_machine = ELF.get_e_machine_from_name(value)
            elif key == 'FILE_SIZE':
                self.file_size = int(value)
            elif key == 'RO_SEG_FILE_SIZE':
                self.ro_seg_file_size = int(value)
            elif key == 'RO_SEG_MEM_SIZE':
                self.ro_seg_mem_size = int(value)
            elif key == 'RW_SEG_FILE_SIZE':
                self.rw_seg_file_size = int(value)
            elif key == 'RW_SEG_MEM_SIZE':
                self.rw_seg_mem_size = int(value)
            elif key == 'DT_RPATH':
                self.dt_rpath.append(intern(value))
            elif key == 'DT_RUNPATH':
                self.dt_runpath.append(intern(value))
            elif key == 'DT_NEEDED':
                self.dt_needed.append(intern(value))
            elif key == 'EXP_SYMBOL':
                self.exported_symbols.add(intern(value))
            elif key == 'IMP_SYMBOL':
                self.imported_symbols.add(intern(value))
            else:
                print('error: {}: {}: unknown tag name: {}'
                        .format(path, line_no + 1, key), file=sys.stderr)

    def _parse_from_dump_file(self, path):
        """Load information from ELF dump file."""
        with open(path, 'r') as f:
            self._parse_from_dump_lines(path, f)

    def _parse_from_dump_buf(self, buf):
        """Load information from ELF dump buffer."""
        self._parse_from_dump_lines('<str:0x{:x}>'.format(id(buf)),
                                    buf.splitlines())

    @staticmethod
    def load(path):
        """Create an ELF instance from the file path"""
        elf = ELF()
        elf._parse_from_file(path)
        return elf

    @staticmethod
    def loads(buf):
        """Create an ELF instance from the buffer"""
        elf = ELF()
        elf._parse_from_buf(buf)
        return elf

    @staticmethod
    def load_dump(path):
        """Create an ELF instance from a dump file path"""
        elf = ELF()
        elf._parse_from_dump_file(path)
        return elf

    @staticmethod
    def load_dumps(buf):
        """Create an ELF instance from a dump file buffer"""
        elf = ELF()
        elf._parse_from_dump_buf(buf)
        return elf

    def is_jni_lib(self):
        """Test whether the ELF file looks like a JNI library."""
        for name in ['libnativehelper.so', 'libandroid_runtime.so']:
            if name in self.dt_needed:
                return True
        for symbol in itertools.chain(self.imported_symbols,
                                      self.exported_symbols):
            if symbol.startswith('JNI_') or symbol.startswith('Java_') or \
               symbol == 'jniRegisterNativeMethods':
                return True
        return False


#------------------------------------------------------------------------------
# APK / Dex File Reader
#------------------------------------------------------------------------------

class DexFileReader(object):
    @classmethod
    def extract_dex_string(cls, buf, offset=0):
        end = buf.find(b'\0', offset)
        res = buf[offset:] if end == -1 else buf[offset:end]
        return res.decode('mutf-8', 'ignore')

    if sys.version_info < (3,):
        _extract_dex_string = extract_dex_string

        @classmethod
        def extract_dex_string(cls, buf, offset=0):
            return cls._extract_dex_string(buf, offset).encode('utf-8')


    @classmethod
    def extract_uleb128(cls, buf, offset=0):
        num_bytes = 0
        result = 0
        shift = 0
        while True:
            byte = buf[offset + num_bytes]
            result |= (byte & 0x7f) << shift
            num_bytes += 1
            if (byte & 0x80) == 0:
                break
            shift += 7
        return (result, num_bytes)


    Header = create_struct('Header', (
        ('magic', '4s'),
        ('version', '4s'),
        ('checksum', 'I'),
        ('signature', '20s'),
        ('file_size', 'I'),
        ('header_size', 'I'),
        ('endian_tag', 'I'),
        ('link_size', 'I'),
        ('link_off', 'I'),
        ('map_off', 'I'),
        ('string_ids_size', 'I'),
        ('string_ids_off', 'I'),
        ('type_ids_size', 'I'),
        ('type_ids_off', 'I'),
        ('proto_ids_size', 'I'),
        ('proto_ids_off', 'I'),
        ('field_ids_size', 'I'),
        ('field_ids_off', 'I'),
        ('method_ids_size', 'I'),
        ('method_ids_off', 'I'),
        ('class_defs_size', 'I'),
        ('class_defs_off', 'I'),
        ('data_size', 'I'),
        ('data_off', 'I'),
    ))


    StringId = create_struct('StringId', (
        ('string_data_off', 'I'),
    ))


    @staticmethod
    def generate_classes_dex_names():
        yield 'classes.dex'
        for i in itertools.count(start=2):
            yield 'classes{}.dex'.format(i)


    @classmethod
    def enumerate_dex_strings_buf(cls, buf, offset=0, data_offset=None):
        buf = get_py3_bytes(buf)
        header = cls.Header.unpack_from(buf, offset=offset)

        if data_offset is None:
            if header.magic == b'dex\n':
                # In the standard dex file, the data_offset is the offset of
                # the dex header.
                data_offset = offset
            else:
                # In the compact dex file, the data_offset is sum of the offset
                # of the dex header and header.data_off.
                data_offset = offset + header.data_off

        StringId = cls.StringId
        struct_size = StringId.struct_size

        offset_start = offset + header.string_ids_off
        offset_end = offset_start + header.string_ids_size * struct_size

        for offset in range(offset_start, offset_end, struct_size):
            offset = StringId.unpack_from(buf, offset).string_data_off
            offset += data_offset

            # Skip the ULEB128 integer for UTF-16 string length
            offset += cls.extract_uleb128(buf, offset)[1]

            # Extract the string
            yield cls.extract_dex_string(buf, offset)


    @classmethod
    def _read_first_bytes(cls, apk_file, num_bytes):
        try:
            with open(apk_file, 'rb') as fp:
                return fp.read(num_bytes)
        except IOError:
            return b''

    @classmethod
    def is_zipfile(cls, apk_file_path):
        magic = cls._read_first_bytes(apk_file_path, 2)
        return magic == b'PK' and zipfile.is_zipfile(apk_file_path)

    @classmethod
    def enumerate_dex_strings_apk(cls, apk_file_path):
        with zipfile.ZipFile(apk_file_path, 'r') as zip_file:
            for name in cls.generate_classes_dex_names():
                try:
                    with zip_file.open(name) as dex_file:
                        for s in cls.enumerate_dex_strings_buf(dex_file.read()):
                            yield s
                except KeyError:
                    break

    @classmethod
    def is_vdex_file(cls, vdex_file_path):
        return vdex_file_path.endswith('.vdex')

    VdexHeader = create_struct('VdexHeader', (
        ('magic', '4s'),
        ('version', '4s'),
        ('number_of_dex_files', 'I'),
        ('dex_size', 'I'),
        # ('dex_shared_data_size', 'I'),  # >= 016
        ('verifier_deps_size', 'I'),
        ('quickening_info_size', 'I'),
    ))

    @classmethod
    def enumerate_dex_strings_vdex_buf(cls, buf):
        buf = get_py3_bytes(buf)
        vdex_header = cls.VdexHeader.unpack_from(buf, offset=0)

        quickening_table_off_size = 0
        if vdex_header.version > b'010\x00':
            quickening_table_off_size = 4

        # Skip vdex file header size
        offset = cls.VdexHeader.struct_size

        # Skip `dex_shared_data_size`
        if vdex_header.version >= b'016\x00':
            offset += 4

        # Skip dex file checksums size
        offset += 4 * vdex_header.number_of_dex_files

        # Skip this vdex file if there is no dex file section
        if vdex_header.dex_size == 0:
            return

        for i in range(vdex_header.number_of_dex_files):
            # Skip quickening_table_off size
            offset += quickening_table_off_size

            # Check the dex file magic
            dex_magic = buf[offset:offset + 4]
            if dex_magic != b'dex\n' and dex_magic != b'cdex':
                raise ValueError('bad dex file offset {}'.format(offset))

            dex_header = cls.Header.unpack_from(buf, offset)
            dex_file_end = offset + dex_header.file_size
            for s in cls.enumerate_dex_strings_buf(buf, offset):
                yield s
            offset = (dex_file_end + 3) // 4 * 4

    @classmethod
    def enumerate_dex_strings_vdex(cls, vdex_file_path):
        with open(vdex_file_path, 'rb') as vdex_file:
            return cls.enumerate_dex_strings_vdex_buf(vdex_file.read())


#------------------------------------------------------------------------------
# TaggedDict
#------------------------------------------------------------------------------

class TaggedDict(object):
    def _define_tag_constants(local_ns):
        tag_list = [
            'll_ndk', 'll_ndk_indirect',
            'vndk_sp', 'vndk_sp_indirect', 'vndk_sp_indirect_private',
            'vndk',
            'fwk_only', 'fwk_only_rs',
            'sp_hal', 'sp_hal_dep',
            'vnd_only',
            'remove',
        ]
        assert len(tag_list) < 32

        tags = {}
        for i, tag in enumerate(tag_list):
            local_ns[tag.upper()] = 1 << i
            tags[tag] = 1 << i

        local_ns['TAGS'] = tags

    _define_tag_constants(locals())
    del _define_tag_constants

    _TAG_ALIASES = {
        'hl_ndk': 'fwk_only',  # Treat HL-NDK as FWK-ONLY.
        'sp_ndk': 'll_ndk',
        'sp_ndk_indirect': 'll_ndk_indirect',
        'vndk_indirect': 'vndk',  # Legacy
        'vndk_sp_hal': 'vndk_sp',  # Legacy
        'vndk_sp_both': 'vndk_sp',  # Legacy

        # FIXME: LL-NDK-Private, VNDK-Private and VNDK-SP-Private are new tags.
        # They should not be treated as aliases.
        # TODO: Refine the code that compute and verify VNDK sets and reverse
        # the aliases.
        'll_ndk_private': 'll_ndk_indirect',
        'vndk_private': 'vndk',
        'vndk_sp_private': 'vndk_sp_indirect_private',
    }

    @classmethod
    def _normalize_tag(cls, tag):
        tag = tag.lower().replace('-', '_')
        tag = cls._TAG_ALIASES.get(tag, tag)
        if tag not in cls.TAGS:
            raise ValueError('unknown lib tag ' + tag)
        return tag

    _LL_NDK_VIS = {'ll_ndk', 'll_ndk_indirect'}
    _VNDK_SP_VIS = {'ll_ndk', 'vndk_sp', 'vndk_sp_indirect',
                    'vndk_sp_indirect_private', 'fwk_only_rs'}
    _FWK_ONLY_VIS = {'ll_ndk', 'll_ndk_indirect',
                     'vndk_sp', 'vndk_sp_indirect', 'vndk_sp_indirect_private',
                     'vndk', 'fwk_only', 'fwk_only_rs', 'sp_hal'}
    _SP_HAL_VIS = {'ll_ndk', 'vndk_sp', 'sp_hal', 'sp_hal_dep'}

    _TAG_VISIBILITY = {
        'll_ndk': _LL_NDK_VIS,
        'll_ndk_indirect': _LL_NDK_VIS,

        'vndk_sp': _VNDK_SP_VIS,
        'vndk_sp_indirect': _VNDK_SP_VIS,
        'vndk_sp_indirect_private': _VNDK_SP_VIS,

        'vndk': {'ll_ndk', 'vndk_sp', 'vndk_sp_indirect', 'vndk'},

        'fwk_only': _FWK_ONLY_VIS,
        'fwk_only_rs': _FWK_ONLY_VIS,

        'sp_hal': _SP_HAL_VIS,
        'sp_hal_dep': _SP_HAL_VIS,

        'vnd_only': {'ll_ndk', 'vndk_sp', 'vndk_sp_indirect',
                     'vndk', 'sp_hal', 'sp_hal_dep', 'vnd_only'},

        'remove': set(),
    }

    del _LL_NDK_VIS, _VNDK_SP_VIS, _FWK_ONLY_VIS, _SP_HAL_VIS

    @classmethod
    def is_tag_visible(cls, from_tag, to_tag):
        return to_tag in cls._TAG_VISIBILITY[from_tag]

    def __init__(self, vndk_lib_dirs=None):
        self._path_tag = dict()
        for tag in self.TAGS:
            setattr(self, tag, set())
        self._regex_patterns = []

        if vndk_lib_dirs is None:
            self._vndk_suffixes = ['']
        else:
            self._vndk_suffixes = [VNDKLibDir.create_vndk_dir_suffix(version)
                                   for version in vndk_lib_dirs]

    def add(self, tag, lib):
        lib_set = getattr(self, tag)
        lib_set.add(lib)
        self._path_tag[lib] = tag

    def add_regex(self, tag, pattern):
        self._regex_patterns.append((re.compile(pattern), tag))

    def get_path_tag(self, lib):
        try:
            return self._path_tag[lib]
        except KeyError:
            pass

        for pattern, tag in self._regex_patterns:
            if pattern.match(lib):
                return tag

        return self.get_path_tag_default(lib)

    def get_path_tag_default(self, lib):
        raise NotImplementedError()

    def get_path_tag_bit(self, lib):
        return self.TAGS[self.get_path_tag(lib)]

    def is_path_visible(self, from_lib, to_lib):
        return self.is_tag_visible(self.get_path_tag(from_lib),
                                   self.get_path_tag(to_lib))

    @staticmethod
    def is_ll_ndk(tag_bit):
        return bool(tag_bit & TaggedDict.LL_NDK)

    @staticmethod
    def is_vndk_sp(tag_bit):
        return bool(tag_bit & TaggedDict.VNDK_SP)

    @staticmethod
    def is_vndk_sp_indirect(tag_bit):
        return bool(tag_bit & TaggedDict.VNDK_SP_INDIRECT)

    @staticmethod
    def is_vndk_sp_indirect_private(tag_bit):
        return bool(tag_bit & TaggedDict.VNDK_SP_INDIRECT_PRIVATE)

    @staticmethod
    def is_fwk_only_rs(tag_bit):
        return bool(tag_bit & TaggedDict.FWK_ONLY_RS)

    @staticmethod
    def is_sp_hal(tag_bit):
        return bool(tag_bit & TaggedDict.SP_HAL)


class TaggedPathDict(TaggedDict):
    def load_from_csv(self, fp):
        reader = csv.reader(fp)

        # Read first row and check the existence of the header.
        try:
            row = next(reader)
        except StopIteration:
            return

        try:
            path_col = row.index('Path')
            tag_col = row.index('Tag')
        except ValueError:
            path_col = 0
            tag_col = 1
            self.add(self._normalize_tag(row[tag_col]), row[path_col])

        # Read the rest of rows.
        for row in reader:
            self.add(self._normalize_tag(row[tag_col]), row[path_col])

    @staticmethod
    def create_from_csv(fp, vndk_lib_dirs=None):
        d = TaggedPathDict(vndk_lib_dirs)
        d.load_from_csv(fp)
        return d

    @staticmethod
    def create_from_csv_path(path, vndk_lib_dirs=None):
        with open(path, 'r') as fp:
            return TaggedPathDict.create_from_csv(fp, vndk_lib_dirs)

    def _enumerate_paths_with_lib(self, pattern):
        if '${LIB}' in pattern:
            yield pattern.replace('${LIB}', 'lib')
            yield pattern.replace('${LIB}', 'lib64')
        else:
            yield pattern

    def _enumerate_paths(self, pattern):
        if '${VNDK_VER}' not in pattern:
            for path in self._enumerate_paths_with_lib(pattern):
                yield path
            return
        for suffix in self._vndk_suffixes:
            pattern_with_suffix = pattern.replace('${VNDK_VER}', suffix)
            for path in self._enumerate_paths_with_lib(pattern_with_suffix):
                yield path

    def add(self, tag, path):
        if path.startswith('[regex]'):
            super(TaggedPathDict, self).add_regex(tag, path[7:])
            return
        for path in self._enumerate_paths(path):
            super(TaggedPathDict, self).add(tag, path)

    def get_path_tag_default(self, path):
        return 'vnd_only' if path.startswith('/vendor') else 'fwk_only'


class TaggedLibDict(object):
    def __init__(self):
        self._path_tag = dict()
        for tag in TaggedDict.TAGS:
            setattr(self, tag, set())

    def add(self, tag, lib):
        lib_set = getattr(self, tag)
        lib_set.add(lib)
        self._path_tag[lib] = tag

    @staticmethod
    def create_from_graph(graph, tagged_paths, generic_refs=None):
        d = TaggedLibDict()

        for lib in graph.lib_pt[PT_SYSTEM].values():
            d.add(tagged_paths.get_path_tag(lib.path), lib)

        sp_lib = graph.compute_sp_lib(generic_refs)
        for lib in graph.lib_pt[PT_VENDOR].values():
            if lib in sp_lib.sp_hal:
                d.add('sp_hal', lib)
            elif lib in sp_lib.sp_hal_dep:
                d.add('sp_hal_dep', lib)
            else:
                d.add('vnd_only', lib)
        return d

    def get_path_tag(self, lib):
        try:
            return self._path_tag[lib]
        except KeyError:
            return self.get_path_tag_default(lib)

    def get_path_tag_default(self, lib):
        return 'vnd_only' if lib.path.startswith('/vendor') else 'fwk_only'


class LibProperties(object):
    Properties = collections.namedtuple(
            'Properties', 'vndk vndk_sp vendor_available rule')


    def __init__(self, csv_file=None):
        self.modules = {}

        if csv_file:
            reader = csv.reader(csv_file)

            header = next(reader)
            assert header == ['name', 'vndk', 'vndk_sp', 'vendor_available',
                              'rule'], repr(header)

            for name, vndk, vndk_sp, vendor_available, rule in reader:
                self.modules[name] = self.Properties(
                        vndk == 'True', vndk_sp == 'True',
                        vendor_available == 'True', rule)


    @classmethod
    def load_from_path_or_default(cls, path):
        if not path:
            return LibProperties()

        try:
            with open(path, 'r') as csv_file:
                return LibProperties(csv_file)
        except FileNotFoundError:
            return LibProperties()


    def get(self, name):
        try:
            return self.modules[name]
        except KeyError:
            return self.Properties(False, False, False, None)


    @staticmethod
    def get_lib_properties_file_path(tag_file_path):
        root, ext = os.path.splitext(tag_file_path)
        return root + '-properties' + ext


#------------------------------------------------------------------------------
# ELF Linker
#------------------------------------------------------------------------------

def is_accessible(path):
    try:
        mode = os.stat(path).st_mode
        return (mode & (stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)) != 0
    except FileNotFoundError:
        return False


def scan_accessible_files(root):
    for base, dirs, files in os.walk(root):
        for filename in files:
            path = os.path.join(base, filename)
            if is_accessible(path):
                yield path


def scan_elf_files(root):
    for path in scan_accessible_files(root):
        try:
            yield (path, ELF.load(path))
        except ELFError:
            pass


def scan_elf_dump_files(root):
    for path in scan_accessible_files(root):
        if not path.endswith('.sym'):
            continue
        yield (path[0:-4], ELF.load_dump(path))


PT_SYSTEM = 0
PT_VENDOR = 1
NUM_PARTITIONS = 2


SPLibResult = collections.namedtuple(
        'SPLibResult',
        'sp_hal sp_hal_dep vndk_sp_hal ll_ndk ll_ndk_indirect '
        'vndk_sp_both')


VNDKLibTuple = defaultnamedtuple('VNDKLibTuple', 'vndk_sp vndk', [])


class VNDKLibDir(list):
    """VNDKLibDir is a dict which maps version to VNDK-SP and VNDK directory
    paths."""


    @classmethod
    def create_vndk_dir_suffix(cls, version):
        """Create VNDK version suffix."""
        return '' if version == 'current' else '-' + version


    @classmethod
    def create_vndk_sp_dir_name(cls, version):
        """Create VNDK-SP directory name from a given version."""
        return 'vndk-sp' + cls.create_vndk_dir_suffix(version)


    @classmethod
    def create_vndk_dir_name(cls, version):
        """Create VNDK directory name from a given version."""
        return 'vndk' + cls.create_vndk_dir_suffix(version)


    @classmethod
    def extract_version_from_name(cls, name):
        """Extract VNDK version from a name."""
        if name in {'vndk', 'vndk-sp'}:
            return 'current'
        elif name.startswith('vndk-sp-'):
            return name[len('vndk-sp-'):]
        elif name.startswith('vndk-'):
            return name[len('vndk-'):]
        else:
            return None


    @classmethod
    def extract_path_component(cls, path, index):
        """Extract n-th path component from a posix path."""
        start = 0
        for i in range(index):
            pos = path.find('/', start)
            if pos == -1:
                return None
            start = pos + 1
        end = path.find('/', start)
        if end == -1:
            return None
        return path[start:end]


    @classmethod
    def extract_version_from_path(cls, path):
        """Extract VNDK version from the third path component."""
        component = cls.extract_path_component(path, 3)
        if not component:
            return None
        return cls.extract_version_from_name(component)


    @classmethod
    def is_in_vndk_dir(cls, path):
        """Determine whether a path is under a VNDK directory."""
        component = cls.extract_path_component(path, 3)
        if not component:
            return False
        return (component == 'vndk' or
                (component.startswith('vndk-') and
                    not component == 'vndk-sp' and
                    not component.startswith('vndk-sp-')))


    @classmethod
    def is_in_vndk_sp_dir(cls, path):
        """Determine whether a path is under a VNDK-SP directory."""
        component = cls.extract_path_component(path, 3)
        if not component:
            return False
        return component == 'vndk-sp' or component.startswith('vndk-sp-')


    @classmethod
    def create_vndk_search_paths(cls, lib_dir, version):
        """Create VNDK/VNDK-SP search paths from lib_dir and version."""
        vndk_sp_name = cls.create_vndk_sp_dir_name(version)
        vndk_name = cls.create_vndk_dir_name(version)
        return VNDKLibTuple(
                [posixpath.join('/vendor', lib_dir, vndk_sp_name),
                 posixpath.join('/system', lib_dir, vndk_sp_name)],
                [posixpath.join('/vendor', lib_dir, vndk_name),
                 posixpath.join('/system', lib_dir, vndk_name)])


    @classmethod
    def create_default(cls):
        """Create default VNDK-SP and VNDK paths without versions."""
        vndk_lib_dirs = VNDKLibDir()
        vndk_lib_dirs.append('current')
        return vndk_lib_dirs


    @classmethod
    def create_from_version(cls, version):
        """Create default VNDK-SP and VNDK paths with the specified version."""
        vndk_lib_dirs = VNDKLibDir()
        vndk_lib_dirs.append(version)
        return vndk_lib_dirs


    @classmethod
    def create_from_dirs(cls, system_dirs, vendor_dirs):
        """Scan system_dirs and vendor_dirs and collect all VNDK-SP and VNDK
        directory paths."""

        def collect_versions(base_dirs):
            versions = set()
            for base_dir in base_dirs:
                for lib_dir in ('lib', 'lib64'):
                    lib_dir_path = os.path.join(base_dir, lib_dir)
                    try:
                        for name in os.listdir(lib_dir_path):
                            version = cls.extract_version_from_name(name)
                            if version:
                                versions.add(version)
                    except FileNotFoundError:
                        pass
            return versions

        versions = set()
        if system_dirs:
            versions.update(collect_versions(system_dirs))
        if vendor_dirs:
            versions.update(collect_versions(vendor_dirs))

        # Sanity check: Versions must not be 'sp' or start with 'sp-'.
        bad_versions = [version for version in versions
                        if version == 'sp' or version.startswith('sp-')]
        if bad_versions:
            raise ValueError('bad vndk version: ' + repr(bad_versions))

        return VNDKLibDir(cls.sorted_version(versions))


    def classify_vndk_libs(self, libs):
        """Classify VNDK/VNDK-SP shared libraries."""
        vndk_sp_libs = collections.defaultdict(set)
        vndk_libs = collections.defaultdict(set)
        other_libs = set()

        for lib in libs:
            component = self.extract_path_component(lib.path, 3)
            if component is None:
                other_libs.add(lib)
                continue

            version = self.extract_version_from_name(component)
            if version is None:
                other_libs.add(lib)
                continue

            if component.startswith('vndk-sp'):
                vndk_sp_libs[version].add(lib)
            else:
                vndk_libs[version].add(lib)

        return (vndk_sp_libs, vndk_libs, other_libs)


    @classmethod
    def _get_property(cls, property_file, name):
        """Read a property from a property file."""
        for line in property_file:
            if line.startswith(name + '='):
                return line[len(name) + 1:].strip()
        return None


    @classmethod
    def get_ro_vndk_version(cls, vendor_dirs):
        """Read ro.vendor.version property from vendor partitions."""
        for vendor_dir in vendor_dirs:
            path = os.path.join(vendor_dir, 'default.prop')
            with open(path, 'r') as property_file:
                result = cls._get_property(property_file, 'ro.vndk.version')
                if result is not None:
                    return result
        return None


    @classmethod
    def sorted_version(cls, versions):
        """Sort versions in the following rule:

        1. 'current' is the first.

        2. The versions that cannot be converted to int are sorted
           lexicographically in descendant order.

        3. The versions that can be converted to int are sorted as integers in
           descendant order.
        """

        current = []
        alpha = []
        numeric = []

        for version in versions:
            if version == 'current':
                current.append(version)
                continue
            try:
                numeric.append(int(version))
            except ValueError:
                alpha.append(version)

        alpha.sort(reverse=True)
        numeric.sort(reverse=True)

        return current + alpha + [str(x) for x in numeric]


    def find_vendor_vndk_version(self, vendor_dirs):
        """Find the best-fitting VNDK version."""

        ro_vndk_version = self.get_ro_vndk_version(vendor_dirs)
        if ro_vndk_version is not None:
            return ro_vndk_version

        if not self:
            return 'current'

        return self.sorted_version(self)[0]


class ELFResolver(object):
    def __init__(self, lib_set, default_search_path):
        self.lib_set = lib_set
        self.default_search_path = default_search_path

    def get_candidates(self, name, dt_rpath=None, dt_runpath=None):
        if dt_rpath:
            for d in dt_rpath:
                yield os.path.join(d, name)
        if dt_runpath:
            for d in dt_runpath:
                yield os.path.join(d, name)
        for d in self.default_search_path:
            yield os.path.join(d, name)

    def resolve(self, name, dt_rpath=None, dt_runpath=None):
        for path in self.get_candidates(name, dt_rpath, dt_runpath):
            try:
                return self.lib_set[path]
            except KeyError:
                continue
        return None


class ELFLinkData(object):
    def __init__(self, partition, path, elf, tag_bit):
        self.partition = partition
        self.path = path
        self.elf = elf
        self.deps_needed = set()
        self.deps_needed_hidden = set()
        self.deps_dlopen = set()
        self.deps_dlopen_hidden = set()
        self.users_needed = set()
        self.users_needed_hidden = set()
        self.users_dlopen = set()
        self.users_dlopen_hidden = set()
        self.imported_ext_symbols = collections.defaultdict(set)
        self._tag_bit = tag_bit
        self.unresolved_symbols = set()
        self.unresolved_dt_needed = []
        self.linked_symbols = dict()

    @property
    def is_ll_ndk(self):
        return TaggedDict.is_ll_ndk(self._tag_bit)

    @property
    def is_vndk_sp(self):
        return TaggedDict.is_vndk_sp(self._tag_bit)

    @property
    def is_vndk_sp_indirect(self):
        return TaggedDict.is_vndk_sp_indirect(self._tag_bit)

    @property
    def is_vndk_sp_indirect_private(self):
        return TaggedDict.is_vndk_sp_indirect_private(self._tag_bit)

    @property
    def is_fwk_only_rs(self):
        return TaggedDict.is_fwk_only_rs(self._tag_bit)

    @property
    def is_sp_hal(self):
        return TaggedDict.is_sp_hal(self._tag_bit)

    def add_needed_dep(self, dst):
        assert dst not in self.deps_needed_hidden
        assert self not in dst.users_needed_hidden
        self.deps_needed.add(dst)
        dst.users_needed.add(self)

    def add_dlopen_dep(self, dst):
        assert dst not in self.deps_dlopen_hidden
        assert self not in dst.users_dlopen_hidden
        self.deps_dlopen.add(dst)
        dst.users_dlopen.add(self)

    def hide_needed_dep(self, dst):
        self.deps_needed.remove(dst)
        dst.users_needed.remove(self)
        self.deps_needed_hidden.add(dst)
        dst.users_needed_hidden.add(self)

    def hide_dlopen_dep(self, dst):
        self.deps_dlopen.remove(dst)
        dst.users_dlopen.remove(self)
        self.deps_dlopen_hidden.add(dst)
        dst.users_dlopen_hidden.add(self)

    @property
    def num_deps(self):
        """Get the number of dependencies.  If a library is linked by both
        NEEDED and DLOPEN relationship, then it will be counted twice."""
        return (len(self.deps_needed) + len(self.deps_needed_hidden) +
                len(self.deps_dlopen) + len(self.deps_dlopen_hidden))

    @property
    def deps_all(self):
        return itertools.chain(self.deps_needed, self.deps_needed_hidden,
                               self.deps_dlopen, self.deps_dlopen_hidden)

    @property
    def deps_good(self):
        return itertools.chain(self.deps_needed, self.deps_dlopen)

    @property
    def deps_needed_all(self):
        return itertools.chain(self.deps_needed, self.deps_needed_hidden)

    @property
    def deps_dlopen_all(self):
        return itertools.chain(self.deps_dlopen, self.deps_dlopen_hidden)

    @property
    def num_users(self):
        """Get the number of users.  If a library is linked by both NEEDED and
        DLOPEN relationship, then it will be counted twice."""
        return (len(self.users_needed) + len(self.users_needed_hidden) +
                len(self.users_dlopen) + len(self.users_dlopen_hidden))

    @property
    def users_all(self):
        return itertools.chain(self.users_needed, self.users_needed_hidden,
                               self.users_dlopen, self.users_dlopen_hidden)

    @property
    def users_good(self):
        return itertools.chain(self.users_needed, self.users_dlopen)

    @property
    def users_needed_all(self):
        return itertools.chain(self.users_needed, self.users_needed_hidden)

    @property
    def users_dlopen_all(self):
        return itertools.chain(self.users_dlopen, self.users_dlopen_hidden)

    def has_dep(self, dst):
        return (dst in self.deps_needed or dst in self.deps_needed_hidden or
                dst in self.deps_dlopen or dst in self.deps_dlopen_hidden)

    def has_user(self, dst):
        return (dst in self.users_needed or dst in self.users_needed_hidden or
                dst in self.users_dlopen or dst in self.users_dlopen_hidden)

    def is_system_lib(self):
        return self.partition == PT_SYSTEM

    def get_dep_linked_symbols(self, dep):
        symbols = set()
        for symbol, exp_lib in self.linked_symbols.items():
            if exp_lib == dep:
                symbols.add(symbol)
        return sorted(symbols)

    def __lt__(self, rhs):
        return self.path < rhs.path


def sorted_lib_path_list(libs):
    libs = [lib.path for lib in libs]
    libs.sort()
    return libs

_VNDK_RESULT_FIELD_NAMES = (
        'll_ndk', 'll_ndk_indirect',
        'vndk_sp', 'vndk_sp_unused', 'vndk_sp_indirect',
        'vndk_sp_indirect_unused', 'vndk_sp_indirect_private', 'vndk',
        'vndk_indirect', 'fwk_only', 'fwk_only_rs', 'sp_hal', 'sp_hal_dep',
        'vnd_only', 'vndk_ext', 'vndk_sp_ext', 'vndk_sp_indirect_ext',
        'extra_vendor_libs')

VNDKResult = defaultnamedtuple('VNDKResult', _VNDK_RESULT_FIELD_NAMES, set())

_SIMPLE_VNDK_RESULT_FIELD_NAMES = (
        'vndk_sp', 'vndk_sp_ext', 'extra_vendor_libs')

SimpleVNDKResult = defaultnamedtuple(
        'SimpleVNDKResult', _SIMPLE_VNDK_RESULT_FIELD_NAMES, set())


class ELFLibDict(defaultnamedtuple('ELFLibDict', ('lib32', 'lib64'), {})):
    def get_lib_dict(self, elf_class):
        return self[elf_class - 1]

    def add(self, path, lib):
        self.get_lib_dict(lib.elf.ei_class)[path] = lib

    def remove(self, lib):
        del self.get_lib_dict(lib.elf.ei_class)[lib.path]

    def get(self, path, default=None):
        for lib_set in self:
            res = lib_set.get(path, None)
            if res:
                return res
        return default

    def keys(self):
        return itertools.chain(self.lib32.keys(), self.lib64.keys())

    def values(self):
        return itertools.chain(self.lib32.values(), self.lib64.values())

    def items(self):
        return itertools.chain(self.lib32.items(), self.lib64.items())


class ELFLinker(object):
    def __init__(self, tagged_paths=None, vndk_lib_dirs=None,
                 ro_vndk_version='current'):
        self.lib_pt = [ELFLibDict() for i in range(NUM_PARTITIONS)]

        if vndk_lib_dirs is None:
            vndk_lib_dirs = VNDKLibDir.create_default()

        self.vndk_lib_dirs = vndk_lib_dirs

        if tagged_paths is None:
            script_dir = os.path.dirname(os.path.abspath(__file__))
            dataset_path = os.path.join(
                    script_dir, 'datasets', 'minimum_tag_file.csv')
            self.tagged_paths = TaggedPathDict.create_from_csv_path(
                    dataset_path, vndk_lib_dirs)
        else:
            self.tagged_paths = tagged_paths

        self.ro_vndk_version = ro_vndk_version

    def _add_lib_to_lookup_dict(self, lib):
        self.lib_pt[lib.partition].add(lib.path, lib)

    def _remove_lib_from_lookup_dict(self, lib):
        self.lib_pt[lib.partition].remove(lib)

    def add_lib(self, partition, path, elf):
        lib = ELFLinkData(partition, path, elf,
                          self.tagged_paths.get_path_tag_bit(path))
        self._add_lib_to_lookup_dict(lib)
        return lib

    def add_dlopen_dep(self, src_path, dst_path):
        num_matches = 0
        for elf_class in (ELF.ELFCLASS32, ELF.ELFCLASS64):
            srcs = self._get_libs_in_elf_class(elf_class, src_path)
            dsts = self._get_libs_in_elf_class(elf_class, dst_path)
            for src, dst in itertools.product(srcs, dsts):
                src.add_dlopen_dep(dst)
                num_matches += 1
        if num_matches == 0:
            raise ValueError('Failed to add dlopen dependency from {} to {}'
                             .format(src_path, dst_path))

    def _get_libs_in_elf_class(self, elf_class, path):
        result = set()
        if '${LIB}' in path:
            lib_dir = 'lib' if elf_class == ELF.ELFCLASS32 else 'lib64'
            path = path.replace('${LIB}', lib_dir)
        if path.startswith('[regex]'):
            patt = re.compile(path[7:])
            for partition in range(NUM_PARTITIONS):
                lib_set = self.lib_pt[partition].get_lib_dict(elf_class)
                for path ,lib in lib_set.items():
                    if patt.match(path):
                        result.add(lib)
        else:
            for partition in range(NUM_PARTITIONS):
                lib_set = self.lib_pt[partition].get_lib_dict(elf_class)
                lib = lib_set.get(path)
                if lib:
                    result.add(lib)
        return result

    def get_lib(self, path):
        for lib_set in self.lib_pt:
            lib = lib_set.get(path)
            if lib:
                return lib
        return None

    def get_libs(self, paths, report_error=None):
        result = set()
        for path in paths:
            lib = self.get_lib(path)
            if not lib:
                if report_error is None:
                    raise ValueError('path not found ' + path)
                report_error(path)
                continue
            result.add(lib)
        return result

    def all_libs(self):
        for lib_set in self.lib_pt:
            for lib in lib_set.values():
                yield lib

    def _compute_lib_dict(self, elf_class):
        res = dict()
        for lib_pt in self.lib_pt:
            res.update(lib_pt.get_lib_dict(elf_class))
        return res

    @staticmethod
    def _compile_path_matcher(root, subdirs):
        dirs = [os.path.normpath(os.path.join(root, i)) for i in subdirs]
        patts = ['(?:' + re.escape(i) + os.sep + ')' for i in dirs]
        return re.compile('|'.join(patts))

    def add_executables_in_dir(self, partition_name, partition, root,
                               alter_partition, alter_subdirs, ignored_subdirs,
                               scan_elf_files):
        root = os.path.abspath(root)
        prefix_len = len(root) + 1

        if alter_subdirs:
            alter_patt = ELFLinker._compile_path_matcher(root, alter_subdirs)
        if ignored_subdirs:
            ignored_patt = ELFLinker._compile_path_matcher(root, ignored_subdirs)

        for path, elf in scan_elf_files(root):
            # Ignore ELF files with unknown machine ID (eg. DSP).
            if elf.e_machine not in ELF.ELF_MACHINES:
                continue

            # Ignore ELF files with matched path.
            short_path = os.path.join('/', partition_name, path[prefix_len:])
            if ignored_subdirs and ignored_patt.match(path):
                continue

            if alter_subdirs and alter_patt.match(path):
                self.add_lib(alter_partition, short_path, elf)
            else:
                self.add_lib(partition, short_path, elf)

    def add_dlopen_deps(self, path):
        patt = re.compile('([^:]*):\\s*(.*)')
        with open(path, 'r') as dlopen_dep_file:
            for line_no, line in enumerate(dlopen_dep_file, start=1):
                match = patt.match(line)
                if not match:
                    continue
                try:
                    self.add_dlopen_dep(match.group(1), match.group(2))
                except ValueError as e:
                    print('error:{}:{}: {}.'.format(path, line_no, e),
                          file=sys.stderr)

    def _find_exported_symbol(self, symbol, libs):
        """Find the shared library with the exported symbol."""
        for lib in libs:
            if symbol in lib.elf.exported_symbols:
                return lib
        return None

    def _resolve_lib_imported_symbols(self, lib, imported_libs, generic_refs):
        """Resolve the imported symbols in a library."""
        for symbol in lib.elf.imported_symbols:
            imported_lib = self._find_exported_symbol(symbol, imported_libs)
            if not imported_lib:
                lib.unresolved_symbols.add(symbol)
            else:
                lib.linked_symbols[symbol] = imported_lib
                if generic_refs:
                    ref_lib = generic_refs.refs.get(imported_lib.path)
                    if not ref_lib or not symbol in ref_lib.exported_symbols:
                        lib.imported_ext_symbols[imported_lib].add(symbol)

    def _resolve_lib_dt_needed(self, lib, resolver):
        imported_libs = []
        for dt_needed in lib.elf.dt_needed:
            dep = resolver.resolve(dt_needed, lib.elf.dt_rpath,
                                   lib.elf.dt_runpath)
            if not dep:
                candidates = list(resolver.get_candidates(
                    dt_needed, lib.elf.dt_rpath, lib.elf.dt_runpath))
                print('warning: {}: Missing needed library: {}  Tried: {}'
                      .format(lib.path, dt_needed, candidates), file=sys.stderr)
                lib.unresolved_dt_needed.append(dt_needed)
                continue
            lib.add_needed_dep(dep)
            imported_libs.append(dep)
        return imported_libs

    def _resolve_lib_deps(self, lib, resolver, generic_refs):
        # Resolve DT_NEEDED entries.
        imported_libs = self._resolve_lib_dt_needed(lib, resolver)

        if generic_refs:
            for imported_lib in imported_libs:
                if imported_lib.path not in generic_refs.refs:
                    # Add imported_lib to imported_ext_symbols to make sure
                    # non-AOSP libraries are in the imported_ext_symbols key
                    # set.
                    lib.imported_ext_symbols[imported_lib].update()

        # Resolve imported symbols.
        self._resolve_lib_imported_symbols(lib, imported_libs, generic_refs)

    def _resolve_lib_set_deps(self, lib_set, resolver, generic_refs):
        for lib in lib_set:
            self._resolve_lib_deps(lib, resolver, generic_refs)


    def _get_system_search_paths(self, lib_dir):
        return [
            '/system/' + lib_dir,
            # To find violating dependencies to vendor partitions.
            '/vendor/' + lib_dir,
        ]

    def _get_vendor_search_paths(self, lib_dir, vndk_sp_dirs, vndk_dirs):
        vendor_lib_dirs = [
            '/vendor/' + lib_dir + '/hw',
            '/vendor/' + lib_dir + '/egl',
            '/vendor/' + lib_dir,
        ]
        system_lib_dirs = [
            # For degenerated VNDK libs.
            '/system/' + lib_dir,
        ]
        return vendor_lib_dirs + vndk_sp_dirs + vndk_dirs + system_lib_dirs


    def _get_vndk_sp_search_paths(self, lib_dir, vndk_sp_dirs):
        fallback_lib_dirs = [
            # To find missing VNDK-SP dependencies.
            '/vendor/' + lib_dir,
            # To find missing VNDK-SP dependencies or LL-NDK.
            '/system/' + lib_dir,
        ]
        return vndk_sp_dirs + fallback_lib_dirs


    def _get_vndk_search_paths(self, lib_dir, vndk_sp_dirs, vndk_dirs):
        fallback_lib_dirs = [
            # To find missing VNDK dependencies or LL-NDK.
            '/system/' + lib_dir,
        ]
        return vndk_sp_dirs + vndk_dirs + fallback_lib_dirs


    def _resolve_elf_class_deps(self, lib_dir, elf_class, generic_refs):
        # Classify libs.
        vndk_lib_dirs = self.vndk_lib_dirs
        lib_dict = self._compute_lib_dict(elf_class)

        system_lib_dict = self.lib_pt[PT_SYSTEM].get_lib_dict(elf_class)
        system_vndk_sp_libs, system_vndk_libs, system_libs = \
                vndk_lib_dirs.classify_vndk_libs(system_lib_dict.values())

        vendor_lib_dict = self.lib_pt[PT_VENDOR].get_lib_dict(elf_class)
        vendor_vndk_sp_libs, vendor_vndk_libs, vendor_libs = \
                vndk_lib_dirs.classify_vndk_libs(vendor_lib_dict.values())

        # Resolve system libs.
        search_paths = self._get_system_search_paths(lib_dir)
        resolver = ELFResolver(lib_dict, search_paths)
        self._resolve_lib_set_deps(system_libs, resolver, generic_refs)

        # Resolve vndk-sp libs
        for version in vndk_lib_dirs:
            vndk_sp_dirs, vndk_dirs = \
                    vndk_lib_dirs.create_vndk_search_paths(lib_dir, version)
            vndk_sp_libs = system_vndk_sp_libs[version] | \
                           vendor_vndk_sp_libs[version]
            search_paths = self._get_vndk_sp_search_paths(lib_dir, vndk_sp_dirs)
            resolver = ELFResolver(lib_dict, search_paths)
            self._resolve_lib_set_deps(vndk_sp_libs, resolver, generic_refs)

        # Resolve vndk libs
        for version in vndk_lib_dirs:
            vndk_sp_dirs, vndk_dirs = \
                    vndk_lib_dirs.create_vndk_search_paths(lib_dir, version)
            vndk_libs = system_vndk_libs[version] | vendor_vndk_libs[version]
            search_paths = self._get_vndk_search_paths(
                    lib_dir, vndk_sp_dirs, vndk_dirs)
            resolver = ELFResolver(lib_dict, search_paths)
            self._resolve_lib_set_deps(vndk_libs, resolver, generic_refs)

        # Resolve vendor libs.
        vndk_sp_dirs, vndk_dirs = vndk_lib_dirs.create_vndk_search_paths(
                lib_dir, self.ro_vndk_version)
        search_paths = self._get_vendor_search_paths(
                lib_dir, vndk_sp_dirs, vndk_dirs)
        resolver = ELFResolver(lib_dict, search_paths)
        self._resolve_lib_set_deps(vendor_libs, resolver, generic_refs)


    def resolve_deps(self, generic_refs=None):
        self._resolve_elf_class_deps('lib', ELF.ELFCLASS32, generic_refs)
        self._resolve_elf_class_deps('lib64', ELF.ELFCLASS64, generic_refs)


    def compute_predefined_sp_hal(self):
        """Find all same-process HALs."""
        return set(lib for lib in self.all_libs() if lib.is_sp_hal)


    def compute_sp_lib(self, generic_refs, ignore_hidden_deps=False):
        def is_ll_ndk_or_sp_hal(lib):
            return lib.is_ll_ndk or lib.is_sp_hal

        ll_ndk = set(lib for lib in self.all_libs() if lib.is_ll_ndk)
        ll_ndk_closure = self.compute_deps_closure(
                ll_ndk, is_ll_ndk_or_sp_hal, ignore_hidden_deps)
        ll_ndk_indirect = ll_ndk_closure - ll_ndk

        def is_ll_ndk(lib):
            return lib.is_ll_ndk

        sp_hal = self.compute_predefined_sp_hal()
        sp_hal_closure = self.compute_deps_closure(
                sp_hal, is_ll_ndk, ignore_hidden_deps)

        def is_aosp_lib(lib):
            return (not generic_refs or
                    generic_refs.classify_lib(lib) != GenericRefs.NEW_LIB)

        vndk_sp_hal = set()
        sp_hal_dep = set()
        for lib in sp_hal_closure - sp_hal:
            if is_aosp_lib(lib):
                vndk_sp_hal.add(lib)
            else:
                sp_hal_dep.add(lib)

        vndk_sp_both = ll_ndk_indirect & vndk_sp_hal
        ll_ndk_indirect -= vndk_sp_both
        vndk_sp_hal -= vndk_sp_both

        return SPLibResult(sp_hal, sp_hal_dep, vndk_sp_hal, ll_ndk,
                           ll_ndk_indirect, vndk_sp_both)

    def normalize_partition_tags(self, sp_hals, generic_refs):
        def is_system_lib_or_sp_hal(lib):
            return lib.is_system_lib() or lib in sp_hals

        for lib in self.lib_pt[PT_SYSTEM].values():
            if all(is_system_lib_or_sp_hal(dep) for dep in lib.deps_all):
                continue
            # If a system module is depending on a vendor shared library and
            # such shared library is not a SP-HAL library, then emit an error
            # and hide the dependency.
            for dep in list(lib.deps_needed_all):
                if not is_system_lib_or_sp_hal(dep):
                    print('error: {}: system exe/lib must not depend on '
                          'vendor lib {}.  Assume such dependency does '
                          'not exist.'.format(lib.path, dep.path),
                          file=sys.stderr)
                    lib.hide_needed_dep(dep)
            for dep in list(lib.deps_dlopen_all):
                if not is_system_lib_or_sp_hal(dep):
                    print('error: {}: system exe/lib must not dlopen() '
                          'vendor lib {}.  Assume such dependency does '
                          'not exist.'.format(lib.path, dep.path),
                          file=sys.stderr)
                    lib.hide_dlopen_dep(dep)

    @staticmethod
    def _parse_action_on_ineligible_lib(arg):
        follow = False
        warn = False
        for flag in arg.split(','):
            if flag == 'follow':
                follow = True
            elif flag == 'warn':
                warn = True
            elif flag == 'ignore':
                continue
            else:
                raise ValueError('unknown action \"{}\"'.format(flag))
        return (follow, warn)

    def compute_degenerated_vndk(self, generic_refs, tagged_paths=None,
                                 action_ineligible_vndk_sp='warn',
                                 action_ineligible_vndk='warn'):
        # Find LL-NDK libs.
        ll_ndk = set(lib for lib in self.all_libs() if lib.is_ll_ndk)

        # Find pre-defined libs.
        fwk_only_rs = set(lib for lib in self.all_libs() if lib.is_fwk_only_rs)
        predefined_vndk_sp = set(
                lib for lib in self.all_libs() if lib.is_vndk_sp)
        predefined_vndk_sp_indirect = set(
                lib for lib in self.all_libs() if lib.is_vndk_sp_indirect)
        predefined_vndk_sp_indirect_private = set(
                lib for lib in self.all_libs()
                if lib.is_vndk_sp_indirect_private)

        # FIXME: Don't squash VNDK-SP-Indirect-Private into VNDK-SP-Indirect.
        predefined_vndk_sp_indirect |= predefined_vndk_sp_indirect_private

        # Find SP-HAL libs.
        sp_hal = self.compute_predefined_sp_hal()

        # Normalize partition tags.  We expect many violations from the
        # pre-Treble world.  Guess a resolution for the incorrect partition
        # tag.
        self.normalize_partition_tags(sp_hal, generic_refs)

        # Find SP-HAL-Dep libs.
        def is_aosp_lib(lib):
            if not generic_refs:
                # If generic reference is not available, then assume all system
                # libs are AOSP libs.
                return lib.partition == PT_SYSTEM
            return generic_refs.has_same_name_lib(lib)

        def is_not_sp_hal_dep(lib):
            if lib.is_ll_ndk or lib in sp_hal:
                return True
            return is_aosp_lib(lib)

        sp_hal_dep = self.compute_deps_closure(sp_hal, is_not_sp_hal_dep, True)
        sp_hal_dep -= sp_hal

        # Find VNDK-SP libs.
        def is_not_vndk_sp(lib):
            return lib.is_ll_ndk or lib in sp_hal or lib in sp_hal_dep

        follow_ineligible_vndk_sp, warn_ineligible_vndk_sp = \
                self._parse_action_on_ineligible_lib(action_ineligible_vndk_sp)
        vndk_sp = set()
        for lib in itertools.chain(sp_hal, sp_hal_dep):
            for dep in lib.deps_all:
                if is_not_vndk_sp(dep):
                    continue
                if dep in predefined_vndk_sp:
                    vndk_sp.add(dep)
                    continue
                if warn_ineligible_vndk_sp:
                    print('error: SP-HAL {} depends on non vndk-sp '
                          'library {}.'.format(lib.path, dep.path),
                          file=sys.stderr)
                if follow_ineligible_vndk_sp:
                    vndk_sp.add(dep)

        # Find VNDK-SP-Indirect libs.
        def is_not_vndk_sp_indirect(lib):
            return lib.is_ll_ndk or lib in vndk_sp or lib in fwk_only_rs

        vndk_sp_indirect = self.compute_deps_closure(
                vndk_sp, is_not_vndk_sp_indirect, True)
        vndk_sp_indirect -= vndk_sp

        # Find unused predefined VNDK-SP libs.
        vndk_sp_unused = set(lib for lib in predefined_vndk_sp
                             if VNDKLibDir.is_in_vndk_sp_dir(lib.path))
        vndk_sp_unused -= vndk_sp
        vndk_sp_unused -= vndk_sp_indirect

        # Find dependencies of unused predefined VNDK-SP libs.
        def is_not_vndk_sp_indirect_unused(lib):
            return is_not_vndk_sp_indirect(lib) or lib in vndk_sp_indirect
        vndk_sp_unused_deps = self.compute_deps_closure(
                vndk_sp_unused, is_not_vndk_sp_indirect_unused, True)
        vndk_sp_unused_deps -= vndk_sp_unused

        vndk_sp_indirect_unused = set(lib for lib in predefined_vndk_sp_indirect
                                      if VNDKLibDir.is_in_vndk_sp_dir(lib.path))
        vndk_sp_indirect_unused -= vndk_sp_indirect
        vndk_sp_indirect_unused -= vndk_sp_unused
        vndk_sp_indirect_unused |= vndk_sp_unused_deps

        # TODO: Compute VNDK-SP-Indirect-Private.
        vndk_sp_indirect_private = set()

        assert not (vndk_sp & vndk_sp_indirect)
        assert not (vndk_sp_unused & vndk_sp_indirect_unused)

        # Define helper functions for vndk_sp sets.
        def is_vndk_sp_public(lib):
            return lib in vndk_sp or lib in vndk_sp_unused or \
                   lib in vndk_sp_indirect or \
                   lib in vndk_sp_indirect_unused

        def is_vndk_sp(lib):
            return is_vndk_sp_public(lib) or lib in vndk_sp_indirect_private

        def is_vndk_sp_unused(lib):
            return lib in vndk_sp_unused or lib in vndk_sp_indirect_unused

        def relabel_vndk_sp_as_used(lib):
            assert is_vndk_sp_unused(lib)

            if lib in vndk_sp_unused:
                vndk_sp_unused.remove(lib)
                vndk_sp.add(lib)
            else:
                vndk_sp_indirect_unused.remove(lib)
                vndk_sp_indirect.add(lib)

            # Add the dependencies to vndk_sp_indirect if they are not vndk_sp.
            closure = self.compute_deps_closure(
                    {lib}, lambda lib: lib not in vndk_sp_indirect_unused, True)
            closure.remove(lib)
            vndk_sp_indirect_unused.difference_update(closure)
            vndk_sp_indirect.update(closure)

        # Find VNDK-SP-Ext libs.
        vndk_sp_ext = set()
        def collect_vndk_ext(libs):
            result = set()
            for lib in libs:
                for dep in lib.imported_ext_symbols:
                    if dep in vndk_sp and dep not in vndk_sp_ext:
                        result.add(dep)
            return result

        candidates = collect_vndk_ext(self.lib_pt[PT_VENDOR].values())
        while candidates:
            vndk_sp_ext |= candidates
            candidates = collect_vndk_ext(candidates)

        # Find VNDK-SP-Indirect-Ext libs.
        vndk_sp_indirect_ext = set()
        def collect_vndk_sp_indirect_ext(libs):
            result = set()
            for lib in libs:
                exts = set(lib.imported_ext_symbols.keys())
                for dep in lib.deps_all:
                    if not is_vndk_sp_public(dep):
                        continue
                    if dep in vndk_sp_ext or dep in vndk_sp_indirect_ext:
                        continue
                    # If lib is using extended definition from deps, then we
                    # have to make a copy of dep.
                    if dep in exts:
                        result.add(dep)
                        continue
                    # If lib is using non-predefined VNDK-SP-Indirect, then we
                    # have to make a copy of dep.
                    if dep not in predefined_vndk_sp and \
                            dep not in predefined_vndk_sp_indirect:
                        result.add(dep)
                        continue
            return result

        def is_not_vndk_sp_indirect(lib):
            return lib.is_ll_ndk or lib in vndk_sp or lib in fwk_only_rs

        candidates = collect_vndk_sp_indirect_ext(vndk_sp_ext)
        while candidates:
            vndk_sp_indirect_ext |= candidates
            candidates = collect_vndk_sp_indirect_ext(candidates)

        # Find VNDK libs (a.k.a. system shared libs directly used by vendor
        # partition.)
        def is_not_vndk(lib):
            if lib.is_ll_ndk or is_vndk_sp_public(lib) or lib in fwk_only_rs:
                return True
            return lib.partition != PT_SYSTEM

        def is_eligible_lib_access(lib, dep):
            return not tagged_paths or \
                    tagged_paths.is_path_visible(lib.path, dep.path)

        follow_ineligible_vndk, warn_ineligible_vndk = \
                self._parse_action_on_ineligible_lib(action_ineligible_vndk)
        vndk = set()
        extra_vendor_libs = set()
        def collect_vndk(vendor_libs):
            next_vendor_libs = set()
            for lib in vendor_libs:
                for dep in lib.deps_all:
                    if is_vndk_sp_unused(dep):
                        relabel_vndk_sp_as_used(dep)
                        continue
                    if is_not_vndk(dep):
                        continue
                    if not is_aosp_lib(dep):
                        # The dependency should be copied into vendor partition
                        # as an extra vendor lib.
                        if dep not in extra_vendor_libs:
                            next_vendor_libs.add(dep)
                            extra_vendor_libs.add(dep)
                        continue
                    if is_eligible_lib_access(lib, dep):
                        vndk.add(dep)
                        continue
                    if warn_ineligible_vndk:
                        print('warning: vendor lib/exe {} depends on '
                              'ineligible framework shared lib {}.'
                              .format(lib.path, dep.path), file=sys.stderr)
                    if follow_ineligible_vndk:
                        vndk.add(dep)
            return next_vendor_libs

        candidates = collect_vndk(self.lib_pt[PT_VENDOR].values())
        while candidates:
            candidates = collect_vndk(candidates)

        vndk_indirect = self.compute_deps_closure(vndk, is_not_vndk, True)
        vndk_indirect -= vndk

        def is_vndk(lib):
            return lib in vndk or lib in vndk_indirect

        # Find VNDK-EXT libs (VNDK libs with extended definitions and the
        # extended definitions are used by the vendor modules (including
        # extra_vendor_libs).

        # FIXME: DAUX libraries won't be found by the following algorithm.
        vndk_ext = set()

        def collect_vndk_ext(libs):
            result = set()
            for lib in libs:
                for dep in lib.imported_ext_symbols:
                    if dep in vndk and dep not in vndk_ext:
                        result.add(dep)
            return result

        candidates = collect_vndk_ext(self.lib_pt[PT_VENDOR].values())
        candidates |= collect_vndk_ext(extra_vendor_libs)

        while candidates:
            vndk_ext |= candidates
            candidates = collect_vndk_ext(candidates)

        # Compute LL-NDK-Indirect.
        def is_not_ll_ndk_indirect(lib):
            return lib.is_ll_ndk or lib.is_sp_hal or is_vndk_sp(lib) or \
                   is_vndk_sp(lib) or is_vndk(lib)

        ll_ndk_indirect = self.compute_deps_closure(
                ll_ndk, is_not_ll_ndk_indirect, True)
        ll_ndk_indirect -= ll_ndk

        # Return the VNDK classifications.
        return VNDKResult(
                ll_ndk=ll_ndk,
                ll_ndk_indirect=ll_ndk_indirect,
                vndk_sp=vndk_sp,
                vndk_sp_indirect=vndk_sp_indirect,
                # vndk_sp_indirect_private=vndk_sp_indirect_private,
                vndk_sp_unused=vndk_sp_unused,
                vndk_sp_indirect_unused=vndk_sp_indirect_unused,
                vndk=vndk,
                vndk_indirect=vndk_indirect,
                # fwk_only=fwk_only,
                fwk_only_rs=fwk_only_rs,
                sp_hal=sp_hal,
                sp_hal_dep=sp_hal_dep,
                # vnd_only=vnd_only,
                vndk_ext=vndk_ext,
                vndk_sp_ext=vndk_sp_ext,
                vndk_sp_indirect_ext=vndk_sp_indirect_ext,
                extra_vendor_libs=extra_vendor_libs)

    @staticmethod
    def _compute_closure(root_set, is_excluded, get_successors):
        closure = set(root_set)
        stack = list(root_set)
        while stack:
            lib = stack.pop()
            for succ in get_successors(lib):
                if is_excluded(succ):
                    continue
                if succ not in closure:
                    closure.add(succ)
                    stack.append(succ)
        return closure

    @classmethod
    def compute_deps_closure(cls, root_set, is_excluded,
                             ignore_hidden_deps=False):
        get_successors = (lambda x: x.deps_good) if ignore_hidden_deps else \
                         (lambda x: x.deps_all)
        return cls._compute_closure(root_set, is_excluded, get_successors)

    @classmethod
    def compute_users_closure(cls, root_set, is_excluded,
                              ignore_hidden_users=False):
        get_successors = (lambda x: x.users_good) if ignore_hidden_users else \
                         (lambda x: x.users_all)
        return cls._compute_closure(root_set, is_excluded, get_successors)

    @staticmethod
    def _create_internal(scan_elf_files, system_dirs, system_dirs_as_vendor,
                         system_dirs_ignored, vendor_dirs,
                         vendor_dirs_as_system, vendor_dirs_ignored,
                         extra_deps, generic_refs, tagged_paths,
                         vndk_lib_dirs):
        if vndk_lib_dirs is None:
            vndk_lib_dirs = VNDKLibDir.create_from_dirs(
                    system_dirs, vendor_dirs)
        ro_vndk_version = vndk_lib_dirs.find_vendor_vndk_version(vendor_dirs)
        graph = ELFLinker(tagged_paths, vndk_lib_dirs, ro_vndk_version)

        if system_dirs:
            for path in system_dirs:
                graph.add_executables_in_dir('system', PT_SYSTEM, path,
                                             PT_VENDOR, system_dirs_as_vendor,
                                             system_dirs_ignored,
                                             scan_elf_files)

        if vendor_dirs:
            for path in vendor_dirs:
                graph.add_executables_in_dir('vendor', PT_VENDOR, path,
                                             PT_SYSTEM, vendor_dirs_as_system,
                                             vendor_dirs_ignored,
                                             scan_elf_files)

        if extra_deps:
            for path in extra_deps:
                graph.add_dlopen_deps(path)

        graph.resolve_deps(generic_refs)

        return graph

    @staticmethod
    def create(system_dirs=None, system_dirs_as_vendor=None,
               system_dirs_ignored=None, vendor_dirs=None,
               vendor_dirs_as_system=None, vendor_dirs_ignored=None,
               extra_deps=None, generic_refs=None, tagged_paths=None,
               vndk_lib_dirs=None):
        return ELFLinker._create_internal(
                scan_elf_files, system_dirs, system_dirs_as_vendor,
                system_dirs_ignored, vendor_dirs, vendor_dirs_as_system,
                vendor_dirs_ignored, extra_deps, generic_refs, tagged_paths,
                vndk_lib_dirs)


#------------------------------------------------------------------------------
# Generic Reference
#------------------------------------------------------------------------------

class GenericRefs(object):
    NEW_LIB = 0
    EXPORT_EQUAL = 1
    EXPORT_SUPER_SET = 2
    MODIFIED = 3

    def __init__(self):
        self.refs = dict()
        self._lib_names = set()

    def add(self, path, elf):
        self.refs[path] = elf
        self._lib_names.add(os.path.basename(path))

    def _load_from_sym_dir(self, root):
        root = os.path.abspath(root)
        prefix_len = len(root) + 1
        for base, dirnames, filenames in os.walk(root):
            for filename in filenames:
                if not filename.endswith('.sym'):
                    continue
                path = os.path.join(base, filename)
                lib_path = '/' + path[prefix_len:-4]
                with open(path, 'r') as f:
                    self.add(lib_path, ELF.load_dump(path))

    @staticmethod
    def create_from_sym_dir(root):
        result = GenericRefs()
        result._load_from_sym_dir(root)
        return result

    def _load_from_image_dir(self, root, prefix):
        root = os.path.abspath(root)
        root_len = len(root) + 1
        for path, elf in scan_elf_files(root):
            self.add(os.path.join(prefix, path[root_len:]), elf)

    @staticmethod
    def create_from_image_dir(root, prefix):
        result = GenericRefs()
        result._load_from_image_dir(root, prefix)
        return result

    def classify_lib(self, lib):
        ref_lib = self.refs.get(lib.path)
        if not ref_lib:
            return GenericRefs.NEW_LIB
        exported_symbols = lib.elf.exported_symbols
        if exported_symbols == ref_lib.exported_symbols:
            return GenericRefs.EXPORT_EQUAL
        if exported_symbols > ref_lib.exported_symbols:
            return GenericRefs.EXPORT_SUPER_SET
        return GenericRefs.MODIFIED

    def is_equivalent_lib(self, lib):
        return self.classify_lib(lib) == GenericRefs.EXPORT_EQUAL

    def has_same_name_lib(self, lib):
        return os.path.basename(lib.path) in self._lib_names


#------------------------------------------------------------------------------
# APK Dep
#------------------------------------------------------------------------------
def _build_lib_names_dict(graph, min_name_len=6, lib_ext='.so'):
    names = collections.defaultdict(set)
    for lib in graph.all_libs():
        name = os.path.basename(lib.path)
        root, ext = os.path.splitext(name)

        if ext != lib_ext:
            continue

        if not lib.elf.is_jni_lib():
            continue

        names[name].add(lib)
        names[root].add(lib)

        if root.startswith('lib') and len(root) > min_name_len:
            # FIXME: libandroid.so is a JNI lib.  However, many apps have
            # "android" as a constant string literal, thus "android" is
            # skipped here to reduce the false positives.
            #
            # Note: It is fine to exclude libandroid.so because it is only
            # a user of JNI and it does not define any JNI methods.
            if root != 'libandroid':
                names[root[3:]].add(lib)
    return names


def _enumerate_partition_paths(partition, root):
    prefix_len = len(root) + 1
    for base, dirs, files in os.walk(root):
        for filename in files:
            path = os.path.join(base, filename)
            android_path = posixpath.join('/', partition, path[prefix_len:])
            yield (android_path, path)


def _enumerate_paths(system_dirs, vendor_dirs):
    for root in system_dirs:
        for ap, path in _enumerate_partition_paths('system', root):
            yield (ap, path)
    for root in vendor_dirs:
        for ap, path in _enumerate_partition_paths('vendor', root):
            yield (ap, path)


def scan_apk_dep(graph, system_dirs, vendor_dirs):
    libnames = _build_lib_names_dict(graph)
    results = []

    for ap, path in _enumerate_paths(system_dirs, vendor_dirs):
        # Read the dex file from various file formats
        try:
            if DexFileReader.is_zipfile(path):
                strs = set(DexFileReader.enumerate_dex_strings_apk(path))
            elif DexFileReader.is_vdex_file(path):
                strs = set(DexFileReader.enumerate_dex_strings_vdex(path))
            else:
                continue
        except FileNotFoundError:
            continue

        # Skip the file that does not call System.loadLibrary()
        if 'loadLibrary' not in strs:
            continue

        # Collect libraries from string tables
        libs = set()
        for string in strs:
            try:
                libs.update(libnames[string])
            except KeyError:
                pass

        if libs:
            results.append((ap, sorted_lib_path_list(libs)))

    results.sort()
    return results


#------------------------------------------------------------------------------
# Module Info
#------------------------------------------------------------------------------

class ModuleInfo(object):
    def __init__(self, json=None):
        if not json:
            self._mods = dict()
            return

        mods = collections.defaultdict(set)
        installed_path_patt = re.compile(
                '.*[\\\\/]target[\\\\/]product[\\\\/][^\\\\/]+([\\\\/].*)$')
        for name, module in json.items():
            for path in module['installed']:
                match = installed_path_patt.match(path)
                if match:
                    for path in module['path']:
                        mods[match.group(1)].add(path)
        self._mods = { installed_path: sorted(src_dirs)
                       for installed_path, src_dirs in mods.items() }

    def get_module_path(self, installed_path):
        return self._mods.get(installed_path, [])

    @staticmethod
    def load(f):
        return ModuleInfo(json.load(f))

    @staticmethod
    def load_from_path_or_default(path):
        if not path:
            return ModuleInfo()
        with open(path, 'r') as f:
            return ModuleInfo.load(f)


#------------------------------------------------------------------------------
# Commands
#------------------------------------------------------------------------------

class Command(object):
    def __init__(self, name, help):
        self.name = name
        self.help = help

    def add_argparser_options(self, parser):
        pass

    def main(self, args):
        return 0


class ELFDumpCommand(Command):
    def __init__(self):
        super(ELFDumpCommand, self).__init__(
                'elfdump', help='Dump ELF .dynamic section')

    def add_argparser_options(self, parser):
        parser.add_argument('path', help='path to an ELF file')

    def main(self, args):
        try:
            ELF.load(args.path).dump()
        except ELFError as e:
            print('error: {}: Bad ELF file ({})'.format(args.path, e),
                  file=sys.stderr)
            sys.exit(1)
        return 0


class CreateGenericRefCommand(Command):
    def __init__(self):
        super(CreateGenericRefCommand, self).__init__(
                'create-generic-ref', help='Create generic references')

    def add_argparser_options(self, parser):
        parser.add_argument('dir')

        parser.add_argument(
                '--output', '-o', metavar='PATH', required=True,
                help='output directory')

    def main(self, args):
        root = os.path.abspath(args.dir)
        print(root)
        prefix_len = len(root) + 1
        for path, elf in scan_elf_files(root):
            name = path[prefix_len:]
            print('Processing:', name, file=sys.stderr)
            out = os.path.join(args.output, name) + '.sym'
            makedirs(os.path.dirname(out), exist_ok=True)
            with open(out, 'w') as f:
                elf.dump(f)
        return 0


class ELFGraphCommand(Command):
    def add_argparser_options(self, parser):
        parser.add_argument(
                '--load-extra-deps', action='append',
                help='load extra module dependencies')

        parser.add_argument(
                '--system', action='append',
                help='path to system partition contents')

        parser.add_argument(
                '--vendor', action='append',
                help='path to vendor partition contents')

        parser.add_argument(
                '--system-dir-as-vendor', action='append',
                help='sub directory of system partition that has vendor files')

        parser.add_argument(
                '--system-dir-ignored', action='append',
                help='sub directory of system partition that must be ignored')

        parser.add_argument(
                '--vendor-dir-as-system', action='append',
                help='sub directory of vendor partition that has system files')

        parser.add_argument(
                '--vendor-dir-ignored', action='append',
                help='sub directory of vendor partition that must be ignored')

        parser.add_argument(
                '--load-generic-refs',
                help='compare with generic reference symbols')

        parser.add_argument(
                '--aosp-system',
                help='compare with AOSP generic system image directory')

        parser.add_argument('--tag-file', help='lib tag file')

    def get_generic_refs_from_args(self, args):
        if args.load_generic_refs:
            return GenericRefs.create_from_sym_dir(args.load_generic_refs)
        if args.aosp_system:
            return GenericRefs.create_from_image_dir(args.aosp_system,
                                                     '/system')
        return None

    def _check_arg_dir_exists(self, arg_name, dirs):
        for path in dirs:
            if not os.path.exists(path):
                print('error: Failed to find the directory "{}" specified in {}'
                        .format(path, arg_name), file=sys.stderr)
                sys.exit(1)
            if not os.path.isdir(path):
                print('error: Path "{}" specified in {} is not a directory'
                        .format(path, arg_name), file=sys.stderr)
                sys.exit(1)

    def check_dirs_from_args(self, args):
        self._check_arg_dir_exists('--system', args.system)
        self._check_arg_dir_exists('--vendor', args.vendor)

    def create_from_args(self, args):
        self.check_dirs_from_args(args)

        generic_refs = self.get_generic_refs_from_args(args)

        vndk_lib_dirs = VNDKLibDir.create_from_dirs(args.system, args.vendor)

        if args.tag_file:
            tagged_paths = TaggedPathDict.create_from_csv_path(
                    args.tag_file, vndk_lib_dirs)
        else:
            tagged_paths = None

        graph = ELFLinker.create(args.system, args.system_dir_as_vendor,
                                 args.system_dir_ignored,
                                 args.vendor, args.vendor_dir_as_system,
                                 args.vendor_dir_ignored,
                                 args.load_extra_deps,
                                 generic_refs=generic_refs,
                                 tagged_paths=tagged_paths)

        return (generic_refs, graph, tagged_paths, vndk_lib_dirs)


class VNDKCommandBase(ELFGraphCommand):
    def add_argparser_options(self, parser):
        super(VNDKCommandBase, self).add_argparser_options(parser)

        parser.add_argument('--no-default-dlopen-deps', action='store_true',
                help='do not add default dlopen dependencies')

        parser.add_argument(
                '--action-ineligible-vndk-sp', default='warn',
                help='action when a sp-hal uses non-vndk-sp libs '
                     '(option: follow,warn,ignore)')

        parser.add_argument(
                '--action-ineligible-vndk', default='warn',
                help='action when a vendor lib/exe uses fwk-only libs '
                     '(option: follow,warn,ignore)')

    def create_from_args(self, args):
        """Create all essential data structures for VNDK computation."""

        generic_refs, graph, tagged_paths, vndk_lib_dirs  = \
                super(VNDKCommandBase, self).\
                create_from_args(args)

        if not args.no_default_dlopen_deps:
            script_dir = os.path.dirname(os.path.abspath(__file__))
            minimum_dlopen_deps = os.path.join(script_dir, 'datasets',
                                               'minimum_dlopen_deps.txt')
            graph.add_dlopen_deps(minimum_dlopen_deps)

        return (generic_refs, graph, tagged_paths, vndk_lib_dirs)


class VNDKCommand(VNDKCommandBase):
    def __init__(self):
        super(VNDKCommand, self).__init__(
                'vndk', help='Compute VNDK libraries set')

    def add_argparser_options(self, parser):
        super(VNDKCommand, self).add_argparser_options(parser)

        parser.add_argument(
                '--warn-incorrect-partition', action='store_true',
                help='warn about libraries only have cross partition linkages')

        parser.add_argument(
                '--full', action='store_true',
                help='print all classification')

        parser.add_argument(
                '--output-format', default='tag',
                help='output format for vndk classification')

        parser.add_argument(
                '--file-size-output',
                help='output file for calculated file sizes')

    def _warn_incorrect_partition_lib_set(self, lib_set, partition, error_msg):
        for lib in lib_set.values():
            if not lib.num_users:
                continue
            if all((user.partition != partition for user in lib.users_all)):
                print(error_msg.format(lib.path), file=sys.stderr)

    def _warn_incorrect_partition(self, graph):
        self._warn_incorrect_partition_lib_set(
                graph.lib_pt[PT_VENDOR], PT_VENDOR,
                'warning: {}: This is a vendor library with framework-only '
                'usages.')

        self._warn_incorrect_partition_lib_set(
                graph.lib_pt[PT_SYSTEM], PT_SYSTEM,
                'warning: {}: This is a framework library with vendor-only '
                'usages.')

    @staticmethod
    def _extract_simple_vndk_result(vndk_result):
        field_name_tags = [
            ('vndk_sp', 'vndk_sp'),
            ('vndk_sp_unused', 'vndk_sp'),
            ('vndk_sp_indirect', 'vndk_sp'),
            ('vndk_sp_indirect_unused', 'vndk_sp'),
            ('vndk_sp_indirect_private', 'vndk_sp'),

            ('vndk_sp_ext', 'vndk_sp_ext'),
            ('vndk_sp_indirect_ext', 'vndk_sp_ext'),

            ('vndk_ext', 'extra_vendor_libs'),
            ('extra_vendor_libs', 'extra_vendor_libs'),
        ]
        results = SimpleVNDKResult()
        for field_name, tag in field_name_tags:
            getattr(results, tag).update(getattr(vndk_result, field_name))
        return results

    def _print_tags(self, vndk_lib, full, file=sys.stdout):
        if full:
            result_tags = _VNDK_RESULT_FIELD_NAMES
            results = vndk_lib
        else:
            # Simplified VNDK output with only three sets.
            result_tags = _SIMPLE_VNDK_RESULT_FIELD_NAMES
            results = self._extract_simple_vndk_result(vndk_lib)

        for tag in result_tags:
            libs = getattr(results, tag)
            tag += ':'
            for lib in sorted_lib_path_list(libs):
                print(tag, lib, file=file)

    def _print_make(self, vndk_lib, file=sys.stdout):
        def get_module_name(path):
            name = os.path.basename(path)
            root, ext = os.path.splitext(name)
            return root

        def get_module_names(lib_set):
            return sorted({ get_module_name(lib.path) for lib in lib_set })

        results = self._extract_simple_vndk_result(vndk_lib)
        vndk_sp = get_module_names(results.vndk_sp)
        vndk_sp_ext = get_module_names(results.vndk_sp_ext)
        extra_vendor_libs= get_module_names(results.extra_vendor_libs)

        def format_module_names(module_names):
            return '\\\n    ' +  ' \\\n    '.join(module_names)

        script_dir = os.path.dirname(os.path.abspath(__file__))
        template_path = os.path.join(script_dir, 'templates', 'vndk.txt')
        with open(template_path, 'r') as f:
            template = f.read()

        template = template.replace('##_VNDK_SP_##',
                                    format_module_names(vndk_sp))
        template = template.replace('##_VNDK_SP_EXT_##',
                                    format_module_names(vndk_sp_ext))
        template = template.replace('##_EXTRA_VENDOR_LIBS_##',
                                    format_module_names(extra_vendor_libs))

        file.write(template)

    def _print_file_size_output(self, graph, vndk_lib, file=sys.stderr):
        def collect_tags(lib):
            tags = []
            for field_name in _VNDK_RESULT_FIELD_NAMES:
                if lib in getattr(vndk_lib, field_name):
                    tags.append(field_name)
            return ' '.join(tags)

        writer = csv.writer(file, lineterminator='\n')
        writer.writerow(('Path', 'Tag', 'File size', 'RO segment file size',
                         'RO segment mem size', 'RW segment file size',
                         'RW segment mem size'))

        # Print the file size of all ELF files.
        for lib in sorted(graph.all_libs()):
            writer.writerow((lib.path, collect_tags(lib), lib.elf.file_size,
                             lib.elf.ro_seg_file_size, lib.elf.ro_seg_mem_size,
                             lib.elf.rw_seg_file_size, lib.elf.rw_seg_mem_size))

        # Calculate the summation of each sets.
        def calc_total_size(lib_set):
            total_file_size = 0
            total_ro_seg_file_size = 0
            total_ro_seg_mem_size = 0
            total_rw_seg_file_size = 0
            total_rw_seg_mem_size = 0

            for lib in lib_set:
                total_file_size += lib.elf.file_size
                total_ro_seg_file_size += lib.elf.ro_seg_file_size
                total_ro_seg_mem_size += lib.elf.ro_seg_mem_size
                total_rw_seg_file_size += lib.elf.rw_seg_file_size
                total_rw_seg_mem_size += lib.elf.rw_seg_mem_size

            return [total_file_size, total_ro_seg_file_size,
                    total_ro_seg_mem_size, total_rw_seg_file_size,
                    total_rw_seg_mem_size]

        SEPARATOR = ('----------', None, None, None, None, None, None)

        writer.writerow(SEPARATOR)
        for tag in _VNDK_RESULT_FIELD_NAMES:
            lib_set = getattr(vndk_lib, tag)
            lib_set = [lib for lib in lib_set if lib.elf.is_32bit]
            total = calc_total_size(lib_set)
            writer.writerow(['Subtotal ' + tag + ' (32-bit)', None] + total)

        writer.writerow(SEPARATOR)
        for tag in _VNDK_RESULT_FIELD_NAMES:
            lib_set = getattr(vndk_lib, tag)
            lib_set = [lib for lib in lib_set if not lib.elf.is_32bit]
            total = calc_total_size(lib_set)
            writer.writerow(['Subtotal ' + tag + ' (64-bit)', None] + total)

        writer.writerow(SEPARATOR)
        for tag in _VNDK_RESULT_FIELD_NAMES:
            total = calc_total_size(getattr(vndk_lib, tag))
            writer.writerow(['Subtotal ' + tag + ' (both)', None] + total)

        # Calculate the summation of all ELF files.
        writer.writerow(SEPARATOR)
        writer.writerow(['Total', None] + calc_total_size(graph.all_libs()))

    def main(self, args):
        generic_refs, graph, tagged_paths, vndk_lib_dirs = \
                self.create_from_args(args)

        if args.warn_incorrect_partition:
            self._warn_incorrect_partition(graph)

        # Compute vndk heuristics.
        vndk_lib = graph.compute_degenerated_vndk(
                generic_refs, tagged_paths, args.action_ineligible_vndk_sp,
                args.action_ineligible_vndk)

        # Print results.
        if args.output_format == 'make':
            self._print_make(vndk_lib)
        else:
            self._print_tags(vndk_lib, args.full)

        # Calculate and print file sizes.
        if args.file_size_output:
            with open(args.file_size_output, 'w') as fp:
                self._print_file_size_output(graph, vndk_lib, file=fp)
        return 0


class DepsInsightCommand(VNDKCommandBase):
    def __init__(self):
        super(DepsInsightCommand, self).__init__(
                'deps-insight', help='Generate HTML to show dependencies')

    def add_argparser_options(self, parser):
        super(DepsInsightCommand, self).add_argparser_options(parser)

        parser.add_argument('--module-info')

        parser.add_argument(
                '--output', '-o', help='output directory')

    @staticmethod
    def serialize_data(libs, vndk_lib, module_info):
        strs = []
        strs_dict = dict()

        libs.sort(key=lambda lib: lib.path)
        libs_dict = {lib: i for i, lib in enumerate(libs)}

        def get_str_idx(s):
            try:
                return strs_dict[s]
            except KeyError:
                idx = len(strs)
                strs_dict[s] = idx
                strs.append(s)
                return idx

        def collect_path_sorted_lib_idxs(libs):
            return [libs_dict[lib] for lib in sorted(libs)]

        def collect_deps(lib):
            queue = list(lib.deps_all)
            visited = set(queue)
            visited.add(lib)
            deps = []

            # Traverse dependencies with breadth-first search.
            while queue:
                # Collect dependencies for next queue.
                next_queue = []
                for lib in queue:
                    for dep in lib.deps_all:
                        if dep not in visited:
                            next_queue.append(dep)
                            visited.add(dep)

                # Append current queue to result.
                deps.append(collect_path_sorted_lib_idxs(queue))

                queue = next_queue

            return deps

        def collect_source_dir_paths(lib):
            return [get_str_idx(path)
                    for path in module_info.get_module_path(lib.path)]

        def collect_tags(lib):
            tags = []
            for field_name in _VNDK_RESULT_FIELD_NAMES:
                if lib in getattr(vndk_lib, field_name):
                    tags.append(get_str_idx(field_name))
            return tags

        mods = []
        for lib in libs:
            mods.append([get_str_idx(lib.path),
                         32 if lib.elf.is_32bit else 64,
                         collect_tags(lib),
                         collect_deps(lib),
                         collect_path_sorted_lib_idxs(lib.users_all),
                         collect_source_dir_paths(lib)])

        return (strs, mods)

    def main(self, args):
        generic_refs, graph, tagged_paths, vndk_lib_dirs = \
                self.create_from_args(args)

        module_info = ModuleInfo.load_from_path_or_default(args.module_info)

        # Compute vndk heuristics.
        vndk_lib = graph.compute_degenerated_vndk(
                generic_refs, tagged_paths, args.action_ineligible_vndk_sp,
                args.action_ineligible_vndk)

        # Serialize data.
        strs, mods = self.serialize_data(list(graph.all_libs()), vndk_lib,
                                         module_info)

        # Generate output files.
        makedirs(args.output, exist_ok=True)
        script_dir = os.path.dirname(os.path.abspath(__file__))
        for name in ('index.html', 'insight.css', 'insight.js'):
            shutil.copyfile(os.path.join(script_dir, 'assets', 'insight', name),
                            os.path.join(args.output, name))

        with open(os.path.join(args.output, 'insight-data.js'), 'w') as f:
            f.write('''(function () {
    var strs = ''' + json.dumps(strs) + ''';
    var mods = ''' + json.dumps(mods) + ''';
    insight.init(document, strs, mods);
})();''')

        return 0


class DepsCommand(ELFGraphCommand):
    def __init__(self):
        super(DepsCommand, self).__init__(
                'deps', help='Print binary dependencies for debugging')

    def add_argparser_options(self, parser):
        super(DepsCommand, self).add_argparser_options(parser)

        parser.add_argument(
                '--revert', action='store_true',
                help='print usage dependency')

        parser.add_argument(
                '--leaf', action='store_true',
                help='print binaries without dependencies or usages')

        parser.add_argument(
                '--symbols', action='store_true',
                help='print symbols')

        parser.add_argument('--module-info')

    def main(self, args):
        generic_refs, graph, tagged_paths, vndk_lib_dirs = \
                self.create_from_args(args)

        module_info = ModuleInfo.load_from_path_or_default(args.module_info)

        results = []
        for partition in range(NUM_PARTITIONS):
            for name, lib in graph.lib_pt[partition].items():
                if args.symbols:
                    def collect_symbols(user, definer):
                        return user.get_dep_linked_symbols(definer)
                else:
                    def collect_symbols(user, definer):
                        return ()

                data = []
                if args.revert:
                    for assoc_lib in sorted(lib.users_all):
                        data.append((assoc_lib.path,
                                     collect_symbols(assoc_lib, lib)))
                else:
                    for assoc_lib in sorted(lib.deps_all):
                        data.append((assoc_lib.path,
                                     collect_symbols(lib, assoc_lib)))
                results.append((name, data))
        results.sort()

        if args.leaf:
            for name, deps in results:
                if not deps:
                    print(name)
        else:
            delimiter = ''
            for name, assoc_libs in results:
                print(delimiter, end='')
                delimiter = '\n'

                print(name)
                for module_path in module_info.get_module_path(name):
                    print('\tMODULE_PATH:', module_path)
                for assoc_lib, symbols in assoc_libs:
                    print('\t' + assoc_lib)
                    for module_path in module_info.get_module_path(assoc_lib):
                        print('\t\tMODULE_PATH:', module_path)
                    for symbol in symbols:
                        print('\t\t' + symbol)
        return 0


class DepsClosureCommand(ELFGraphCommand):
    def __init__(self):
        super(DepsClosureCommand, self).__init__(
                'deps-closure', help='Find transitive closure of dependencies')

    def add_argparser_options(self, parser):
        super(DepsClosureCommand, self).add_argparser_options(parser)

        parser.add_argument('lib', nargs='*',
                            help='root set of the shared libraries')

        parser.add_argument('--exclude-lib', action='append', default=[],
                            help='libraries to be excluded')

        parser.add_argument('--exclude-ndk', action='store_true',
                            help='exclude ndk libraries')

        parser.add_argument('--revert', action='store_true',
                            help='print usage dependency')

        parser.add_argument('--enumerate', action='store_true',
                            help='print closure for each lib instead of union')

    def print_deps_closure(self, root_libs, graph, is_excluded_libs,
                           is_reverted, indent):
        if is_reverted:
            closure = graph.compute_users_closure(root_libs, is_excluded_libs)
        else:
            closure = graph.compute_deps_closure(root_libs, is_excluded_libs)

        for lib in sorted_lib_path_list(closure):
            print(indent + lib)


    def main(self, args):
        generic_refs, graph, tagged_paths, vndk_lib_dirs = \
                self.create_from_args(args)

        # Find root/excluded libraries by their paths.
        def report_error(path):
            print('error: no such lib: {}'.format(path), file=sys.stderr)
        root_libs = graph.get_libs(args.lib, report_error)
        excluded_libs = graph.get_libs(args.exclude_lib, report_error)

        # Define the exclusion filter.
        if args.exclude_ndk:
            def is_excluded_libs(lib):
                return lib.is_ll_ndk or lib in excluded_libs
        else:
            def is_excluded_libs(lib):
                return lib in excluded_libs

        if not args.enumerate:
            self.print_deps_closure(root_libs, graph, is_excluded_libs,
                                    args.revert, '')
        else:
            if not root_libs:
                root_libs = list(graph.all_libs())
            for lib in sorted(root_libs):
                print(lib.path)
                self.print_deps_closure({lib}, graph, is_excluded_libs,
                                        args.revert, '\t')
        return 0


class DepsUnresolvedCommand(ELFGraphCommand):
    def __init__(self):
        super(DepsUnresolvedCommand, self).__init__(
                'deps-unresolved',
                help='Show unresolved dt_needed entries or symbols')

    def add_argparser_options(self, parser):
        super(DepsUnresolvedCommand, self).add_argparser_options(parser)
        parser.add_argument('--module-info')
        parser.add_argument('--path-filter')

    def _dump_unresolved(self, lib, module_info, delimiter):
        if not lib.unresolved_symbols and not lib.unresolved_dt_needed:
            return

        print(delimiter, end='')
        print(lib.path)
        for module_path in module_info.get_module_path(lib.path):
            print('\tMODULE_PATH:', module_path)
        for dt_needed in sorted(lib.unresolved_dt_needed):
            print('\tUNRESOLVED_DT_NEEDED:', dt_needed)
        for symbol in sorted(lib.unresolved_symbols):
            print('\tUNRESOLVED_SYMBOL:', symbol)

    def main(self, args):
        generic_refs, graph, tagged_paths, vndk_lib_dirs = \
                self.create_from_args(args)
        module_info = ModuleInfo.load_from_path_or_default(args.module_info)

        libs = graph.all_libs()
        if args.path_filter:
            path_filter = re.compile(args.path_filter)
            libs = [lib for lib in libs if path_filter.match(lib.path)]

        delimiter = ''
        for lib in sorted(libs):
            self._dump_unresolved(lib, module_info, delimiter)
            delimiter = '\n'


class ApkDepsCommand(ELFGraphCommand):
    def __init__(self):
        super(ApkDepsCommand, self).__init__(
                'apk-deps', help='Print APK dependencies for debugging')

    def add_argparser_options(self, parser):
        super(ApkDepsCommand, self).add_argparser_options(parser)

    def main(self, args):
        generic_refs, graph, tagged_paths, vndk_lib_dirs = \
                self.create_from_args(args)

        apk_deps = scan_apk_dep(graph, args.system, args.vendor)

        for apk_path, dep_paths in apk_deps:
            print(apk_path)
            for dep_path in dep_paths:
                print('\t' + dep_path)

        return 0


class CheckDepCommandBase(ELFGraphCommand):
    def __init__(self, *args, **kwargs):
        super(CheckDepCommandBase, self).__init__(*args, **kwargs)
        self.delimiter = ''

    def add_argparser_options(self, parser):
        super(CheckDepCommandBase, self).add_argparser_options(parser)
        parser.add_argument('--module-info')

    def _print_delimiter(self):
        print(self.delimiter, end='')
        self.delimiter = '\n'

    def _dump_dep(self, lib, bad_deps, module_info):
        self._print_delimiter()
        print(lib.path)
        for module_path in module_info.get_module_path(lib.path):
            print('\tMODULE_PATH:', module_path)
        for dep in sorted(bad_deps):
            print('\t' + dep.path)
            for module_path in module_info.get_module_path(dep.path):
                print('\t\tMODULE_PATH:', module_path)
            for symbol in lib.get_dep_linked_symbols(dep):
                print('\t\t' + symbol)

    def _dump_apk_dep(self, apk_path, bad_deps, module_info):
        self._print_delimiter()
        print(apk_path)
        for module_path in module_info.get_module_path(apk_path):
            print('\tMODULE_PATH:', module_path)
        for dep_path in sorted(bad_deps):
            print('\t' + dep_path)
            for module_path in module_info.get_module_path(dep_path):
                print('\t\tMODULE_PATH:', module_path)


class CheckDepCommand(CheckDepCommandBase):
    def __init__(self):
        super(CheckDepCommand, self).__init__(
                'check-dep', help='Check the eligible dependencies')


    def add_argparser_options(self, parser):
        super(CheckDepCommand, self).add_argparser_options(parser)

        group = parser.add_mutually_exclusive_group()

        group.add_argument('--check-apk', action='store_true', default=False,
                           help='Check JNI dependencies in APK files')

        group.add_argument('--no-check-apk', action='store_false',
                           dest='check_apk',
                           help='Do not check JNI dependencies in APK files')

        group = parser.add_mutually_exclusive_group()

        group.add_argument('--check-dt-needed-ordering',
                           action='store_true', default=False,
                           help='Check ordering of DT_NEEDED entries')

        group.add_argument('--no-check-dt-needed-ordering',
                           action='store_false',
                           dest='check_dt_needed_ordering',
                           help='Do not check ordering of DT_NEEDED entries')


    def _check_vendor_dep(self, graph, tagged_libs, lib_properties,
                          module_info):
        """Check whether vendor libs are depending on non-eligible libs."""
        num_errors = 0

        vendor_libs = set(graph.lib_pt[PT_VENDOR].values())

        eligible_libs = (tagged_libs.ll_ndk | tagged_libs.vndk_sp |
                         tagged_libs.vndk_sp_indirect | tagged_libs.vndk)

        for lib in sorted(vendor_libs):
            bad_deps = set()

            # Check whether vendor modules depend on extended NDK symbols.
            for dep, symbols in lib.imported_ext_symbols.items():
                if dep.is_ll_ndk:
                    num_errors += 1
                    bad_deps.add(dep)
                    for symbol in symbols:
                        print('error: vendor lib "{}" depends on extended '
                              'NDK symbol "{}" from "{}".'
                              .format(lib.path, symbol, dep.path),
                              file=sys.stderr)

            # Check whether vendor modules depend on ineligible libs.
            for dep in lib.deps_all:
                if dep not in vendor_libs and dep not in eligible_libs:
                    num_errors += 1
                    bad_deps.add(dep)

                    dep_name = os.path.splitext(os.path.basename(dep.path))[0]
                    dep_properties = lib_properties.get(dep_name)
                    if not dep_properties.vendor_available:
                        print('error: vendor lib "{}" depends on non-eligible '
                              'lib "{}".'.format(lib.path, dep.path),
                              file=sys.stderr)
                    elif dep_properties.vndk_sp:
                        print('error: vendor lib "{}" depends on vndk-sp "{}" '
                              'but it must be copied to '
                              '/system/lib[64]/vndk-sp.'
                              .format(lib.path, dep.path),
                              file=sys.stderr)
                    elif dep_properties.vndk:
                        print('error: vendor lib "{}" depends on vndk "{}" but '
                              'it must be copied to /system/lib[64]/vndk.'
                              .format(lib.path, dep.path),
                              file=sys.stderr)
                    else:
                        print('error: vendor lib "{}" depends on '
                              'vendor_available "{}" but it must be copied to '
                              '/vendor/lib[64].'.format(lib.path, dep.path),
                              file=sys.stderr)

            if bad_deps:
                self._dump_dep(lib, bad_deps, module_info)

        return num_errors


    def _check_dt_needed_ordering(self, graph, module_info):
        """Check DT_NEEDED entries order of all libraries"""

        num_errors = 0

        def _is_libc_prior_to_libdl(lib):
            dt_needed = lib.elf.dt_needed
            try:
                return dt_needed.index('libc.so') < dt_needed.index('libdl.so')
            except ValueError:
                return True

        for lib in sorted(graph.all_libs()):
            if _is_libc_prior_to_libdl(lib):
                continue

            print('error: The ordering of DT_NEEDED entries in "{}" may be '
                  'problematic.  libc.so must be prior to libdl.so.  '
                  'But found: {}.'
                  .format(lib.path, lib.elf.dt_needed), file=sys.stderr)

            num_errors += 1

        return num_errors


    def _check_apk_dep(self, graph, system_dirs, vendor_dirs, module_info):
        num_errors = 0

        def is_in_system_partition(path):
            return path.startswith('/system/') or \
                   path.startswith('/product/') or \
                   path.startswith('/oem/')

        apk_deps = scan_apk_dep(graph, system_dirs, vendor_dirs)

        for apk_path, dep_paths in apk_deps:
            apk_in_system = is_in_system_partition(apk_path)
            bad_deps = []
            for dep_path in dep_paths:
                dep_in_system = is_in_system_partition(dep_path)
                if apk_in_system != dep_in_system:
                    bad_deps.append(dep_path)
                    print('error: apk "{}" has cross-partition dependency '
                          'lib "{}".'.format(apk_path, dep_path),
                          file=sys.stderr)
                    num_errors += 1
            if bad_deps:
                self._dump_apk_dep(apk_path, sorted(bad_deps), module_info)
        return num_errors


    def main(self, args):
        generic_refs, graph, tagged_paths, vndk_lib_dirs = \
                self.create_from_args(args)

        tagged_paths = TaggedPathDict.create_from_csv_path(
                args.tag_file, vndk_lib_dirs)
        tagged_libs = TaggedLibDict.create_from_graph(
                graph, tagged_paths, generic_refs)

        module_info = ModuleInfo.load_from_path_or_default(args.module_info)

        lib_properties_path = \
                LibProperties.get_lib_properties_file_path(args.tag_file)
        lib_properties = \
                LibProperties.load_from_path_or_default(lib_properties_path)

        num_errors = self._check_vendor_dep(graph, tagged_libs, lib_properties,
                                            module_info)

        if args.check_dt_needed_ordering:
            num_errors += self._check_dt_needed_ordering(graph, module_info)

        if args.check_apk:
            num_errors += self._check_apk_dep(graph, args.system, args.vendor,
                                              module_info)

        return 0 if num_errors == 0 else 1


class CheckEligibleListCommand(CheckDepCommandBase):
    def __init__(self):
        super(CheckEligibleListCommand, self).__init__(
                'check-eligible-list', help='Check the eligible list')


    def _check_eligible_vndk_dep(self, graph, tagged_libs, module_info):
        """Check whether eligible sets are self-contained."""
        num_errors = 0

        indirect_libs = (tagged_libs.ll_ndk_indirect |
                         tagged_libs.vndk_sp_indirect_private |
                         tagged_libs.fwk_only_rs)

        eligible_libs = (tagged_libs.ll_ndk | tagged_libs.vndk_sp |
                         tagged_libs.vndk_sp_indirect | tagged_libs.vndk)

        # Check eligible vndk is self-contained.
        for lib in sorted(eligible_libs):
            bad_deps = []
            for dep in lib.deps_all:
                if dep not in eligible_libs and dep not in indirect_libs:
                    print('error: eligible lib "{}" should not depend on '
                          'non-eligible lib "{}".'.format(lib.path, dep.path),
                          file=sys.stderr)
                    bad_deps.append(dep)
                    num_errors += 1
            if bad_deps:
                self._dump_dep(lib, bad_deps, module_info)

        # Check the libbinder dependencies.
        for lib in sorted(eligible_libs):
            bad_deps = []
            for dep in lib.deps_all:
                if os.path.basename(dep.path) == 'libbinder.so':
                    print('error: eligible lib "{}" should not depend on '
                          'libbinder.so.'.format(lib.path), file=sys.stderr)
                    bad_deps.append(dep)
                    num_errors += 1
            if bad_deps:
                self._dump_dep(lib, bad_deps, module_info)

        return num_errors


    def main(self, args):
        generic_refs, graph, tagged_paths, vndk_lib_dirs = \
                self.create_from_args(args)

        tagged_paths = TaggedPathDict.create_from_csv_path(
                args.tag_file, vndk_lib_dirs)
        tagged_libs = TaggedLibDict.create_from_graph(
                graph, tagged_paths, generic_refs)

        module_info = ModuleInfo.load_from_path_or_default(args.module_info)

        num_errors = self._check_eligible_vndk_dep(graph, tagged_libs,
                                                   module_info)
        return 0 if num_errors == 0 else 1


class DepGraphCommand(ELFGraphCommand):
    def __init__(self):
        super(DepGraphCommand, self).__init__(
                'dep-graph', help='Show the eligible dependencies graph')

    def add_argparser_options(self, parser):
        super(DepGraphCommand, self).add_argparser_options(parser)

        parser.add_argument('--output', '-o', help='output directory')

    def _get_tag_from_lib(self, lib, tagged_paths):
        tag_hierarchy = dict()
        for tag in TaggedPathDict.TAGS:
            if tag in {'sp_hal', 'sp_hal_dep', 'vnd_only'}:
                tag_hierarchy[tag] = 'vendor.private.{}'.format(tag)
            else:
                vendor_visible = TaggedPathDict.is_tag_visible('vnd_only', tag)
                pub = 'public' if vendor_visible else 'private'
                tag_hierarchy[tag] = 'system.{}.{}'.format(pub, tag)

        return tag_hierarchy[tagged_paths.get_path_tag(lib.path)]

    def _check_if_allowed(self, my_tag, other_tag):
        my = my_tag.split('.')
        other = other_tag.split('.')
        if my[0] == 'system' and other[0] == 'vendor':
            return False
        if my[0] == 'vendor' and other[0] == 'system' \
                             and other[1] == 'private':
            return False
        return True

    def _get_dep_graph(self, graph, tagged_paths):
        data = []
        violate_libs = dict()
        system_libs = graph.lib_pt[PT_SYSTEM].values()
        vendor_libs = graph.lib_pt[PT_VENDOR].values()
        for lib in itertools.chain(system_libs, vendor_libs):
            tag = self._get_tag_from_lib(lib, tagged_paths)
            violate_count = 0
            lib_item = {
                'name': lib.path,
                'tag': tag,
                'depends': [],
                'violates': [],
            }
            for dep in lib.deps_all:
                if self._check_if_allowed(tag,
                        self._get_tag_from_lib(dep, tagged_paths)):
                    lib_item['depends'].append(dep.path)
                else:
                    lib_item['violates'].append([dep.path, lib.get_dep_linked_symbols(dep)])
                    violate_count += 1;
            lib_item['violate_count'] = violate_count
            if violate_count > 0:
                if not tag in violate_libs:
                    violate_libs[tag] = []
                violate_libs[tag].append((lib.path, violate_count))
            data.append(lib_item)
        return data, violate_libs

    def main(self, args):
        generic_refs, graph, tagged_paths, vndk_lib_dirs = \
                self.create_from_args(args)

        tagged_paths = TaggedPathDict.create_from_csv_path(
                args.tag_file, vndk_lib_dirs)
        data, violate_libs = self._get_dep_graph(graph, tagged_paths)
        data.sort(key=lambda lib_item: (lib_item['tag'],
                                        lib_item['violate_count']))
        for libs in violate_libs.values():
            libs.sort(key=lambda libs: libs[1], reverse=True)

        makedirs(args.output, exist_ok=True)
        script_dir = os.path.dirname(os.path.abspath(__file__))
        for name in ('index.html', 'dep-graph.js', 'dep-graph.css'):
            shutil.copyfile(os.path.join(script_dir, 'assets', 'visual', name),
                            os.path.join(args.output, name))
        with open(os.path.join(args.output, 'dep-data.js'), 'w') as f:
            f.write('var violatedLibs = ' + json.dumps(violate_libs) +
                    '\nvar depData = ' + json.dumps(data) + ';')

        return 0


def main():
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest='subcmd')
    subcmds = dict()

    def register_subcmd(cmd):
        subcmds[cmd.name] = cmd
        cmd.add_argparser_options(
                subparsers.add_parser(cmd.name, help=cmd.help))

    register_subcmd(ELFDumpCommand())
    register_subcmd(CreateGenericRefCommand())
    register_subcmd(VNDKCommand())
    register_subcmd(DepsCommand())
    register_subcmd(DepsClosureCommand())
    register_subcmd(DepsInsightCommand())
    register_subcmd(DepsUnresolvedCommand())
    register_subcmd(ApkDepsCommand())
    register_subcmd(CheckDepCommand())
    register_subcmd(CheckEligibleListCommand())
    register_subcmd(DepGraphCommand())

    args = parser.parse_args()
    if not args.subcmd:
        parser.print_help()
        sys.exit(1)
    return subcmds[args.subcmd].main(args)

if __name__ == '__main__':
    sys.exit(main())
