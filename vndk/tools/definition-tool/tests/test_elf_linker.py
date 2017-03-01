#!/usr/bin/env python3

from __future__ import print_function

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import unittest

from compat import StringIO
from vndk_definition_tool import ELF, ELFLinker, PT_SYSTEM, PT_VENDOR


class GraphBuilder(object):
    _PARTITION_NAMES = {
        PT_SYSTEM: 'system',
        PT_VENDOR: 'vendor',
    }

    _LIB_DIRS = {
        ELF.ELFCLASS32: 'lib',
        ELF.ELFCLASS64: 'lib64',
    }

    def __init__(self):
        self.graph = ELFLinker()

    def add_lib(self, partition, klass, name, dt_needed, exported_symbols,
                imported_symbols):
        """Create and add a shared library to ELFLinker."""

        elf = ELF(klass, ELF.ELFDATA2LSB, dt_needed=dt_needed,
                  exported_symbols=exported_symbols,
                  imported_symbols=imported_symbols)
        setattr(self, 'elf' + elf.elf_class_name + '_' + name, elf)

        path = os.path.join('/', self._PARTITION_NAMES[partition],
                            self._LIB_DIRS[klass], name + '.so')
        self.graph.add(partition, path, elf)

    def add_multilib(self, partition, name, dt_needed, exported_symbols,
                     imported_symbols):
        """Add 32-bit / 64-bit shared libraries to ELFLinker."""

        for klass in (ELF.ELFCLASS32, ELF.ELFCLASS64):
            self.add_lib(partition, klass, name, dt_needed,
                         exported_symbols, imported_symbols)

    def resolve(self):
        self.graph.resolve_deps()


