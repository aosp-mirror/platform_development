#!/usr/bin/env python3

from __future__ import print_function

import argparse
import collections
import mmap
import os
import re
import stat
import struct
import sys


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

SHN_UNDEF = 0

STB_LOCAL = 0
STB_GLOBAL = 1
STB_WEAK = 2


Elf_Hdr = collections.namedtuple(
        'Elf_Hdr',
        'ei_class ei_data ei_version ei_osabi e_type e_machine e_version ' +
        'e_entry e_phoff e_shoff e_flags e_ehsize e_phentsize e_phnum ' +
        'e_shentsize e_shnum e_shstridx')


Elf_Shdr = collections.namedtuple(
        'Elf_Shdr',
        'sh_name sh_type sh_flags sh_addr sh_offset sh_size sh_link sh_info ' +
        'sh_addralign sh_entsize')


Elf_Dyn = collections.namedtuple('Elf_Dyn', 'd_tag d_val')


class Elf_Sym(object):
    __slots__ = (
        'st_name', 'st_value', 'st_size', 'st_info', 'st_other', 'st_shndx'
    )

    def __init__(self, st_name, st_value, st_size, st_info, st_other, st_shndx):
        self.st_name = st_name
        self.st_value = st_value
        self.st_size = st_size
        self.st_info = st_info
        self.st_other = st_other
        self.st_shndx = st_shndx

    def __str__(self):
        return ('Elf_Sym(' +
                'st_name=' + repr(self.st_name) + ', '
                'st_value=' + repr(self.st_value) + ', '
                'st_size=' + repr(self.st_size) + ', '
                'st_info=' + repr(self.st_info) + ', '
                'st_other=' + repr(self.st_other) + ', '
                'st_shndx=' + repr(self.st_shndx) + ')')

    @staticmethod
    def _make(p):
        return Elf_Sym(*p)

    @property
    def st_bind(self):
        return (self.st_info >> 4)


def _get_elf_class_name(ei_class):
    if ei_class == ELFCLASS32:
        return '32'
    if ei_class == ELFCLASS64:
        return '64'
    return 'None'


def _get_elf_data_name(ei_data):
    if ei_data == ELFDATA2LSB:
        return 'Little-Endian'
    if ei_data == ELFDATA2MSB:
        return 'Big-Endian'
    return 'None'


_ELF_MACHINE_ID_TABLE = {
    0: 'EM_NONE',
    3: 'EM_386',
    8: 'EM_MIPS',
    40: 'EM_ARM',
    62: 'EM_X86_64',
    183: 'EM_AARCH64',
}


def _get_elf_machine_name(e_machine):
    return _ELF_MACHINE_ID_TABLE.get(e_machine, str(e_machine))


def _extract_zero_end_slice(buf, offset):
    end = offset
    try:
        while buf[end] != 0:
            end += 1
    except IndexError:
        pass
    return buf[offset:end]


if sys.version_info >= (3, 0):
    def _extract_zero_end_str(buf, offset):
        return intern(_extract_zero_end_slice(buf, offset).decode('utf-8'))
else:
    def _extract_zero_end_str(buf, offset):
        return intern(_extract_zero_end_slice(buf, offset))


class ELFError(ValueError):
    pass


