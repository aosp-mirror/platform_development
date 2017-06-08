#!/usr/bin/env python3

from __future__ import print_function

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import argparse
import unittest

from compat import TemporaryDirectory, makedirs
from vndk_definition_tool import GenericRefs


SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))


class MockELF(object):
    def __init__(self, exported_symbols):
        self.exported_symbols = exported_symbols


class MockLib(object):
    def __init__(self, path, exported_symbols):
        self.path = path
        self.elf = MockELF(exported_symbols)


class GenericRefsTest(unittest.TestCase):
    def setUp(self):
        self.ref = GenericRefs()
        self.ref.add('/system/lib/libc.so',
                     MockELF({'fclose', 'fopen', 'fread', 'fwrite'}))
        self.ref.add('/system/lib/libm.so',
                     MockELF({'cos', 'sin', 'tan'}))
        self.ref.add('/system/lib64/libc.so',
                     MockELF({'fclose', 'fopen', 'fread', 'fwrite'}))
        self.ref.add('/system/lib64/libm.so',
                     MockELF({'cos', 'sin', 'tan'}))

    def test_create_from_sym_dir(self):
        input_dir = os.path.join(SCRIPT_DIR, 'testdata', 'test_generic_refs')

        g = GenericRefs.create_from_sym_dir(input_dir)
        self.assertEqual(4, len(g.refs))

        self.assertIn('/system/lib/libc.so', g.refs)
        self.assertIn('/system/lib/libm.so', g.refs)
        self.assertIn('/system/lib64/libc.so', g.refs)
        self.assertIn('/system/lib64/libm.so', g.refs)

        self.assertEqual({'fclose', 'fopen', 'fread', 'fwrite'},
                         g.refs['/system/lib/libc.so'].exported_symbols)
        self.assertEqual({'fclose', 'fopen', 'fread', 'fwrite'},
                         g.refs['/system/lib64/libc.so'].exported_symbols)

        self.assertEqual({'cos', 'sin', 'tan'},
                         g.refs['/system/lib/libm.so'].exported_symbols)
        self.assertEqual({'cos', 'sin', 'tan'},
                         g.refs['/system/lib64/libm.so'].exported_symbols)

    def test_classify_lib(self):
        libc_sub = MockLib('/system/lib/libc.so', {'fclose', 'fopen', 'fread'})
        libc_sup = MockLib('/system/lib/libc.so',
                           {'fclose', 'fopen', 'fread', 'fwrite', 'open'})
        libc_eq = MockLib('/system/lib/libc.so',
                          {'fclose', 'fopen', 'fread', 'fwrite'})
        libfoo = MockLib('/system/lib/libfoo.so', {})

        self.assertEqual(GenericRefs.MODIFIED, self.ref.classify_lib(libc_sub))
        self.assertEqual(GenericRefs.EXPORT_SUPER_SET,
                         self.ref.classify_lib(libc_sup))
        self.assertEqual(GenericRefs.EXPORT_EQUAL,
                         self.ref.classify_lib(libc_eq))
        self.assertEqual(GenericRefs.NEW_LIB, self.ref.classify_lib(libfoo))

    def test_is_equivalent_lib(self):
        libc_sub = MockLib('/system/lib/libc.so', {'fclose', 'fopen', 'fread'})
        libc_sup = MockLib('/system/lib/libc.so',
                           {'fclose', 'fopen', 'fread', 'fwrite', 'open'})
        libc_eq = MockLib('/system/lib/libc.so',
                          {'fclose', 'fopen', 'fread', 'fwrite'})

        self.assertFalse(self.ref.is_equivalent_lib(libc_sub))
        self.assertFalse(self.ref.is_equivalent_lib(libc_sup))

        self.assertTrue(self.ref.is_equivalent_lib(libc_eq))

    def test_has_same_name_lib(self):
        self.assertTrue(self.ref.has_same_name_lib(
            MockLib('/vendor/lib/libc.so', {})))
        self.assertFalse(self.ref.has_same_name_lib(
            MockLib('/vendor/lib/lib_does_not_exist.so', {})))


if __name__ == '__main__':
    unittest.main()
