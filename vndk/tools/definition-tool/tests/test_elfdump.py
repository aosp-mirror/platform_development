#!/usr/bin/env python3

from __future__ import print_function

import argparse
import collections
import difflib
import os
import subprocess
import sys
import unittest

import targets

try:
    from tempfile import TemporaryDirectory
except ImportError:
    import shutil
    import tempfile

    class TemporaryDirectory(object):
        def __init__(self, suffix='', prefix='tmp', dir=None):
            self.name = tempfile.mkdtemp(suffix, prefix, dir)

        def __del__(self):
            self.cleanup()

        def __enter__(self):
            return self.name

        def __exit__(self, exc, value, tb):
            self.cleanup()

        def cleanup(self):
            if self.name:
                shutil.rmtree(self.name)
                self.name = None

if sys.version_info >= (3, 0):
    from os import makedirs
else:
    def makedirs(path, exist_ok):
        if exist_ok and os.path.exists(path):
            return
        return os.makedirs(path)

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
VNDK_DEF_TOOL = os.path.join(SCRIPT_DIR, '..', 'vndk_definition_tool.py')

expected_dir = os.path.join(SCRIPT_DIR, 'expected')
test_dir_base = None

def run_elf_dump(path):
    cmd = [sys.executable, VNDK_DEF_TOOL, 'elfdump', path]
    return subprocess.check_output(cmd, universal_newlines=True)


class ELFDumpTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.targets = targets.create_targets()

    def setUp(self):
        if test_dir_base:
            self.test_dir_base = test_dir_base
        else:
            self.tmp_dir = TemporaryDirectory()
            self.test_dir_base = self.tmp_dir.name

    def tearDown(self):
        if not test_dir_base:
            self.tmp_dir.cleanup()

    def _prepare_dir(self, target_name):
        self.expected_dir = os.path.join(expected_dir, target_name)
        self.test_dir = os.path.join(self.test_dir_base, target_name)
        makedirs(self.test_dir, exist_ok=True)

    def _assert_equal_to_file(self, expected_file_name, actual):
        actual = actual.splitlines(True)
        expected_file_path = os.path.join(self.expected_dir, expected_file_name)
        with open(expected_file_path, 'r') as f:
            expected = f.readlines()
        self.assertEqual(expected, actual)

    def _test_main_out(self, target):
        self._prepare_dir(target.name)

        src_file = os.path.join(SCRIPT_DIR, 'input', 'main.c')
        obj_file = os.path.join(self.test_dir, 'main.o')
        target.compile(obj_file, src_file, [])

        out_file = os.path.join(self.test_dir, 'main.out')
        target.link(out_file, [obj_file], ['-ldl', '-lc', '-lstdc++'])
        self._assert_equal_to_file('main.out.txt', run_elf_dump(out_file))

    def _test_libtest(self, target, ldflags, output_name, expected_file_name):
        self._prepare_dir(target.name)

        src_file = os.path.join(SCRIPT_DIR, 'input', 'test.c')
        obj_file = os.path.join(self.test_dir, 'test.o')
        target.compile(obj_file, src_file, [])

        out_file = os.path.join(self.test_dir, output_name)
        target.link(out_file, [obj_file], ['-shared', '-lc'] + ldflags)
        self._assert_equal_to_file(expected_file_name, run_elf_dump(out_file))


def create_target_test(target_name):
    def test_main(self):
        self._test_main_out(self.targets[target_name])

    def test_libtest(self):
        self._test_libtest(
                self.targets[target_name], [], 'libtest.so', 'libtest.so.txt')

    def test_libtest_rpath(self):
        self._test_libtest(
                self.targets[target_name], ['-Wl,-rpath,$ORIGIN/../lib'],
                'libtest-rpath.so', 'libtest-rpath.so.txt')

    def test_libtest_runpath(self):
        self._test_libtest(
                self.targets[target_name],
                ['-Wl,-rpath,$ORIGIN/../lib', '-Wl,--enable-new-dtags'],
                'libtest-runpath.so', 'libtest-runpath.so.txt')

    class_name = 'ELFDumpTest_' + target_name
    globals()[class_name] = type(
            class_name, (ELFDumpTest,),
            dict(test_main=test_main,
                 test_libtest=test_libtest,
                 test_libtest_rpath=test_libtest_rpath,
                 test_libtest_runpath=test_libtest_runpath))


for target in ('arm', 'arm64', 'mips', 'mips64', 'x86', 'x86_64'):
    create_target_test(target)


def main():
    # Parse command line arguments.
    parser = argparse.ArgumentParser()
    parser.add_argument('--test-dir',
                        help='directory for temporary files')
    parser.add_argument('--expected-dir', help='directory with expected output')
    args, unittest_args = parser.parse_known_args()

    # Convert command line options.
    global expected_dir
    global test_dir_base

    if args.expected_dir:
        expected_dir = args.expected_dir
    if args.test_dir:
        test_dir_base = args.test_dir
        makedirs(test_dir_base, exist_ok=True)

    # Run unit test.
    unittest.main(argv=[sys.argv[0]] + unittest_args)

if __name__ == '__main__':
    sys.exit(main())