class ELF(object):
    def __init__(self, ei_class=ELFCLASSNONE, ei_data=ELFDATANONE, e_machine=0,
                 dt_rpath=None, dt_runpath=None, dt_needed=None,
                 exported_symbols=None):
        self.ei_class = ei_class
        self.ei_data = ei_data
        self.e_machine = e_machine
        self.dt_rpath = dt_rpath
        self.dt_runpath = dt_runpath
        self.dt_needed = dt_needed if dt_needed is not None else []
        self.exported_symbols = \
                exported_symbols if exported_symbols is not None else []

    def __str__(self):
        return ('ELF(' +
                'ei_class=' + repr(self.ei_class) + ', ' +
                'ei_data=' + repr(self.ei_data) + ', ' +
                'e_machine=' + repr(self.e_machine) + ', ' +
                'dt_rpath=' + repr(self.dt_rpath) + ', ' +
                'dt_runpath=' + repr(self.dt_runpath) + ', ' +
                'dt_needed=' + repr(self.dt_needed) + ')')

    def dump(self, file=None):
        file = file if file is not None else sys.stdout

        print('EI_CLASS\t' + _get_elf_class_name(self.ei_class), file=file)
        print('EI_DATA\t\t' + _get_elf_data_name(self.ei_data), file=file)
        print('E_MACHINE\t' + _get_elf_machine_name(self.e_machine), file=file)
        if self.dt_rpath:
            print('DT_RPATH\t' + self.dt_rpath, file=file)
        if self.dt_runpath:
            print('DT_RUNPATH\t' + self.dt_runpath, file=file)
        for dt_needed in self.dt_needed:
            print('DT_NEEDED\t' + dt_needed, file=file)
        for symbol in self.exported_symbols:
            print('SYMBOL\t\t' + symbol, file=file)

    def dump_exported_symbols(self, file=None):
        file = file if file is not None else sys.stdout

        for symbol in self.exported_symbols:
            print(symbol, file=file)

    def _parse_from_buf_internal(self, buf):
        # Check ELF ident.
        if buf.size() < 8:
            raise ELFError('bad ident')

        if buf[0:4] != b'\x7fELF':
            raise ELFError('bad magic')

        self.ei_class = buf[EI_CLASS]
        if self.ei_class != ELFCLASS32 and self.ei_class != ELFCLASS64:
            raise ELFError('unknown word size')

        self.ei_data = buf[EI_DATA]
        if self.ei_data != ELFDATA2LSB and self.ei_data != ELFDATA2MSB:
            raise ELFError('unknown endianness')

        # ELF structure definitions.
        endian_fmt = '<' if self.ei_data == ELFDATA2LSB else '>'

        if self.ei_class == ELFCLASS32:
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

        if self.ei_class == ELFCLASS32:
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
            return _extract_zero_end_str(buf, offset)

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
            if ent.d_tag == DT_NEEDED:
                self.dt_needed.append(extract_str(dynstr_off + ent.d_val))
            elif ent.d_tag == DT_RPATH:
                self.dt_rpath = extract_str(dynstr_off + ent.d_val)
            elif ent.d_tag == DT_RUNPATH:
                self.dt_runpath = extract_str(dynstr_off + ent.d_val)

        # Parse exported symbols in .dynsym section.
        dynsym_shdr = sections.get('.dynsym')
        if dynsym_shdr:
            exported_symbols = []
            dynsym_off = dynsym_shdr.sh_offset
            dynsym_end = dynsym_off + dynsym_shdr.sh_size
            dynsym_entsize = dynsym_shdr.sh_entsize
            for ent_off in range(dynsym_off, dynsym_end, dynsym_entsize):
                ent = parse_elf_sym(ent_off)
                if ent.st_bind != STB_LOCAL and ent.st_shndx != SHN_UNDEF:
                    exported_symbols.append(
                            extract_str(dynstr_off + ent.st_name))
            exported_symbols.sort()
            self.exported_symbols = exported_symbols

    def _parse_from_buf(self, buf):
        try:
            self._parse_from_buf_internal(buf)
        except IndexError:
            raise ELFError('bad offset')

    def _parse_from_file(self, path):
        with open(path, 'rb') as f:
            st = os.fstat(f.fileno())
            if not st.st_size:
                raise ELFError('empty file')
            with mmap(f.fileno(), st.st_size, access=ACCESS_READ) as image:
                self._parse_from_buf(image)

    @staticmethod
    def load(path):
        elf = ELF()
        elf._parse_from_file(path)
        return elf

    @staticmethod
    def loads(buf):
        elf = ELF()
        elf._parse_from_buf(buf)
        return elf


PT_SYSTEM = 0
PT_VENDOR = 1
NUM_PARTITIONS = 2


NDK_LOW_LEVEL = {
    'libc.so', 'libstdc++.so', 'libdl.so', 'liblog.so', 'libm.so', 'libz.so',
}


NDK_HIGH_LEVEL = {
    'libandroid.so', 'libcamera2ndk.so', 'libEGL.so', 'libGLESv1_CM.so',
    'libGLESv2.so', 'libGLESv3.so', 'libjnigraphics.so', 'libmediandk.so',
    'libOpenMAXAL.so', 'libOpenSLES.so', 'libvulkan.so',
}

def _is_ndk_lib(path):
    lib_name = os.path.basename(path)
    return lib_name in NDK_LOW_LEVEL or lib_name in NDK_HIGH_LEVEL


BANNED_LIBS = {
    'libbinder.so',
}


def is_accessible(path):
    try:
        mode = os.stat(path).st_mode
        return (mode & (stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)) != 0
    except FileNotFoundError:
        return False


def scan_executables(root):
    for base, dirs, files in os.walk(root):
        for filename in files:
            path = os.path.join(base, filename)
            if is_accessible(path):
                yield path


class GraphNode(object):
    def __init__(self, partition, path, elf):
        self.partition = partition
        self.path = path
        self.elf = elf
        self.deps = set()
        self.users = set()
        self.is_ndk = _is_ndk_lib(path)

    def add_dep(self, dst):
        self.deps.add(dst)
        dst.users.add(self)


def sorted_lib_path_list(libs):
    libs = [lib.path for lib in libs]
    libs.sort()
    return libs


