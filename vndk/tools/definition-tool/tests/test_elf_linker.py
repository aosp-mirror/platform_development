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

    def add_lib(self, partition, klass, name, dt_needed=[],
                exported_symbols=set(), imported_symbols=set(),
                extra_dir=None):
        """Create and add a shared library to ELFLinker."""

        lib_dir = os.path.join('/', self._PARTITION_NAMES[partition],
                               self._LIB_DIRS[klass])
        if extra_dir:
            lib_dir = os.path.join(lib_dir, extra_dir)

        path = os.path.join(lib_dir, name + '.so')

        elf = ELF(klass, ELF.ELFDATA2LSB, dt_needed=dt_needed,
                  exported_symbols=exported_symbols,
                  imported_symbols=imported_symbols)

        node = self.graph.add(partition, path, elf)
        setattr(self, name + '_' + elf.elf_class_name, node)
        return node

    def add_multilib(self, partition, name, dt_needed=[],
                     exported_symbols=set(), imported_symbols=set(),
                     extra_dir=None):
        """Add 32-bit / 64-bit shared libraries to ELFLinker."""
        return (
            self.add_lib(partition, ELF.ELFCLASS32, name, dt_needed,
                         exported_symbols, imported_symbols, extra_dir),
            self.add_lib(partition, ELF.ELFCLASS64, name, dt_needed,
                         exported_symbols, imported_symbols, extra_dir)
        )

    def resolve(self):
        self.graph.resolve_deps()


