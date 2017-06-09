#!/usr/bin/env python3

from __future__ import print_function

import argparse
import collections
import copy
import csv
import itertools
import json
import os
import re
import shutil
import stat
import struct
import sys


#------------------------------------------------------------------------------
# Python 2 and 3 Compatibility Layer
#------------------------------------------------------------------------------

if sys.version_info >= (3, 0):
    from os import makedirs
    from mmap import ACCESS_READ, mmap
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

    FileNotFoundError = OSError

try:
    from sys import intern
except ImportError:
    pass


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

    _ELF_MACHINE_IDS = {
        0: 'EM_NONE',
        3: 'EM_386',
        8: 'EM_MIPS',
        40: 'EM_ARM',
        62: 'EM_X86_64',
        183: 'EM_AARCH64',
    }


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
        return ELF._dict_find_key_by_value(ELF._ELF_MACHINE_IDS, name)


    __slots__ = ('ei_class', 'ei_data', 'e_machine', 'dt_rpath', 'dt_runpath',
                 'dt_needed', 'exported_symbols', 'imported_symbols',)


    def __init__(self, ei_class=ELFCLASSNONE, ei_data=ELFDATANONE, e_machine=0,
                 dt_rpath=None, dt_runpath=None, dt_needed=None,
                 exported_symbols=None, imported_symbols=None):
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
        return self._ELF_MACHINE_IDS.get(self.e_machine, str(self.e_machine))

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

        # ELF structure definitions.
        endian_fmt = '<' if self.ei_data == ELF.ELFDATA2LSB else '>'

        if self.is_32bit:
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


#------------------------------------------------------------------------------
# NDK and Banned Libraries
#------------------------------------------------------------------------------

class NDKLibDict(object):
    NOT_NDK = 0
    LL_NDK = 1
    SP_NDK = 2
    HL_NDK = 3

    LL_NDK_LIB_NAMES = (
        'libc.so',
        'libdl.so',
        'liblog.so',
        'libm.so',
        'libstdc++.so',
        'libvndksupport.so',
        'libandroid_net.so',
        'libz.so',
    )

    SP_NDK_LIB_NAMES = (
        'libEGL.so',
        'libGLESv1_CM.so',
        'libGLESv2.so',
        'libGLESv3.so',
        'libnativewindow.so',
        'libsync.so',
        'libvulkan.so',
    )

    HL_NDK_LIB_NAMES = (
        'libOpenMAXAL.so',
        'libOpenSLES.so',
        'libandroid.so',
        'libcamera2ndk.so',
        'libjnigraphics.so',
        'libmediandk.so',
    )

    @staticmethod
    def _create_pattern(names):
        return '|'.join('(?:^\\/system\\/lib(?:64)?\\/' + re.escape(i) + '$)'
                        for i in names)

    @staticmethod
    def _compile_path_matcher(names):
        return re.compile(NDKLibDict._create_pattern(names))

    @staticmethod
    def _compile_multi_path_matcher(name_lists):
        patt = '|'.join('(' + NDKLibDict._create_pattern(names) + ')'
                        for names in name_lists)
        return re.compile(patt)

    def __init__(self):
        self.ll_ndk_patterns = self._compile_path_matcher(self.LL_NDK_LIB_NAMES)
        self.sp_ndk_patterns = self._compile_path_matcher(self.SP_NDK_LIB_NAMES)
        self.hl_ndk_patterns = self._compile_path_matcher(self.HL_NDK_LIB_NAMES)
        self.ndk_patterns = self._compile_multi_path_matcher(
                (self.LL_NDK_LIB_NAMES, self.SP_NDK_LIB_NAMES,
                 self.HL_NDK_LIB_NAMES))

    def is_ll_ndk(self, path):
        return self.ll_ndk_patterns.match(path)

    def is_sp_ndk(self, path):
        return self.sp_ndk_patterns.match(path)

    def is_hl_ndk(self, path):
        return self.hl_ndk_patterns.match(path)

    def is_ndk(self, path):
        return self.ndk_patterns.match(path)

    def classify(self, path):
        match = self.ndk_patterns.match(path)
        if not match:
            return 0
        return match.lastindex

NDK_LIBS = NDKLibDict()


BannedLib = collections.namedtuple(
        'BannedLib', ('name', 'reason', 'action',))

BA_WARN = 0
BA_EXCLUDE = 1

class BannedLibDict(object):
    def __init__(self):
        self.banned_libs = dict()

    def add(self, name, reason, action):
        self.banned_libs[name] = BannedLib(name, reason, action)

    def get(self, name):
        return self.banned_libs.get(name)

    def is_banned(self, path):
        return self.get(os.path.basename(path))

    @staticmethod
    def create_default():
        d = BannedLibDict()
        d.add('libbinder.so', 'un-versioned IPC', BA_WARN)
        d.add('libselinux.so', 'policydb might be incompatible', BA_WARN)
        return d


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
        'sp_hal sp_hal_dep vndk_sp_hal sp_ndk sp_ndk_indirect '
        'vndk_sp_both')

