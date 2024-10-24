#!/usr/bin/env python3

import os
import shutil
import stat
import subprocess
import sys
import tempfile
import unittest

import_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
import_path = os.path.abspath(os.path.join(import_path, 'utils'))
sys.path.insert(1, import_path)

from utils import (run_abi_diff, run_and_read_abi_diff, run_header_abi_dumper,
                   run_header_abi_linker)
from module import Module


SCRIPT_DIR = os.path.abspath(os.path.dirname(__file__))
INPUT_DIR = os.path.join(SCRIPT_DIR, 'input')
EXPECTED_DIR = os.path.join(SCRIPT_DIR, 'expected')
EXPORTED_HEADER_DIRS = (INPUT_DIR,)
REF_DUMP_DIR = os.path.join(SCRIPT_DIR, 'reference_dumps')


def make_and_copy_dump(module, dump_dir):
    dump_dir = os.path.join(dump_dir, module.arch)
    os.makedirs(dump_dir, exist_ok=True)
    dump_path = os.path.join(dump_dir, module.get_dump_name())
    module.make_dump(dump_path)
    return dump_path


def _read_output_content(dump_path):
    with open(dump_path, 'r') as f:
        return f.read()


class HeaderCheckerTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.maxDiff = None

    def setUp(self):
        self.tmp_dir = None

    def tearDown(self):
        if self.tmp_dir:
            self.tmp_dir.cleanup()
            self.tmp_dir = None

    def get_tmp_dir(self):
        if not self.tmp_dir:
            self.tmp_dir = tempfile.TemporaryDirectory()
        return self.tmp_dir.name

    def run_and_compare(self, input_path, expected_path, cflags=[]):
        with open(expected_path, 'r') as f:
            expected_output = f.read()
        with tempfile.NamedTemporaryFile(dir=self.get_tmp_dir(),
                                         delete=False) as f:
            output_path = f.name
        run_header_abi_dumper(input_path, output_path, cflags,
                              EXPORTED_HEADER_DIRS)
        actual_output = _read_output_content(output_path)
        self.assertEqual(actual_output, expected_output)

    def run_and_compare_name(self, name, cflags=[]):
        input_path = os.path.join(INPUT_DIR, name)
        expected_path = os.path.join(EXPECTED_DIR, name)
        self.run_and_compare(input_path, expected_path, cflags)

    def run_and_compare_name_cpp(self, name, cflags=[]):
        self.run_and_compare_name(name, cflags + ['-x', 'c++', '-std=c++11'])

    def run_and_compare_name_c_cpp(self, name, cflags=[]):
        self.run_and_compare_name(name, cflags)
        self.run_and_compare_name_cpp(name, cflags)

    def run_and_compare_abi_diff(self, old_dump, new_dump, lib, arch,
                                 expected_return_code, flags=[]):
        return_code, output = run_and_read_abi_diff(
            old_dump, new_dump, arch, lib, flags)
        self.assertEqual(return_code, expected_return_code)
        return output

    def prepare_and_run_abi_diff(self, old_ref_dump_path, new_ref_dump_path,
                                 target_arch, expected_return_code, flags=[]):
        return self.run_and_compare_abi_diff(
            old_ref_dump_path, new_ref_dump_path, 'test', target_arch,
            expected_return_code, flags)

    def get_or_create_dump(self, module, create):
        if create:
            return make_and_copy_dump(module, self.get_tmp_dir())
        self.assertTrue(module.has_reference_dump,
                        f'Module {module.name} is not configured to generate '
                        f'reference dump.')
        return os.path.join(REF_DUMP_DIR, module.arch, module.get_dump_name())

    def prepare_and_run_abi_diff_all_archs(self, old_lib, new_lib,
                                           expected_return_code, flags=[],
                                           create_old=False, create_new=True):
        old_modules = Module.get_test_modules_by_name(old_lib)
        new_modules = Module.get_test_modules_by_name(new_lib)
        self.assertEqual(len(old_modules), len(new_modules))
        self.assertTrue(len(old_modules))

        for old_module, new_module in zip(old_modules, new_modules):
            self.assertEqual(old_module.arch, new_module.arch)
            old_dump_path = self.get_or_create_dump(old_module, create_old)
            new_dump_path = self.get_or_create_dump(new_module, create_new)
            output = self.prepare_and_run_abi_diff(
                old_dump_path, new_dump_path, new_module.arch,
                expected_return_code, flags)
        # Since most test cases are independent of architecture, verifying one
        # of the reports is sufficient.
        return output

    def prepare_and_absolute_diff_all_archs(self, old_lib, new_lib):
        old_modules = Module.get_test_modules_by_name(old_lib)
        new_modules = Module.get_test_modules_by_name(new_lib)
        self.assertEqual(len(old_modules), len(new_modules))

        for old_module, new_module in zip(old_modules, new_modules):
            self.assertEqual(old_module.arch, new_module.arch)
            old_dump_path = self.get_or_create_dump(old_module, False)
            new_dump_path = self.get_or_create_dump(new_module, True)
            self.assertEqual(_read_output_content(old_dump_path),
                             _read_output_content(new_dump_path))

    def test_example1_cpp(self):
        self.run_and_compare_name_cpp('example1.cpp')

    def test_example1_h(self):
        self.run_and_compare_name_cpp('example1.h')

    def test_example2_h(self):
        self.run_and_compare_name_cpp('example2.h')

    def test_example3_h(self):
        self.run_and_compare_name_cpp('example3.h')

    def test_libc_and_cpp(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libc_and_cpp", "libc_and_cpp", 0)

    def test_libc_and_cpp_and_libc_and_cpp_with_unused_struct(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libc_and_cpp", "libc_and_cpp_with_unused_struct", 0)

    def test_libc_and_cpp_and_libc_and_cpp_with_unused_struct_allow(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libc_and_cpp", "libc_and_cpp_with_unused_struct", 0,
            ["-allow-unreferenced-changes"])

    def test_libc_and_cpp_and_libc_and_cpp_with_unused_struct_check_all(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libc_and_cpp", "libc_and_cpp_with_unused_struct", 1,
            ["-check-all-apis"])
        self.prepare_and_run_abi_diff_all_archs(
            "libc_and_cpp", "libc_and_cpp_with_unused_struct", 0,
            ["-check-all-apis",
             "-ignore-linker-set-key", "_ZTI12UnusedStruct"])

    def test_libc_and_cpp_with_unused_struct_and_libc_and_cpp_with_unused_cstruct(
            self):
        self.prepare_and_run_abi_diff_all_archs(
            "libc_and_cpp_with_unused_struct",
            "libc_and_cpp_with_unused_cstruct", 0,
            ['-check-all-apis', '-allow-unreferenced-changes'])

    def test_libc_and_cpp_and_libc_and_cpp_with_unused_struct_check_all_advice(
            self):
        self.prepare_and_run_abi_diff_all_archs(
            "libc_and_cpp", "libc_and_cpp_with_unused_struct", 0,
            ['-check-all-apis', '-advice-only'])

    def test_libc_and_cpp_opaque_pointer_diff(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libc_and_cpp_with_opaque_ptr_a",
            "libc_and_cpp_with_opaque_ptr_b", 8,
            ['-consider-opaque-types-different'], True, True)

    def test_libgolden_cpp_return_type_diff(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_return_type_diff", 8)
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_return_type_diff", 0,
            ["-ignore-linker-set-key", "_ZN17HighVolumeSpeaker6ListenEv",
             "-ignore-linker-set-key", "_ZN16LowVolumeSpeaker6ListenEv"])

    def test_libgolden_cpp_add_odr(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_odr", 0,
            ['-check-all-apis', '-allow-unreferenced-changes'])

    def test_libgolden_cpp_add_function(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_add_function", 6)

    def test_libgolden_cpp_add_function_allow_extension(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_add_function", 0,
            ['-allow-extensions'])

    def test_libgolden_cpp_add_function_and_elf_symbol(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_add_function_and_unexported_elf",
            6)

    def test_libgolden_cpp_fabricated_function_ast_removed_diff(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp_add_function_sybmol_only",
            "libgolden_cpp_add_function", 0, [], False, False)

    def test_libgolden_cpp_change_function_access(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_change_function_access", 8)

    def test_libgolden_cpp_add_global_variable(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_add_global_variable", 6)

    def test_libgolden_cpp_change_global_var_access(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp_add_global_variable",
            "libgolden_cpp_add_global_variable_private", 8)

    def test_libgolden_cpp_parameter_type_diff(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_parameter_type_diff", 8)

    def test_libgolden_cpp_with_vtable_diff(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_vtable_diff", 8)

    def test_libgolden_cpp_member_diff_advice_only(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_member_diff", 0, ['-advice-only'])

    def test_libgolden_cpp_member_diff(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_member_diff", 8)
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_member_diff", 0,
            ["-ignore-linker-set-key", "_ZTI16LowVolumeSpeaker"])

    def test_libgolden_cpp_change_member_access(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_change_member_access", 8)

    def test_libgolden_cpp_enum_extended(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_enum_extended", 4)

    def test_libgolden_cpp_enum_diff(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_enum_diff", 8)
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_enum_diff", 0,
            ["-ignore-linker-set-key", "_ZTIN12SuperSpeaker6VolumeE"])

    def test_libgolden_cpp_member_fake_diff(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_member_fake_diff", 0)

    def test_libgolden_cpp_member_integral_type_diff(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_member_integral_type_diff", 8)

    def test_libgolden_cpp_member_cv_diff(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_member_cv_diff", 8)

    def test_libgolden_cpp_unreferenced_elf_symbol_removed(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_unreferenced_elf_symbol_removed",
            16)

    def test_libgolden_cpp_unreferenced_elf_symbol_added(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp_unreferenced_elf_symbol_removed", "libgolden_cpp",
            2, create_old=True, create_new=False)

    def test_libreproducability(self):
        self.prepare_and_absolute_diff_all_archs(
            "libreproducability", "libreproducability")

    def test_libgolden_cpp_member_name_changed(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_member_name_changed", 0)

    def test_libgolden_cpp_member_function_pointer_changed(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp_function_pointer",
            "libgolden_cpp_function_pointer_parameter_added", 8, [],
            True, True)

    def test_libgolden_cpp_internal_struct_access_upgraded(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp_internal_private_struct",
            "libgolden_cpp_internal_public_struct", 4, [], True, True)

    def test_libgolden_cpp_internal_struct_access_downgraded(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp_internal_public_struct",
            "libgolden_cpp_internal_private_struct", 8, [], True, True)

    def test_libgolden_cpp_inheritance_type_changed(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_inheritance_type_changed", 8, [],
            True, True)

    def test_libpure_virtual_function(self):
        self.prepare_and_absolute_diff_all_archs(
            "libpure_virtual_function", "libpure_virtual_function")

    def test_libc_and_cpp_in_json(self):
        self.prepare_and_absolute_diff_all_archs(
            "libgolden_cpp_json", "libgolden_cpp_json")

    def test_libc_and_cpp_in_protobuf_and_json(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_json", 0,
            ["-input-format-old", "ProtobufTextFormat",
             "-input-format-new", "Json"])

    def test_opaque_type_self_diff(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libopaque_type", "libopaque_type", 0,
            ["-input-format-old", "Json", "-input-format-new", "Json",
             "-consider-opaque-types-different"],
            create_old=False, create_new=False)

    def test_allow_adding_removing_weak_symbols(self):
        module_old = Module.get_test_modules_by_name("libweak_symbols_old")[0]
        module_new = Module.get_test_modules_by_name("libweak_symbols_new")[0]
        lsdump_old = self.get_or_create_dump(module_old, False)
        lsdump_new = self.get_or_create_dump(module_new, False)

        options = ["-input-format-old", "Json", "-input-format-new", "Json"]

        # If `-allow-adding-removing-weak-symbols` is not specified, removing a
        # weak symbol must be treated as an incompatible change.
        self.run_and_compare_abi_diff(
            lsdump_old, lsdump_new, "libweak_symbols", "arm64", 8, options)

        # If `-allow-adding-removing-weak-symbols` is specified, removing a
        # weak symbol must be fine and mustn't be a fatal error.
        self.run_and_compare_abi_diff(
            lsdump_old, lsdump_new, "libweak_symbols", "arm64", 0,
            options + ["-allow-adding-removing-weak-symbols"])

    def test_linker_shared_object_file_and_version_script(self):
        base_dir = os.path.join(
            SCRIPT_DIR, 'integration', 'version_script_example')

        cases = [
            'libversion_script_example',
            'libversion_script_example_no_mytag',
            'libversion_script_example_no_private',
            'libversion_script_example_api_level',
        ]

        for module_name in cases:
            module = Module.get_test_modules_by_name(module_name)[0]
            example_lsdump_old = self.get_or_create_dump(module, False)
            example_lsdump_new = self.get_or_create_dump(module, True)
            self.run_and_compare_abi_diff(
                example_lsdump_old, example_lsdump_new,
                module_name, "arm64", 0,
                ["-input-format-old", "Json", "-input-format-new", "Json"])

    def test_no_source(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libempty", "libempty", 0,
            ["-input-format-old", "Json", "-input-format-new", "Json"])

    def test_golden_anonymous_enum(self):
        self.prepare_and_absolute_diff_all_archs(
            "libgolden_anonymous_enum", "libgolden_anonymous_enum")

    def test_swap_anonymous_enum(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_anonymous_enum", "libswap_anonymous_enum", 0,
            ["-input-format-old", "Json", "-input-format-new", "Json",
             "-check-all-apis"])

    def test_swap_anonymous_enum_field(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_anonymous_enum", "libswap_anonymous_enum_field", 0,
            ["-input-format-old", "Json", "-input-format-new", "Json",
             "-check-all-apis"])

    def test_anonymous_enum_odr(self):
        self.prepare_and_absolute_diff_all_archs(
            "libanonymous_enum_odr", "libanonymous_enum_odr")

    def test_libifunc(self):
        self.prepare_and_absolute_diff_all_archs(
            "libifunc", "libifunc")

    def test_merge_multi_definitions(self):
        self.prepare_and_absolute_diff_all_archs(
            "libmerge_multi_definitions", "libmerge_multi_definitions")
        self.prepare_and_run_abi_diff_all_archs(
            "libmerge_multi_definitions", "libdiff_multi_definitions", 0,
            flags=["-input-format-new", "Json", "-input-format-old", "Json",
                   "-consider-opaque-types-different"],
            create_old=False, create_new=False)

    def test_print_resource_dir(self):
        dumper_path = shutil.which("header-abi-dumper")
        self.assertIsNotNone(dumper_path)
        dumper_path = os.path.realpath(dumper_path)
        common_dir = os.path.dirname(os.path.dirname(dumper_path))
        resource_dir = subprocess.check_output(
            ["header-abi-dumper", "-print-resource-dir"], text=True,
            stderr=subprocess.DEVNULL).strip()
        self.assertIn(os.path.dirname(resource_dir),
                      (os.path.join(common_dir, "lib64", "clang"),
                       os.path.join(common_dir, "lib", "clang")))
        self.assertRegex(os.path.basename(resource_dir), r"^[\d.]+$")

    def test_struct_extensions(self):
        output = self.prepare_and_run_abi_diff_all_archs(
            "libstruct_extensions", "liballowed_struct_extensions", 4,
            flags=["-input-format-new", "Json", "-input-format-old", "Json"],
            create_old=False, create_new=False)
        self.assertEqual(output.count("record_type_extension_diffs"), 6)

        output = self.prepare_and_run_abi_diff_all_archs(
            "liballowed_struct_extensions", "libstruct_extensions", 8,
            flags=["-input-format-new", "Json", "-input-format-old", "Json"],
            create_old=False, create_new=False)
        self.assertEqual(output.count("record_type_diffs"), 6)

    def test_param_size_diff(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libpass_by_value", "libparam_size_diff", 8,
            flags=["-input-format-new", "Json", "-input-format-old", "Json"],
            create_old=False, create_new=False)

    def test_return_size_diff(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libpass_by_value", "libreturn_size_diff", 8,
            flags=["-input-format-new", "Json", "-input-format-old", "Json"],
            create_old=False, create_new=False)

    def test_function_extensions(self):
        diff = self.prepare_and_run_abi_diff_all_archs(
            "libfunction_extensions", "liballowed_function_extensions", 4,
            flags=["-input-format-new", "Json", "-input-format-old", "Json"],
            create_old=False, create_new=False)
        self.assertEqual(6, diff.count('function_extension_diffs'))

        diff = self.prepare_and_run_abi_diff_all_archs(
            "liballowed_function_extensions", "libfunction_extensions", 8,
            flags=["-input-format-new", "Json", "-input-format-old", "Json"],
            create_old=False, create_new=False)
        # Adding and removing __restrict__ at the first level are extensions.
        self.assertEqual(1, diff.count('function_extension_diffs'))
        self.assertEqual(5, diff.count('function_diffs'))

    def test_array_diff(self):
        self.prepare_and_absolute_diff_all_archs("libarray", "libarray")
        self.prepare_and_absolute_diff_all_archs(
            "libarray_diff", "libarray_diff")
        diff = self.prepare_and_run_abi_diff_all_archs(
            "libarray", "libarray_diff", 8,
            flags=["-input-format-new", "Json", "-input-format-old", "Json"],
            create_old=False, create_new=False)

        for type_id in ["StructMember", "Pointer", "DoublePointer"]:
            self.assertNotIn(f'"{type_id}"', diff,
                             f'"{type_id}" should not be in the diff report.')

        for type_id in ["Struct", "PointerToArray", "PointerTo2DArray",
                        "Reference", "Element"]:
            self.assertIn(f'"{type_id}"', diff,
                          f'"{type_id}" should be in the diff report.')

    def test_union_diff(self):
        diff = self.prepare_and_run_abi_diff_all_archs(
            "libunion", "libunion_diff", 8,
            flags=["-input-format-new", "Json", "-input-format-old", "Json"],
            create_old=False, create_new=True)
        self.assertIn('"ChangeType"', diff)
        self.assertIn('"ChangeTypeInStruct"', diff)
        self.assertEqual(2, diff.count("fields_diff"))
        self.assertNotIn("fields_added", diff)
        self.assertNotIn("fields_removed", diff)

    def test_enum_diff(self):
        self.prepare_and_absolute_diff_all_archs("libenum", "libenum")

    def test_io_error(self):
        cpp_path = os.path.join(self.get_tmp_dir(), "test.cpp")
        sdump_path = os.path.join(self.get_tmp_dir(), "test.sdump")
        version_script_path = os.path.join(self.get_tmp_dir(), "map.txt")
        lsdump_path = os.path.join(self.get_tmp_dir(), "test.lsdump")
        abidiff_path = os.path.join(self.get_tmp_dir(), "test.abidiff")
        read_only_path = os.path.join(self.get_tmp_dir(), "read_only.txt")

        with open(cpp_path, "w") as cpp_file:
            cpp_file.write("void func(int) {}")
        with open(version_script_path, "w") as version_script_file:
            pass
        fd = os.open(read_only_path, flags=(os.O_CREAT | os.O_EXCL),
                     mode=(stat.S_IROTH | stat.S_IRGRP | stat.S_IRUSR))
        self.assertGreaterEqual(fd, 0)
        os.close(fd)

        # Make sure that the commands are valid.
        dumper_flags = ('-output-format', 'Json')
        run_header_abi_dumper(cpp_path, sdump_path, flags=dumper_flags)
        self.assertGreater(os.stat(sdump_path).st_size, 0)
        linker_flags = ('-input-format', 'Json',
                        '-output-format', 'ProtobufTextFormat')
        run_header_abi_linker([sdump_path], lsdump_path, version_script_path,
                              "current", "x86_64", linker_flags)
        self.assertGreater(os.stat(lsdump_path).st_size, 0)
        diff_flags = ('-input-format-old', 'ProtobufTextFormat',
                      '-input-format-new', 'ProtobufTextFormat')
        return_code = run_abi_diff(lsdump_path, lsdump_path, abidiff_path,
                                   'x86_64', 'libtest', diff_flags)
        self.assertEqual(return_code, 0)
        self.assertGreater(os.stat(abidiff_path).st_size, 0)

        # Test with output error.
        with self.assertRaises(subprocess.CalledProcessError) as assertion:
            run_header_abi_dumper(cpp_path, read_only_path, flags=dumper_flags)
        self.assertEqual(assertion.exception.returncode, 1)

        with self.assertRaises(subprocess.CalledProcessError) as assertion:
            run_header_abi_linker([sdump_path], read_only_path,
                                  version_script_path, "current", "x86_64",
                                  linker_flags)
        self.assertEqual(assertion.exception.returncode, 255)

        return_code = run_abi_diff(lsdump_path, lsdump_path, read_only_path,
                                   'x86_64', 'libtest', diff_flags)
        self.assertEqual(return_code, 1)

    def test_bit_field_diff(self):
        self.prepare_and_absolute_diff_all_archs(
            "libbit_field", "libbit_field")
        self.prepare_and_run_abi_diff_all_archs(
            "libbit_field", "libbit_field_diff", 8,
            flags=["-input-format-new", "Json", "-input-format-old", "Json"],
            create_old=False, create_new=True)

    def test_availability(self):
        self.prepare_and_absolute_diff_all_archs(
            "libavailability", "libavailability")
        self.prepare_and_absolute_diff_all_archs(
            "libavailability_35", "libavailability_35")


if __name__ == '__main__':
    unittest.main()
