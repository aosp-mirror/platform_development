#!/usr/bin/env python3

from __future__ import print_function

import argparse
import collections
import itertools
import os
import re
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
        end = offset
        try:
            while buf[end] != 0:
                end += 1
        except IndexError:
            pass
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
    LLNDK_LIB_NAMES = (
        'libc.so',
        'libdl.so',
        'liblog.so',
        'libm.so',
        'libstdc++.so',
        'libz.so',
    )

    SPNDK_LIB_NAMES = (
        'libEGL.so',
        'libGLESv1_CM.so',
        'libGLESv2.so',
        'libGLESv3.so',
    )

    HLNDK_LIB_NAMES = (
        'libOpenMAXAL.so',
        'libOpenSLES.so',
        'libandroid.so',
        'libcamera2ndk.so',
        'libjnigraphics.so',
        'libmediandk.so',
        'libvulkan.so',
    )

    @staticmethod
    def _compile_path_matcher(names):
        patts = '|'.join('(?:^\\/system\\/lib(?:64)?\\/' + re.escape(i) + '$)'
                         for i in names)
        return re.compile(patts)

    def __init__(self):
        self.llndk_patterns = self._compile_path_matcher(self.LLNDK_LIB_NAMES)
        self.spndk_patterns = self._compile_path_matcher(self.SPNDK_LIB_NAMES)
        self.hlndk_patterns = self._compile_path_matcher(self.HLNDK_LIB_NAMES)
        self.ndk_patterns = self._compile_path_matcher(
                self.LLNDK_LIB_NAMES + self.SPNDK_LIB_NAMES +
                self.HLNDK_LIB_NAMES)

    def is_ndk(self, path):
        return self.ndk_patterns.match(path)

    def is_llndk(self, path):
        return self.llndk_patterns.match(path)

    def is_spndk(self, path):
        return self.spndk_patterns.match(path)

    def is_hlndk(self, path):
        return self.hlndk_patterns.match(path)

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


VNDKHeuristics = collections.namedtuple(
        'VNDKHeuristics',
        'extra_vendor_libs vndk_core vndk_indirect vndk_fwk_ext vndk_vnd_ext')


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
        self.is_ndk = NDK_LIBS.is_ndk(path)
        self.unresolved_symbols = set()
        self.linked_symbols = dict()

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


def sorted_lib_path_list(libs):
    libs = [lib.path for lib in libs]
    libs.sort()
    return libs