class Graph(object):
    def __init__(self):
        self.lib32 = dict()
        self.lib64 = dict()
        self.lib_pt = [dict() for i in range(NUM_PARTITIONS)]

    def add(self, partition, path, elf):
        node = GraphNode(partition, path, elf)
        if elf.ei_class == ELFCLASS32:
            self.lib32[path] = node
        else:
            self.lib64[path] = node
        self.lib_pt[partition][path] = node

    def add_dep(self, src_path, dst_path):
        for lib_set in (self.lib32, self.lib64):
            src = lib_set.get(src_path)
            dst = lib_set.get(dst_path)
            if src and dst:
                src.add_dep(dst)

    @staticmethod
    def _compile_path_matcher(root, subdirs):
        dirs = [os.path.normpath(os.path.join(root, i)) for i in subdirs]
        patts = ['(?:' + re.escape(i) + ')' for i in dirs]
        return re.compile('|'.join(patts))

    def add_executables_in_dir(self, partition_name, partition, root,
                               alter_partition, alter_subdirs):
        root = os.path.abspath(root)
        prefix_len = len(root) + 1

        if alter_subdirs:
            alter_patt = Graph._compile_path_matcher(root, alter_subdirs)

        for path in scan_executables(root):
            try:
                elf = ELF.load(path)
            except ELFError as e:
                continue

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
                    self.add_dep(match.group(1), match.group(2))

    def _resolve_deps_lib_set(self, lib_set, system_lib, vendor_lib):
        for lib in lib_set.values():
            for dt_needed in lib.elf.dt_needed:
                candidates = [
                    dt_needed,
                    os.path.join(system_lib, dt_needed),
                    os.path.join(vendor_lib, dt_needed),
                ]
                for candidate in candidates:
                    dep = lib_set.get(candidate)
                    if dep:
                        break
                if not dep:
                    print('warning: {}: Missing needed library: {}  Tried: {}'
                          .format(lib.path, dt_needed, candidates),
                          file=sys.stderr)
                    continue
                lib.add_dep(dep)

    def resolve_deps(self):
        self._resolve_deps_lib_set(self.lib32, '/system/lib', '/vendor/lib')
        self._resolve_deps_lib_set(self.lib64, '/system/lib64',
                                   '/vendor/lib64')

    def compute_vndk_libs(self, generic_refs):
        vndk_core = set()
        vndk_ext = set()

        def collect_lib_with_partition_user(result, lib_set, partition):
            for lib in lib_set.values():
                for user in lib.users:
                    if user.partition == partition:
                        result.add(lib)
                        break

        # Check library usages from vendor to system.
        collect_lib_with_partition_user(
                vndk_core, self.lib_pt[PT_SYSTEM], PT_VENDOR)

        # Check library usages from system to vendor.
        collect_lib_with_partition_user(
                vndk_ext, self.lib_pt[PT_VENDOR], PT_SYSTEM)

        # Remove NDK libraries.
        def remove_ndk_libs(libs):
            return set(lib for lib in libs if not lib.is_ndk)

        vndk_core = remove_ndk_libs(vndk_core)
        vndk_ext = remove_ndk_libs(vndk_ext)

        # Compute transitive closure.
        def get_transitive_closure(root, boundary):
            closure = set(root)
            stack = list(root)
            while stack:
                lib = stack.pop()
                for dep in lib.deps:
                    if dep.is_ndk:
                        continue
                    if dep not in closure and dep not in boundary:
                        closure.add(dep)
                        stack.append(dep)
            return closure

        vndk_indirect = get_transitive_closure(vndk_core, vndk_ext) - vndk_core
        vndk_ext = get_transitive_closure(vndk_ext, vndk_core)

        # Move extended libraries from vndk_core to vndk_ext.
        if generic_refs:
            stack = list(vndk_core)
            stacked = vndk_core
            vndk_core = set()

            while stack:
                lib = stack.pop()
                if generic_refs.is_equivalent_lib(lib):
                    vndk_core.add(lib)
                    continue

                print('warning: {}: This is a VNDK extension and must be '
                      'moved to vendor partition.'.format(lib.path),
                      file=sys.stderr)

                # Move the library from vndk_core to vndk_ext.
                vndk_ext.add(lib)
                for dep in lib.deps:
                    # Skip all NDK dependencies. They are not VNDK.
                    if dep.is_ndk:
                        continue
                    # Skip vndk_ext and possibly vndk_core.
                    if dep in vndk_ext or dep in stacked:
                        continue
                    # Promote the dependency from vndk_indirect to vndk_core.
                    assert dep in vndk_indirect
                    vndk_indirect.remove(dep)
                    stack.append(dep)
                    stacked.add(dep)

        return (vndk_core, vndk_indirect, vndk_ext)

    @staticmethod
    def create(system_dirs=None, system_dirs_as_vendor=None, vendor_dirs=None,
               vendor_dirs_as_system=None, extra_deps=None):
        graph = Graph()

        if system_dirs:
            for path in system_dirs:
                graph.add_executables_in_dir('system', PT_SYSTEM, path,
                                             PT_VENDOR, system_dirs_as_vendor)

        if vendor_dirs:
            for path in vendor_dirs:
                graph.add_executables_in_dir('vendor', PT_VENDOR, path,
                                             PT_SYSTEM, vendor_dirs_as_system)

        if extra_deps:
            for path in extra_deps:
                graph.load_extra_deps(path)

        graph.resolve_deps()

        return graph