def print_sp_lib(sp_lib, file=sys.stdout):
    # SP-NDK
    for lib in sorted_lib_path_list(sp_lib.sp_ndk):
        print('sp-ndk:', lib, file=file)
    for lib in sorted_lib_path_list(sp_lib.sp_ndk_indirect):
        print('sp-ndk-indirect:', lib, file=file)

    # SP-HAL
    for lib in sorted_lib_path_list(sp_lib.sp_hal):
        print('sp-hal:', lib, file=file)
    for lib in sorted_lib_path_list(sp_lib.sp_hal_dep):
        print('sp-hal-dep:', lib, file=file)
    for lib in sorted_lib_path_list(sp_lib.vndk_sp_hal):
        print('vndk-sp-hal:', lib, file=file)

    # SP-both
    for lib in sorted_lib_path_list(sp_lib.vndk_sp_both):
        print('vndk-sp-both:', lib, file=file)


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
    NEEDED = 0  # Dependencies recorded in DT_NEEDED entries.
    DLOPEN = 1  # Dependencies introduced by dlopen().

    def __init__(self, partition, path, elf):
        self.partition = partition
        self.path = path
        self.elf = elf
        self._deps = (set(), set())
        self._users = (set(), set())
        self.imported_ext_symbols = collections.defaultdict(set)
        self._ndk_classification = NDK_LIBS.classify(path)
        self.unresolved_symbols = set()
        self.linked_symbols = dict()

    @property
    def is_ndk(self):
        return self._ndk_classification != NDKLibDict.NOT_NDK

    @property
    def is_ll_ndk(self):
        return self._ndk_classification == NDKLibDict.LL_NDK

    @property
    def is_sp_ndk(self):
        return self._ndk_classification == NDKLibDict.SP_NDK

    @property
    def is_hl_ndk(self):
        return self._ndk_classification == NDKLibDict.HL_NDK

    def add_dep(self, dst, ty):
        self._deps[ty].add(dst)
        dst._users[ty].add(self)

    def remove_dep(self, dst, ty):
        self._deps[ty].remove(dst)
        dst._users[ty].remove(self)

    @property
    def num_deps(self):
        """Get the number of dependencies.  If a library is linked by both
        NEEDED and DLOPEN relationship, then it will be counted twice."""
        return sum(len(deps) for deps in self._deps)

    @property
    def deps(self):
        return itertools.chain.from_iterable(self._deps)

    @property
    def deps_with_type(self):
        dt_deps = zip(self._deps[self.NEEDED], itertools.repeat(self.NEEDED))
        dl_deps = zip(self._deps[self.DLOPEN], itertools.repeat(self.DLOPEN))
        return itertools.chain(dt_deps, dl_deps)

    @property
    def dt_deps(self):
        return self._deps[self.NEEDED]

    @property
    def dl_deps(self):
        return self._deps[self.DLOPEN]

    @property
    def num_users(self):
        """Get the number of users.  If a library is linked by both NEEDED and
        DLOPEN relationship, then it will be counted twice."""
        return sum(len(users) for users in self._users)

    @property
    def users(self):
        return itertools.chain.from_iterable(self._users)

    @property
    def users_with_type(self):
        dt_users = zip(self._users[self.NEEDED], itertools.repeat(self.NEEDED))
        dl_users = zip(self._users[self.DLOPEN], itertools.repeat(self.DLOPEN))
        return itertools.chain(dt_users, dl_users)

    @property
    def dt_users(self):
        return self._users[self.NEEDED]

    @property
    def dl_users(self):
        return self._users[self.DLOPEN]

    def has_dep(self, dst):
        return any(dst in deps for deps in self._deps)

    def has_user(self, dst):
        return any(dst in users for users in self._users)

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
        'll_ndk', 'll_ndk_indirect', 'sp_ndk', 'sp_ndk_indirect',
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
    def __init__(self):
        self.lib_pt = [ELFLibDict() for i in range(NUM_PARTITIONS)]

    def _add_lib_to_lookup_dict(self, lib):
        self.lib_pt[lib.partition].add(lib.path, lib)

    def _remove_lib_from_lookup_dict(self, lib):
        self.lib_pt[lib.partition].remove(lib)

    def add_lib(self, partition, path, elf):
        lib = ELFLinkData(partition, path, elf)
        self._add_lib_to_lookup_dict(lib)
        return lib

    def rename_lib(self, lib, new_partition, new_path):
        self._remove_lib_from_lookup_dict(lib)
        lib.path = new_path
        lib.partition = new_partition
        self._add_lib_to_lookup_dict(lib)

    def add_dep(self, src_path, dst_path, ty):
        for elf_class in (ELF.ELFCLASS32, ELF.ELFCLASS64):
            src = self.get_lib_in_elf_class(elf_class, src_path)
            dst = self.get_lib_in_elf_class(elf_class, dst_path)
            if src and dst:
                src.add_dep(dst, ty)
                return
        print('error: cannot add dependency from {} to {}.'
              .format(src_path, dst_path), file=sys.stderr)

    def get_lib_in_elf_class(self, elf_class, path, default=None):
        for partition in range(NUM_PARTITIONS):
            res = self.lib_pt[partition].get_lib_dict(elf_class).get(path)
            if res:
                return res
        return default

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
            short_path = os.path.join('/', partition_name, path[prefix_len:])
            if ignored_subdirs and ignored_patt.match(path):
                continue
            if alter_subdirs and alter_patt.match(path):
                self.add_lib(alter_partition, short_path, elf)
            else:
                self.add_lib(partition, short_path, elf)

    def load_extra_deps(self, path):
        patt = re.compile('([^:]*):\\s*(.*)')
        with open(path, 'r') as f:
            for line in f:
                match = patt.match(line)
                if match:
                    self.add_dep(match.group(1), match.group(2),
                                 ELFLinkData.DLOPEN)

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
                continue
            lib.add_dep(dep, ELFLinkData.NEEDED)
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

    SYSTEM_SEARCH_PATH = (
        '/system/${LIB}',
        '/vendor/${LIB}',
    )

    VENDOR_SEARCH_PATH = (
        '/vendor/${LIB}',
        '/vendor/${LIB}/vndk-sp',
        '/system/${LIB}/vndk-sp',
        '/system/${LIB}',  # For degenerated VNDK libs.
    )

    VNDK_SP_SEARCH_PATH = (
        '/vendor/${LIB}/vndk-sp',
        '/system/${LIB}/vndk-sp',
        '/vendor/${LIB}',  # To discover missing vndk-sp dependencies.
        '/system/${LIB}',  # To discover missing vndk-sp dependencies.
    )

    @staticmethod
    def _subst_search_path(search_path, elf_class):
        lib_dir_name = 'lib' if elf_class == ELF.ELFCLASS32 else 'lib64'
        return [path.replace('${LIB}', lib_dir_name) for path in search_path]

    @staticmethod
    def _is_in_vndk_sp_dir(path):
        return os.path.basename(os.path.dirname(path)).startswith('vndk-sp')

    def _resolve_elf_class_deps(self, elf_class, generic_refs):
        system_lib_dict = self.lib_pt[PT_SYSTEM].get_lib_dict(elf_class)
        vendor_lib_dict = self.lib_pt[PT_VENDOR].get_lib_dict(elf_class)
        lib_dict = self._compute_lib_dict(elf_class)

        # Resolve system libs.
        system_libs = [lib for lib in system_lib_dict.values()
                       if not self._is_in_vndk_sp_dir(lib.path)]
        search_path = self._subst_search_path(
                self.SYSTEM_SEARCH_PATH, elf_class)
        resolver = ELFResolver(lib_dict, search_path)
        self._resolve_lib_set_deps(system_libs, resolver, generic_refs)

        # Resolve vendor libs.
        vendor_libs = [lib for lib in vendor_lib_dict.values()
                       if not self._is_in_vndk_sp_dir(lib.path)]
        search_path = self._subst_search_path(
                self.VENDOR_SEARCH_PATH, elf_class)
        resolver = ELFResolver(lib_dict, search_path)
        self._resolve_lib_set_deps(vendor_libs, resolver, generic_refs)

        # Resolve vndk-sp libs
        vndk_sp = [lib for lib in lib_dict.values()
                   if self._is_in_vndk_sp_dir(lib.path)]
        search_path = self._subst_search_path(
                self.VNDK_SP_SEARCH_PATH, elf_class)
        resolver = ELFResolver(lib_dict, search_path)
        self._resolve_lib_set_deps(vndk_sp, resolver, generic_refs)

    def resolve_deps(self, generic_refs=None):
        self._resolve_elf_class_deps(ELF.ELFCLASS32, generic_refs)
        self._resolve_elf_class_deps(ELF.ELFCLASS64, generic_refs)

    def compute_path_matched_lib(self, path_patterns):
        patt = re.compile('|'.join('(?:' + p + ')' for p in path_patterns))
        return set(lib for lib in self.all_libs() if patt.match(lib.path))

    def compute_predefined_fwk_only_rs(self):
        """Find all fwk-only-rs libraries."""
        path_patterns = (
            '^/system/lib(?:64)?/(?:vndk-sp/)?libft2\\.so$',
            '^/system/lib(?:64)?/(?:vndk-sp/)?libmediandk\\.so',
        )
        return self.compute_path_matched_lib(path_patterns)

    def compute_predefined_vndk_sp(self):
        """Find all vndk-sp libraries."""
        path_patterns = (
            # Visible to SP-HALs
            '^.*/android\\.hardware\\.graphics\\.allocator@2\\.0\\.so$',
            '^.*/android\\.hardware\\.graphics\\.common@1\\.0\\.so$',
            '^.*/android\\.hardware\\.graphics\\.mapper@2\\.0\\.so$',
            '^.*/android\\.hardware\\.renderscript@1\\.0\\.so$',
            '^.*/libRSCpuRef\\.so$',
            '^.*/libRSDriver\\.so$',
            '^.*/libRS_internal\\.so$',
            '^.*/libbase\\.so$',
            '^.*/libbcinfo\\.so$',
            '^.*/libc\\+\\+\\.so$',
            '^.*/libcompiler_rt\\.so$',
            '^.*/libcutils\\.so$',
            '^.*/libhardware\\.so$',
            '^.*/libhidlbase\\.so$',
            '^.*/libhidltransport\\.so$',
            '^.*/libhwbinder\\.so$',
            '^.*/libutils\\.so$',

            # Only for o-release
            '^.*/android\\.hidl\\.base@1\\.0\\.so$',
        )
        return self.compute_path_matched_lib(path_patterns)

    def compute_predefined_vndk_sp_indirect(self):
        """Find all vndk-sp-indirect libraries."""
        path_patterns = (
            # Invisible to SP-HALs
            '^.*/libbacktrace\\.so$',
            '^.*/libblas\\.so$',
            '^.*/liblzma\\.so$',
            '^.*/libpng\\.so$',
            '^.*/libunwind\\.so$',
        )
        return self.compute_path_matched_lib(path_patterns)

    def compute_predefined_sp_hal(self):
        """Find all same-process HALs."""
        path_patterns = (
            # OpenGL-related
            '^/vendor/.*/libEGL_.*\\.so$',
            '^/vendor/.*/libGLES_.*\\.so$',
            '^/vendor/.*/libGLESv1_CM_.*\\.so$',
            '^/vendor/.*/libGLESv2_.*\\.so$',
            '^/vendor/.*/libGLESv3_.*\\.so$',
            # Vulkan
            '^/vendor/.*/vulkan.*\\.so$',
            # libRSDriver
            '^.*/android\\.hardware\\.renderscript@1\\.0-impl\\.so$',
            '^/vendor/.*/libPVRRS\\.so$',
            '^/vendor/.*/libRSDriver.*\\.so$',
            # Gralloc mapper
            '^.*/gralloc\\..*\\.so$',
            '^.*/android\\.hardware\\.graphics\\.mapper@\\d+\\.\\d+-impl\\.so$',
        )
        return self.compute_path_matched_lib(path_patterns)

    def compute_sp_ndk(self):
        """Find all SP-NDK libraries."""
        return set(lib for lib in self.all_libs() if lib.is_sp_ndk)

    def compute_sp_lib(self, generic_refs):
        def is_ndk(lib):
            return lib.is_ndk

        sp_ndk = self.compute_sp_ndk()
        sp_ndk_closure = self.compute_closure(sp_ndk, is_ndk)
        sp_ndk_indirect = sp_ndk_closure - sp_ndk

        sp_hal = self.compute_predefined_sp_hal()
        sp_hal_closure = self.compute_closure(sp_hal, is_ndk)

        def is_aosp_lib(lib):
            return (not generic_refs or \
                    generic_refs.classify_lib(lib) != GenericRefs.NEW_LIB)

        vndk_sp_hal = set()
        sp_hal_dep = set()
        for lib in sp_hal_closure - sp_hal:
            if is_aosp_lib(lib):
                vndk_sp_hal.add(lib)
            else:
                sp_hal_dep.add(lib)

        vndk_sp_both = sp_ndk_indirect & vndk_sp_hal
        sp_ndk_indirect -= vndk_sp_both
        vndk_sp_hal -= vndk_sp_both

        return SPLibResult(sp_hal, sp_hal_dep, vndk_sp_hal, sp_ndk,
                           sp_ndk_indirect, vndk_sp_both)

    def _po_sorted(self, lib_set, get_successors):
        result = []
        visited = set()
        def traverse(lib):
            for succ in get_successors(lib):
                if succ in lib_set and succ not in visited:
                    visited.add(succ)
                    traverse(succ)
            result.append(lib)
        for lib in lib_set:
            if lib not in visited:
                visited.add(lib)
                traverse(lib)
        return result

    def _deps_po_sorted(self, lib_set):
        return self._po_sorted(lib_set, lambda x: x.deps)

    def _users_po_sorted(self, lib_set):
        return self._po_sorted(lib_set, lambda x: x.users)

    def normalize_partition_tags(self, sp_hals, generic_refs):
        system_libs = set(self.lib_pt[PT_SYSTEM].values())
        system_libs_po = self._deps_po_sorted(system_libs)

        def is_system_lib_or_sp_hal(lib):
            return lib.is_system_lib() or lib in sp_hals

        for lib in system_libs_po:
            if all(is_system_lib_or_sp_hal(dep) for dep in lib.deps):
                # Good system lib.  Do nothing.
                continue
            if not generic_refs or generic_refs.refs.get(lib.path):
                # If lib is in AOSP generic reference, then we assume that the
                # non-SP-HAL dependencies are errors.  Emit errors and remove
                # the dependencies.
                for dep in list(lib.dt_deps):
                    if not is_system_lib_or_sp_hal(dep):
                        print('error: {}: system exe/lib must not depend on '
                              'vendor lib {}.  Assume such dependency does '
                              'not exist.'.format(lib.path, dep.path),
                              file=sys.stderr)
                        lib.remove_dep(dep, ELFLinkData.NEEDED)
                for dep in list(lib.dl_deps):
                    if not is_system_lib_or_sp_hal(dep):
                        print('error: {}: system exe/lib must not dlopen() '
                              'vendor lib {}.  Assume such dependency does '
                              'not exist.'.format(lib.path, dep.path),
                              file=sys.stderr)
                        lib.remove_dep(dep, ELFLinkData.DLOPEN)
            else:
                # If lib is not in AOSP generic reference, then we assume that
                # lib must be moved to vendor partition.
                for dep in lib.deps:
                    if not is_system_lib_or_sp_hal(dep):
                        print('warning: {}: system exe/lib must not depend on '
                              'vendor lib {}.  Assuming {} should be placed in '
                              'vendor partition.'
                              .format(lib.path, dep.path, lib.path),
                              file=sys.stderr)
                new_path = lib.path.replace('/system/', '/vendor/')
                self.rename_lib(lib, PT_VENDOR, new_path)

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

    def compute_degenerated_vndk(self, sp_lib, generic_refs,
                                 tagged_paths=None,
                                 action_ineligible_vndk_sp='warn',
                                 action_ineligible_vndk='warn'):
        # Find LL-NDK and SP-NDK libs.
        ll_ndk = set(lib for lib in self.all_libs() if lib.is_ll_ndk)
        sp_ndk = set(lib for lib in self.all_libs() if lib.is_sp_ndk)

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
            if lib.is_ll_ndk or lib.is_sp_ndk or lib in sp_hal:
                return True
            return is_aosp_lib(lib)

        sp_hal_dep = self.compute_closure(sp_hal, is_not_sp_hal_dep)
        sp_hal_dep -= sp_hal

        # Find FWK-ONLY-RS libs.
        fwk_only_rs = self.compute_predefined_fwk_only_rs()

        # Find VNDK-SP libs.
        def is_not_vndk_sp(lib):
            return lib.is_ll_ndk or lib.is_sp_ndk or lib in sp_hal or \
                   lib in sp_hal_dep

        follow_ineligible_vndk_sp, warn_ineligible_vndk_sp = \
                self._parse_action_on_ineligible_lib(action_ineligible_vndk_sp)
        predefined_vndk_sp = self.compute_predefined_vndk_sp()
        vndk_sp = set()
        for lib in itertools.chain(sp_hal, sp_hal_dep):
            for dep in lib.deps:
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
            return lib.is_ll_ndk or lib.is_sp_ndk or lib in vndk_sp or \
                   lib in fwk_only_rs

        vndk_sp_indirect = self.compute_closure(
                vndk_sp, is_not_vndk_sp_indirect)
        vndk_sp_indirect -= vndk_sp

        # Find unused predefined VNDK-SP libs.
        vndk_sp_unused = set(lib for lib in predefined_vndk_sp
                             if self._is_in_vndk_sp_dir(lib.path))
        vndk_sp_unused -= vndk_sp
        vndk_sp_unused -= vndk_sp_indirect

        # Find dependencies of unused predefined VNDK-SP libs.
        def is_not_vndk_sp_indirect_unused(lib):
            return is_not_vndk_sp_indirect(lib) or lib in vndk_sp_indirect
        vndk_sp_indirect_unused = self.compute_closure(
                vndk_sp_unused, is_not_vndk_sp_indirect_unused)
        vndk_sp_indirect_unused -= vndk_sp_unused

        # TODO: Compute VNDK-SP-Indirect-Private.
        vndk_sp_indirect_private = set()

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

            closure = self.compute_closure({lib}, is_not_vndk_sp_indirect)
            closure -= vndk_sp
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
        predefined_vndk_sp_indirect = self.compute_predefined_vndk_sp_indirect()
        vndk_sp_indirect_ext = set()
        def collect_vndk_sp_indirect_ext(libs):
            result = set()
            for lib in libs:
                exts = set(lib.imported_ext_symbols.keys())
                for dep in lib.deps:
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
            return lib.is_ll_ndk or lib.is_sp_ndk or lib in vndk_sp or \
                   lib in fwk_only_rs

        candidates = collect_vndk_sp_indirect_ext(vndk_sp_ext)
        while candidates:
            vndk_sp_indirect_ext |= candidates
            candidates = collect_vndk_sp_indirect_ext(candidates)

        # Find VNDK libs (a.k.a. system shared libs directly used by vendor
        # partition.)
        def is_not_vndk(lib):
            if lib.is_ll_ndk or lib.is_sp_ndk or is_vndk_sp_public(lib) or \
               lib in fwk_only_rs:
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
                for dep in lib.deps:
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

        vndk_indirect = self.compute_closure(vndk, is_not_vndk)
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

        # Compute LL-NDK-Indirect and SP-NDK-Indirect.
        def is_not_ll_ndk_indirect(lib):
            return lib.is_ll_ndk or is_vndk_sp(lib) or is_vndk(lib)

        ll_ndk_indirect = self.compute_closure(ll_ndk, is_not_ll_ndk_indirect)
        ll_ndk_indirect -= ll_ndk

        def is_not_sp_ndk_indirect(lib):
            return lib.is_ll_ndk or lib.is_sp_ndk or lib in ll_ndk_indirect or \
                   is_vndk_sp(lib) or is_vndk(lib)

        sp_ndk_indirect = self.compute_closure(sp_ndk, is_not_sp_ndk_indirect)
        sp_ndk_indirect -= sp_ndk

        # Return the VNDK classifications.
        return VNDKResult(
                ll_ndk=ll_ndk,
                ll_ndk_indirect=ll_ndk_indirect,
                sp_ndk=sp_ndk,
                sp_ndk_indirect=sp_ndk_indirect,
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

    def compute_vndk_cap(self, banned_libs):
        # ELF files on vendor partitions are banned unconditionally.  ELF files
        # on the system partition are banned if their file extensions are not
        # '.so' or their file names are listed in banned_libs.  LL-NDK and
        # SP-NDK libraries are treated as a special case which will not be
        # considered as banned libraries at the moment.
        def is_banned(lib):
            if lib.is_ndk:
                return lib.is_hl_ndk
            return (banned_libs.is_banned(lib.path) or
                    not lib.is_system_lib() or
                    not lib.path.endswith('.so'))

        # Find all libraries that are banned.
        banned_set = set()
        for lib_set in self.lib_pt:
            for lib in lib_set.values():
                if is_banned(lib):
                    banned_set.add(lib)

        # Find the transitive closure of the banned libraries.
        stack = list(banned_set)
        while stack:
            lib = stack.pop()
            for user in lib.users:
                if not user.is_ndk and user not in banned_set:
                    banned_set.add(user)
                    stack.append(user)

        # Find the non-NDK non-banned libraries.
        vndk_cap = set()
        for lib in self.lib_pt[PT_SYSTEM].values():
            if not lib.is_ndk and lib not in banned_set:
                vndk_cap.add(lib)

        return vndk_cap

    @staticmethod
    def compute_closure(root_set, is_excluded):
        closure = set(root_set)
        stack = list(root_set)
        while stack:
            lib = stack.pop()
            for dep in lib.deps:
                if is_excluded(dep):
                    continue
                if dep not in closure:
                    closure.add(dep)
                    stack.append(dep)
        return closure

    @staticmethod
    def _create_internal(scan_elf_files, system_dirs, system_dirs_as_vendor,
                         system_dirs_ignored, vendor_dirs,
                         vendor_dirs_as_system, vendor_dirs_ignored,
                         extra_deps, generic_refs):
        graph = ELFLinker()

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
                graph.load_extra_deps(path)

        graph.resolve_deps(generic_refs)

        return graph

    @staticmethod
    def create(system_dirs=None, system_dirs_as_vendor=None,
               system_dirs_ignored=None, vendor_dirs=None,
               vendor_dirs_as_system=None, vendor_dirs_ignored=None,
               extra_deps=None, generic_refs=None):
        return ELFLinker._create_internal(
                scan_elf_files, system_dirs, system_dirs_as_vendor,
                system_dirs_ignored, vendor_dirs, vendor_dirs_as_system,
                vendor_dirs_ignored, extra_deps, generic_refs)

    @staticmethod
    def create_from_dump(system_dirs=None, system_dirs_as_vendor=None,
                         vendor_dirs=None, vendor_dirs_as_system=None,
                         extra_deps=None, generic_refs=None):
        return ELFLinker._create_internal(
                scan_elf_dump_files, system_dirs, system_dirs_as_vendor,
                vendor_dirs, vendor_dirs_as_system, extra_deps, generic_refs)


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

        graph = ELFLinker.create(args.system, args.system_dir_as_vendor,
                                 args.system_dir_ignored,
                                 args.vendor, args.vendor_dir_as_system,
                                 args.vendor_dir_ignored,
                                 args.load_extra_deps,
                                 generic_refs=generic_refs)

        return (generic_refs, graph)


class VNDKCommandBase(ELFGraphCommand):
    def add_argparser_options(self, parser):
        super(VNDKCommandBase, self).add_argparser_options(parser)

        parser.add_argument('--no-default-dlopen-deps', action='store_true',
                help='do not add default dlopen dependencies')

    def create_from_args(self, args):
        """Create all essential data structures for VNDK computation."""

        generic_refs, graph = \
                super(VNDKCommandBase, self).create_from_args(args)

        if not args.no_default_dlopen_deps:
            script_dir = os.path.dirname(os.path.abspath(__file__))
            minimum_dlopen_deps = os.path.join(script_dir, 'datasets',
                                               'minimum_dlopen_deps.txt')
            graph.load_extra_deps(minimum_dlopen_deps)

        return (generic_refs, graph)


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

        parser.add_argument('--tag-file', help='lib tag file')

        parser.add_argument(
                '--action-ineligible-vndk-sp', default='warn',
                help='action when a sp-hal uses non-vndk-sp libs '
                     '(option: follow,warn,ignore)')

        parser.add_argument(
                '--action-ineligible-vndk', default='warn',
                help='action when a vendor lib/exe uses fwk-only libs '
                     '(option: follow,warn,ignore)')

        parser.add_argument(
                '--output-format', default='tag',
                help='output format for vndk classification')

    def _warn_incorrect_partition_lib_set(self, lib_set, partition, error_msg):
        for lib in lib_set.values():
            if not lib.num_users:
                continue
            if all((user.partition != partition for user in lib.users)):
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

    def _check_ndk_extensions(self, graph, generic_refs):
        for lib_set in graph.lib_pt:
            for lib in lib_set.values():
                if lib.is_ndk and not generic_refs.is_equivalent_lib(lib):
                    print('warning: {}: NDK library should not be extended.'
                            .format(lib.path), file=sys.stderr)

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

    def main(self, args):
        generic_refs, graph = self.create_from_args(args)

        # Check the API extensions to NDK libraries.
        if generic_refs:
            self._check_ndk_extensions(graph, generic_refs)

        if args.warn_incorrect_partition:
            self._warn_incorrect_partition(graph)

        if args.tag_file:
            tagged_paths = TaggedPathDict.create_from_csv_path(args.tag_file)
        else:
            tagged_paths = None

        # Compute vndk heuristics.
        sp_lib = graph.compute_sp_lib(generic_refs)
        vndk_lib = graph.compute_degenerated_vndk(
                sp_lib, generic_refs, tagged_paths,
                args.action_ineligible_vndk_sp, args.action_ineligible_vndk)

        # Print results.
        if args.output_format == 'make':
            self._print_make(vndk_lib)
        else:
            self._print_tags(vndk_lib, args.full)

        return 0


class DepsInsightCommand(VNDKCommandBase):
    def __init__(self):
        super(DepsInsightCommand, self).__init__(
                'deps-insight', help='Generate HTML to show dependencies')

    def add_argparser_options(self, parser):
        super(DepsInsightCommand, self).add_argparser_options(parser)

        parser.add_argument(
                '--output', '-o', help='output directory')

    def main(self, args):
        generic_refs, graph = self.create_from_args(args)

        # Compute vndk heuristics.
        sp_lib = graph.compute_sp_lib(generic_refs)
        vndk_lib = graph.compute_degenerated_vndk(sp_lib, generic_refs)

        # Serialize data.
        strs = []
        strs_dict = dict()

        libs = list(graph.all_libs())
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
            queue = list(lib.deps)
            visited = set(queue)
            visited.add(lib)
            deps = []

            # Traverse dependencies with breadth-first search.
            while queue:
                # Collect dependencies for next queue.
                next_queue = []
                for lib in queue:
                    for dep in lib.deps:
                        if dep not in visited:
                            next_queue.append(dep)
                            visited.add(dep)

                # Append current queue to result.
                deps.append(collect_path_sorted_lib_idxs(queue))

                queue = next_queue

            return deps

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
                         collect_path_sorted_lib_idxs(lib.users)])

        # Generate output files.
        makedirs(args.output, exist_ok=True)
        script_dir = os.path.dirname(os.path.abspath(__file__))
        for name in ('index.html', 'insight.css', 'insight.js'):
            shutil.copyfile(os.path.join(script_dir, 'assets', name),
                            os.path.join(args.output, name))

        with open(os.path.join(args.output, 'insight-data.js'), 'w') as f:
            f.write('''(function () {
    var strs = ''' + json.dumps(strs) + ''';
    var mods = ''' + json.dumps(mods) + ''';
    insight.init(document, strs, mods);
})();''')

        return 0