class ELFLinkerTest(unittest.TestCase):
    def _create_normal_graph(self):
        gb = GraphBuilder()

        gb.add_multilib(PT_SYSTEM, 'libdl', dt_needed=[],
                        exported_symbols={'dlclose', 'dlopen', 'dlsym'},
                        imported_symbols={})

        gb.add_multilib(PT_SYSTEM, 'libm', dt_needed=[],
                        exported_symbols={'cos', 'sin'},
                        imported_symbols={})

        gb.add_multilib(PT_SYSTEM, 'libc', dt_needed=['libdl.so', 'libm.so'],
                        exported_symbols={'fclose', 'fopen', 'fread'},
                        imported_symbols={'dlclose', 'dlopen', 'cos', 'sin'})

        gb.add_multilib(PT_SYSTEM, 'libRS', dt_needed=['libdl.so'],
                        exported_symbols={'rsContextCreate'},
                        imported_symbols={'dlclose', 'dlopen', 'dlsym'})

        gb.add_multilib(PT_SYSTEM, 'libcutils',
                        dt_needed=['libc.so', 'libdl.so'],
                        exported_symbols={},
                        imported_symbols={'dlclose', 'dlopen', 'fclose',
                                          'fopen'})

        gb.add_multilib(PT_VENDOR, 'libEGL',
                        dt_needed=['libc.so', 'libcutils.so', 'libdl.so'],
                        exported_symbols={'eglGetDisplay'},
                        imported_symbols={'fclose', 'fopen'})

        gb.resolve()
        return gb

    def _get_paths_from_nodes(self, nodes):
        return sorted([node.path for node in nodes])

    def test_map_path_to_lib(self):
        gb = self._create_normal_graph()
        graph = gb.graph

        node = graph.map_path_to_lib('/system/lib/libc.so')
        self.assertEqual(gb.elf32_libc, node.elf)
        self.assertEqual('/system/lib/libc.so', node.path)

        node = graph.map_path_to_lib('/system/lib64/libdl.so')
        self.assertEqual(gb.elf64_libdl, node.elf)
        self.assertEqual('/system/lib64/libdl.so', node.path)

        node = graph.map_path_to_lib('/vendor/lib64/libEGL.so')
        self.assertEqual(gb.elf64_libEGL, node.elf)
        self.assertEqual('/vendor/lib64/libEGL.so', node.path)

        self.assertEqual(None, graph.map_path_to_lib('/no/such/path.so'))

    def test_map_paths_to_libs(self):
        gb = self._create_normal_graph()
        graph = gb.graph

        bad = []
        paths = ['/system/lib/libc.so', '/system/lib/libdl.so']
        nodes = graph.map_paths_to_libs(paths, bad.append)

        self.assertEqual([], bad)
        self.assertEqual(2, len(nodes))
        self.assertEqual(paths, self._get_paths_from_nodes(nodes))

        bad = []
        paths = ['/no/such/path.so', '/system/lib64/libdl.so']
        nodes = graph.map_paths_to_libs(paths, bad.append)
        self.assertEqual(['/no/such/path.so'], bad)
        self.assertEqual(['/system/lib64/libdl.so'],
                         self._get_paths_from_nodes(nodes))

    def test_elf_class(self):
        gb = self._create_normal_graph()
        graph = gb.graph
        self.assertEqual(6, len(graph.lib32))
        self.assertEqual(6, len(graph.lib64))

    def test_partitions(self):
        gb = self._create_normal_graph()
        graph = gb.graph
        self.assertEqual(10, len(gb.graph.lib_pt[PT_SYSTEM]))
        self.assertEqual(2, len(gb.graph.lib_pt[PT_VENDOR]))

    def test_deps(self):
        gb = self._create_normal_graph()
        graph = gb.graph

        # Check the dependencies of libc.so.
        node = gb.graph.map_path_to_lib('/system/lib/libc.so')
        self.assertEqual(['/system/lib/libdl.so', '/system/lib/libm.so'],
                         self._get_paths_from_nodes(node.deps))

        # Check the dependencies of libRS.so.
        node = gb.graph.map_path_to_lib('/system/lib64/libRS.so')
        self.assertEqual(['/system/lib64/libdl.so'],
                         self._get_paths_from_nodes(node.deps))

        # Check the dependencies of libEGL.so.
        node = gb.graph.map_path_to_lib('/vendor/lib64/libEGL.so')
        self.assertEqual(['/system/lib64/libc.so', '/system/lib64/libcutils.so',
                          '/system/lib64/libdl.so'],
                         self._get_paths_from_nodes(node.deps))

    def test_users(self):
        gb = self._create_normal_graph()
        graph = gb.graph

        # Check the users of libc.so.
        node = graph.map_path_to_lib('/system/lib/libc.so')
        self.assertEqual(['/system/lib/libcutils.so', '/vendor/lib/libEGL.so'],
                         self._get_paths_from_nodes(node.users))

        # Check the users of libdl.so.
        node = graph.map_path_to_lib('/system/lib/libdl.so')
        self.assertEqual(['/system/lib/libRS.so', '/system/lib/libc.so',
                          '/system/lib/libcutils.so', '/vendor/lib/libEGL.so'],
                         self._get_paths_from_nodes(node.users))

        # Check the users of libRS.so.
        node = graph.map_path_to_lib('/system/lib64/libRS.so')
        self.assertEqual([], self._get_paths_from_nodes(node.users))

        # Check the users of libEGL.so.
        node = graph.map_path_to_lib('/vendor/lib64/libEGL.so')
        self.assertEqual([], self._get_paths_from_nodes(node.users))

    def test_compute_vndk_libs(self):
        gb = self._create_normal_graph()
        graph = gb.graph

        class MockBannedLibs(object):
            def is_banned(self, name):
                return False

        vndk_core, vndk_indirect, vndk_ext = \
                graph.compute_vndk_libs(None, MockBannedLibs())

        self.assertEqual(['/system/lib/libcutils.so',
                          '/system/lib64/libcutils.so'],
                         self._get_paths_from_nodes(vndk_core))
        self.assertEqual([], self._get_paths_from_nodes(vndk_indirect))
        self.assertEqual([], self._get_paths_from_nodes(vndk_ext))


if __name__ == '__main__':
    unittest.main()
