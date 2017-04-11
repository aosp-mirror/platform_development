#!/usr/bin/env python3

from __future__ import print_function

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import unittest

from compat import StringIO
from vndk_definition_tool import (ELF, ELFLinker, PT_SYSTEM, PT_VENDOR,
                                  GenericRefs, SPLibResult)

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
TESTDATA_DIR = os.path.join(SCRIPT_DIR ,'testdata', 'test_vndk')

class MockBannedLibs(object):
    def is_banned(self, name):
        return False

class ELFLinkerVNDKTest(unittest.TestCase):
    def _get_paths_from_nodes(self, nodes):
        return sorted([node.path for node in nodes])

    def _create_graph_gr(self, input_dir, generic_refs_dir):
        if not generic_refs_dir:
            generic_refs = None
        else:
            generic_refs_dir = os.path.join(TESTDATA_DIR, generic_refs_dir)
            generic_refs = GenericRefs.create_from_dir(generic_refs_dir)

        input_dir = os.path.join(TESTDATA_DIR, input_dir)

        graph = ELFLinker.create_from_dump(
                system_dirs=[os.path.join(input_dir, 'system')],
                vendor_dirs=[os.path.join(input_dir, 'vendor')],
                generic_refs=generic_refs)

        return (graph, generic_refs)

    def _create_graph_vndk(self, input_dir, generic_refs_dir):
        graph, generic_refs = self._create_graph_gr(input_dir, generic_refs_dir)

        vndk = graph._compute_vndk(
                sp_lib=SPLibResult(set(), set(), set(), set(), set(), set()),
                vndk_customized_for_system=set(),
                vndk_customized_for_vendor=set(),
                generic_refs=generic_refs,
                banned_libs=MockBannedLibs())

        return (graph, vndk)

    def test_compute_vndk(self):
        graph, vndk = self._create_graph_vndk('pre_treble', None)

        self.assertEqual(['/system/lib/vndk/libcutils.so',
                          '/system/lib64/vndk/libcutils.so'],
                         self._get_paths_from_nodes(vndk.vndk_core))
        self.assertEqual([], self._get_paths_from_nodes(vndk.vndk_fwk_ext))
        self.assertEqual([], self._get_paths_from_nodes(vndk.vndk_vnd_ext))

    def test_compute_vndk_indirect_no_gr(self):
        graph, vndk = self._create_graph_vndk('vndk_indirect', None)

        self.assertEqual(['/system/lib/vndk/libcutils.so',
                          '/system/lib64/vndk/libcutils.so'],
                         self._get_paths_from_nodes(vndk.vndk_core))
        self.assertEqual(['/system/lib/vndk/libcutils_dep.so',
                          '/system/lib64/vndk/libcutils_dep.so'],
                         self._get_paths_from_nodes(vndk.vndk_indirect))

    def test_compute_vndk_fwk_ext(self):
        graph, vndk = self._create_graph_vndk('vndk_fwk_ext', 'vndk_gr')

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
        graph, vndk = self._create_graph_vndk('vndk_vnd_ext', 'vndk_gr')

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
        graph, generic_refs = self._create_graph_gr(
                'vndk_inward_customization', 'vndk_gr')

        # Make sure libjpeg.so was loaded from the input dir.
        libjpeg_32 = graph.get_lib('/system/lib/libjpeg.so')
        self.assertIsNotNone(libjpeg_32)
        libjpeg_64 = graph.get_lib('/system/lib64/libjpeg.so')
        self.assertIsNotNone(libjpeg_64)

        # Compute vndk sets and move libraries to the correct directories.
        vndk = graph._compute_vndk(
                sp_lib=SPLibResult(set(), set(), set(), set(), set(), set()),
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

        graph, vndk = self._create_graph_vndk('vndk_indirect_ext',
                                              'vndk_indirect_ext_gr')

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

    def test_compute_vndk_ext_deps(self):
        # This test case reveals a bug in the vndk-core dependencies assertion.
        # This will happen when libA and libB are added to both vndk-fwk-ext and
        # vndk-vnd-ext in the first round, libA depends libC, libC depends
        # libB.

        graph, vndk = self._create_graph_vndk('vndk_ext_dep', 'vndk_ext_dep_gr')

        self.assertEqual(['/system/lib/vndk/libA.so',
                          '/system/lib/vndk/libB.so',
                          '/system/lib/vndk/libC.so'],
                         self._get_paths_from_nodes(vndk.vndk_core))

        self.assertEqual(['/system/lib/vndk-ext/libA.so',
                          '/system/lib/vndk-ext/libB.so',
                          '/system/lib/vndk-ext/libC.so'],
                         self._get_paths_from_nodes(vndk.vndk_fwk_ext))

        self.assertEqual(['/vendor/lib/vndk-ext/libA.so',
                          '/vendor/lib/vndk-ext/libB.so',
                          '/vendor/lib/vndk-ext/libC.so'],
                         self._get_paths_from_nodes(vndk.vndk_vnd_ext))


if __name__ == '__main__':
    unittest.main()