class VNDKCapCommand(ELFGraphCommand):
    def __init__(self):
        super(VNDKCapCommand, self).__init__(
                'vndk-cap', help='Compute VNDK set upper bound')

    def add_argparser_options(self, parser):
        super(VNDKCapCommand, self).add_argparser_options(parser)

    def main(self, args):
        generic_refs, graph = self.create_from_args(args)

        banned_libs = BannedLibDict.create_default()

        vndk_cap = graph.compute_vndk_cap(banned_libs)

        for lib in sorted_lib_path_list(vndk_cap):
            print(lib)


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

    def main(self, args):
        generic_refs, graph = self.create_from_args(args)

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
                    for assoc_lib in sorted(lib.users):
                        data.append((assoc_lib.path,
                                     collect_symbols(assoc_lib, lib)))
                else:
                    for assoc_lib in sorted(lib.deps):
                        data.append((assoc_lib.path,
                                     collect_symbols(lib, assoc_lib)))
                results.append((name, data))
        results.sort()

        if args.leaf:
            for name, deps in results:
                if not deps:
                    print(name)
        else:
            for name, assoc_libs in results:
                print(name)
                for assoc_lib, symbols in assoc_libs:
                    print('\t' + assoc_lib)
                    for symbol in symbols:
                        print('\t\t' + symbol)
        return 0


