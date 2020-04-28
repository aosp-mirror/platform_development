#!/usr/bin/env python3

import os
import sys
import tempfile

import_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
import_path = os.path.abspath(os.path.join(import_path, 'utils'))
sys.path.insert(1, import_path)

from utils import run_header_abi_dumper
from utils import run_header_abi_dumper_on_file
from utils import run_header_abi_linker
from utils import SOURCE_ABI_DUMP_EXT


SCRIPT_DIR = os.path.abspath(os.path.dirname(__file__))
ARCH_TARGET_CFLAGS = {
    'arm': ('-target', 'arm-linux-androideabi'),
    'arm64': ('-target', 'aarch64-linux-android'),
    'x86': ('-target', 'i386-linux-androideabi'),
    'x86_64': ('-target', 'x86_64-linux-android'),
    'mips': ('-target', 'mips-linux-androideabi'),
    'mips64': ('-target', 'mips64-linux-android'),
}
TARGET_ARCHES = ['arm', 'arm64', 'x86', 'x86_64', 'mips', 'mips64']


def relative_to_abs_path(relative_path):
    return os.path.join(SCRIPT_DIR, relative_path)


def relative_to_abs_path_list(relative_path_list):
    abs_paths = []
    for relative_path in relative_path_list:
        abs_paths.append(relative_to_abs_path(relative_path))
    return abs_paths


class Module(object):
    def __init__(self, name, arch, cflags, export_include_dirs):
        self.name = name
        self.arch = arch
        self.cflags = tuple(cflags)
        self.arch_cflags = ARCH_TARGET_CFLAGS.get(self.arch, tuple())
        self.export_include_dirs = relative_to_abs_path_list(
            export_include_dirs)

    def get_dump_name(self):
        """Returns the module name followed by file extension."""
        raise NotImplementedError()

    def make_dump(self):
        """Returns the dump content as a string."""
        raise NotImplementedError()

    def mutate_for_arch(self, target_arch):
        """Returns a clone of this instance with arch=target_arch."""
        raise NotImplementedError()

    def mutate_for_all_arches(self):
        if self.arch:
            return [self]
        modules = []
        for target_arch in TARGET_ARCHES:
            modules.append(self.mutate_for_arch(target_arch))
        return modules

    @staticmethod
    def get_test_modules():
        modules = []
        for module in TEST_MODULES.values():
            modules += module.mutate_for_all_arches()
        return modules

    @staticmethod
    def get_test_modules_by_name(name):
        return TEST_MODULES.get(name).mutate_for_all_arches()


class SdumpModule(Module):
    def __init__(self, name, src, export_include_dirs=tuple(), cflags=tuple(),
                 arch='', dumper_flags=tuple()):
        super(SdumpModule, self).__init__(name, arch, cflags,
                                          export_include_dirs)
        self.src = relative_to_abs_path(src)
        self.dumper_flags = dumper_flags

    def get_dump_name(self):
        return self.name + '.sdump'

    def make_dump(self):
        return run_header_abi_dumper(
            self.src, cflags=self.cflags,
            export_include_dirs=self.export_include_dirs,
            flags=self.dumper_flags)

    def mutate_for_arch(self, target_arch):
        return SdumpModule(self.name, self.src, self.export_include_dirs,
                           self.cflags, target_arch, self.dumper_flags)


class LsdumpModule(Module):
    def __init__(self, name, srcs, version_script, export_include_dirs,
                 cflags=tuple(), arch='', api='current', dumper_flags=tuple(),
                 linker_flags=tuple()):
        super(LsdumpModule, self).__init__(name, arch, cflags,
                                           export_include_dirs)
        self.srcs = relative_to_abs_path_list(srcs)
        self.version_script = relative_to_abs_path(version_script)
        self.api = api
        self.dumper_flags = dumper_flags
        self.linker_flags = linker_flags

    def get_dump_name(self):
        return self.name + SOURCE_ABI_DUMP_EXT

    def make_dump(self):
        """For each source file, produce a .sdump file, and link them to form
           an lsump file."""
        dumps_to_link = []
        with tempfile.TemporaryDirectory() as tmp:
            output_lsdump = os.path.join(tmp, self.get_dump_name())
            for src in self.srcs:
                output_path = os.path.join(tmp,
                                           os.path.basename(src) + '.sdump')
                dumps_to_link.append(output_path)
                run_header_abi_dumper_on_file(
                    src, output_path, self.export_include_dirs,
                    self.cflags + self.arch_cflags,
                    self.dumper_flags)
            return run_header_abi_linker(output_lsdump, dumps_to_link,
                                         self.version_script, self.api,
                                         self.arch, self.linker_flags)

    def mutate_for_arch(self, target_arch):
        return LsdumpModule(self.name, self.srcs, self.version_script,
                            self.export_include_dirs, self.cflags, target_arch,
                            self.api, self.dumper_flags, self.linker_flags)