class ELFLinker(object):
    LIB32_SEARCH_PATH = (
        '/system/lib',
        '/system/lib/vndk',
        '/system/lib/vndk-ext',
        '/vendor/lib',
    )

    LIB64_SEARCH_PATH = (
        '/system/lib64',
        '/system/lib64/vndk',
        '/system/lib64/vndk-ext',
        '/vendor/lib64',
    )


    def __init__(self):
        self.lib32 = dict()
        self.lib64 = dict()
        self.lib_pt = [dict() for i in range(NUM_PARTITIONS)]

        self.lib32_resolver = ELFResolver(self.lib32, self.LIB32_SEARCH_PATH)
        self.lib64_resolver = ELFResolver(self.lib64, self.LIB64_SEARCH_PATH)

    def add(self, partition, path, elf):
        node = ELFLinkData(partition, path, elf)
        if elf.is_32bit:
            self.lib32[path] = node
        else:
            self.lib64[path] = node
        self.lib_pt[partition][path] = node
        return node

    def add_dep(self, src_path, dst_path, ty):
        for lib_set in (self.lib32, self.lib64):
            src = lib_set.get(src_path)
            dst = lib_set.get(dst_path)
            if src and dst:
                src.add_dep(dst, ty)
                return
        print('error: cannot add dependency from {} to {}.'
              .format(src_path, dst_path), file=sys.stderr)

    def map_path_to_lib(self, path):
        for lib_set in (self.lib32, self.lib64):
            lib = lib_set.get(path)
            if lib:
                return lib
        return None

    def map_paths_to_libs(self, paths, report_error):
        result = set()
        for path in paths:
            lib = self.map_path_to_lib(path)
            if not lib:
                report_error(path)
                continue
            result.add(lib)
        return result

    @staticmethod
    def _compile_path_matcher(root, subdirs):
        dirs = [os.path.normpath(os.path.join(root, i)) for i in subdirs]
        patts = ['(?:' + re.escape(i) + os.sep + ')' for i in dirs]
        return re.compile('|'.join(patts))

    def add_executables_in_dir(self, partition_name, partition, root,
                               alter_partition, alter_subdirs, scan_elf_files):
        root = os.path.abspath(root)
        prefix_len = len(root) + 1

        if alter_subdirs:
            alter_patt = ELFLinker._compile_path_matcher(root, alter_subdirs)

        for path, elf in scan_elf_files(root):
            short_path = os.path.join('/', partition_name, path[prefix_len:])
            if alter_subdirs and alter_patt.match(path):
                self.add(alter_partition, short_path, elf)
            else:
                self.add(partition, short_path, elf)

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
        imported_libs = self._resolve_lib_dt_needed(lib, resolver)
        self._resolve_lib_imported_symbols(lib, imported_libs, generic_refs)

    def _resolve_lib_set_deps(self, lib_set, resolver, generic_refs):
        for lib in lib_set.values():
            self._resolve_lib_deps(lib, resolver, generic_refs)

    def resolve_deps(self, generic_refs=None):
        self._resolve_lib_set_deps(
                self.lib32, self.lib32_resolver, generic_refs)
        self._resolve_lib_set_deps(
                self.lib64, self.lib64_resolver, generic_refs)

    def compute_matched_libs(self, path_patterns, closure=False,
                             is_excluded_libs=None):
        patt = re.compile('|'.join('(?:' + p + ')' for p in path_patterns))

        # Find libraries with matching paths.
        libs = set()
        for lib_set in self.lib_pt:
            for lib in lib_set.values():
                if patt.match(lib.path):
                    libs.add(lib)

        if closure:
            # Compute transitive closure.
            if not is_excluded_libs:
                def is_excluded_libs(lib):
                    return False
            libs = self.compute_closure(libs, is_excluded_libs)

        return libs

    def compute_vndk_stable(self, closure):
        """Find all vndk stable libraries."""

        path_patterns = (
            # HIDL libraries used by android.hardware.graphics.mapper@2.0-impl.
            '^.*/libhidlbase\\.so$',
            '^.*/libhidltransport\\.so$',
            '^.*/libhidlmemory\\.so$',
            '^.*/libfmp\\.so$',
            '^.*/libhwbinder\\.so$',

            # UI libraries used by libEGL.
            #'^.*/libui\\.so$',
            #'^.*/libnativewindow\\.so$',
        )

        def is_excluded_libs(lib):
            return lib.is_ndk

        return self.compute_matched_libs(path_patterns, closure,
                                         is_excluded_libs)

    def compute_sp_hal(self, vndk_stable, closure):
        """Find all same-process HALs."""

        path_patterns = (
            # OpenGL-related
            '^/vendor/.*/libEGL_.*\\.so$',
            '^/vendor/.*/libGLESv1_CM_.*\\.so$',
            '^/vendor/.*/libGLESv2_.*\\.so$',
            '^/vendor/.*/libGLESv3_.*\\.so$',
            # Vulkan
            '^/vendor/.*/vulkan.*\\.so$',
            # libRSDriver
            '^/vendor/.*/libRSDriver.*\\.so$',
            '^/vendor/.*/libPVRRS\\.so$',
            # Gralloc mapper
            '^.*/gralloc\\..*\\.so$',
            '^.*/android\\.hardware\\.graphics\\.mapper@\\d+\\.\\d+-impl\\.so$',
        )

        def is_excluded_libs(lib):
            return lib.is_ndk or lib in vndk_stable

        return self.compute_matched_libs(path_patterns, closure,
                                         is_excluded_libs)

    def _po_component_sorted(self, lib_set, get_successors,
                             get_strong_successors):
        result = []

        idx_dict = {}
        idx_counter = 0
        has_scc = set()

        s = []
        p = []

        def traverse(v):
            idx_dict[v] = len(idx_dict)

            s.append(v)
            p.append(v)

            for succ in get_successors(v):
                if succ not in lib_set:
                    continue
                succ_idx = idx_dict.get(succ)
                if succ_idx is None:
                    traverse(succ)
                elif succ not in has_scc:
                    while idx_dict[p[-1]] > succ_idx:
                        p.pop()

            if p[-1] is v:
                scc = set()
                while True:
                    w = s.pop()
                    scc.add(w)
                    has_scc.add(w)
                    if w is v:
                        break
                p.pop()
                result.append(self._po_sorted(scc, get_strong_successors))

        for v in lib_set:
            if v not in idx_dict:
                traverse(v)

        return result

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
        system_libs_po = self._deps_po_sorted(self.lib_pt[PT_SYSTEM].values())
        system_libs = self.lib_pt[PT_SYSTEM]
        vendor_libs = self.lib_pt[PT_VENDOR]

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
                lib.partition = PT_VENDOR
                vendor_libs[lib.path] = lib
                del system_libs[lib.path]

    def find_existing_vndk(self):
        def collect_libs_with_path_pattern(pattern):
            result = set()
            pattern = re.compile(pattern)
            for lib_set in (self.lib32.values(), self.lib64.values()):
                for lib in lib_set:
                    if pattern.match(lib.path):
                        result.add(lib)
            return result

        vndk_core = collect_libs_with_path_pattern(
                '^/system/lib(?:64)?/vndk(?:-\\d+)?/')
        vndk_fwk_ext = collect_libs_with_path_pattern(
                '^/system/lib(?:64)?/vndk(?:-\\d+)?-ext?/')
        vndk_vnd_ext = collect_libs_with_path_pattern(
                '^/vendor/lib(?:64)?/vndk(?:-\\d+)?-ext?/')

        return (vndk_core, vndk_fwk_ext, vndk_vnd_ext)

    def compute_vndk(self, sp_hals, vndk_stable, vndk_customized_for_system,
                     vndk_customized_for_vendor, generic_refs, banned_libs):
        # Collect existing VNDK libraries.
        vndk_core, vndk_fwk_ext, vndk_vnd_ext = self.find_existing_vndk()

        # Collect VNDK candidates.
        def is_not_vndk(lib):
            return (lib.is_ndk or banned_libs.is_banned(lib.path) or
                    (lib in sp_hals) or (lib in vndk_stable))

        def collect_libs_with_partition_user(lib_set, partition):
            result = set()
            for lib in lib_set:
                if is_not_vndk(lib):
                    continue
                if any(user.partition == partition for user in lib.users):
                    result.add(lib)
            return result

        vndk_candidates = collect_libs_with_partition_user(
                self.lib_pt[PT_SYSTEM].values(), PT_VENDOR)

        vndk_visited = set(vndk_candidates)

        # Sets for missing libraries.
        extra_vendor_libs = set()

        def get_vndk_core_lib_name(lib):
            lib_name = os.path.basename(lib.path)
            lib_dir_name = 'lib' if lib.elf.is_32bit else 'lib64'
            return os.path.join('/system', lib_dir_name, 'vndk', lib_name)

        def add_to_vndk_core(lib):
            """Add a library to vndk-core."""
            elf = generic_refs.refs[lib.path]

            # Create new vndk-core lib from generic reference.
            vndk_lib_path = get_vndk_core_lib_name(lib)
            vndk_lib = self.add(PT_SYSTEM, vndk_lib_path, elf)

            # Resovle the library dependencies.
            resolver = self.lib32_resolver if lib.elf.is_32bit else \
                       self.lib64_resolver
            self._resolve_lib_deps(vndk_lib, resolver, generic_refs)

            # Add vndk-core to the set.
            vndk_core.add(vndk_lib)

        # Compute vndk-core, vndk-fwk-ext and vndk-vnd-ext.
        if not generic_refs:
            vndk_core.update(vndk_candidates)
        else:
            while vndk_candidates:
                if __debug__:
                    # Loop invariant: These set should be pairwise independent.
                    # Each VNDK libraries should have their ELFLinkData
                    # instance.
                    assert not (vndk_core & vndk_fwk_ext)
                    assert not (vndk_core & vndk_vnd_ext)
                    assert not (vndk_fwk_ext & vndk_vnd_ext)

                    # Loop invariant: The library names in vndk_fwk_ext and
                    # vndk_vnd_ext must exist in vndk_core as well.
                    vndk_core_lib_names = \
                            set(os.path.basename(x.path) for x in vndk_core)
                    vndk_fwk_ext_lib_names = \
                            set(os.path.basename(x.path) for x in vndk_fwk_ext)
                    vndk_vnd_ext_lib_names = \
                            set(os.path.basename(x.path) for x in vndk_vnd_ext)
                    assert vndk_fwk_ext_lib_names <= vndk_core_lib_names
                    assert vndk_vnd_ext_lib_names <= vndk_core_lib_names

                prev_vndk_candidates = vndk_candidates
                vndk_candidates = set()

                def add_to_vndk_fwk_ext(lib):
                    vndk_fwk_ext.add(lib)

                def add_to_vndk_vnd_ext(lib):
                    """Add a library to vndk-vnd-ext."""
                    path = lib.path

                    # Clone lib object for vndk-vnd-ext.
                    cloned_lib = self.add(PT_VENDOR, path, lib.elf)

                    # Update the usages.
                    for user in list(lib.dt_users):
                        if user.is_system_lib():
                            user.remove_dep(lib, ELFLinkData.NEEDED)
                    for user in list(lib.dl_users):
                        if user.is_system_lib():
                            user.remove_dep(lib, ELFLinkData.DLOPEN)

                    # Resolve the dependencies.
                    resolver = self.lib32_resolver if lib.elf.is_32bit else \
                               self.lib64_resolver
                    self._resolve_lib_deps(cloned_lib, resolver, generic_refs)

                    add_deps_to_vndk_candidate(cloned_lib)

                    vndk_vnd_ext.add(cloned_lib)

                def add_to_vndk_candidate(lib):
                    if is_not_vndk(lib):
                        return
                    if lib not in vndk_visited:
                        vndk_candidates.add(lib)
                        vndk_visited.add(lib)

                def add_deps_to_vndk_candidate(lib):
                    for dep in lib.deps:
                        if dep.is_system_lib():
                            add_to_vndk_candidate(dep)

                # Remove non-AOSP libraries.
                vndk_extended_candidates = set()
                vndk_customized_candidates = set()
                for lib in prev_vndk_candidates:
                    category = generic_refs.classify_lib(lib)
                    if category == GenericRefs.NEW_LIB:
                        extra_vendor_libs.add(lib)
                        add_deps_to_vndk_candidate(lib)
                    elif category == GenericRefs.EXPORT_EQUAL:
                        vndk_customized_candidates.add(lib)
                    elif category == GenericRefs.EXPORT_SUPER_SET:
                        vndk_extended_candidates.add(lib)
                    else:
                        print('error: {}: vndk library must not be modified.'
                              .format(lib.path), file=sys.stderr)

                # Classify VNDK customized candidates.
                for lib in vndk_customized_candidates:
                    if not lib.imported_ext_symbols:
                        # Inward-customized VNDK-core libraries.
                        add_to_vndk_core(lib)
                    else:
                        # Outward-customized VNDK libraries.

                        # Add a vndk-core counterpart for this lib.
                        add_to_vndk_core(lib)

                        # Add this lib to vndk-ext sets.
                        if lib in vndk_customized_for_system:
                            add_to_vndk_fwk_ext(lib)
                        if lib in vndk_customized_for_vendor:
                            add_to_vndk_vnd_ext(lib)

                # Compute VNDK extension candidates.
                for lib in self._users_po_sorted(vndk_extended_candidates):
                    # Check the users of the extended exported symbols.
                    has_system_users = False
                    has_vendor_users = False
                    for user in lib.users:
                        if lib in user.imported_ext_symbols:
                            if user.is_system_lib():
                                has_system_users = True
                            else:
                                has_vendor_users = True
                        if has_system_users and has_vendor_users:
                            break

                    # Add a vndk-core counterpart for this lib.
                    add_to_vndk_core(lib)

                    # Add this lib to vndk-ext sets.
                    if has_system_users:
                        add_to_vndk_fwk_ext(lib)
                    if has_vendor_users:
                        add_to_vndk_vnd_ext(lib)

        # Compute the closure of the VNDK libs.
        vndk_core_paths = set(lib.path for lib in vndk_core)
        stack = list(vndk_core)
        while stack:
            lib = stack.pop()
            if is_not_vndk(lib):
                continue

            stack.extend(lib.deps)

            vndk_lib_path = get_vndk_core_lib_name(lib)
            if vndk_lib_path in vndk_core_paths:
                continue
            vndk_core_paths.add(vndk_lib_path)

            if lib.imported_ext_symbols or \
                    (generic_refs and not generic_refs.is_equivalent_lib(lib)):
                vndk_fwk_ext.add(lib)
            if generic_refs:
                add_to_vndk_core(lib)
            else:
                vndk_core.add(lib)

        # Truncate all vendor libs and resolve it again.
        VENDOR_SEARCH_PATH32 = (
            '/system/lib/vndk',
            '/vendor/lib',

            # FIXME: Remove following line after we fixed vndk-stable
            # resolution.
            '/system/lib',
        )

        VENDOR_SEARCH_PATH64 = (
            '/system/lib64/vndk',
            '/vendor/lib64',

            # FIXME: Remove following line after we fixed vndk-stable
            # resolution.
            '/system/lib64',
        )

        vendor_resolver32 = ELFResolver(self.lib32, VENDOR_SEARCH_PATH32)
        vendor_resolver64 = ELFResolver(self.lib64, VENDOR_SEARCH_PATH64)

        for lib in self.lib_pt[PT_VENDOR].values():
            lib._deps = (set(), set())
            lib._users = (set(), set())
            lib.imported_ext_symbols = collections.defaultdict(set)
            lib.unresolved_symbols = set()
            lib.linked_symbols = dict()

        for lib in self.lib_pt[PT_VENDOR].values():
            resolver = vendor_resolver32 if lib.elf.is_32bit else \
                       vendor_resolver64
            self._resolve_lib_deps(lib, resolver, generic_refs)

        # Separate vndk-core and vndk-indirect.
        vndk_core_indirect = vndk_core
        vndk_core = set()
        vndk_indirect = set()
        for lib in vndk_core_indirect:
            if any(not user.is_system_lib() for user in lib.users):
                vndk_core.add(lib)
            else:
                vndk_indirect.add(lib)

        return VNDKHeuristics(extra_vendor_libs, vndk_core, vndk_indirect,
                              vndk_fwk_ext, vndk_vnd_ext)

    def compute_vndk_cap(self, banned_libs):
        # ELF files on vendor partitions are banned unconditionally.  ELF files
        # on the system partition are banned if their file extensions are not
        # '.so' or their file names are listed in banned_libs.  LL-NDK and
        # SP-NDK libraries are treated as a special case which will not be
        # considered as banned libraries at the moment.
        def is_banned(lib):
            if lib.is_ndk:
                return NDK_LIBS.is_hlndk(lib.path)
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
                         vendor_dirs, vendor_dirs_as_system, extra_deps,
                         generic_refs):
        graph = ELFLinker()

        if system_dirs:
            for path in system_dirs:
                graph.add_executables_in_dir('system', PT_SYSTEM, path,
                                             PT_VENDOR, system_dirs_as_vendor,
                                             scan_elf_files)

        if vendor_dirs:
            for path in vendor_dirs:
                graph.add_executables_in_dir('vendor', PT_VENDOR, path,
                                             PT_SYSTEM, vendor_dirs_as_system,
                                             scan_elf_files)

        if extra_deps:
            for path in extra_deps:
                graph.load_extra_deps(path)

        graph.resolve_deps(generic_refs)

        return graph

    @staticmethod
    def create(system_dirs=None, system_dirs_as_vendor=None, vendor_dirs=None,
               vendor_dirs_as_system=None, extra_deps=None, generic_refs=None):
        return ELFLinker._create_internal(
                scan_elf_files, system_dirs, system_dirs_as_vendor, vendor_dirs,
                vendor_dirs_as_system, extra_deps, generic_refs)

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

    def add(self, name, elf):
        self.refs[name] = elf

    def _load_from_dir(self, root):
        root = os.path.abspath(root)
        prefix_len = len(root) + 1
        for base, dirnames, filenames in os.walk(root):
            for filename in filenames:
                if not filename.endswith('.sym'):
                    continue
                path = os.path.join(base, filename)
                lib_name = '/' + path[prefix_len:-4]
                with open(path, 'r') as f:
                    self.add(lib_name, ELF.load_dump(path))

    @staticmethod
    def create_from_dir(root):
        result = GenericRefs()
        result._load_from_dir(root)
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
                '--vendor-dir-as-system', action='append',
                help='sub directory of vendor partition that has system files')