class DepsClosureCommand(ELFGraphCommand):
    def __init__(self):
        super(DepsClosureCommand, self).__init__(
                'deps-closure', help='Find transitive closure of dependencies')

    def add_argparser_options(self, parser):
        super(DepsClosureCommand, self).add_argparser_options(parser)

        parser.add_argument('lib', nargs='+',
                            help='root set of the shared libraries')

        parser.add_argument('--exclude-lib', action='append', default=[],
                            help='libraries to be excluded')

        parser.add_argument('--exclude-ndk', action='store_true',
                            help='exclude ndk libraries')

    def main(self, args):
        generic_refs, graph = self.create_from_args(args)

        # Find root/excluded libraries by their paths.
        def report_error(path):
            print('error: no such lib: {}'.format(path), file=sys.stderr)
        root_libs = graph.get_libs(args.lib, report_error)
        excluded_libs = graph.get_libs(args.exclude_lib, report_error)

        # Compute and print the closure.
        if args.exclude_ndk:
            def is_excluded_libs(lib):
                return lib.is_ndk or lib in excluded_libs
        else:
            def is_excluded_libs(lib):
                return lib in excluded_libs

        closure = graph.compute_closure(root_libs, is_excluded_libs)
        for lib in sorted_lib_path_list(closure):
            print(lib)
        return 0


