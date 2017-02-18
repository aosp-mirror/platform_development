#!/usr/bin/env python3

from __future__ import print_function

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import unittest

from compat import StringIO
from vndk_definition_tool import ELF, ELFLinker, PT_SYSTEM, PT_VENDOR

class ELFLinkerTest(unittest.TestCase):
    _PARTITION_NAMES = {
        PT_SYSTEM: 'system',
        PT_VENDOR: 'vendor',
    }

    _LIB_DIRS = {
        ELF.ELFCLASS32: 'lib',
        ELF.ELFCLASS64: 'lib64',
    }

    def _create_elf(self, partition, klass, name, dt_needed, exported_symbols):
        elf = ELF(klass, ELF.ELFDATA2LSB, dt_needed=dt_needed,
                  exported_symbols=exported_symbols)
        setattr(self, 'elf' + elf.elf_class_name + '_' + name, elf)

        path = os.path.join('/', self._PARTITION_NAMES[partition],
                            self._LIB_DIRS[klass], name + '.so')
        self.graph.add(partition, path, elf)

    def _create_elfs(self, partition, name, dt_needed, exported_symbols):
        for klass in (ELF.ELFCLASS32, ELF.ELFCLASS64):
            self._create_elf(partition, klass, name, dt_needed,
                             exported_symbols)

    def setUp(self):
        self.graph = ELFLinker()
        self._create_elfs(PT_SYSTEM, 'libdl', dt_needed=[],
                          exported_symbols={'dlclose', 'dlopen', 'dlsym'})
        self._create_elfs(PT_SYSTEM, 'libm', dt_needed=[],
                          exported_symbols={'cos', 'sin'})
        self._create_elfs(PT_SYSTEM, 'libc', dt_needed=['libdl.so', 'libm.so'],
                          exported_symbols={'fclose', 'fopen', 'fread'})
        self._create_elfs(PT_SYSTEM, 'libRS', dt_needed=['libdl.so'],
                          exported_symbols={'rsContextCreate'})
        self._create_elfs(PT_SYSTEM, 'libcutils',
                          dt_needed=['libc.so', 'libdl.so'],
                          exported_symbols={})
        self._create_elfs(PT_VENDOR, 'libEGL',
                          dt_needed=['libc.so', 'libcutils.so', 'libdl.so'],
                          exported_symbols={'eglGetDisplay'})
        self.graph.resolve_deps()

    def test_map_path_to_lib(self):
        node = self.graph.map_path_to_lib('/system/lib/libc.so')
        self.assertEqual(self.elf32_libc, node.elf)
        self.assertEqual('/system/lib/libc.so', node.path)

        node = self.graph.map_path_to_lib('/system/lib64/libdl.so')
        self.assertEqual(self.elf64_libdl, node.elf)
        self.assertEqual('/system/lib64/libdl.so', node.path)

        node = self.graph.map_path_to_lib('/vendor/lib64/libEGL.so')
        self.assertEqual(self.elf64_libEGL, node.elf)
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