class ELFLinkerTest(unittest.TestCase):
    def _create_normal_graph(self):
        gb = GraphBuilder()

        gb.add_multilib(PT_SYSTEM, 'libdl',
                        exported_symbols={'dlclose', 'dlopen', 'dlsym'})

        gb.add_multilib(PT_SYSTEM, 'libm', exported_symbols={'cos', 'sin'})

        gb.add_multilib(PT_SYSTEM, 'libc', dt_needed=['libdl.so', 'libm.so'],
                        exported_symbols={'fclose', 'fopen', 'fread'},
                        imported_symbols={'dlclose', 'dlopen', 'cos', 'sin'})

        gb.add_multilib(PT_SYSTEM, 'libRS', dt_needed=['libdl.so'],
                        exported_symbols={'rsContextCreate'},
                        imported_symbols={'dlclose', 'dlopen', 'dlsym'})

        gb.add_multilib(PT_SYSTEM, 'libcutils',
                        dt_needed=['libc.so', 'libdl.so'],
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
        self.assertEqual(gb.libc_32, node)
        self.assertEqual('/system/lib/libc.so', node.path)

        node = graph.map_path_to_lib('/system/lib64/libdl.so')
        self.assertEqual(gb.libdl_64, node)
        self.assertEqual('/system/lib64/libdl.so', node.path)

        node = graph.map_path_to_lib('/vendor/lib64/libEGL.so')
        self.assertEqual(gb.libEGL_64, node)
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

    def test_linked_symbols(self):
        gb = self._create_normal_graph()
        graph = gb.graph

        # Check the unresolved symbols.
        for lib_set in (graph.lib32, graph.lib64):
            for lib in lib_set.values():
                self.assertEqual(set(), lib.unresolved_symbols)

        # Check the linked symbols.
        for lib in ('lib', 'lib64'):
            libdl = graph.map_path_to_lib('/system/' + lib + '/libdl.so')
            libm = graph.map_path_to_lib('/system/' + lib + '/libm.so')
            libc = graph.map_path_to_lib('/system/' + lib + '/libc.so')
            libRS = graph.map_path_to_lib('/system/' + lib + '/libRS.so')
            libcutils = \
                    graph.map_path_to_lib('/system/' + lib + '/libcutils.so')
            libEGL = graph.map_path_to_lib('/vendor/' + lib + '/libEGL.so')

            # Check the linked symbols for libc.so.
            self.assertIs(libdl, libc.linked_symbols['dlclose'])
            self.assertIs(libdl, libc.linked_symbols['dlopen'])
            self.assertIs(libm, libc.linked_symbols['cos'])
            self.assertIs(libm, libc.linked_symbols['sin'])

            # Check the linked symbols for libRS.so.
            self.assertIs(libdl, libRS.linked_symbols['dlclose'])
            self.assertIs(libdl, libRS.linked_symbols['dlopen'])
            self.assertIs(libdl, libRS.linked_symbols['dlsym'])

            # Check the linked symbols for libcutils.so.
            self.assertIs(libdl, libcutils.linked_symbols['dlclose'])
            self.assertIs(libdl, libcutils.linked_symbols['dlopen'])
            self.assertIs(libc, libcutils.linked_symbols['fclose'])
            self.assertIs(libc, libcutils.linked_symbols['fopen'])

            # Check the linked symbols for libEGL.so.
            self.assertIs(libc, libEGL.linked_symbols['fclose'])
            self.assertIs(libc, libEGL.linked_symbols['fopen'])

    def test_unresolved_symbols(self):
        gb = GraphBuilder()
        gb.add_lib(PT_SYSTEM, ELF.ELFCLASS64, 'libfoo', dt_needed=[],
                   exported_symbols={'foo', 'bar'},
                   imported_symbols={'__does_not_exist'})
        gb.resolve()

        lib = gb.graph.map_path_to_lib('/system/lib64/libfoo.so')
        self.assertEqual({'__does_not_exist'}, lib.unresolved_symbols)

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

    def test_compute_vndk_stable(self):
        gb = GraphBuilder()

        # HIDL libraries.
        gb.add_multilib(PT_SYSTEM, 'libhidlbase', extra_dir='vndk-stable')
        gb.add_multilib(PT_SYSTEM, 'libhidltransport', extra_dir='vndk-stable')
        gb.add_multilib(PT_SYSTEM, 'libhidlmemory', extra_dir='vndk-stable')
        gb.add_multilib(PT_SYSTEM, 'libfmp', extra_dir='vndk-stable')
        gb.add_multilib(PT_SYSTEM, 'libhwbinder', extra_dir='vndk-stable')

        # UI libraries.
        # TODO: Add libui.so here.

        gb.resolve()

        # Compute VNDK-stable.
        vndk_stable = set(
                lib.path for lib in gb.graph.compute_vndk_stable(False))

        for lib in ('lib', 'lib64'):
            # Check HIDL libraries.
            self.assertIn('/system/' + lib + '/vndk-stable/libhidlbase.so',
                          vndk_stable)
            self.assertIn('/system/' + lib + '/vndk-stable/libhidltransport.so',
                          vndk_stable)
            self.assertIn('/system/' + lib + '/vndk-stable/libhidlmemory.so',
                          vndk_stable)
            self.assertIn('/system/' + lib + '/vndk-stable/libfmp.so',
                          vndk_stable)
            self.assertIn('/system/' + lib + '/vndk-stable/libhwbinder.so',
                          vndk_stable)

            # TODO: Check libui.so here.

    def test_compute_vndk_stable_closure(self):
        gb = GraphBuilder()

        libc = gb.add_lib(PT_SYSTEM, ELF.ELFCLASS64, 'libc')

        libhidlbase = gb.add_lib(PT_SYSTEM, ELF.ELFCLASS64, 'libhidlbase',
                                 dt_needed=['libfoo.so'],
                                 extra_dir='vndk-stable')

        libfoo = gb.add_lib(PT_SYSTEM, ELF.ELFCLASS64, 'libfoo')

        gb.resolve()

        # Compute VNDK-stable.
        vndk_stable = gb.graph.compute_vndk_stable(False)
        vndk_stable_closure = gb.graph.compute_vndk_stable(True)

        self.assertSetEqual({libhidlbase}, vndk_stable)
        self.assertSetEqual({libhidlbase, libfoo}, vndk_stable_closure)
        self.assertNotIn(libc, vndk_stable)
        self.assertNotIn(libc, vndk_stable_closure)

    def test_compute_sp_hal(self):
        gb = GraphBuilder()

        # HIDL SP-HAL implementation.
        gb.add_multilib(PT_SYSTEM, 'gralloc.default', extra_dir='hw')
        gb.add_multilib(PT_SYSTEM, 'gralloc.chipset', extra_dir='hw')
        gb.add_multilib(PT_SYSTEM, 'android.hardware.graphics.mapper@2.0-impl',
                        extra_dir='hw')

        # NDK loader libraries should not be considered as SP-HALs.
        gb.add_multilib(PT_SYSTEM, 'libvulkan')
        gb.add_multilib(PT_SYSTEM, 'libEGL')
        gb.add_multilib(PT_SYSTEM, 'libGLESv1_CM')
        gb.add_multilib(PT_SYSTEM, 'libGLESv2')
        gb.add_multilib(PT_SYSTEM, 'libGLESv3')

        # OpenGL implementation.
        gb.add_multilib(PT_VENDOR, 'libEGL_chipset', extra_dir='egl')
        gb.add_multilib(PT_VENDOR, 'libGLESv1_CM_chipset', extra_dir='egl')
        gb.add_multilib(PT_VENDOR, 'libGLESv2_chipset', extra_dir='egl')
        gb.add_multilib(PT_VENDOR, 'libGLESv3_chipset', extra_dir='egl')

        # Renderscript implementation.
        gb.add_multilib(PT_VENDOR, 'libRSDriver_chipset')
        gb.add_multilib(PT_VENDOR, 'libPVRRS')

        # Vulkan implementation.
        gb.add_multilib(PT_VENDOR, 'vulkan.chipset', extra_dir='hw')

        # Some un-related libraries.
        gb.add_multilib(PT_SYSTEM, 'libfoo')
        gb.add_multilib(PT_VENDOR, 'libfoo')

        gb.resolve()

        # Compute SP-HAL.
        sp_hals = set(lib.path for lib in gb.graph.compute_sp_hal(set(), False))

        for lib in ('lib', 'lib64'):
            # Check HIDL SP-HAL implementation.
            self.assertIn('/system/' + lib + '/hw/gralloc.default.so', sp_hals)
            self.assertIn('/system/' + lib + '/hw/gralloc.chipset.so', sp_hals)
            self.assertIn('/system/' + lib + '/hw/'
                          'android.hardware.graphics.mapper@2.0-impl.so',
                          sp_hals)


            # Check that NDK loaders are not SP-HALs.
            self.assertNotIn('/system/' + lib + '/libvulkan.so', sp_hals)
            self.assertNotIn('/system/' + lib + '/libEGL.so', sp_hals)
            self.assertNotIn('/system/' + lib + '/libGLESv1_CM.so', sp_hals)
            self.assertNotIn('/system/' + lib + '/libGLESv2.so', sp_hals)
            self.assertNotIn('/system/' + lib + '/libGLESv3.so', sp_hals)

            # Check that OpenGL implementations are SP-HALs.
            self.assertIn('/vendor/' + lib + '/egl/libEGL_chipset.so', sp_hals)
            self.assertIn('/vendor/' + lib + '/egl/libGLESv1_CM_chipset.so',
                          sp_hals)
            self.assertIn('/vendor/' + lib + '/egl/libGLESv2_chipset.so',
                          sp_hals)
            self.assertIn('/vendor/' + lib + '/egl/libGLESv3_chipset.so',
                          sp_hals)

            # Check that Renderscript implementations are SP-HALs.
            self.assertIn('/vendor/' + lib + '/libRSDriver_chipset.so', sp_hals)
            self.assertIn('/vendor/' + lib + '/libPVRRS.so', sp_hals)

            # Check that vulkan implementation are SP-HALs.
            self.assertIn('/vendor/' + lib + '/libPVRRS.so', sp_hals)

            # Check that un-related libraries are not SP-HALs.
            self.assertNotIn('/system/' + lib + '/libfoo.so', sp_hals)
            self.assertNotIn('/vendor/' + lib + '/libfoo.so', sp_hals)

    def test_compute_sp_hal_closure(self):
        gb = GraphBuilder()

        libc = gb.add_lib(PT_SYSTEM, ELF.ELFCLASS64, 'libc')

        libhidlbase = gb.add_lib(PT_SYSTEM, ELF.ELFCLASS64, 'libhidlbase')

        libhidltransport = gb.add_lib(PT_SYSTEM, ELF.ELFCLASS64,
                                      'libhidltransport')

        gralloc_mapper = gb.add_lib(
                PT_VENDOR, ELF.ELFCLASS64,
                name='android.hardware.graphics.mapper@2.0-impl',
                dt_needed=['libhidlbase.so', 'libhidltransport.so',
                           'libc.so', 'gralloc_vnd.so'],
                extra_dir='sameprocess')

        gralloc_vnd = gb.add_lib(PT_VENDOR, ELF.ELFCLASS64, 'gralloc_vnd')

        gb.resolve()

        vndk_stable = {libhidlbase, libhidltransport}

        sp_hal = gb.graph.compute_sp_hal(vndk_stable, closure=False)
        sp_hal_closure = gb.graph.compute_sp_hal(vndk_stable, closure=True)

        self.assertSetEqual({gralloc_mapper}, sp_hal)

        self.assertSetEqual({gralloc_mapper, gralloc_vnd}, sp_hal_closure)
        self.assertNotIn(libhidlbase, sp_hal_closure)
        self.assertNotIn(libhidltransport, sp_hal_closure)
        self.assertNotIn(libc, sp_hal_closure)


if __name__ == '__main__':
    unittest.main()