class TaggedDict(object):
    TAGS = {
        'll_ndk', 'll_ndk_indirect', 'sp_ndk', 'sp_ndk_indirect',
        'vndk_sp', 'vndk_sp_indirect', 'vndk_sp_indirect_private',
        'vndk',
        'fwk_only', 'fwk_only_rs',
        'sp_hal', 'sp_hal_dep',
        'vnd_only',
        'remove',
    }

    _TAG_ALIASES = {
        'hl_ndk': 'fwk_only',  # Treat HL-NDK as FWK-ONLY.
        'vndk_indirect': 'vndk',  # Legacy
        'vndk_sp_hal': 'vndk_sp',  # Legacy
        'vndk_sp_both': 'vndk_sp',  # Legacy
    }

    @classmethod
    def _normalize_tag(cls, tag):
        tag = tag.lower().replace('-', '_')
        tag = cls._TAG_ALIASES.get(tag, tag)
        if tag not in cls.TAGS:
            raise ValueError('unknown lib tag ' + tag)
        return tag

    _LL_NDK_VIS = {'ll_ndk', 'll_ndk_indirect'}
    _SP_NDK_VIS = {'ll_ndk', 'll_ndk_indirect', 'sp_ndk', 'sp_ndk_indirect'}
    _VNDK_SP_VIS = {'ll_ndk', 'sp_ndk', 'vndk_sp', 'vndk_sp_indirect',
                    'vndk_sp_indirect_private', 'fwk_only_rs'}
    _FWK_ONLY_VIS = {'ll_ndk', 'll_ndk_indirect', 'sp_ndk', 'sp_ndk_indirect',
                     'vndk_sp', 'vndk_sp_indirect', 'vndk_sp_indirect_private',
                     'vndk', 'fwk_only', 'fwk_only_rs', 'sp_hal'}
    _SP_HAL_VIS = {'ll_ndk', 'sp_ndk', 'vndk_sp', 'sp_hal', 'sp_hal_dep'}

    _TAG_VISIBILITY = {
        'll_ndk': _LL_NDK_VIS,
        'll_ndk_indirect': _LL_NDK_VIS,
        'sp_ndk': _SP_NDK_VIS,
        'sp_ndk_indirect': _SP_NDK_VIS,

        'vndk_sp': _VNDK_SP_VIS,
        'vndk_sp_indirect': _VNDK_SP_VIS,
        'vndk_sp_indirect_private': _VNDK_SP_VIS,

        'vndk': {'ll_ndk', 'sp_ndk', 'vndk_sp', 'vndk_sp_indirect', 'vndk'},

        'fwk_only': _FWK_ONLY_VIS,
        'fwk_only_rs': _FWK_ONLY_VIS,

        'sp_hal': _SP_HAL_VIS,
        'sp_hal_dep': _SP_HAL_VIS,

        'vnd_only': {'ll_ndk', 'sp_ndk', 'vndk_sp', 'vndk_sp_indirect',
                     'vndk', 'sp_hal', 'sp_hal_dep', 'vnd_only'},

        'remove': set(),
    }

    del _LL_NDK_VIS, _SP_NDK_VIS, _VNDK_SP_VIS, _FWK_ONLY_VIS, _SP_HAL_VIS

    @classmethod
    def is_tag_visible(cls, from_tag, to_tag):
        return to_tag in cls._TAG_VISIBILITY[from_tag]

    def __init__(self):
        self._path_tag = dict()
        for tag in self.TAGS:
            setattr(self, tag, set())

    def add(self, tag, lib):
        lib_set = getattr(self, tag)
        lib_set.add(lib)
        self._path_tag[lib] = tag

    def get_path_tag(self, lib):
        try:
            return self._path_tag[lib]
        except KeyError:
            return self.get_path_tag_default(lib)

    def get_path_tag_default(self, lib):
        raise NotImplementedError()

    def is_path_visible(self, from_lib, to_lib):
        return self.is_tag_visible(self.get_path_tag(from_lib),
                                   self.get_path_tag(to_lib))


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
    def create_from_csv(fp):
        d = TaggedPathDict()
        d.load_from_csv(fp)
        return d

    @staticmethod
    def create_from_csv_path(path):
        with open(path, 'r') as fp:
            return TaggedPathDict.create_from_csv(fp)

    @staticmethod
    def _enumerate_paths(pattern):
        if '${LIB}' in pattern:
            yield pattern.replace('${LIB}', 'lib')
            yield pattern.replace('${LIB}', 'lib64')
        else:
            yield pattern

    def add(self, tag, path):
        for path in self._enumerate_paths(path):
            super(TaggedPathDict, self).add(tag, path)

    def get_path_tag_default(self, path):
        return 'vnd_only' if path.startswith('/vendor') else 'fwk_only'