class VNDKCommand(ELFGraphCommand):
    def __init__(self):
        super(VNDKCommand, self).__init__(
                'vndk', help='Compute VNDK libraries set')

    def add_argparser_options(self, parser):
        super(VNDKCommand, self).add_argparser_options(parser)

        parser.add_argument(
                '--load-generic-refs',
                help='compare with generic reference symbols')

        parser.add_argument(
                '--warn-incorrect-partition', action='store_true',
                help='warn about libraries only have cross partition linkages')

        parser.add_argument(
                '--warn-high-level-ndk-deps', action='store_true',
                help='warn about VNDK depends on high-level NDK')

        parser.add_argument(
                '--warn-banned-vendor-lib-deps', action='store_true',
                help='warn when a vendor binaries depends on banned lib')

        parser.add_argument(
                '--ban-vendor-lib-dep', action='append',
                help='library that must not be used by vendor binaries')

        parser.add_argument(
                '--outward-customization-default-partition', default='system',
                help='default partition for outward customized vndk libs')

        parser.add_argument(
                '--outward-customization-for-system', action='append',
                help='outward customized vndk for system partition')

        parser.add_argument(
                '--outward-customization-for-vendor', action='append',
                help='outward customized vndk for vendor partition')

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

    def _warn_high_level_ndk_deps(self, lib_sets):
        for lib_set in lib_sets:
            for lib in lib_set:
                for dep in lib.deps:
                    if NDK_LIBS.is_hlndk(dep.path):
                        print('warning: {}: VNDK is using high-level NDK {}.'
                                .format(lib.path, dep.path), file=sys.stderr)

    def _warn_banned_vendor_lib_deps(self, graph, banned_libs):
        for lib in graph.lib_pt[PT_VENDOR].values():
            for dep in lib.deps:
                banned = banned_libs.is_banned(dep.path)
                if banned:
                    print('warning: {}: Vendor binary depends on banned {} '
                          '(reason: {})'.format(
                              lib.path, dep.path, banned.reason),
                          file=sys.stderr)

    def _check_ndk_extensions(self, graph, generic_refs):
        for lib_set in (graph.lib32, graph.lib64):
            for lib in lib_set.values():
                if lib.is_ndk and not generic_refs.is_equivalent_lib(lib):
                    print('warning: {}: NDK library should not be extended.'
                            .format(lib.path), file=sys.stderr)

    def main(self, args):
        # Load the generic reference.
        generic_refs = None
        if args.load_generic_refs:
            generic_refs = GenericRefs.create_from_dir(args.load_generic_refs)

        # Link ELF objects.
        graph = ELFLinker.create(args.system, args.system_dir_as_vendor,
                                 args.vendor, args.vendor_dir_as_system,
                                 args.load_extra_deps,
                                 generic_refs=generic_refs)

        # Check the API extensions to NDK libraries.
        if generic_refs:
            self._check_ndk_extensions(graph, generic_refs)

        # Create banned libraries.
        if not args.ban_vendor_lib_dep:
            banned_libs = BannedLibDict.create_default()
        else:
            banned_libs = BannedLibDict()
            for name in args.ban_vendor_lib_dep:
                banned_libs.add(name, 'user-banned', BA_WARN)

        if args.warn_incorrect_partition:
            self._warn_incorrect_partition(graph)

        if args.warn_banned_vendor_lib_deps:
            self._warn_banned_vendor_lib_deps(graph, banned_libs)

        # Compute sp-hal and vndk-stable.
        vndk_stable = graph.compute_vndk_stable(closure=True)
        sp_hals = graph.compute_sp_hal(vndk_stable, closure=False)
        sp_hals_closure = graph.compute_sp_hal(vndk_stable, closure=True)

        # Normalize partition tags.  We expect many violations from the
        # pre-Treble world.  Guess a resolution for the incorrect partition
        # tag.
        graph.normalize_partition_tags(sp_hals, generic_refs)

        # User may specify the partition for outward-customized vndk libs.  The
        # following code converts the path into ELFLinkData.
        vndk_customized_for_system = set()
        vndk_customized_for_vendor = set()

        system_libs = graph.lib_pt[PT_SYSTEM].values()
        if args.outward_customization_default_partition in {'system', 'both'}:
            vndk_customized_for_system.update(system_libs)

        if args.outward_customization_default_partition in {'vendor', 'both'}:
            vndk_customized_for_vendor.update(system_libs)

        if args.outward_customization_for_system:
            vndk_customized_for_system.update(
                    graph.map_paths_to_libs(
                        args.outward_customization_for_system, lambda x: None))

        if args.outward_customization_for_vendor:
            vndk_customized_for_vendor.update(
                    graph.map_paths_to_libs(
                        args.outward_customization_for_vendor, lambda x: None))

        # Compute vndk heuristics.
        vndk = graph.compute_vndk(
                sp_hals_closure, vndk_stable, vndk_customized_for_system,
                vndk_customized_for_vendor, generic_refs, banned_libs)

        if args.warn_high_level_ndk_deps:
            self._warn_high_level_ndk_deps(
                    (vndk.vndk_core, vndk.vndk_indirect, vndk.vndk_fwk_ext,
                     vndk.vndk_vnd_ext))

        for lib in sorted_lib_path_list(sp_hals_closure):
            print('sp-hals:', lib)

        for lib in sorted_lib_path_list(vndk_stable):
            print('vndk-stable:', lib)

        for lib in sorted_lib_path_list(vndk.vndk_core):
            print('vndk-core:', lib)

        for lib in sorted_lib_path_list(vndk.vndk_indirect):
            print('vndk-indirect:', lib)

        for lib in sorted_lib_path_list(vndk.vndk_fwk_ext):
            print('vndk-fwk-ext:', lib)

        for lib in sorted_lib_path_list(vndk.vndk_vnd_ext):
            print('vndk-vnd-ext:', lib)

        for lib in sorted_lib_path_list(vndk.extra_vendor_libs):
            print('extra-vendor-lib:', lib)

        return 0


