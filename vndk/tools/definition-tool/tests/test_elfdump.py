#!/usr/bin/env python3

from __future__ import print_function

import argparse
import collections
import difflib
import os
import re
import subprocess
import sys
import unittest

from compat import TemporaryDirectory, makedirs
import ndk_toolchain


SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
VNDK_DEF_TOOL = os.path.join(SCRIPT_DIR, '..', 'vndk_definition_tool.py')

INPUT_DIR = os.path.join(SCRIPT_DIR ,'testdata', 'test_elfdump', 'input')
EXPECTED_DIR = os.path.join(SCRIPT_DIR, 'testdata', 'test_elfdump', 'expected')
test_dir_base = None


def run_elf_dump(path):
    cmd = [sys.executable, VNDK_DEF_TOOL, 'elfdump', path]
    return subprocess.check_output(cmd, universal_newlines=True)


class ELFDumpTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.targets = ndk_toolchain.create_targets()

        if test_dir_base:
            cls.test_dir_base = test_dir_base
        else:
            cls.tmp_dir = TemporaryDirectory()
            cls.test_dir_base = cls.tmp_dir.name

        cls._build_fixtures(cls.target_name)

    @classmethod
    def tearDownClass(cls):
        if not test_dir_base:
            cls.tmp_dir.cleanup()

    @classmethod
    def _build_fixtures(cls, target_name):
        target = cls.targets[target_name]

        cls.expected_dir = os.path.join(EXPECTED_DIR, target_name)
        cls.test_dir = os.path.join(cls.test_dir_base, target_name)

        makedirs(cls.test_dir, exist_ok=True)

        # Compile main.o.
        src_file = os.path.join(INPUT_DIR, 'main.c')
        obj_file = os.path.join(cls.test_dir, 'main.o')
        target.compile(obj_file, src_file, [])

        # Link main.out.
        out_file = os.path.join(cls.test_dir, 'main.out')
        target.link(out_file, [obj_file], ['-ldl', '-lc', '-lstdc++'])

        # Compile test.o.
        src_file = os.path.join(INPUT_DIR, 'test.c')
        obj_file = os.path.join(cls.test_dir, 'test.o')
        target.compile(obj_file, src_file, [])

        # Link libtest.so.
        out_file = os.path.join(cls.test_dir, 'libtest.so')
        target.link(out_file, [obj_file], ['-shared', '-lc'])

        # Link libtest-rpath.so.
        out_file = os.path.join(cls.test_dir, 'libtest-rpath.so')
        target.link(out_file, [obj_file],
                    ['-shared', '-lc', '-Wl,-rpath,$ORIGIN/../lib',
                     '-Wl,--disable-new-dtags'])

        # Link libtest-rpath-multi.so.
        out_file = os.path.join(cls.test_dir, 'libtest-rpath-multi.so')
        target.link(out_file, [obj_file],
                    ['-shared', '-lc', '-Wl,-rpath,/system/lib:/vendor/lib',
                     '-Wl,--disable-new-dtags'])

        # Link libtest-runpath.so.
        out_file = os.path.join(cls.test_dir, 'libtest-runpath.so')
        target.link(out_file, [obj_file],
                    ['-shared', '-lc', '-Wl,-rpath,$ORIGIN/../lib',
                     '-Wl,--enable-new-dtags'])

        # Link libtest-runpath-multi.so.
        out_file = os.path.join(cls.test_dir, 'libtest-runpath-multi.so')
        target.link(out_file, [obj_file],
                    ['-shared', '-lc', '-Wl,-rpath,/system/lib:/vendor/lib',
                     '-Wl,--enable-new-dtags'])

    def _remove_size_lines(self, lines):
        """Remove file size information because they may vary."""
        prefixes = (
            'FILE_SIZE\t',
            'RO_SEG_FILE_SIZE\t',
            'RO_SEG_MEM_SIZE\t',
            'RW_SEG_FILE_SIZE\t',
            'RW_SEG_MEM_SIZE\t',
        )
        patt = re.compile('|'.join('(?:' + re.escape(x) +')' for x in prefixes))
        return [line for line in lines if not patt.match(line)]

    def _assert_equal_to_file(self, expected_file_name, actual):
        actual = actual.splitlines(True)
        expected_file_path = os.path.join(self.expected_dir, expected_file_name)
        with open(expected_file_path, 'r') as f:
            expected = f.readlines()
        self.assertEqual(self._remove_size_lines(expected),
                         self._remove_size_lines(actual))

    def _test_main_out(self):
        out_file = os.path.join(self.test_dir, 'main.out')
        self._assert_equal_to_file('main.out.txt', run_elf_dump(out_file))

    def _test_libtest(self, expected_file_name, lib_name):
        lib_file = os.path.join(self.test_dir, lib_name)
        self._assert_equal_to_file(expected_file_name, run_elf_dump(lib_file))


def create_target_test(target_name):
    def test_main(self):
        self._test_main_out()

    def test_libtest(self):
        self._test_libtest('libtest.so.txt', 'libtest.so')

    def test_libtest_rpath(self):
        self._test_libtest('libtest-rpath.so.txt', 'libtest-rpath.so')

    def test_libtest_rpath_multi(self):
        self._test_libtest('libtest-rpath-multi.so.txt',
                           'libtest-rpath-multi.so')

    def test_libtest_runpath(self):
        self._test_libtest('libtest-runpath.so.txt', 'libtest-runpath.so')

    def test_libtest_runpath_multi(self):
        self._test_libtest('libtest-runpath-multi.so.txt',
                           'libtest-runpath-multi.so')

    class_name = 'ELFDumpTest_' + target_name
    globals()[class_name] = type(
            class_name, (ELFDumpTest,),
            dict(test_main=test_main,
                 test_libtest=test_libtest,
                 test_libtest_rpath=test_libtest_rpath,
                 test_libtest_rpath_multi=test_libtest_rpath_multi,
                 test_libtest_runpath=test_libtest_runpath,
                 test_libtest_runpath_multi=test_libtest_runpath_multi,
                 target_name=target_name))


for target in ('arm', 'arm64', 'mips', 'mips64', 'x86', 'x86_64'):
    create_target_test(target)


def main():
    # Parse command line arguments.
    parser = argparse.ArgumentParser()
    parser.add_argument('--test-dir', help='directory for temporary files')
    parser.add_argument('--expected-dir', help='directory with expected output')
    args, unittest_args = parser.parse_known_args()

    # Convert command line options.
    global expected_dir
    global test_dir_base

    if args.expected_dir:
        expected_dir = args.expected_dir
    if args.test_dir:
        test_dir_base = args.test_dir

    # Run unit test.
    unittest.main(argv=[sys.argv[0]] + unittest_args)

if __name__ == '__main__':
    main()
