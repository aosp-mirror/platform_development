#!/usr/bin/env python3

from __future__ import print_function

import os
import subprocess
import unittest
import zipfile

from vndk_definition_tool import DexFileReader, UnicodeSurrogateDecodeError

from .compat import TemporaryDirectory

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
INPUT_DIR = os.path.join(SCRIPT_DIR, 'testdata', 'test_dex_file')


class ModifiedUTF8Test(unittest.TestCase):
    def test_encode(self):
        self.assertEqual(b'\xc0\x80', u'\u0000'.encode('mutf-8'))
        self.assertEqual(b'\x09', u'\u0009'.encode('mutf-8'))
        self.assertEqual(b'\x7f', u'\u007f'.encode('mutf-8'))
        self.assertEqual(b'\xc2\x80', u'\u0080'.encode('mutf-8'))
        self.assertEqual(b'\xdf\xbf', u'\u07ff'.encode('mutf-8'))
        self.assertEqual(b'\xe0\xa0\x80', u'\u0800'.encode('mutf-8'))
        self.assertEqual(b'\xe7\xbf\xbf', u'\u7fff'.encode('mutf-8'))
        self.assertEqual(b'\xed\xa0\x81\xed\xb0\x80',
                         u'\U00010400'.encode('mutf-8'))


    def test_decode(self):
        self.assertEqual(u'\u0000', b'\xc0\x80'.decode('mutf-8'))
        self.assertEqual(u'\u0009', b'\x09'.decode('mutf-8'))
        self.assertEqual(u'\u007f', b'\x7f'.decode('mutf-8'))
        self.assertEqual(u'\u0080', b'\xc2\x80'.decode('mutf-8'))
        self.assertEqual(u'\u07ff', b'\xdf\xbf'.decode('mutf-8'))
        self.assertEqual(u'\u0800', b'\xe0\xa0\x80'.decode('mutf-8'))
        self.assertEqual(u'\u7fff', b'\xe7\xbf\xbf'.decode('mutf-8'))
        self.assertEqual(u'\U00010400',
                         b'\xed\xa0\x81\xed\xb0\x80'.decode('mutf-8'))


    def test_decode_exception(self):
        # Low surrogate does not come after high surrogate
        with self.assertRaises(UnicodeSurrogateDecodeError):
            b'\xed\xa0\x81\x40'.decode('mutf-8')

        # Low surrogate without prior high surrogate
        with self.assertRaises(UnicodeSurrogateDecodeError):
            b'\xed\xb0\x80\x40'.decode('mutf-8')

        # Unexpected end after high surrogate
        with self.assertRaises(UnicodeSurrogateDecodeError):
            b'\xed\xa0\x81'.decode('mutf-8')

        # Unexpected end after low surrogate
        with self.assertRaises(UnicodeSurrogateDecodeError):
            b'\xed\xb0\x80'.decode('mutf-8')

        # Out-of-order surrogate
        with self.assertRaises(UnicodeSurrogateDecodeError):
            b'\xed\xb0\x80\xed\xa0\x81'.decode('mutf-8')


class DexFileTest(unittest.TestCase):
    def _assemble_smali(self, dest, source):
        """Assemble a smali source file.  Skip the test if the smali command is
        unavailable."""
        try:
            subprocess.check_call(['smali', 'a', source, '-o', dest])
        except EnvironmentError:
            self.skipTest('smali not available')


    def _create_zip_file(self, dest, paths):
        """Create a zip file from several input files."""
        with zipfile.ZipFile(dest, 'w') as zip_file:
            for path in paths:
                zip_file.write(path, os.path.basename(path))


    def test_generate_classes_dex_names(self):
        seq = DexFileReader.generate_classes_dex_names()
        self.assertEqual('classes.dex', next(seq))
        self.assertEqual('classes2.dex', next(seq))
        self.assertEqual('classes3.dex', next(seq))


    def test_enumerate_dex_strings_buf(self):
        with TemporaryDirectory() as tmp_dir:
            smali_file = os.path.join(INPUT_DIR, 'Hello.smali')
            classes_dex = os.path.join(tmp_dir, 'classes.dex')
            self._assemble_smali(classes_dex, smali_file)

            with open(classes_dex, 'rb') as classes_dex:
                buf = classes_dex.read()

            strs = set(DexFileReader.enumerate_dex_strings_buf(buf))

            self.assertIn(b'hello', strs)
            self.assertIn(b'world', strs)


    def test_enumerate_dex_strings_apk(self):
        with TemporaryDirectory() as tmp_dir:
            smali_file = os.path.join(INPUT_DIR, 'Hello.smali')
            classes_dex = os.path.join(tmp_dir, 'classes.dex')
            self._assemble_smali(classes_dex, smali_file)

            smali_file = os.path.join(INPUT_DIR, 'Example.smali')
            classes2_dex = os.path.join(tmp_dir, 'classes2.dex')
            self._assemble_smali(classes2_dex, smali_file)

            zip_file = os.path.join(tmp_dir, 'example.apk')
            self._create_zip_file(zip_file, [classes_dex, classes2_dex])

            strs = set(DexFileReader.enumerate_dex_strings_apk(zip_file))

            self.assertIn(b'hello', strs)
            self.assertIn(b'world', strs)
            self.assertIn(b'foo', strs)
            self.assertIn(b'bar', strs)
