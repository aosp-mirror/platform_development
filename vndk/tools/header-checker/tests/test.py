#!/usr/bin/env python3

import os
import unittest
import sys
import tempfile

import_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
import_path = os.path.abspath(os.path.join(import_path, 'utils'))
sys.path.insert(1, import_path)

from utils import (AOSP_DIR, read_output_content, run_abi_diff,
                   run_header_abi_dumper)
from module import Module


SCRIPT_DIR = os.path.abspath(os.path.dirname(__file__))
INPUT_DIR = os.path.join(SCRIPT_DIR, 'input')
EXPECTED_DIR = os.path.join(SCRIPT_DIR, 'expected')
REF_DUMP_DIR = os.path.join(SCRIPT_DIR, 'reference_dumps')


def make_and_copy_reference_dumps(module, reference_dump_dir=REF_DUMP_DIR):
    output_content = module.make_dump()

    dump_dir = os.path.join(reference_dump_dir, module.arch)
    os.makedirs(dump_dir, exist_ok=True)

    dump_path = os.path.join(dump_dir, module.get_dump_name())
    with open(dump_path, 'w') as f:
        f.write(output_content)

    return dump_path


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
        actual_output = run_header_abi_dumper(input_path, cflags)
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
        actual_output = run_abi_diff(old_dump, new_dump, arch, lib, flags)
        self.assertEqual(actual_output, expected_return_code)

    def prepare_and_run_abi_diff(self, old_ref_dump_path, new_ref_dump_path,
                                 target_arch, expected_return_code, flags=[]):
        self.run_and_compare_abi_diff(old_ref_dump_path, new_ref_dump_path,
                                      'test', target_arch,
                                      expected_return_code, flags)

    def get_or_create_ref_dump(self, module, create):
        if create:
            return make_and_copy_reference_dumps(module, self.get_tmp_dir())
        return os.path.join(REF_DUMP_DIR, module.arch, module.get_dump_name())

    def prepare_and_run_abi_diff_all_archs(self, old_lib, new_lib,
                                           expected_return_code, flags=[],
                                           create_old=False, create_new=True):
        old_modules = Module.get_test_modules_by_name(old_lib)
        new_modules = Module.get_test_modules_by_name(new_lib)
        self.assertEqual(len(old_modules), len(new_modules))

        for old_module, new_module in zip(old_modules, new_modules):
            self.assertEqual(old_module.arch, new_module.arch)
            old_ref_dump_path = self.get_or_create_ref_dump(old_module,
                                                            create_old)
            new_ref_dump_path = self.get_or_create_ref_dump(new_module,
                                                            create_new)
            self.prepare_and_run_abi_diff(
                old_ref_dump_path, new_ref_dump_path, new_module.arch,
                expected_return_code, flags)

    def prepare_and_absolute_diff_all_archs(self, old_lib, new_lib):
        old_modules = Module.get_test_modules_by_name(old_lib)
        new_modules = Module.get_test_modules_by_name(new_lib)
        self.assertEqual(len(old_modules), len(new_modules))

        for old_module, new_module in zip(old_modules, new_modules):
            self.assertEqual(old_module.arch, new_module.arch)
            old_ref_dump_path = self.get_or_create_ref_dump(old_module, False)
            new_ref_dump_path = self.get_or_create_ref_dump(new_module, True)
            self.assertEqual(
                read_output_content(old_ref_dump_path, AOSP_DIR),
                read_output_content(new_ref_dump_path, AOSP_DIR))

    def test_example1_cpp(self):
        self.run_and_compare_name_cpp('example1.cpp')

    def test_example1_h(self):
        self.run_and_compare_name_cpp('example1.h')

    def test_example2_h(self):
        self.run_and_compare_name_cpp('example2.h')

    def test_example3_h(self):
        self.run_and_compare_name_cpp('example3.h')

    def test_undeclared_types_h(self):
        self.prepare_and_absolute_diff_all_archs(
            'undeclared_types.h', 'undeclared_types.h')

    def test_known_issues_h(self):
        self.prepare_and_absolute_diff_all_archs(
            'known_issues.h', 'known_issues.h')

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
            ['-check-all-apis'])

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

    def test_libgolden_cpp_add_odr(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_odr", 0,
            ['-check-all-apis', '-allow-unreferenced-changes'])

    def test_libgolden_cpp_add_function(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_add_function", 4)

    def test_libgolden_cpp_add_function_allow_extension(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_add_function", 0,
            ['-allow-extensions'])

    def test_libgolden_cpp_add_function_and_elf_symbol(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_add_function_and_unexported_elf",
            4)

    def test_libgolden_cpp_fabricated_function_ast_removed_diff(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp_add_function_sybmol_only",
            "libgolden_cpp_add_function", 0, [], False, False)

    def test_libgolden_cpp_change_function_access(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_change_function_access", 8)

    def test_libgolden_cpp_add_global_variable(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_add_global_variable", 4)

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

    def test_libgolden_cpp_change_member_access(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_change_member_access", 8)

    def test_libgolden_cpp_enum_extended(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_enum_extended", 4)

    def test_libgolden_cpp_enum_diff(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_enum_diff", 8)

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
            "libgolden_cpp_internal_public_struct", 0, [], True, True)

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
        lsdump = os.path.join(
            SCRIPT_DIR, "abi_dumps", "opaque_ptr_types.lsdump")
        self.run_and_compare_abi_diff(
            lsdump, lsdump, "libexample", "arm64", 0,
            ["-input-format-old", "Json", "-input-format-new", "Json",
             "-consider-opaque-types-different"])

    def test_allow_adding_removing_weak_symbols(self):
        module_old = Module.get_test_modules_by_name("libweak_symbols_old")[0]
        module_new = Module.get_test_modules_by_name("libweak_symbols_new")[0]
        lsdump_old = self.get_or_create_ref_dump(module_old, False)
        lsdump_new = self.get_or_create_ref_dump(module_new, False)

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
        ]

        for module_name in cases:
            module = Module.get_test_modules_by_name(module_name)[0]
            example_lsdump_old = self.get_or_create_ref_dump(module, False)
            example_lsdump_new = self.get_or_create_ref_dump(module, True)
            self.run_and_compare_abi_diff(
                example_lsdump_old, example_lsdump_new,
                module_name, "arm64", 0,
                ["-input-format-old", "Json", "-input-format-new", "Json"])


if __name__ == '__main__':
    unittest.main()
