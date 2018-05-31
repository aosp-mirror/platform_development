#!/usr/bin/env python3

import os
import unittest
import sys
import tempfile

import_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
import_path = os.path.abspath(os.path.join(import_path, 'utils'))
sys.path.insert(1, import_path)

from utils import (
        AOSP_DIR, SOURCE_ABI_DUMP_EXT, TARGET_ARCHS, read_output_content,
        run_abi_diff, run_header_abi_dumper)
from module import Module
from gen_all import make_and_copy_reference_dumps

SCRIPT_DIR = os.path.abspath(os.path.dirname(__file__))
INPUT_DIR = os.path.join(SCRIPT_DIR, 'input')
EXPECTED_DIR = os.path.join(SCRIPT_DIR, 'expected')
REF_DUMP_DIR = os.path.join(SCRIPT_DIR,  'reference_dumps')

class MyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.maxDiff = None

    def get_reference_dump_path(self, name, target_arch):
        ref_dump_dir = os.path.join(REF_DUMP_DIR, target_arch)
        ref_dump_path = os.path.join(ref_dump_dir,
                                     name + SOURCE_ABI_DUMP_EXT)
        return ref_dump_path

    def run_and_compare(self, input_path, expected_path, cflags=[]):
        with open(expected_path, 'r') as f:
            expected_output = f.read()
        actual_output = run_header_abi_dumper(input_path, True, cflags)
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
                                 expected_return_code, flags=[]) :
        actual_output = run_abi_diff(old_dump, new_dump, arch, lib, flags)
        self.assertEqual(actual_output, expected_return_code)

    def prepare_and_run_abi_diff(self, old_ref_dump_path, new_ref_dump_path,
                                 target_arch, expected_return_code, flags=[]):
        self.run_and_compare_abi_diff(old_ref_dump_path, new_ref_dump_path,
                                      'test', target_arch, expected_return_code,
                                      flags)

    def create_ref_dump(self, name, dir_name, target_arch):
        module_bare = Module.get_test_module_by_name(name)
        module = Module.mutate_module_for_arch(module_bare, target_arch)
        return make_and_copy_reference_dumps(module, [],
                                             dir_name)

    def get_or_create_ref_dump(self, name, target_arch, dir_name, create):
        if create == True:
            return self.create_ref_dump(name, dir_name, target_arch)
        return self.get_reference_dump_path(name, target_arch)

    def prepare_and_run_abi_diff_all_archs(self, old_lib, new_lib,
                                           expected_return_code, flags=[],
                                           create_old=False, create_new=True):
        with tempfile.TemporaryDirectory() as tmp:
            for target_arch in TARGET_ARCHS:
                old_ref_dump_path = self.get_or_create_ref_dump(old_lib,
                                                                target_arch,
                                                                tmp, create_old)
                new_ref_dump_path = self.get_or_create_ref_dump(new_lib,
                                                                target_arch,
                                                                tmp, create_new)
                self.prepare_and_run_abi_diff(old_ref_dump_path,
                                              new_ref_dump_path, target_arch,
                                              expected_return_code, flags)

    def prepare_and_absolute_diff_all_archs(self, old_lib, new_lib,
                                            flags=[], create=True):
        with tempfile.TemporaryDirectory() as tmp:
            for target_arch in TARGET_ARCHS:
                old_ref_dump_path = self.get_or_create_ref_dump(old_lib,
                                                                target_arch,
                                                                tmp, False)
                new_ref_dump_path = self.get_or_create_ref_dump(new_lib,
                                                                target_arch,
                                                                tmp, create)
                self.assertEqual(
                    read_output_content(old_ref_dump_path, AOSP_DIR),
                    read_output_content(new_ref_dump_path, AOSP_DIR))

    def test_func_decl_no_args(self):
        self.run_and_compare_name_c_cpp('func_decl_no_args.h')

    def test_func_decl_one_arg(self):
        self.run_and_compare_name_c_cpp('func_decl_one_arg.h')

    def test_func_decl_two_args(self):
        self.run_and_compare_name_c_cpp('func_decl_two_args.h')

    def test_func_decl_one_arg_ret(self):
        self.run_and_compare_name_c_cpp('func_decl_one_arg_ret.h')

    def test_example1(self):
        self.run_and_compare_name_cpp('example1.h')
        self.run_and_compare_name_cpp('example2.h')

    def test_libc_and_cpp(self):
        self.prepare_and_run_abi_diff_all_archs("libc_and_cpp", "libc_and_cpp",
                                                 0)

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

    def test_libc_and_cpp_with_unused_struct_and_libc_and_cpp_with_unused_cstruct(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libc_and_cpp_with_unused_struct",
            "libc_and_cpp_with_unused_cstruct", 0,
            ['-check-all-apis', '-allow-unreferenced-changes'])

    def test_libc_and_cpp_and_libc_and_cpp_with_unused_struct_check_all_advice(
        self):
        self.prepare_and_run_abi_diff_all_archs(
            "libc_and_cpp", "libc_and_cpp_with_unused_struct", 0,
            ['-check-all-apis', '-advice-only'])

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
        self.prepare_and_run_abi_diff_all_archs("libgolden_cpp",
                                                "libgolden_cpp_vtable_diff", 8)

    def test_libgolden_cpp_member_diff_advice_only(self):
        self.prepare_and_run_abi_diff_all_archs("libgolden_cpp",
                                                "libgolden_cpp_member_diff",
                                                 0, ['-advice-only'])

    def test_libgolden_cpp_member_diff(self):
        self.prepare_and_run_abi_diff_all_archs("libgolden_cpp",
                                                "libgolden_cpp_member_diff", 8)

    def test_libgolden_cpp_change_member_access(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp", "libgolden_cpp_change_member_access", 8)

    def test_libgolden_cpp_enum_extended(self):
        self.prepare_and_run_abi_diff_all_archs("libgolden_cpp",
                                                "libgolden_cpp_enum_extended",
                                                4)
    def test_libgolden_cpp_enum_diff(self):
        self.prepare_and_run_abi_diff_all_archs("libgolden_cpp",
                                                "libgolden_cpp_enum_diff", 8)

    def test_libgolden_cpp_fabricated_function_ast_removed_diff(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp_fabricated_function_ast_removed", "libgolden_cpp", 0,
            [], False)

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
            "libgolden_cpp",
            "libgolden_cpp_unreferenced_elf_symbol_removed", 16)

    def test_libreproducability(self):
        self.prepare_and_absolute_diff_all_archs("libreproducability",
                                                 "libreproducability")

    def test_libgolden_cpp_member_name_changed(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp",
            "libgolden_cpp_member_name_changed", 0)

    def test_libgolden_cpp_member_function_pointer_changed(self):
        self.prepare_and_run_abi_diff_all_archs(
            "libgolden_cpp_function_pointer",
            "libgolden_cpp_function_pointer_parameter_added", 8, [], False,
            False)



if __name__ == '__main__':
    unittest.main()
