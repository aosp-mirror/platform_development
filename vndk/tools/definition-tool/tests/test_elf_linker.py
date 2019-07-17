#!/usr/bin/env python3

import re
import tempfile

from vndk_definition_tool import (
    ELF, ELFLinker, GenericRefs, PT_SYSTEM, PT_VENDOR, VNDKLibDir)

from .compat import StringIO, TestCase, patch
from .utils import GraphBuilder


class ELFLinkerTest(TestCase):
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


    def test_get_lib(self):
        gb = self._create_normal_graph()
        graph = gb.graph

        node = graph.get_lib('/system/lib/libc.so')
        self.assertEqual(gb.libc_32, node)
        self.assertEqual('/system/lib/libc.so', node.path)

        node = graph.get_lib('/system/lib64/libdl.so')
        self.assertEqual(gb.libdl_64, node)
        self.assertEqual('/system/lib64/libdl.so', node.path)

        node = graph.get_lib('/vendor/lib64/libEGL.so')
        self.assertEqual(gb.libEGL_64, node)
        self.assertEqual('/vendor/lib64/libEGL.so', node.path)

        self.assertEqual(None, graph.get_lib('/no/such/path.so'))


    def test_map_paths_to_libs(self):
        gb = self._create_normal_graph()
        graph = gb.graph

        bad = []
        paths = ['/system/lib/libc.so', '/system/lib/libdl.so']
        nodes = graph.get_libs(paths, bad.append)

        self.assertEqual([], bad)
        self.assertEqual(2, len(nodes))
        self.assertEqual(paths, self._get_paths_from_nodes(nodes))

        bad = []
        paths = ['/no/such/path.so', '/system/lib64/libdl.so']
        nodes = graph.get_libs(paths, bad.append)
        self.assertEqual(['/no/such/path.so'], bad)
        self.assertEqual(['/system/lib64/libdl.so'],
                         self._get_paths_from_nodes(nodes))


    def test_elf_class_and_partitions(self):
        gb = self._create_normal_graph()
        graph = gb.graph
        self.assertEqual(5, len(graph.lib_pt[PT_SYSTEM].lib32))
        self.assertEqual(5, len(graph.lib_pt[PT_SYSTEM].lib64))
        self.assertEqual(1, len(graph.lib_pt[PT_VENDOR].lib32))
        self.assertEqual(1, len(graph.lib_pt[PT_VENDOR].lib64))


    def test_deps(self):
        gb = self._create_normal_graph()

        # Check the dependencies of libc.so.
        node = gb.graph.get_lib('/system/lib/libc.so')
        self.assertEqual(['/system/lib/libdl.so', '/system/lib/libm.so'],
                         self._get_paths_from_nodes(node.deps_all))

        # Check the dependencies of libRS.so.
        node = gb.graph.get_lib('/system/lib64/libRS.so')
        self.assertEqual(['/system/lib64/libdl.so'],
                         self._get_paths_from_nodes(node.deps_all))

        # Check the dependencies of libEGL.so.
        node = gb.graph.get_lib('/vendor/lib64/libEGL.so')
        self.assertEqual(['/system/lib64/libc.so', '/system/lib64/libcutils.so',
                          '/system/lib64/libdl.so'],
                         self._get_paths_from_nodes(node.deps_all))


    def test_linked_symbols(self):
        gb = self._create_normal_graph()
        graph = gb.graph

        # Check the unresolved symbols.
        for lib_set in graph.lib_pt:
            for lib in lib_set.values():
                self.assertEqual(set(), lib.unresolved_symbols)

        # Check the linked symbols.
        for lib in ('lib', 'lib64'):
            libdl = graph.get_lib('/system/' + lib + '/libdl.so')
            libm = graph.get_lib('/system/' + lib + '/libm.so')
            libc = graph.get_lib('/system/' + lib + '/libc.so')
            libRS = graph.get_lib('/system/' + lib + '/libRS.so')
            libcutils = \
                    graph.get_lib('/system/' + lib + '/libcutils.so')
            libEGL = graph.get_lib('/vendor/' + lib + '/libEGL.so')

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

        lib = gb.graph.get_lib('/system/lib64/libfoo.so')
        self.assertEqual({'__does_not_exist'}, lib.unresolved_symbols)


    def test_users(self):
        gb = self._create_normal_graph()
        graph = gb.graph

        # Check the users of libc.so.
        node = graph.get_lib('/system/lib/libc.so')
        self.assertEqual(['/system/lib/libcutils.so', '/vendor/lib/libEGL.so'],
                         self._get_paths_from_nodes(node.users_all))

        # Check the users of libdl.so.
        node = graph.get_lib('/system/lib/libdl.so')
        self.assertEqual(['/system/lib/libRS.so', '/system/lib/libc.so',
                          '/system/lib/libcutils.so', '/vendor/lib/libEGL.so'],
                         self._get_paths_from_nodes(node.users_all))

        # Check the users of libRS.so.
        node = graph.get_lib('/system/lib64/libRS.so')
        self.assertEqual([], self._get_paths_from_nodes(node.users_all))

        # Check the users of libEGL.so.
        node = graph.get_lib('/vendor/lib64/libEGL.so')
        self.assertEqual([], self._get_paths_from_nodes(node.users_all))


    def test_compute_predefined_sp_hal(self):
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
        gb.add_multilib(PT_VENDOR, 'libGLES_chipset', extra_dir='egl')
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
        sp_hals = set(lib.path for lib in gb.graph.compute_predefined_sp_hal())

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
            self.assertIn('/vendor/' + lib + '/egl/libGLES_chipset.so',
                          sp_hals)
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


    def test_compute_sp_lib(self):
        # Create graph.
        gb = GraphBuilder()

        # LL-NDK (should be excluded from result)
        gb.add_multilib(PT_SYSTEM, 'libc')

        libEGL_32, libEGL_64 = \
                gb.add_multilib(PT_SYSTEM, 'libEGL',
                                dt_needed=['libc.so', 'libutils.so'])

        # LL-NDK dependencies
        gb.add_multilib(PT_SYSTEM, 'libutils',
                        dt_needed=['libc.so', 'libcutils.so'])

        # VNDK-SP used by both LL-NDK and SP-HAL
        gb.add_multilib(PT_SYSTEM, 'libsp_both_vs')

        # VNDK-SP used by LL-NDK
        gb.add_multilib(PT_SYSTEM, 'libcutils_dep', dt_needed=['libc.so'])
        gb.add_multilib(PT_SYSTEM, 'libcutils',
                        dt_needed=['libc.so', 'libcutils_dep.so',
                                   'libsp_both_vs.so'])

        # VNDK-SP used by SP-HAL
        gb.add_multilib(PT_SYSTEM, 'libhidlbase')
        gb.add_multilib(PT_SYSTEM, 'libhidlmemory',
                        dt_needed=['libhidlbase.so', 'libsp_both_vs.so'])

        # SP-HAL dependencies
        gb.add_multilib(PT_VENDOR, 'libllvm_vendor_dep')
        gb.add_multilib(PT_VENDOR, 'libllvm_vendor',
                        dt_needed=['libc.so', 'libllvm_vendor_dep.so'])

        # SP-HAL
        libEGL_chipset_32, libEGL_chipset_64 = \
                gb.add_multilib(PT_VENDOR, 'libEGL_chipset', extra_dir='egl',
                                dt_needed=['libc.so', 'libllvm_vendor.so',
                                           'libhidlmemory.so'])

        gb.resolve()

        # Add dlopen() dependencies from libEGL to libEGL_chipset.
        libEGL_32.add_dlopen_dep(libEGL_chipset_32)
        libEGL_64.add_dlopen_dep(libEGL_chipset_64)

        # Create generic reference.
        class MockGenericRefs(object):
            # pylint: disable=too-few-public-methods
            def classify_lib(self, lib):
                if 'libllvm_vendor' in lib.path:
                    return GenericRefs.NEW_LIB
                return GenericRefs.EXPORT_EQUAL

        sp_lib = gb.graph.compute_sp_lib(MockGenericRefs())

        self.assertEqual(2 * 1, len(sp_lib.sp_hal))
        self.assertEqual(2 * 2, len(sp_lib.sp_hal_dep))
        self.assertEqual(2 * 2, len(sp_lib.vndk_sp_hal))
        self.assertEqual(2 * 2, len(sp_lib.ll_ndk))
        self.assertEqual(2 * 3, len(sp_lib.ll_ndk_private))
        self.assertEqual(2 * 1, len(sp_lib.vndk_sp_both))

        sp_hal = self._get_paths_from_nodes(sp_lib.sp_hal)
        sp_hal_dep = self._get_paths_from_nodes(sp_lib.sp_hal_dep)
        vndk_sp_hal = self._get_paths_from_nodes(sp_lib.vndk_sp_hal)

        ll_ndk = self._get_paths_from_nodes(sp_lib.ll_ndk)
        ll_ndk_private = self._get_paths_from_nodes(sp_lib.ll_ndk_private)

        vndk_sp_both = self._get_paths_from_nodes(sp_lib.vndk_sp_both)

        for lib_dir in ('lib', 'lib64'):
            # VNDK-SP used by both LL-NDK and SP-HAL
            self.assertIn('/system/{}/libsp_both_vs.so'.format(lib_dir),
                          vndk_sp_both)

            # VNDK-SP used by LL-NDK
            self.assertIn('/system/{}/libcutils.so'.format(lib_dir),
                          ll_ndk_private)
            self.assertIn('/system/{}/libcutils_dep.so'.format(lib_dir),
                          ll_ndk_private)
            self.assertIn('/system/{}/libutils.so'.format(lib_dir),
                          ll_ndk_private)

            # VNDK-SP used by SP-HAL
            self.assertIn('/system/{}/libhidlbase.so'.format(lib_dir),
                          vndk_sp_hal)
            self.assertIn('/system/{}/libhidlmemory.so'.format(lib_dir),
                          vndk_sp_hal)

            # SP-HAL dependencies
            self.assertIn('/vendor/{}/libllvm_vendor.so'.format(lib_dir),
                          sp_hal_dep)
            self.assertIn('/vendor/{}/libllvm_vendor_dep.so'.format(lib_dir),
                          sp_hal_dep)

            # SP-HAL
            self.assertIn('/vendor/{}/egl/libEGL_chipset.so'.format(lib_dir),
                          sp_hal)

            # LL-NDK
            self.assertIn('/system/{}/libEGL.so'.format(lib_dir), ll_ndk)
            self.assertIn('/system/{}/libc.so'.format(lib_dir), ll_ndk)

            # LL-NDK must not in sp_hal, sp_hal_dep, and vndk_sp_hal.
            libc_path = '/system/{}/libc.so'.format(lib_dir)
            self.assertNotIn(libc_path, sp_hal)
            self.assertNotIn(libc_path, sp_hal_dep)
            self.assertNotIn(libc_path, vndk_sp_hal)
            self.assertNotIn(libc_path, ll_ndk_private)


    def test_link_vndk_ver_dirs(self):
        gb = GraphBuilder()

        libc_32, libc_64 = gb.add_multilib(PT_SYSTEM, 'libc')

        libvndk_a_32, libvndk_a_64 = gb.add_multilib(
            PT_SYSTEM, 'libvndk_a', extra_dir='vndk-28',
            dt_needed=['libc.so', 'libvndk_b.so', 'libvndk_sp_b.so'])

        libvndk_b_32, libvndk_b_64 = gb.add_multilib(
            PT_SYSTEM, 'libvndk_b', extra_dir='vndk-28',
            dt_needed=['libc.so', 'libvndk_sp_b.so'])

        libvndk_c_32, libvndk_c_64 = gb.add_multilib(
            PT_VENDOR, 'libvndk_c', extra_dir='vndk-28',
            dt_needed=['libc.so', 'libvndk_d.so', 'libvndk_sp_d.so'])

        libvndk_d_32, libvndk_d_64 = gb.add_multilib(
            PT_VENDOR, 'libvndk_d', extra_dir='vndk-28',
            dt_needed=['libc.so', 'libvndk_sp_d.so'])

        libvndk_sp_a_32, libvndk_sp_a_64 = gb.add_multilib(
            PT_SYSTEM, 'libvndk_sp_a', extra_dir='vndk-sp-28',
            dt_needed=['libc.so', 'libvndk_sp_b.so'])

        libvndk_sp_b_32, libvndk_sp_b_64 = gb.add_multilib(
            PT_SYSTEM, 'libvndk_sp_b', extra_dir='vndk-sp-28',
            dt_needed=['libc.so'])

        libvndk_sp_c_32, libvndk_sp_c_64 = gb.add_multilib(
            PT_VENDOR, 'libvndk_sp_c', extra_dir='vndk-sp-28',
            dt_needed=['libc.so', 'libvndk_sp_d.so'])

        libvndk_sp_d_32, libvndk_sp_d_64 = gb.add_multilib(
            PT_VENDOR, 'libvndk_sp_d', extra_dir='vndk-sp-28',
            dt_needed=['libc.so'])

        gb.resolve(VNDKLibDir.create_from_version('28'), '28')

        # 32-bit shared libraries
        self.assertIn(libc_32, libvndk_a_32.deps_all)
        self.assertIn(libc_32, libvndk_b_32.deps_all)
        self.assertIn(libc_32, libvndk_c_32.deps_all)
        self.assertIn(libc_32, libvndk_d_32.deps_all)
        self.assertIn(libc_32, libvndk_sp_a_32.deps_all)
        self.assertIn(libc_32, libvndk_sp_b_32.deps_all)
        self.assertIn(libc_32, libvndk_sp_c_32.deps_all)
        self.assertIn(libc_32, libvndk_sp_d_32.deps_all)

        self.assertIn(libvndk_b_32, libvndk_a_32.deps_all)
        self.assertIn(libvndk_sp_b_32, libvndk_a_32.deps_all)
        self.assertIn(libvndk_sp_b_32, libvndk_b_32.deps_all)
        self.assertIn(libvndk_sp_b_32, libvndk_sp_a_32.deps_all)

        self.assertIn(libvndk_d_32, libvndk_c_32.deps_all)
        self.assertIn(libvndk_sp_d_32, libvndk_c_32.deps_all)
        self.assertIn(libvndk_sp_d_32, libvndk_d_32.deps_all)
        self.assertIn(libvndk_sp_d_32, libvndk_sp_c_32.deps_all)

        # 64-bit shared libraries
        self.assertIn(libc_64, libvndk_a_64.deps_all)
        self.assertIn(libc_64, libvndk_b_64.deps_all)
        self.assertIn(libc_64, libvndk_c_64.deps_all)
        self.assertIn(libc_64, libvndk_d_64.deps_all)
        self.assertIn(libc_64, libvndk_sp_a_64.deps_all)
        self.assertIn(libc_64, libvndk_sp_b_64.deps_all)
        self.assertIn(libc_64, libvndk_sp_c_64.deps_all)
        self.assertIn(libc_64, libvndk_sp_d_64.deps_all)

        self.assertIn(libvndk_b_64, libvndk_a_64.deps_all)
        self.assertIn(libvndk_sp_b_64, libvndk_a_64.deps_all)
        self.assertIn(libvndk_sp_b_64, libvndk_b_64.deps_all)
        self.assertIn(libvndk_sp_b_64, libvndk_sp_a_64.deps_all)

        self.assertIn(libvndk_d_64, libvndk_c_64.deps_all)
        self.assertIn(libvndk_sp_d_64, libvndk_c_64.deps_all)
        self.assertIn(libvndk_sp_d_64, libvndk_d_64.deps_all)
        self.assertIn(libvndk_sp_d_64, libvndk_sp_c_64.deps_all)


    def test_rewrite_apex_modules(self):
        graph = ELFLinker()

        libfoo = graph.add_lib(PT_SYSTEM, '/system/apex/foo/lib/libfoo.so',
                               ELF(ELF.ELFCLASS32, ELF.ELFDATA2LSB))
        libbar = graph.add_lib(PT_SYSTEM, '/system/apex/bar/lib/libbar.so',
                               ELF(ELF.ELFCLASS32, ELF.ELFDATA2LSB))

        graph.rewrite_apex_modules()

        self.assertEqual(libfoo.path, '/apex/foo/lib/libfoo.so')
        self.assertEqual(libbar.path, '/apex/bar/lib/libbar.so')


    def test_link_apex_modules(self):
        graph = ELFLinker()

        libfoo = graph.add_lib(PT_SYSTEM, '/system/apex/foo/lib/libfoo.so',
                               ELF(ELF.ELFCLASS32, ELF.ELFDATA2LSB))
        libbar = graph.add_lib(PT_SYSTEM, '/system/lib/libbar.so',
                               ELF(ELF.ELFCLASS32, ELF.ELFDATA2LSB,
                                   dt_needed=['libfoo.so']))

        graph.rewrite_apex_modules()
        graph.resolve_deps()

        self.assertIn(libfoo, libbar.deps_all)


    def test_link_apex_bionic(self):
        graph = ELFLinker()

        libc = graph.add_lib(
            PT_SYSTEM, '/system/apex/com.android.runtime/lib/bionic/libc.so',
            ELF(ELF.ELFCLASS32, ELF.ELFDATA2LSB))
        libbar = graph.add_lib(
            PT_SYSTEM, '/system/lib/libbar.so',
            ELF(ELF.ELFCLASS32, ELF.ELFDATA2LSB, dt_needed=['libc.so']))

        graph.rewrite_apex_modules()
        graph.resolve_deps()

        self.assertIn(libc, libbar.deps_all)