class TaggedLibDict(TaggedDict):
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

    def get_path_tag_default(self, lib):
        return 'vnd_only' if lib.path.startswith('/vendor') else 'fwk_only'


class ModuleInfo(object):
    def __init__(self, module_info_path=None):
        if not module_info_path:
            self.json = dict()
        else:
            with open(module_info_path, 'r') as f:
                self.json = json.load(f)

    def get_module_path(self, installed_path):
        for name, module in  self.json.items():
            if any(path.endswith(installed_path)
                   for path in module['installed']):
                return module['path']
        return []


class CheckDepCommand(ELFGraphCommand):
    def __init__(self):
        super(CheckDepCommand, self).__init__(
                'check-dep', help='Check the eligible dependencies')

    def add_argparser_options(self, parser):
        super(CheckDepCommand, self).add_argparser_options(parser)

        parser.add_argument('--tag-file', required=True)

        parser.add_argument('--module-info')

    @staticmethod
    def _dump_dep(lib, bad_deps, module_info):
        print(lib.path)
        for module_path in sorted(module_info.get_module_path(lib.path)):
            print('\tMODULE_PATH:', module_path)
        for dep in sorted(bad_deps):
            print('\t' + dep.path)
            for symbol in lib.get_dep_linked_symbols(dep):
                print('\t\t' + symbol)

    def _check_eligible_vndk_dep(self, graph, tagged_libs, module_info):
        """Check whether eligible sets are self-contained."""
        num_errors = 0

        indirect_libs = (tagged_libs.ll_ndk_indirect | \
                         tagged_libs.sp_ndk_indirect | \
                         tagged_libs.vndk_sp_indirect_private | \
                         tagged_libs.fwk_only_rs)

        eligible_libs = (tagged_libs.ll_ndk | tagged_libs.sp_ndk | \
                         tagged_libs.vndk_sp | tagged_libs.vndk_sp_indirect | \
                         tagged_libs.vndk)

        # Check eligible vndk is self-contained.
        for lib in sorted(eligible_libs):
            bad_deps = []
            for dep in lib.deps:
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
            for dep in lib.deps:
                if os.path.basename(dep.path) == 'libbinder.so':
                    print('error: eligible lib "{}" should not depend on '
                          'libbinder.so.'.format(lib.path), file=sys.stderr)
                    bad_deps.append(dep)
                    num_errors += 1
            if bad_deps:
                self._dump_dep(lib, bad_deps, module_info)

        return num_errors

    def _check_vendor_dep(self, graph, tagged_libs, module_info):
        """Check whether vendor libs are depending on non-eligible libs."""
        num_errors = 0

        vendor_libs = graph.lib_pt[PT_VENDOR].values()

        eligible_libs = (tagged_libs.ll_ndk | tagged_libs.sp_ndk | \
                         tagged_libs.vndk_sp | tagged_libs.vndk_sp_indirect | \
                         tagged_libs.vndk)

        for lib in sorted(vendor_libs):
            bad_deps = []
            for dep in lib.deps:
                if dep not in vendor_libs and dep not in eligible_libs:
                    print('error: vendor lib "{}" depends on non-eligible '
                          'lib "{}".'.format(lib.path, dep.path),
                          file=sys.stderr)
                    bad_deps.append(dep)
                    num_errors += 1
            if bad_deps:
                self._dump_dep(lib, bad_deps, module_info)

        return num_errors

    def main(self, args):
        generic_refs, graph = self.create_from_args(args)

        tagged_paths = TaggedPathDict.create_from_csv_path(args.tag_file)
        tagged_libs = TaggedLibDict.create_from_graph(graph, tagged_paths)

        module_info = ModuleInfo(args.module_info)

        num_errors = self._check_eligible_vndk_dep(graph, tagged_libs,
                                                   module_info)
        num_errors += self._check_vendor_dep(graph, tagged_libs, module_info)

        return 0 if num_errors == 0 else 1