TEST_MODULES = [
    SdumpModule(
        name='undeclared_types.h',
        src='integration/cpp/header/undeclared_types.h',
        arch='',
        dumper_flags=['-suppress-errors', '-output-format', 'Json']),
    SdumpModule(
        name='known_issues.h',
        src='integration/cpp/header/known_issues.h',
        arch='',
        dumper_flags=['-suppress-errors', '-output-format', 'Json']),
    LsdumpModule(
        name='libc_and_cpp',
        srcs=[
            'integration/c_and_cpp/source1.cpp',
            'integration/c_and_cpp/source2.c',
        ],
        version_script='integration/c_and_cpp/map.txt',
        export_include_dirs=['integration/c_and_cpp/include'],
    ),
    LsdumpModule(
        name='libc_and_cpp_with_opaque_ptr_a',
        srcs=[
            'integration/c_and_cpp/source1.cpp',
            'integration/c_and_cpp/source2.c',
        ],
        version_script='integration/c_and_cpp/map.txt',
        export_include_dirs=['integration/c_and_cpp/include'],
        cflags=['-DOPAQUE_STRUCT_A=1'],
    ),
    LsdumpModule(
        name='libc_and_cpp_with_opaque_ptr_b',
        srcs=[
            'integration/c_and_cpp/source1.cpp',
            'integration/c_and_cpp/source2.c',
        ],
        version_script='integration/c_and_cpp/map.txt',
        export_include_dirs=['integration/c_and_cpp/include'],
        cflags=['-DOPAQUE_STRUCT_B=1'],
    ),
    LsdumpModule(
        name='libc_and_cpp_with_unused_struct',
        srcs=[
            'integration/c_and_cpp/source1.cpp',
            'integration/c_and_cpp/source2.c',
        ],
        version_script='integration/c_and_cpp/map.txt',
        export_include_dirs=['integration/c_and_cpp/include'],
        cflags=['-DINCLUDE_UNUSED_STRUCTS=1'],
    ),
    LsdumpModule(
        name='libc_and_cpp_with_unused_cstruct',
        srcs=[
            'integration/c_and_cpp/source1.cpp',
            'integration/c_and_cpp/source2.c',
        ],
        version_script='integration/c_and_cpp/map.txt',
        export_include_dirs=['integration/c_and_cpp/include'],
        cflags=['-DINCLUDE_UNUSED_STRUCTS=1', '-DMAKE_UNUSED_STRUCT_C=1'],
    ),
    LsdumpModule(
        name='libgolden_cpp',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
    ),
    LsdumpModule(
        name='libgolden_cpp_odr',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DTEST_ODR'],
    ),
    LsdumpModule(
        name='libgolden_cpp_add_function',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map_add_function.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_ADD_FUNCTION=1'],
    ),
    LsdumpModule(
        name='libgolden_cpp_add_function_and_unexported_elf',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map_add_function_elf_symbol.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_ADD_FUNCTION=1', '-DADD_UNEXPORTED_ELF_SYMBOL'],
        arch='',
        api='current',
    ),
    LsdumpModule(
        name='libgolden_cpp_add_function_sybmol_only',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map_add_function.txt',
        export_include_dirs=['integration/cpp/gold/include'],
    ),
    LsdumpModule(
        name='libgolden_cpp_change_function_access',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_CHANGE_FUNCTION_ACCESS=1'],
    ),
    LsdumpModule(
        name='libgolden_cpp_add_global_variable',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map_added_globvar.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_ADD_GLOBVAR=1'],
    ),
    LsdumpModule(
        name='libgolden_cpp_add_global_variable_private',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map_added_globvar.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_ADD_GLOBVAR=1', '-DGOLDEN_ADD_GLOBVAR_PRIVATE'],
    ),
    LsdumpModule(
        name='libgolden_cpp_return_type_diff',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_RETURN_TYPE_DIFF=1'],
    ),
    LsdumpModule(
        name='libgolden_cpp_parameter_type_diff',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map_parameter_type_diff.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_PARAMETER_TYPE_DIFF=1'],
    ),
    LsdumpModule(
        name='libgolden_cpp_vtable_diff',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_VTABLE_DIFF=1'],
    ),
    LsdumpModule(
        name='libgolden_cpp_member_diff',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_MEMBER_DIFF=1'],
    ),
    LsdumpModule(
        name='libgolden_cpp_member_fake_diff',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_MEMBER_FAKE_DIFF=1'],
    ),
    LsdumpModule(
        name='libgolden_cpp_member_cv_diff',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_MEMBER_CV_DIFF=1'],
    ),
    LsdumpModule(
        name='libgolden_cpp_change_member_access',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_CHANGE_MEMBER_ACCESS=1'],
    ),
    LsdumpModule(
        name='libgolden_cpp_member_integral_type_diff',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_MEMBER_INTEGRAL_TYPE_DIFF=1'],
    ),
    LsdumpModule(
        name='libgolden_cpp_enum_diff',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_ENUM_DIFF=1'],
    ),
    LsdumpModule(
        name='libgolden_cpp_enum_extended',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_ENUM_EXTENSION=1'],
    ),
    LsdumpModule(
        name='libgolden_cpp_unreferenced_elf_symbol_removed',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map_elf_symbol_removed.txt',
        export_include_dirs=['integration/cpp/gold/include'],
    ),
    LsdumpModule(
        name='libreproducability',
        srcs=['integration/c_and_cpp/reproducability.c'],
        version_script='integration/c_and_cpp/repro_map.txt',
        export_include_dirs=['integration/c_and_cpp/include'],
    ),
    LsdumpModule(
        name='libgolden_cpp_member_name_changed',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_CHANGE_MEMBER_NAME_SAME_OFFSET=1'],
    ),
    LsdumpModule(
        name='libgolden_cpp_function_pointer',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_FUNCTION_POINTER=1'],
    ),
    LsdumpModule(
        name='libgolden_cpp_function_pointer_parameter_added',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_FUNCTION_POINTER_ADD_PARAM=1',
                '-DGOLDEN_FUNCTION_POINTER=1'],
    ),
    LsdumpModule(
        name='libgolden_cpp_internal_public_struct',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_WITH_INTERNAL_STRUCT',
                '-DGOLDEN_WITH_PUBLIC_INTERNAL_STRUCT'],
    ),
    LsdumpModule(
        name='libgolden_cpp_internal_private_struct',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_WITH_INTERNAL_STRUCT'],
    ),
    LsdumpModule(
        name='libgolden_cpp_inheritance_type_changed',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        cflags=['-DGOLDEN_CHANGE_INHERITANCE_TYPE'],
    ),
    LsdumpModule(
        name='libpure_virtual_function',
        srcs=['integration/cpp/pure_virtual/pure_virtual_function.cpp'],
        export_include_dirs=['integration/cpp/pure_virtual/include'],
        version_script='',
    ),
    LsdumpModule(
        name='libgolden_cpp_json',
        srcs=[
            'integration/cpp/gold/golden_1.cpp',
            'integration/cpp/gold/high_volume_speaker.cpp',
            'integration/cpp/gold/low_volume_speaker.cpp',
        ],
        version_script='integration/cpp/gold/map.txt',
        export_include_dirs=['integration/cpp/gold/include'],
        dumper_flags=['-output-format', 'Json'],
        linker_flags=['-input-format', 'Json', '-output-format', 'Json']
    ),
    LsdumpModule(
        name='libversion_script_example',
        arch='arm64',
        srcs=[
            'integration/version_script_example/example.cpp',
        ],
        version_script='integration/version_script_example/example.map.txt',
        export_include_dirs=['integration/version_script_example'],
        dumper_flags=['-output-format', 'Json'],
        linker_flags=[
            '-input-format', 'Json',
            '-output-format', 'Json',
            '-so', relative_to_abs_path(
                'integration/version_script_example/prebuilts/' +
                'libversion_script_example.so'
            ),
        ]
    ),
    LsdumpModule(
        name='libversion_script_example_no_private',
        arch='arm64',
        srcs=[
            'integration/version_script_example/example.cpp',
        ],
        version_script='integration/version_script_example/example.map.txt',
        export_include_dirs=['integration/version_script_example'],
        dumper_flags=['-output-format', 'Json'],
        linker_flags=[
            '-input-format', 'Json',
            '-output-format', 'Json',
            '-so', relative_to_abs_path(
                'integration/version_script_example/prebuilts/' +
                'libversion_script_example.so'
            ),
            '--exclude-symbol-version', 'LIBVERSION_SCRIPT_EXAMPLE_PRIVATE',
        ]
    ),
    LsdumpModule(
        name='libversion_script_example_no_mytag',
        arch='arm64',
        srcs=[
            'integration/version_script_example/example.cpp',
        ],
        version_script='integration/version_script_example/example.map.txt',
        export_include_dirs=['integration/version_script_example'],
        dumper_flags=['-output-format', 'Json'],
        linker_flags=[
            '-input-format', 'Json',
            '-output-format', 'Json',
            '-so', relative_to_abs_path(
                'integration/version_script_example/prebuilts/' +
                'libversion_script_example.so'
            ),
            '--exclude-symbol-tag', 'mytag',
        ]
    ),

    # Test data for test_allow_adding_removing_weak_symbols
    LsdumpModule(
        name='libweak_symbols_old',
        arch='arm64',
        srcs=[
            'integration/weak_symbols/example.c',
        ],
        version_script='integration/weak_symbols/libexample_old.map.txt',
        export_include_dirs=[],
        dumper_flags=['-output-format', 'Json'],
        linker_flags=[
            '-input-format', 'Json',
            '-output-format', 'Json',
        ]
    ),
    LsdumpModule(
        name='libweak_symbols_new',
        arch='arm64',
        srcs=[
            'integration/weak_symbols/example.c',
        ],
        version_script='integration/weak_symbols/libexample_new.map.txt',
        export_include_dirs=[],
        dumper_flags=['-output-format', 'Json'],
        linker_flags=[
            '-input-format', 'Json',
            '-output-format', 'Json',
        ],
        cflags=['-DNEW=1']
    ),
]

TEST_MODULES = {m.name: m for m in TEST_MODULES}
