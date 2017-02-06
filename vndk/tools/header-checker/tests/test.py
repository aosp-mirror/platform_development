#!/usr/bin/env python3

import os
import unittest

from utils import run_header_checker

SCRIPT_DIR = os.path.abspath(os.path.dirname(__file__))
INPUT_DIR = os.path.join(SCRIPT_DIR, 'input')
EXPECTED_DIR = os.path.join(SCRIPT_DIR, 'expected')

class MyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.maxDiff = None

    def run_and_compare(self, input_path, expected_path, cflags=[]):
        with open(expected_path, 'r') as f:
            expected_output = f.read()
        actual_output = run_header_checker(input_path, cflags)
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

if __name__ == '__main__':
    unittest.main()