class DepGraphCommand(ELFGraphCommand):
    def __init__(self):
        super(DepGraphCommand, self).__init__(
                'dep-graph', help='Show the eligible dependencies graph')

    def add_argparser_options(self, parser):
        super(DepGraphCommand, self).add_argparser_options(parser)

        parser.add_argument('--tag-file', required=True)
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
            for dep in lib.deps:
                if self._check_if_allowed(tag,
                        self._get_tag_from_lib(dep, tagged_paths)):
                    lib_item['depends'].append(dep.path)
                else:
                    lib_item['violates'].append(dep.path)
                    violate_count += 1;
            lib_item['violate_count'] = violate_count
            if violate_count > 0:
                if not tag in violate_libs:
                    violate_libs[tag] = []
                violate_libs[tag].append((lib.path, violate_count))
            data.append(lib_item)
        return data, violate_libs

    def main(self, args):
        generic_refs, graph = self.create_from_args(args)

        tagged_paths = TaggedPathDict.create_from_csv_path(args.tag_file)
        data, violate_libs = self._get_dep_graph(graph, tagged_paths)
        data.sort(key=lambda lib_item: (lib_item['tag'],
                                        lib_item['violate_count']))
        for libs in violate_libs.values():
            libs.sort(key=lambda libs: libs[1], reverse=True)

        makedirs(args.output, exist_ok=True)
        script_dir = os.path.dirname(os.path.abspath(__file__))
        for name in ('index.html', 'dep-graph.js', 'dep-graph.css'):
            shutil.copyfile(os.path.join(script_dir, 'assets/visual', name),
                            os.path.join(args.output, name))
        with open(os.path.join(args.output, 'dep-data.js'), 'w') as f:
            f.write('var violatedLibs = ' + json.dumps(violate_libs) +
                    '\nvar depData = ' + json.dumps(data) + ';')

        return 0


