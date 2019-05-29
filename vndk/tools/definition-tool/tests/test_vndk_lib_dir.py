#!/usr/bin/env python3

import os
import posixpath
import unittest

from vndk_definition_tool import VNDKLibDir

from .compat import StringIO


SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))


class VNDKLibDirTest(unittest.TestCase):
    def test_create_vndk_dir_suffix(self):
        self.assertEqual('', VNDKLibDir.create_vndk_dir_suffix('current'))
        self.assertEqual('-28', VNDKLibDir.create_vndk_dir_suffix('28'))


    def test_create_vndk_sp_dir_name(self):
        self.assertEqual(
            'vndk-sp', VNDKLibDir.create_vndk_sp_dir_name('current'))
        self.assertEqual(
            'vndk-sp-28', VNDKLibDir.create_vndk_sp_dir_name('28'))


    def test_create_vndk_dir_name(self):
        self.assertEqual(
            'vndk', VNDKLibDir.create_vndk_dir_name('current'))
        self.assertEqual(
            'vndk-28', VNDKLibDir.create_vndk_dir_name('28'))


    def test_extract_vndk_version_from_name(self):
        self.assertEqual(
            'current', VNDKLibDir.extract_version_from_name('vndk'))
        self.assertEqual(
            'current', VNDKLibDir.extract_version_from_name('vndk-sp'))
        self.assertEqual(
            '28', VNDKLibDir.extract_version_from_name('vndk-28'))
        self.assertEqual(
            '28', VNDKLibDir.extract_version_from_name('vndk-sp-28'))
        self.assertEqual(
            'p', VNDKLibDir.extract_version_from_name('vndk-p'))
        self.assertEqual(
            'p', VNDKLibDir.extract_version_from_name('vndk-sp-p'))


    def test_extract_vndk_version_from_path(self):
        ans = VNDKLibDir.extract_version_from_path(
            '/system/lib64/vndk/libexample.so')
        self.assertEqual('current', ans)

        ans = VNDKLibDir.extract_version_from_path(
            '/system/lib64/vndk-sp/libexample.so')
        self.assertEqual('current', ans)

        ans = VNDKLibDir.extract_version_from_path(
            '/system/lib64/vndk-28/libexample.so')
        self.assertEqual('28', ans)

        ans = VNDKLibDir.extract_version_from_path(
            '/system/lib64/vndk-sp-28/libexample.so')
        self.assertEqual('28', ans)

        ans = VNDKLibDir.extract_version_from_path(
            '/system/lib64/vndk-p/libexample.so')
        self.assertEqual('p', ans)

        ans = VNDKLibDir.extract_version_from_path(
            '/system/lib64/vndk-sp-p/libexample.so')
        self.assertEqual('p', ans)

        ans = VNDKLibDir.extract_version_from_path(
            '/system/lib64/vndk-sp-p/hw/libexample.so')
        self.assertEqual('p', ans)

        ans = VNDKLibDir.extract_version_from_path(
            '/system/lib64/libexample.so')
        self.assertEqual(None, ans)


    def test_is_in_vndk_sp_dir(self):
        self.assertFalse(VNDKLibDir.is_in_vndk_sp_dir('/system/lib/liba.so'))
        self.assertFalse(
            VNDKLibDir.is_in_vndk_sp_dir('/system/lib/vndk/liba.so'))
        self.assertFalse(
            VNDKLibDir.is_in_vndk_sp_dir('/system/lib/vndk-28/liba.so'))
        self.assertFalse(
            VNDKLibDir.is_in_vndk_sp_dir('/system/lib/vndk-spec/liba.so'))
        self.assertTrue(
            VNDKLibDir.is_in_vndk_sp_dir('/system/lib/vndk-sp/liba.so'))
        self.assertTrue(
            VNDKLibDir.is_in_vndk_sp_dir('/system/lib/vndk-sp-28/liba.so'))


    def test_is_in_vndk_dir(self):
        self.assertFalse(VNDKLibDir.is_in_vndk_dir('/system/lib/liba.so'))
        self.assertTrue(VNDKLibDir.is_in_vndk_dir('/system/lib/vndk/liba.so'))
        self.assertTrue(
            VNDKLibDir.is_in_vndk_dir('/system/lib/vndk-28/liba.so'))
        self.assertTrue(
            VNDKLibDir.is_in_vndk_dir('/system/lib/vndk-spec/liba.so'))
        self.assertFalse(
            VNDKLibDir.is_in_vndk_dir('/system/lib/vndk-sp/liba.so'))
        self.assertFalse(
            VNDKLibDir.is_in_vndk_dir('/system/lib/vndk-sp-28/liba.so'))


    def test_get_vndk_lib_dirs(self):
        for version in ('current', '28'):
            for lib_dir in ('lib', 'lib64'):
                vndk_sp_name = VNDKLibDir.create_vndk_sp_dir_name(version)
                vndk_name = VNDKLibDir.create_vndk_dir_name(version)

                expected_vndk_sp = [
                    posixpath.join('/vendor', lib_dir, vndk_sp_name),
                    posixpath.join('/system', lib_dir, vndk_sp_name),
                ]
                expected_vndk = [
                    posixpath.join('/vendor', lib_dir, vndk_name),
                    posixpath.join('/system', lib_dir, vndk_name),
                ]

                vndk_sp_dirs, vndk_dirs = \
                    VNDKLibDir.get_vndk_lib_dirs(lib_dir, version)
                self.assertEqual(expected_vndk_sp, vndk_sp_dirs)
                self.assertEqual(expected_vndk, vndk_dirs)


    def test_add_version_current(self):
        vndk_lib_dirs = VNDKLibDir()
        vndk_lib_dirs.append('current')
        self.assertIn('current', vndk_lib_dirs)


    def test_create_from_dirs_unversioned(self):
        input_dir = os.path.join(
            SCRIPT_DIR, 'testdata', 'test_vndk_lib_dir', 'vndk_unversioned')

        vndk_lib_dirs = VNDKLibDir.create_from_dirs(
            [os.path.join(input_dir, 'system')],
            [os.path.join(input_dir, 'vendor')])

        self.assertIn('current', vndk_lib_dirs)


    def test_create_from_dirs_versioned(self):
        input_dir = os.path.join(
            SCRIPT_DIR, 'testdata', 'test_vndk_lib_dir', 'vndk_versioned')

        vndk_lib_dirs = VNDKLibDir.create_from_dirs(
            [os.path.join(input_dir, 'system')],
            [os.path.join(input_dir, 'vendor')])

        self.assertIn('28', vndk_lib_dirs)


    def test_create_from_dirs_versioned_multiple(self):
        input_dir = os.path.join(
            SCRIPT_DIR, 'testdata', 'test_vndk_lib_dir',
            'vndk_versioned_multiple')

        vndk_lib_dirs = VNDKLibDir.create_from_dirs(
            [os.path.join(input_dir, 'system')],
            [os.path.join(input_dir, 'vendor')])

        self.assertIn('28', vndk_lib_dirs)
        self.assertIn('29', vndk_lib_dirs)


    def test_create_from_dirs_versioned_32bit_only(self):
        input_dir = os.path.join(
            SCRIPT_DIR, 'testdata', 'test_vndk_lib_dir', 'vndk_32')

        vndk_lib_dirs = VNDKLibDir.create_from_dirs(
            [os.path.join(input_dir, 'system')],
            [os.path.join(input_dir, 'vendor')])

        self.assertIn('28', vndk_lib_dirs)


    def test_get_property(self):
        property_file = StringIO('ro.vndk.version=example\n')
        ans = VNDKLibDir._get_property(property_file, 'ro.vndk.version')
        self.assertEqual('example', ans)

        property_file = StringIO('# comments\n')
        ans = VNDKLibDir._get_property(property_file, 'ro.vndk.version')
        self.assertEqual(None, ans)


    def test_get_ro_vndk_version(self):
        input_dir = os.path.join(
            SCRIPT_DIR, 'testdata', 'test_vndk_lib_dir',
            'vndk_versioned_multiple')

        vendor_dirs = [os.path.join(input_dir, 'vendor')]

        self.assertEqual('29', VNDKLibDir.get_ro_vndk_version(vendor_dirs))


    def test_sorted_versions(self):
        self.assertEqual(
            ['20', '10', '2', '1'],
            VNDKLibDir.sorted_version(['1', '2', '10', '20']))

        self.assertEqual(
            ['b', 'a', '20', '10', '2', '1'],
            VNDKLibDir.sorted_version(['1', '2', '10', '20', 'a', 'b']))

        self.assertEqual(
            ['a', '10b', '10', '2', '1'],
            VNDKLibDir.sorted_version(['1', '2', '10', 'a', '10b']))

        self.assertEqual(
            ['current', 'd', 'a', '10', '1'],
            VNDKLibDir.sorted_version(['1', '10', 'a', 'd', 'current']))