class GenericRefs(object):
    def __init__(self):
        self.refs = dict()

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
                    self.refs[lib_name] = [line.strip() for line in f]

    @staticmethod
    def create_from_dir(root):
        result = GenericRefs()
        result._load_from_dir(root)
        return result

    def is_equivalent_lib(self, lib):
        return self.refs.get(lib.path) == lib.elf.exported_symbols


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
        for path in scan_executables(root):
            name = path[prefix_len:]
            try:
                print('Processing:', name, file=sys.stderr)
                elf = ELF.load(path)
                out = os.path.join(args.output, name) + '.sym'
                makedirs(os.path.dirname(out), exist_ok=True)
                with open(out, 'w') as f:
                    elf.dump_exported_symbols(f)
            except ELFError:
                pass
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

    def _warn_incorrect_partition_lib_set(self, lib_set, partition, error_msg):
        for lib in lib_set.values():
            if not lib.users:
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
                    dep_name = os.path.basename(dep.path)
                    if dep_name in NDK_HIGH_LEVEL:
                        print('warning: {}: VNDK is using high-level NDK {}.'
                                .format(lib.path, dep.path), file=sys.stderr)

    def _warn_banned_vendor_lib_deps(self, graph, banned_libs):
        for lib in graph.lib_pt[PT_VENDOR].values():
            for dep in lib.deps:
                dep_name = os.path.basename(dep.path)
                if dep_name in banned_libs:
                    print('warning: {}: Vendor binary depends on banned {}.'
                            .format(lib.path, dep.path), file=sys.stderr)

    def _check_ndk_extensions(self, graph, generic_refs):
        for lib_set in (graph.lib32, graph.lib64):
            for lib in lib_set.values():
                if lib.is_ndk and not generic_refs.is_equivalent_lib(lib):
                    print('warning: {}: NDK library should not be extended.'
                            .format(lib.path), file=sys.stderr)

    def main(self, args):
        graph = Graph.create(args.system, args.system_dir_as_vendor,
                             args.vendor, args.vendor_dir_as_system,
                             args.load_extra_deps)

        generic_refs = None
        if args.load_generic_refs:
            generic_refs = GenericRefs.create_from_dir(args.load_generic_refs)
            self._check_ndk_extensions(graph, generic_refs)

        if args.warn_incorrect_partition:
            self._warn_incorrect_partition(graph)

        vndk_core, vndk_indirect, vndk_ext = \
                graph.compute_vndk_libs(generic_refs)

        if args.warn_high_level_ndk_deps:
            self._warn_high_level_ndk_deps((vndk_core, vndk_indirect, vndk_ext))

        if args.warn_banned_vendor_lib_deps:
            if args.ban_vendor_lib_dep:
                banned_libs = set(args.ban_vendor_lib_dep)
            else:
                banned_libs = BANNED_LIBS
            self._warn_banned_vendor_lib_deps(graph, banned_libs)

        for lib in sorted_lib_path_list(vndk_core):
            print('vndk-core:', lib)
        for lib in sorted_lib_path_list(vndk_indirect):
            print('vndk-indirect:', lib)
        for lib in sorted_lib_path_list(vndk_ext):
            print('vndk-ext:', lib)

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

    def main(self, args):
        graph = Graph.create(args.system, args.system_dir_as_vendor,
                             args.vendor, args.vendor_dir_as_system,
                             args.load_extra_deps)

        results = []
        for partition in range(NUM_PARTITIONS):
            for name, lib in graph.lib_pt[partition].items():
                assoc_libs = lib.users if args.revert else lib.deps
                results.append((name, sorted_lib_path_list(assoc_libs)))
        results.sort()

        if args.leaf:
            for name, deps in results:
                if not deps:
                    print(name)
        else:
            for name, deps in results:
                print(name)
                for dep in deps:
                    print('\t' + dep)
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

    args = parser.parse_args()
    if not args.subcmd:
        parser.print_help()
        sys.exit(1)
    return subcmds[args.subcmd].main(args)

if __name__ == '__main__':
    sys.exit(main())