class ELFLinkerDlopenDepsTest(TestCase):
    def test_add_dlopen_deps(self):
        gb = GraphBuilder()
        liba = gb.add_lib32(PT_SYSTEM, 'liba')
        libb = gb.add_lib32(PT_SYSTEM, 'libb')
        gb.resolve()

        with tempfile.NamedTemporaryFile(mode='w') as tmp_file:
            tmp_file.write('/system/lib/liba.so: /system/lib/libb.so')
            tmp_file.seek(0)
            gb.graph.add_dlopen_deps(tmp_file.name)

        self.assertIn(libb, liba.deps_dlopen)
        self.assertIn(liba, libb.users_dlopen)

        self.assertNotIn(libb, liba.deps_needed)
        self.assertNotIn(liba, libb.users_needed)


    def test_add_dlopen_deps_lib_subst(self):
        gb = GraphBuilder()
        liba_32, liba_64 = gb.add_multilib(PT_SYSTEM, 'liba')
        libb_32, libb_64 = gb.add_multilib(PT_SYSTEM, 'libb')
        gb.resolve()

        with tempfile.NamedTemporaryFile(mode='w') as tmp_file:
            tmp_file.write('/system/${LIB}/liba.so: /system/${LIB}/libb.so')
            tmp_file.seek(0)
            gb.graph.add_dlopen_deps(tmp_file.name)

        self.assertIn(libb_32, liba_32.deps_dlopen)
        self.assertIn(liba_32, libb_32.users_dlopen)

        self.assertIn(libb_64, liba_64.deps_dlopen)
        self.assertIn(liba_64, libb_64.users_dlopen)


    def test_add_dlopen_deps_lib_subset_single_bitness(self):
        gb = GraphBuilder()
        liba_32, liba_64 = gb.add_multilib(PT_SYSTEM, 'liba')
        libb_32 = gb.add_lib32(PT_SYSTEM, 'libb')
        gb.resolve()

        with tempfile.NamedTemporaryFile(mode='w') as tmp_file:
            tmp_file.write('/system/${LIB}/libb.so: /system/${LIB}/liba.so')
            tmp_file.seek(0)

            stderr = StringIO()
            with patch('sys.stderr', stderr):
                gb.graph.add_dlopen_deps(tmp_file.name)

            self.assertEqual('', stderr.getvalue())

        self.assertIn(liba_32, libb_32.deps_dlopen)
        self.assertIn(libb_32, liba_32.users_dlopen)

        self.assertEqual(0, len(liba_64.users_dlopen))


    def test_add_dlopen_deps_regex(self):
        gb = GraphBuilder()
        liba = gb.add_lib32(PT_SYSTEM, 'liba')
        libb = gb.add_lib32(PT_SYSTEM, 'libb')
        gb.resolve()

        with tempfile.NamedTemporaryFile(mode='w') as tmp_file:
            tmp_file.write('[regex].*libb\\.so: [regex].*/${LIB}/liba\\.so')
            tmp_file.seek(0)

            stderr = StringIO()
            with patch('sys.stderr', stderr):
                gb.graph.add_dlopen_deps(tmp_file.name)

            self.assertEqual('', stderr.getvalue())

        self.assertIn(liba, libb.deps_dlopen)
        self.assertIn(libb, liba.users_dlopen)


    def test_add_dlopen_deps_error(self):
        gb = GraphBuilder()
        gb.add_lib32(PT_SYSTEM, 'liba')
        gb.add_lib32(PT_SYSTEM, 'libb')
        gb.resolve()

        with tempfile.NamedTemporaryFile(mode='w') as tmp_file:
            tmp_file.write('/system/lib/libc.so: /system/lib/libd.so')
            tmp_file.seek(0)

            stderr = StringIO()
            with patch('sys.stderr', stderr):
                gb.graph.add_dlopen_deps(tmp_file.name)

            self.assertRegex(
                stderr.getvalue(),
                'error:' + re.escape(tmp_file.name) + ':1: ' +
                'Failed to add dlopen dependency from .* to .*\\.\n')