class VNDKCapCommand(ELFGraphCommand):
    def __init__(self):
        super(VNDKCapCommand, self).__init__(
                'vndk-cap', help='Compute VNDK set upper bound')

    def add_argparser_options(self, parser):
        super(VNDKCapCommand, self).add_argparser_options(parser)

    def main(self, args):
        graph = ELFLinker.create(args.system, args.system_dir_as_vendor,
                                 args.vendor, args.vendor_dir_as_system,
                                 args.load_extra_deps)

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
        graph = ELFLinker.create(args.system, args.system_dir_as_vendor,
                                 args.vendor, args.vendor_dir_as_system,
                                 args.load_extra_deps)

        results = []
        for partition in range(NUM_PARTITIONS):
            for name, lib in graph.lib_pt[partition].items():
                if not args.symbols:
                    def collect_symbols(user, definer):
                        return ()
                else:
                    def collect_symbols(user, definer):
                        symbols = set()
                        for symbol, exp_lib in user.linked_symbols.items():
                            if exp_lib == definer:
                                symbols.add(symbol)
                        return sorted(symbols)

                data = []
                if args.revert:
                    for assoc_lib in sorted(lib.users, key=lambda x: x.path):
                        data.append((assoc_lib.path,
                                     collect_symbols(assoc_lib, lib)))
                else:
                    for assoc_lib in sorted(lib.deps, key=lambda x: x.path):
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
        graph = ELFLinker.create(args.system, args.system_dir_as_vendor,
                                 args.vendor, args.vendor_dir_as_system,
                                 args.load_extra_deps)

        # Find root/excluded libraries by their paths.
        def report_error(path):
            print('error: no such lib: {}'.format(path), file=sys.stderr)
        root_libs = graph.map_paths_to_libs(args.lib, report_error)
        excluded_libs = graph.map_paths_to_libs(args.exclude_lib, report_error)

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


