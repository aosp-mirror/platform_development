#!/usr/bin/env python3

from __future__ import print_function

from vndk_definition_tool import (PT_SYSTEM, PT_VENDOR)

from .compat import StringIO, TestCase, patch
from .utils import GraphBuilder


class ELFLinkerVNDKTest(TestCase):
    def test_normalize_partition_tags_bad_vendor_deps(self):
        """Check whether normalize_partition_tags() hides the dependencies from
        the system partition to the vendor partition if the dependencies are
        not SP-HAL libraries."""

        gb = GraphBuilder()
        libfwk = gb.add_lib32(PT_SYSTEM, 'libfwk', dt_needed=['libvnd.so'])
        libvnd = gb.add_lib32(PT_VENDOR, 'libvnd')
        gb.resolve()

        self.assertIn(libvnd, libfwk.deps_needed)
        self.assertIn(libfwk, libvnd.users_needed)

        stderr = StringIO()
        with patch('sys.stderr', stderr):
            gb.graph.normalize_partition_tags(set(), None)

        self.assertRegex(
            stderr.getvalue(),
            'error: .*: system exe/lib must not depend on vendor lib .*.  '
            'Assume such dependency does not exist.')

        self.assertNotIn(libvnd, libfwk.deps_needed)
        self.assertNotIn(libfwk, libvnd.users_needed)

        self.assertIn(libvnd, libfwk.deps_needed_hidden)
        self.assertIn(libfwk, libvnd.users_needed_hidden)

        self.assertIn(libvnd, libfwk.deps_all)
        self.assertIn(libvnd, libfwk.deps_needed_all)
        self.assertNotIn(libvnd, libfwk.deps_good)

        self.assertIn(libfwk, libvnd.users_all)
        self.assertIn(libfwk, libvnd.users_needed_all)
        self.assertNotIn(libfwk, libvnd.users_good)


    def test_normalize_partition_tags_sp_hal(self):
        """Check whether normalize_partition_tags() keep dependencies to SP-HAL
        libraries as-is."""

        gb = GraphBuilder()
        libfwk = gb.add_lib32(PT_SYSTEM, 'libfwk', dt_needed=['libsphal.so'])
        libsphal = gb.add_lib32(PT_VENDOR, 'libsphal')
        gb.resolve()

        self.assertIn(libsphal, libfwk.deps_needed)
        self.assertIn(libfwk, libsphal.users_needed)

        gb.graph.normalize_partition_tags({libsphal}, None)

        # SP-HALs should be kept as-is.
        self.assertIn(libsphal, libfwk.deps_needed)
        self.assertIn(libfwk, libsphal.users_needed)


    def test_vndk(self):
        """Check the computation of vndk without generic references."""

        gb = GraphBuilder()
        gb.add_lib32(PT_SYSTEM, 'libfwk')
        libvndk = gb.add_lib32(PT_SYSTEM, 'libvndk', extra_dir='vndk')
        libvndk_sp = gb.add_lib32(PT_SYSTEM, 'libutils', extra_dir='vndk-sp')
        libvnd = gb.add_lib32(PT_VENDOR, 'libvnd',
                              dt_needed=['libvndk.so', 'libutils.so'])
        gb.resolve()

        self.assertIn(libvndk, libvnd.deps_all)
        self.assertIn(libvndk_sp, libvnd.deps_all)

        vndk_sets = gb.graph.compute_degenerated_vndk(None)

        self.assertIn(libvndk, vndk_sets.vndk)
        self.assertIn(libvndk_sp, vndk_sets.vndk_sp)


    def test_vndk_bad_vendor_deps(self):
        """Check the computation of vndk without generic references."""

        gb = GraphBuilder()

        libvndk = gb.add_lib32(
            PT_SYSTEM, 'libvndk', dt_needed=['libvnd_bad.so'],
            extra_dir='vndk')

        libvndk_sp = gb.add_lib32(
            PT_SYSTEM, 'libutils', dt_needed=['libvnd_bad.so'],
            extra_dir='vndk-sp')

        libvnd_bad = gb.add_lib32(PT_VENDOR, 'libvnd_bad', extra_dir='vndk-sp')

        gb.resolve()

        self.assertIn(libvnd_bad, libvndk.deps_all)
        self.assertIn(libvnd_bad, libvndk_sp.deps_all)

        with patch('sys.stderr', StringIO()):
            vndk_sets = gb.graph.compute_degenerated_vndk(None)

        self.assertNotIn(libvnd_bad, vndk_sets.vndk)
        self.assertNotIn(libvnd_bad, vndk_sets.vndk_sp)


    def test_ll_ndk_private_without_sp_hal(self):
        """Check the computation of ll_ndk_private excludes sp_hal."""

        gb = GraphBuilder()
        libEGL = gb.add_lib32(PT_SYSTEM, 'libEGL',
                              dt_needed=['libEGL_dep.so'])
        libEGL_dep = gb.add_lib32(PT_SYSTEM, 'libEGL_dep')
        libEGL_chipset = gb.add_lib32(PT_VENDOR, 'libEGL_chipset',
                                      extra_dir='egl',
                                      dt_needed=['libEGL.so'])
        gb.resolve()

        libEGL.add_dlopen_dep(libEGL_chipset)

        vndk_sets = gb.graph.compute_degenerated_vndk(None)

        self.assertIn(libEGL, vndk_sets.ll_ndk)
        self.assertIn(libEGL_dep, vndk_sets.ll_ndk_private)
        self.assertIn(libEGL_chipset, vndk_sets.sp_hal)

        self.assertNotIn(libEGL_chipset, vndk_sets.ll_ndk_private)
