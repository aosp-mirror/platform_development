#!/usr/bin/env python3

import os
import sys
import tempfile

import_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
import_path = os.path.abspath(os.path.join(import_path, 'utils'))
sys.path.insert(1, import_path)

from utils import run_header_abi_dumper_on_file
from utils import run_header_abi_linker
from utils import TARGET_ARCHS
from utils import SOURCE_ABI_DUMP_EXT

SCRIPT_DIR = os.path.abspath(os.path.dirname(__file__))
INPUT_DIR = os.path.join(SCRIPT_DIR, 'input')
EXPECTED_DIR = os.path.join(SCRIPT_DIR, 'expected')
REF_DUMP_DIR = os.path.join(SCRIPT_DIR,  'reference_dumps')
ARCH_TARGET_CFLAGS = {'arm': ['-target', 'arm-linux-androideabi'],
                      'arm64': ['-target', 'aarch64-linux-android'],
                      'x86' : ['-target', 'i386-linux-androideabi'],
                      'x86_64' : ['-target', 'x86_64-linux-android'],
                      'mips' : ['-target', 'mips-linux-androideabi'],
                      'mips64' : ['-target', 'mips64-linux-android'],}

def relative_to_abs_path(relative_path):
    return os.path.join(SCRIPT_DIR, relative_path)

def relative_to_abs_path_list(relative_path_list):
    abs_paths = []
    for relative_path in relative_path_list:
        abs_paths.append(relative_to_abs_path(relative_path))
    return abs_paths

class Module(object):
    def __init__(self, name, arch, srcs, version_script, cflags,
                 export_include_dirs, api):
        self.name = name
        self.arch = arch
        self.srcs = relative_to_abs_path_list(srcs)
        self.version_script = relative_to_abs_path(version_script)
        self.cflags = cflags
        self.arch_cflags = ['']
        if self.arch != '':
            self.arch_cflags = ARCH_TARGET_CFLAGS.get(self.arch)
        self.export_include_dirs = relative_to_abs_path_list(export_include_dirs)
        self.api = api

    def get_name(self):
        return self.name

    def get_arch(self):
        return self.arch

    def get_srcs(self):
        return self.srcs

    def get_export_include_dirs(self):
        return self.export_include_dirs

    def get_cflags(self):
        return self.cflags

    def get_version_script(self):
        return self.version_script

    def get_api(self):
        return self.api

    def make_lsdump(self, default_cflags):
        """ For each source file, produce a .sdump file, and link them to form
            an lsump file"""
        dumps_to_link = []
        with tempfile.TemporaryDirectory() as tmp:
            output_lsdump = os.path.join(tmp, self.name) + SOURCE_ABI_DUMP_EXT
            for src in self.srcs:
                output_path = os.path.join(tmp, os.path.basename(src)) + '.sdump'
                dumps_to_link.append(output_path)
                run_header_abi_dumper_on_file(
                    src, output_path, self.export_include_dirs,
                    self.cflags + self.arch_cflags + default_cflags)
            return run_header_abi_linker(output_lsdump, dumps_to_link,
                                         self.version_script, self.api,
                                         self.arch)
    @staticmethod
    def mutate_module_for_all_arches(module):
        modules = []
        name = module.get_name()
        srcs = module.get_srcs()
        version_script = module.get_version_script()
        cflags = module.get_cflags()
        export_include_dirs = module.get_export_include_dirs()
        api = module.get_api()
        for target_arch in TARGET_ARCHS:
            modules.append(Module(name, target_arch, srcs, version_script,
                                  cflags, export_include_dirs, api))
        return modules

    @staticmethod
    def get_test_modules():
        modules = []
        for module in TEST_MODULES:
            if module.get_arch() == '':
                modules += Module.mutate_module_for_all_arches(module)
        return modules

