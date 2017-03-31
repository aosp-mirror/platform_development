#!/usr/bin/env python3

from __future__ import print_function

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import unittest

from compat import StringIO
from vndk_definition_tool import (ELF, ELFLinker, PT_SYSTEM, PT_VENDOR,
                                  GenericRefs)

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
TESTDATA_DIR = os.path.join(SCRIPT_DIR ,'testdata', 'test_vndk')

class MockBannedLibs(object):
    def is_banned(self, name):
        return False

class ELFLinkerVNDKTest(unittest.TestCase):
    def _get_paths_from_nodes(self, nodes):
        return sorted([node.path for node in nodes])

    def test_compute_vndk(self):
        input_dir = os.path.join(TESTDATA_DIR, 'pre_treble')

        graph = ELFLinker.create_from_dump(
                system_dirs=[os.path.join(input_dir, 'system')],
                vendor_dirs=[os.path.join(input_dir, 'vendor')])

        vndk = graph.compute_vndk(sp_hals=set(), vndk_stable=set(),
                                  vndk_customized_for_system=set(),
                                  vndk_customized_for_vendor=set(),
                                  generic_refs=None,
                                  banned_libs=MockBannedLibs())

        self.assertEqual(['/system/lib/libcutils.so',
                          '/system/lib64/libcutils.so'],
                         self._get_paths_from_nodes(vndk.vndk_core))
        self.assertEqual([], self._get_paths_from_nodes(vndk.vndk_fwk_ext))
        self.assertEqual([], self._get_paths_from_nodes(vndk.vndk_vnd_ext))

    def test_compute_vndk_fwk_ext(self):
        generic_refs_dir = os.path.join(TESTDATA_DIR, 'vndk_gr')

        generic_refs = GenericRefs.create_from_dir(generic_refs_dir)

        input_dir = os.path.join(TESTDATA_DIR, 'vndk_fwk_ext')

        graph = ELFLinker.create_from_dump(
                system_dirs=[os.path.join(input_dir, 'system')],
                vendor_dirs=[os.path.join(input_dir, 'vendor')],
                generic_refs=generic_refs)

        vndk = graph.compute_vndk(sp_hals=set(), vndk_stable=set(),
                                  vndk_customized_for_system=set(),
                                  vndk_customized_for_vendor=set(),
                                  generic_refs=generic_refs,
                                  banned_libs=MockBannedLibs())

        self.assertEqual(['/system/lib/vndk/libRS.so',
                          '/system/lib/vndk/libcutils.so',
                          '/system/lib64/vndk/libRS.so',
                          '/system/lib64/vndk/libcutils.so'],
                         self._get_paths_from_nodes(vndk.vndk_core))
        self.assertEqual(['/system/lib/vndk-ext/libRS.so',
                          '/system/lib64/vndk-ext/libRS.so'],
                         self._get_paths_from_nodes(vndk.vndk_fwk_ext))
        self.assertEqual([], self._get_paths_from_nodes(vndk.vndk_vnd_ext))

    def test_compute_vndk_vnd_ext(self):
        generic_refs_dir = os.path.join(TESTDATA_DIR, 'vndk_gr')

        generic_refs = GenericRefs.create_from_dir(generic_refs_dir)

        input_dir = os.path.join(TESTDATA_DIR, 'vndk_vnd_ext')

        graph = ELFLinker.create_from_dump(
                system_dirs=[os.path.join(input_dir, 'system')],
                vendor_dirs=[os.path.join(input_dir, 'vendor')],
                generic_refs=generic_refs)

        vndk = graph.compute_vndk(sp_hals=set(), vndk_stable=set(),
                                  vndk_customized_for_system=set(),
                                  vndk_customized_for_vendor=set(),
                                  generic_refs=generic_refs,
                                  banned_libs=MockBannedLibs())

        self.assertEqual(['/system/lib/vndk/libRS.so',
                          '/system/lib/vndk/libcutils.so',
                          '/system/lib64/vndk/libRS.so',
                          '/system/lib64/vndk/libcutils.so'],
                         self._get_paths_from_nodes(vndk.vndk_core))
        self.assertEqual([], self._get_paths_from_nodes(vndk.vndk_fwk_ext))
        self.assertEqual(['/vendor/lib/vndk-ext/libRS.so',
                          '/vendor/lib64/vndk-ext/libRS.so'],
                         self._get_paths_from_nodes(vndk.vndk_vnd_ext))

    def test_compute_vndk_inward_customization(self):
        generic_refs_dir = os.path.join(TESTDATA_DIR, 'vndk_gr')

        generic_refs = GenericRefs.create_from_dir(generic_refs_dir)

        input_dir = os.path.join(TESTDATA_DIR, 'vndk_inward_customization')

        graph = ELFLinker.create_from_dump(
                system_dirs=[os.path.join(input_dir, 'system')],
                vendor_dirs=[os.path.join(input_dir, 'vendor')],
                generic_refs=generic_refs)

        # Make sure libjpeg.so was loaded from the input dir.
        libjpeg_32 = graph.get_lib('/system/lib/libjpeg.so')
        self.assertIsNotNone(libjpeg_32)
        libjpeg_64 = graph.get_lib('/system/lib64/libjpeg.so')
        self.assertIsNotNone(libjpeg_64)

        # Compute vndk sets and move libraries to the correct directories.
        vndk = graph.compute_vndk(sp_hals=set(), vndk_stable=set(),
                                  vndk_customized_for_system=set(),
                                  vndk_customized_for_vendor=set(),
                                  generic_refs=generic_refs,
                                  banned_libs=MockBannedLibs())

        # Check vndk-core libraries.
        self.assertEqual(['/system/lib/vndk/libRS.so',
                          '/system/lib/vndk/libcutils.so',
                          '/system/lib64/vndk/libRS.so',
                          '/system/lib64/vndk/libcutils.so'],
                         self._get_paths_from_nodes(vndk.vndk_core))

        # Check vndk-indirect libraries.
        self.assertEqual(['/system/lib/vndk/libjpeg.so',
                          '/system/lib64/vndk/libjpeg.so'],
                         self._get_paths_from_nodes(vndk.vndk_indirect))

        # Check libjpeg.so (inward-customization) has been renamed.
        self.assertIsNone(graph.get_lib('/system/lib/libjpeg.so'))
        self.assertIsNone(graph.get_lib('/system/lib64/libjpeg.so'))
        self.assertIs(libjpeg_32,
                      graph.get_lib('/system/lib/vndk/libjpeg.so'))
        self.assertIs(libjpeg_64,
                      graph.get_lib('/system/lib64/vndk/libjpeg.so'))

        # Check the absence of vndk-ext libraries.
        self.assertEqual([], self._get_paths_from_nodes(vndk.vndk_fwk_ext))
        self.assertEqual([], self._get_paths_from_nodes(vndk.vndk_vnd_ext))

    def test_compute_vndk_indirect_ext(self):
        # This test case reveals a corner case that will break vndk-indirect
        # computation.  To reproduce the case, the following condition must be
        # satisfied:
        #
        # 1. libA depends on libB.
        # 2. libA is a vndk-fwk-ext.
        # 3. libB is an outward-customized vndk which depends on non-AOSP libC.
        #
        # Both AOSP libA and libB will be added to vndk-core.  But,
        # unfortunately, libA will be resolved to libB in vndk-fwk-ext and this
        # will break the vndk-indirect computation because libC is not in
        # generic references.

        generic_refs_dir = os.path.join(TESTDATA_DIR, 'vndk_indirect_ext_gr')

        generic_refs = GenericRefs.create_from_dir(generic_refs_dir)

        input_dir = os.path.join(TESTDATA_DIR, 'vndk_indirect_ext')

        graph = ELFLinker.create_from_dump(
                system_dirs=[os.path.join(input_dir, 'system')],
                vendor_dirs=[os.path.join(input_dir, 'vendor')],
                generic_refs=generic_refs)

        vndk = graph.compute_vndk(sp_hals=set(), vndk_stable=set(),
                                  vndk_customized_for_system=set(),
                                  vndk_customized_for_vendor=set(),
                                  generic_refs=generic_refs,
                                  banned_libs=MockBannedLibs())

        self.assertEqual(['/system/lib/vndk/libRS.so',
                          '/system/lib/vndk/libcutils.so',
                          '/system/lib64/vndk/libRS.so',
                          '/system/lib64/vndk/libcutils.so'],
                         self._get_paths_from_nodes(vndk.vndk_core))

        self.assertEqual(['/system/lib/vndk/libRS_internal.so',
                          '/system/lib64/vndk/libRS_internal.so'],
                         self._get_paths_from_nodes(vndk.vndk_indirect))

        self.assertEqual(['/system/lib/vndk-ext/libRS_internal.so',
                          '/system/lib64/vndk-ext/libRS_internal.so'],
                         self._get_paths_from_nodes(vndk.vndk_fwk_ext))



if __name__ == '__main__':
    unittest.main()
