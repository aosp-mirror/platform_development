#!/usr/bin/env python3

from __future__ import print_function

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import argparse
import unittest

from compat import TemporaryDirectory, makedirs
from vndk_definition_tool import GenericRefs


test_dir = None

class GenericRefsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if test_dir:
            cls.test_dir = test_dir
        else:
            cls.tmp_dir = TemporaryDirectory()
            cls.test_dir = cls.tmp_dir.name

        cls._build_fixtures()

    @classmethod
    def tearDownClass(cls):
        if not test_dir:
            cls.tmp_dir.cleanup()

    @classmethod
    def _build_fixture(cls, path, content):
        makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w') as f:
            f.write(content)

    @classmethod
    def _build_fixtures(cls):
        lib32 = os.path.join(cls.test_dir, 'system', 'lib')
        lib64 = os.path.join(cls.test_dir, 'system', 'lib64')

        for lib_dir in (lib32, lib64):
            cls._build_fixture(os.path.join(lib_dir, 'libc.so.sym'),
                               'fclose\nfopen\nfread\nfwrite\n')
            cls._build_fixture(os.path.join(lib_dir, 'libm.so.sym'),
                               'cos\nsin\ntan\n')

    def test_create_from_dir(self):
        g = GenericRefs.create_from_dir(self.test_dir)
        self.assertEqual(4, len(g.refs))

        self.assertIn('/system/lib/libc.so', g.refs)
        self.assertIn('/system/lib/libm.so', g.refs)
        self.assertIn('/system/lib64/libc.so', g.refs)
        self.assertIn('/system/lib64/libm.so', g.refs)

        self.assertEqual(['fclose', 'fopen', 'fread', 'fwrite'],
                         g.refs['/system/lib/libc.so'])
        self.assertEqual(['fclose', 'fopen', 'fread', 'fwrite'],
                         g.refs['/system/lib64/libc.so'])

        self.assertEqual(['cos', 'sin', 'tan'],
                         g.refs['/system/lib/libm.so'])
        self.assertEqual(['cos', 'sin', 'tan'],
                         g.refs['/system/lib64/libm.so'])


    def test_is_equivalent_lib(self):
        g = GenericRefs.create_from_dir(self.test_dir)

        class MockELF(object):
            def __init__(self, exported_symbols):
                self.exported_symbols = exported_symbols

        class MockLib(object):
            def __init__(self, path, exported_symbols):
                self.path = path
                self.elf = MockELF(exported_symbols)

        libc_sub = MockLib('/system/lib/libc.so', ['fclose', 'fopen', 'fread'])
        libc_sup = MockLib('/system/lib/libc.so',
                           ['fclose', 'fopen', 'fread', 'fwrite', 'open'])
        libc_eq = MockLib('/system/lib/libc.so',
                          ['fclose', 'fopen', 'fread', 'fwrite'])

        self.assertFalse(g.is_equivalent_lib(libc_sub))
        self.assertFalse(g.is_equivalent_lib(libc_sup))

        self.assertTrue(g.is_equivalent_lib(libc_eq))


def main():
    # Parse command line arguments.
    parser = argparse.ArgumentParser()
    parser.add_argument('--test-dir', help='directory for temporary files')
    args, unittest_args = parser.parse_known_args()

    # Convert command line options.
    global test_dir

    if args.test_dir:
        test_dir = args.test_dir

    # Run unit test.
    unittest.main(argv=[sys.argv[0]] + unittest_args)

if __name__ == '__main__':
    main()
