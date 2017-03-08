#!/usr/bin/env python3

from __future__ import print_function

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import argparse
import unittest

from compat import TemporaryDirectory, makedirs
from vndk_definition_tool import GenericRefs


test_dir_base = None


class MockELF(object):
    def __init__(self, exported_symbols):
        self.exported_symbols = exported_symbols


class MockLib(object):
    def __init__(self, path, exported_symbols):
        self.path = path
        self.elf = MockELF(exported_symbols)


class GenericRefsTest(unittest.TestCase):
    def _build_file_fixture(self, path, content):
        makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w') as f:
            f.write(content)

    def _build_dir_fixtures(self, test_dir):
        lib32 = os.path.join(test_dir, 'system', 'lib')
        lib64 = os.path.join(test_dir, 'system', 'lib64')

        for lib_dir in (lib32, lib64):
            self._build_file_fixture(os.path.join(lib_dir, 'libc.so.sym'),
                                     'fclose\nfopen\nfread\nfwrite\n')
            self._build_file_fixture(os.path.join(lib_dir, 'libm.so.sym'),
                                     'cos\nsin\ntan\n')

    def _build_fixture(self):
        res = GenericRefs()
        res.add('/system/lib/libc.so', {'fclose', 'fopen', 'fread', 'fwrite'})
        res.add('/system/lib/libm.so', {'cos', 'sin', 'tan'})
        res.add('/system/lib64/libc.so', {'fclose', 'fopen', 'fread', 'fwrite'})
        res.add('/system/lib64/libm.so', {'cos', 'sin', 'tan'})
        return res

    def test_create_from_dir(self):
        try:
            if test_dir_base:
                test_dir = test_dir_base
            else:
                tmp_dir = TemporaryDirectory()
                test_dir = tmp_dir.name

            self._build_dir_fixtures(test_dir)
            g = GenericRefs.create_from_dir(test_dir)
            self.assertEqual(4, len(g.refs))

            self.assertIn('/system/lib/libc.so', g.refs)
            self.assertIn('/system/lib/libm.so', g.refs)
            self.assertIn('/system/lib64/libc.so', g.refs)
            self.assertIn('/system/lib64/libm.so', g.refs)

            self.assertEqual({'fclose', 'fopen', 'fread', 'fwrite'},
                             g.refs['/system/lib/libc.so'])
            self.assertEqual({'fclose', 'fopen', 'fread', 'fwrite'},
                             g.refs['/system/lib64/libc.so'])

            self.assertEqual({'cos', 'sin', 'tan'},
                             g.refs['/system/lib/libm.so'])
            self.assertEqual({'cos', 'sin', 'tan'},
                             g.refs['/system/lib64/libm.so'])
        finally:
            if not test_dir_base:
                tmp_dir.cleanup()

    def test_classify_lib(self):
        g = self._build_fixture()

        libc_sub = MockLib('/system/lib/libc.so', {'fclose', 'fopen', 'fread'})
        libc_sup = MockLib('/system/lib/libc.so',
                           {'fclose', 'fopen', 'fread', 'fwrite', 'open'})
        libc_eq = MockLib('/system/lib/libc.so',
                          {'fclose', 'fopen', 'fread', 'fwrite'})
        libfoo = MockLib('/system/lib/libfoo.so', {})

        self.assertEqual(GenericRefs.MODIFIED, g.classify_lib(libc_sub))
        self.assertEqual(GenericRefs.EXPORT_SUPER_SET, g.classify_lib(libc_sup))
        self.assertEqual(GenericRefs.EXPORT_EQUAL, g.classify_lib(libc_eq))
        self.assertEqual(GenericRefs.NEW_LIB, g.classify_lib(libfoo))

    def test_is_equivalent_lib(self):
        g = self._build_fixture()

        libc_sub = MockLib('/system/lib/libc.so', {'fclose', 'fopen', 'fread'})
        libc_sup = MockLib('/system/lib/libc.so',
                           {'fclose', 'fopen', 'fread', 'fwrite', 'open'})
        libc_eq = MockLib('/system/lib/libc.so',
                          {'fclose', 'fopen', 'fread', 'fwrite'})

        self.assertFalse(g.is_equivalent_lib(libc_sub))
        self.assertFalse(g.is_equivalent_lib(libc_sup))

        self.assertTrue(g.is_equivalent_lib(libc_eq))


def main():
    # Parse command line arguments.
    parser = argparse.ArgumentParser()
    parser.add_argument('--test-dir', help='directory for temporary files')
    args, unittest_args = parser.parse_known_args()

    # Convert command line options.
    global test_dir_base

    if args.test_dir:
        test_dir_base = args.test_dir

    # Run unit test.
    unittest.main(argv=[sys.argv[0]] + unittest_args)

if __name__ == '__main__':
    main()