TEST_MODULES = [
    Module(
        name = 'libc_and_cpp',
        srcs = ['integration/c_and_cpp/source1.cpp',
                'integration/c_and_cpp/source2.c',
                ],
        version_script = 'integration/c_and_cpp/map.txt',
        export_include_dirs = ['integration/c_and_cpp/include'],
        cflags = [],
        arch = '',
        api = 'current',
    ),
    Module(
        name = 'libc_and_cpp_with_unused_struct',
        srcs = ['integration/c_and_cpp/source1.cpp',
                'integration/c_and_cpp/source2.c',
                ],
        version_script = 'integration/c_and_cpp/map.txt',
        export_include_dirs = ['integration/c_and_cpp/include'],
        cflags = ['-DINCLUDE_UNUSED_STRUCTS=1'],
        arch = '',
        api = 'current',
    ),
    Module(
        name = 'libgolden_cpp',
        srcs = ['integration/cpp/gold/golden_1.cpp',
                'integration/cpp/gold/high_volume_speaker.cpp',
                'integration/cpp/gold/low_volume_speaker.cpp',
                ],
        version_script = 'integration/cpp/gold/map.txt',
        export_include_dirs = ['integration/cpp/gold/include'],
        cflags = [],
        arch = '',
        api = 'current',
    ),
    Module(
        name = 'libgolden_cpp_add_function',
        srcs = ['integration/cpp/gold/golden_1.cpp',
                'integration/cpp/gold/high_volume_speaker.cpp',
                'integration/cpp/gold/low_volume_speaker.cpp',
                ],
        version_script = 'integration/cpp/gold/map_add_function.txt',
        export_include_dirs = ['integration/cpp/gold/include'],
        cflags = ['-DGOLDEN_ADD_FUNCTION=1'],
        arch = '',
        api = 'current',
    ),
    Module(
        name = 'libgolden_cpp_change_function_access',
        srcs = ['integration/cpp/gold/golden_1.cpp',
                'integration/cpp/gold/high_volume_speaker.cpp',
                'integration/cpp/gold/low_volume_speaker.cpp',
                ],
        version_script = 'integration/cpp/gold/map.txt',
        export_include_dirs = ['integration/cpp/gold/include'],
        cflags = ['-DGOLDEN_CHANGE_FUNCTION_ACCESS=1'],
        arch = '',
        api = 'current',
    ),
    Module(
        name = 'libgolden_cpp_add_global_variable',
        srcs = ['integration/cpp/gold/golden_1.cpp',
                'integration/cpp/gold/high_volume_speaker.cpp',
                'integration/cpp/gold/low_volume_speaker.cpp',
                ],
        version_script = 'integration/cpp/gold/map_added_globvar.txt',
        export_include_dirs = ['integration/cpp/gold/include'],
        cflags = ['-DGOLDEN_ADD_GLOBVAR=1'],
        arch = '',
        api = 'current',
    ),
    Module(
        name = 'libgolden_cpp_return_type_diff',
        srcs = ['integration/cpp/gold/golden_1.cpp',
                'integration/cpp/gold/high_volume_speaker.cpp',
                'integration/cpp/gold/low_volume_speaker.cpp',
                ],
        version_script = 'integration/cpp/gold/map.txt',
        export_include_dirs = ['integration/cpp/gold/include'],
        cflags = ['-DGOLDEN_RETURN_TYPE_DIFF=1'],
        arch = '',
        api = 'current',
    ),
    Module(
        name = 'libgolden_cpp_parameter_type_diff',
        srcs = ['integration/cpp/gold/golden_1.cpp',
                'integration/cpp/gold/high_volume_speaker.cpp',
                'integration/cpp/gold/low_volume_speaker.cpp',
                ],
        version_script = 'integration/cpp/gold/map_parameter_type_diff.txt',
        export_include_dirs = ['integration/cpp/gold/include'],
        cflags = ['-DGOLDEN_PARAMETER_TYPE_DIFF=1'],
        arch = '',
        api = 'current',
    ),
    Module(
        name = 'libgolden_cpp_vtable_diff',
        srcs = ['integration/cpp/gold/golden_1.cpp',
                'integration/cpp/gold/high_volume_speaker.cpp',
                'integration/cpp/gold/low_volume_speaker.cpp',
                ],
        version_script = 'integration/cpp/gold/map.txt',
        export_include_dirs = ['integration/cpp/gold/include'],
        cflags = ['-DGOLDEN_VTABLE_DIFF=1'],
        arch = '',
        api = 'current',
    ),
    Module(
        name = 'libgolden_cpp_member_diff',
        srcs = ['integration/cpp/gold/golden_1.cpp',
                'integration/cpp/gold/high_volume_speaker.cpp',
                'integration/cpp/gold/low_volume_speaker.cpp',
                ],
        version_script = 'integration/cpp/gold/map.txt',
        export_include_dirs = ['integration/cpp/gold/include'],
        cflags = ['-DGOLDEN_MEMBER_DIFF=1'],
        arch = '',
        api = 'current',
    ),
    Module(
        name = 'libgolden_cpp_member_fake_diff',
        srcs = ['integration/cpp/gold/golden_1.cpp',
                'integration/cpp/gold/high_volume_speaker.cpp',
                'integration/cpp/gold/low_volume_speaker.cpp',
                ],
        version_script = 'integration/cpp/gold/map.txt',
        export_include_dirs = ['integration/cpp/gold/include'],
        cflags = ['-DGOLDEN_MEMBER_FAKE_DIFF=1'],
        arch = '',
        api = 'current',
    ),
    Module(
        name = 'libgolden_cpp_member_cv_diff',
        srcs = ['integration/cpp/gold/golden_1.cpp',
                'integration/cpp/gold/high_volume_speaker.cpp',
                'integration/cpp/gold/low_volume_speaker.cpp',
                ],
        version_script = 'integration/cpp/gold/map.txt',
        export_include_dirs = ['integration/cpp/gold/include'],
        cflags = ['-DGOLDEN_MEMBER_CV_DIFF=1'],
        arch = '',
        api = 'current',
    ),
    Module(
        name = 'libgolden_cpp_change_member_access',
        srcs = ['integration/cpp/gold/golden_1.cpp',
                'integration/cpp/gold/high_volume_speaker.cpp',
                'integration/cpp/gold/low_volume_speaker.cpp',
                ],
        version_script = 'integration/cpp/gold/map.txt',
        export_include_dirs = ['integration/cpp/gold/include'],
        cflags = ['-DGOLDEN_CHANGE_MEMBER_ACCESS=1'],
        arch = '',
        api = 'current',
    ),
    Module(
        name = 'libgolden_cpp_member_integral_type_diff',
        srcs = ['integration/cpp/gold/golden_1.cpp',
                'integration/cpp/gold/high_volume_speaker.cpp',
                'integration/cpp/gold/low_volume_speaker.cpp',
                ],
        version_script = 'integration/cpp/gold/map.txt',
        export_include_dirs = ['integration/cpp/gold/include'],
        cflags = ['-DGOLDEN_MEMBER_INTEGRAL_TYPE_DIFF=1'],
        arch = '',
        api = 'current',
    ),
    Module(
        name = 'libgolden_cpp_enum_diff',
        srcs = ['integration/cpp/gold/golden_1.cpp',
                'integration/cpp/gold/high_volume_speaker.cpp',
                'integration/cpp/gold/low_volume_speaker.cpp',
                ],
        version_script = 'integration/cpp/gold/map.txt',
        export_include_dirs = ['integration/cpp/gold/include'],
        cflags = ['-DGOLDEN_ENUM_DIFF=1'],
        arch = '',
        api = 'current',
    ),
    Module(
        name = 'libgolden_cpp_enum_extended',
        srcs = ['integration/cpp/gold/golden_1.cpp',
                'integration/cpp/gold/high_volume_speaker.cpp',
                'integration/cpp/gold/low_volume_speaker.cpp',
                ],
        version_script = 'integration/cpp/gold/map.txt',
        export_include_dirs = ['integration/cpp/gold/include'],
        cflags = ['-DGOLDEN_ENUM_EXTENSION=1'],
        arch = '',
        api = 'current',
    ),
    Module(
        name = 'libgolden_cpp_unreferenced_elf_symbol_removed',
        srcs = ['integration/cpp/gold/golden_1.cpp',
                'integration/cpp/gold/high_volume_speaker.cpp',
                'integration/cpp/gold/low_volume_speaker.cpp',
                ],
        version_script = 'integration/cpp/gold/map_elf_symbol_removed.txt',
        export_include_dirs = ['integration/cpp/gold/include'],
        cflags = [],
        arch = '',
        api = 'current',
    ),
]