class VNDKSPCommand(ELFGraphCommand):
    def __init__(self):
        super(VNDKSPCommand, self).__init__(
                'vndk-sp', help='List pre-defined VNDK-SP')

    def add_argparser_options(self, parser):
        super(VNDKSPCommand, self).add_argparser_options(parser)

    def main(self, args):
        generic_refs, graph = self.create_from_args(args)

        vndk_sp = graph.compute_predefined_vndk_sp()
        for lib in sorted_lib_path_list(vndk_sp):
            print('vndk-sp:', lib)
        vndk_sp_indirect = graph.compute_predefined_vndk_sp_indirect()
        for lib in sorted_lib_path_list(vndk_sp_indirect):
            print('vndk-sp-indirect:', lib)
        return 0


class SpLibCommand(ELFGraphCommand):
    def __init__(self):
        super(SpLibCommand, self).__init__(
                'sp-lib', help='Define sp-ndk, sp-hal, and vndk-sp')

    def add_argparser_options(self, parser):
        super(SpLibCommand, self).add_argparser_options(parser)

    def main(self, args):
        generic_refs, graph = self.create_from_args(args)
        print_sp_lib(graph.compute_sp_lib(generic_refs))
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
    register_subcmd(VNDKCapCommand())
    register_subcmd(DepsCommand())
    register_subcmd(DepsClosureCommand())
    register_subcmd(DepsInsightCommand())
    register_subcmd(CheckDepCommand())
    register_subcmd(DepGraphCommand())
    register_subcmd(SpLibCommand())
    register_subcmd(VNDKSPCommand())

    args = parser.parse_args()
    if not args.subcmd:
        parser.print_help()
        sys.exit(1)
    return subcmds[args.subcmd].main(args)

if __name__ == '__main__':
    sys.exit(main())