class VNDKStableCommand(ELFGraphCommand):
    def __init__(self):
        super(VNDKStableCommand, self).__init__(
                'vndk-stable', help='Find transitive closure of VNDK stable')

    def add_argparser_options(self, parser):
        super(VNDKStableCommand, self).add_argparser_options(parser)

        parser.add_argument('--closure', action='store_true',
                            help='show the closure')

    def main(self, args):
        graph = ELFLinker.create(args.system, args.system_dir_as_vendor,
                                 args.vendor, args.vendor_dir_as_system,
                                 args.load_extra_deps)

        vndk_stable = graph.compute_vndk_stable(closure=args.closure)
        for lib in sorted_lib_path_list(vndk_stable):
            print(lib)
        return 0


class SpHalCommand(ELFGraphCommand):
    def __init__(self):
        super(SpHalCommand, self).__init__(
                'sp-hal', help='Find transitive closure of same-process HALs')

    def add_argparser_options(self, parser):
        super(SpHalCommand, self).add_argparser_options(parser)

        parser.add_argument('--closure', action='store_true',
                            help='show the closure')

    def main(self, args):
        graph = ELFLinker.create(args.system, args.system_dir_as_vendor,
                                 args.vendor, args.vendor_dir_as_system,
                                 args.load_extra_deps)

        vndk_stable = graph.compute_vndk_stable(closure=True)
        sp_hals = graph.compute_sp_hal(vndk_stable, closure=args.closure)
        for lib in sorted_lib_path_list(sp_hals):
            print(lib)
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
    register_subcmd(SpHalCommand())
    register_subcmd(VNDKStableCommand())

    args = parser.parse_args()
    if not args.subcmd:
        parser.print_help()
        sys.exit(1)
    return subcmds[args.subcmd].main(args)

if __name__ == '__main__':
    sys.exit(main())
