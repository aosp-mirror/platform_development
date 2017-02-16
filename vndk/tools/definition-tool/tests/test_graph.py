#!/usr/bin/env python3

from __future__ import print_function

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import unittest

from compat import StringIO
from vndk_definition_tool import ELF, Graph, PT_SYSTEM, PT_VENDOR

class GraphTest(unittest.TestCase):
    def setUp(self):
        # 32-bit libraries on the system partition.
        self.elf_libdl_32 = ELF(
                ELF.ELFCLASS32, ELF.ELFDATA2LSB,
                exported_symbols={'dlclose', 'dlopen', 'dlsym'})

        self.elf_libm_32 = ELF(
                ELF.ELFCLASS32, ELF.ELFDATA2LSB,
                exported_symbols={'cos', 'sin'})

        self.elf_libc_32 = ELF(
                ELF.ELFCLASS32, ELF.ELFDATA2LSB,
                dt_needed=['libdl.so', 'libm.so'],
                exported_symbols={'fclose', 'fopen', 'fread'})

        self.elf_libRS_32 = ELF(
                ELF.ELFCLASS32, ELF.ELFDATA2LSB,
                dt_needed=['libdl.so'],
                exported_symbols={'rsContextCreate'})

        self.elf_libcutils_32 = ELF(
                ELF.ELFCLASS32, ELF.ELFDATA2LSB,
                dt_needed=['libc.so', 'libdl.so'])

        # 64-bit libraries on the system partition.
        self.elf_libdl_64 = ELF(
                ELF.ELFCLASS64, ELF.ELFDATA2LSB,
                exported_symbols={'dlclose', 'dlopen', 'dlsym'})

        self.elf_libm_64 = ELF(
                ELF.ELFCLASS64, ELF.ELFDATA2LSB,
                exported_symbols={'cos', 'sin'})

        self.elf_libc_64 = ELF(
                ELF.ELFCLASS64, ELF.ELFDATA2LSB,
                dt_needed=['libdl.so', 'libm.so'],
                exported_symbols={'fclose', 'fopen', 'fread'})

        self.elf_libRS_64 = ELF(
                ELF.ELFCLASS64, ELF.ELFDATA2LSB,
                dt_needed=['libdl.so'],
                exported_symbols={'rsContextCreate'})

        self.elf_libcutils_64 = ELF(
                ELF.ELFCLASS64, ELF.ELFDATA2LSB,
                dt_needed=['libc.so', 'libdl.so'])

        # 32-bit libraries on the vendor partition.
        self.elf_libEGL_32 = ELF(
                ELF.ELFCLASS32, ELF.ELFDATA2LSB,
                dt_needed=['libc.so', 'libcutils.so', 'libdl.so'],
                exported_symbols={'eglGetDisplay'})

        # 64-bit libraries on the vendor partition.
        self.elf_libEGL_64 = ELF(
                ELF.ELFCLASS64, ELF.ELFDATA2LSB,
                dt_needed=['libc.so', 'libcutils.so', 'libdl.so'],
                exported_symbols={'eglGetDisplay'})

        # Build the linker.
        g = Graph()
        g.add(PT_SYSTEM, '/system/lib/libc.so', self.elf_libc_32)
        g.add(PT_SYSTEM, '/system/lib/libcutils.so', self.elf_libcutils_32)
        g.add(PT_SYSTEM, '/system/lib/libdl.so', self.elf_libdl_32)
        g.add(PT_SYSTEM, '/system/lib/libm.so', self.elf_libm_32)
        g.add(PT_SYSTEM, '/system/lib/libRS.so', self.elf_libRS_32)
        g.add(PT_SYSTEM, '/system/lib64/libc.so', self.elf_libc_64)
        g.add(PT_SYSTEM, '/system/lib64/libcutils.so', self.elf_libcutils_64)
        g.add(PT_SYSTEM, '/system/lib64/libdl.so', self.elf_libdl_64)
        g.add(PT_SYSTEM, '/system/lib64/libm.so', self.elf_libm_64)
        g.add(PT_SYSTEM, '/system/lib64/libRS.so', self.elf_libRS_64)
        g.add(PT_VENDOR, '/vendor/lib/libEGL.so', self.elf_libEGL_32)
        g.add(PT_VENDOR, '/vendor/lib64/libEGL.so', self.elf_libEGL_64)
        g.resolve_deps()
        self.graph = g

    def test_map_path_to_lib(self):
        node = self.graph.map_path_to_lib('/system/lib/libc.so')
        self.assertEqual(self.elf_libc_32, node.elf)
        self.assertEqual('/system/lib/libc.so', node.path)

        node = self.graph.map_path_to_lib('/system/lib64/libdl.so')
        self.assertEqual(self.elf_libdl_64, node.elf)
        self.assertEqual('/system/lib64/libdl.so', node.path)

        node = self.graph.map_path_to_lib('/vendor/lib64/libEGL.so')
        self.assertEqual(self.elf_libEGL_64, node.elf)
        self.assertEqual('/vendor/lib64/libEGL.so', node.path)

        self.assertEqual(None, self.graph.map_path_to_lib('/no/such/path.so'))

    def _get_paths_from_nodes(self, nodes):
        return sorted([node.path for node in nodes])

    def test_map_paths_to_libs(self):
        bad = []
        paths = ['/system/lib/libc.so', '/system/lib/libdl.so']
        nodes = self.graph.map_paths_to_libs(paths,  lambda x: bad.append(x))

        self.assertEqual([], bad)
        self.assertEqual(2, len(nodes))
        self.assertEqual(paths, self._get_paths_from_nodes(nodes))

        bad = []
        paths = ['/no/such/path.so', '/system/lib64/libdl.so']
        nodes = self.graph.map_paths_to_libs(paths, lambda x: bad.append(x))
        self.assertEqual(['/no/such/path.so'], bad)
        self.assertEqual(['/system/lib64/libdl.so'],
                         self._get_paths_from_nodes(nodes))

    def test_elf_class(self):
        self.assertEqual(6, len(self.graph.lib32))
        self.assertEqual(6, len(self.graph.lib64))

    def test_partitions(self):
        self.assertEqual(10, len(self.graph.lib_pt[PT_SYSTEM]))
        self.assertEqual(2, len(self.graph.lib_pt[PT_VENDOR]))

    def test_deps(self):
        libc_32 = self.graph.map_path_to_lib('/system/lib/libc.so')
        self.assertEqual(['/system/lib/libdl.so', '/system/lib/libm.so'],
                         self._get_paths_from_nodes(libc_32.deps))

        libRS_64 = self.graph.map_path_to_lib('/system/lib64/libRS.so')
        self.assertEqual(['/system/lib64/libdl.so'],
                         self._get_paths_from_nodes(libRS_64.deps))

        libEGL_64 = self.graph.map_path_to_lib('/vendor/lib64/libEGL.so')
        self.assertEqual(['/system/lib64/libc.so', '/system/lib64/libcutils.so',
                          '/system/lib64/libdl.so'],
                         self._get_paths_from_nodes(libEGL_64.deps))

    def test_users(self):
        libc_32 = self.graph.map_path_to_lib('/system/lib/libc.so')
        self.assertEqual(['/system/lib/libcutils.so', '/vendor/lib/libEGL.so'],
                         self._get_paths_from_nodes(libc_32.users))

        libdl_32 = self.graph.map_path_to_lib('/system/lib/libdl.so')
        self.assertEqual(['/system/lib/libRS.so', '/system/lib/libc.so',
                          '/system/lib/libcutils.so', '/vendor/lib/libEGL.so'],
                         self._get_paths_from_nodes(libdl_32.users))

        libRS_64 = self.graph.map_path_to_lib('/system/lib64/libRS.so')
        self.assertEqual([], self._get_paths_from_nodes(libRS_64.users))

        libEGL_64 = self.graph.map_path_to_lib('/vendor/lib64/libEGL.so')
        self.assertEqual([], self._get_paths_from_nodes(libEGL_64.users))

    def test_compute_vndk_libs(self):
        class MockBannedLibs(object):
            def get(self, name):
                return None

        vndk_core, vndk_indirect, vndk_ext = \
                self.graph.compute_vndk_libs(None, MockBannedLibs())

        self.assertEqual(['/system/lib/libcutils.so',
                          '/system/lib64/libcutils.so'],
                         self._get_paths_from_nodes(vndk_core))
        self.assertEqual([], self._get_paths_from_nodes(vndk_indirect))
        self.assertEqual([], self._get_paths_from_nodes(vndk_ext))


if __name__ == '__main__':
    unittest.main()
