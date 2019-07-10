#!/usr/bin/env python3

from __future__ import print_function

import tempfile
import unittest

from vndk_definition_tool import TaggedDict, TaggedPathDict, TaggedLibDict, \
                                 NUM_PARTITIONS, PT_SYSTEM, PT_VENDOR

from .compat import StringIO


_TEST_DATA = '''Path,Tag
/system/lib/lib_ll_ndk.so,ll-ndk
/system/lib/lib_ll_ndk_private.so,ll-ndk-private
/system/lib/lib_vndk_sp.so,vndk-sp
/system/lib/lib_vndk_sp_private.so,vndk-sp-private
/system/lib/lib_vndk.so,vndk
/system/lib/lib_system_only.so,system-only
/system/lib/lib_system_only_rs.so,system-only-rs
/vendor/lib/lib_sp_hal.so,sp-hal
/vendor/lib/lib_sp_hal_dep.so,sp-hal-dep
/vendor/lib/lib_vendor_only.so,vendor-only
/product/lib/lib_product_only.so,product-only
/system_ext/lib/lib_system_ext_only.so,system_ext-only
/system/lib/lib_remove.so,remove
/system/lib/lib_hl_ndk.so,hl-ndk
/system/lib/lib_vndk_private.so,vndk-private
/system/lib/lib_vndk_sp_both.so,vndk-sp-both
/system/lib/lib_vndk_sp_hal.so,vndk-sp-hal
/system/lib/lib_vndk_indirect.so,vndk-private
/system/lib/lib_vndk_sp_indirect.so,vndk-sp
/system/lib/lib_vndk_sp_indirect_private.so,vndk-sp-private
/system/lib/lib_fwk_only.so,system-only
/system/lib/lib_fwk_only_rs.so,system-only-rs
/vendor/lib/lib_vnd_only.so,vendor-only
'''

_TEST_DATA_ALIAS_PATHS = {
    '/system/lib/lib_hl_ndk.so',
    '/system/lib/lib_vndk_indirect.so',
    '/system/lib/lib_vndk_sp_both.so',
    '/system/lib/lib_vndk_sp_hal.so',
    '/system/lib/lib_vndk_sp_indirect.so',
    '/system/lib/lib_vndk_sp_indirect_private.so',
    '/system/lib/lib_fwk_only.so',
    '/system/lib/lib_fwk_only_rs.so',
    '/vendor/lib/lib_vnd_only.so',
}


class TaggedDictTest(unittest.TestCase):
    def test_normalize_tag(self):
        self.assertEqual('ll_ndk', TaggedDict._normalize_tag('ll-ndk'))
        self.assertEqual('ll_ndk', TaggedDict._normalize_tag('LL-NDK'))
        self.assertEqual('ll_ndk', TaggedDict._normalize_tag('LL_NDK'))
        with self.assertRaises(ValueError):
            TaggedDict._normalize_tag('unknown-lib-tag')


    def _check_tag_visibility(self, d, from_tag, visible_tags):
        for to_tag in visible_tags:
            self.assertTrue(d.is_tag_visible(from_tag, to_tag))
        for to_tag in TaggedDict.TAGS:
            self.assertEqual(d.is_tag_visible(from_tag, to_tag),
                             to_tag in visible_tags)


    def test_is_tag_visible(self):
        d = TaggedDict()

        # LL-NDK
        visible_tags = {'ll_ndk', 'll_ndk_private'}
        self._check_tag_visibility(d, 'll_ndk', visible_tags)
        self._check_tag_visibility(d, 'll_ndk_private', visible_tags)

        # VNDK-SP
        visible_tags = {
            'll_ndk', 'vndk_sp', 'vndk_sp_private', 'system_only_rs',
        }
        self._check_tag_visibility(d, 'vndk_sp', visible_tags)
        self._check_tag_visibility(d, 'vndk_sp_private', visible_tags)

        # VNDK
        visible_tags = {
            'll_ndk', 'vndk_sp', 'vndk_sp_private', 'vndk', 'vndk_private',
        }
        self._check_tag_visibility(d, 'vndk', visible_tags)

        # SYSTEM-ONLY and SYSTEM_EXT-ONLY
        visible_tags = {
            'll_ndk', 'll_ndk_private',
            'vndk_sp', 'vndk_sp_private',
            'vndk', 'vndk_private',
            'system_only', 'system_only_rs',
            'system_ext_only',
            'sp_hal'
        }
        self._check_tag_visibility(d, 'system_only', visible_tags)
        self._check_tag_visibility(d, 'system_only_rs', visible_tags)
        self._check_tag_visibility(d, 'system_ext_only', visible_tags)

        # SP-HAL
        visible_tags = {'ll_ndk', 'vndk_sp', 'sp_hal', 'sp_hal_dep'}
        self._check_tag_visibility(d, 'sp_hal', visible_tags)
        self._check_tag_visibility(d, 'sp_hal_dep', visible_tags)

        # VENDOR-ONLY
        visible_tags = {
            'll_ndk', 'vndk_sp', 'vndk', 'sp_hal', 'sp_hal_dep', 'vendor_only',
        }
        self._check_tag_visibility(d, 'vendor_only', visible_tags)

        # PRODUCT-ONLY
        visible_tags = {
            'll_ndk', 'vndk_sp', 'vndk', 'sp_hal',
            # Remove the following after VNDK-ext can be checked separately.
            'sp_hal_dep', 'vendor_only',
        }
        self._check_tag_visibility(d, 'vendor_only', visible_tags)

        # Remove
        self._check_tag_visibility(d, 'remove', set())


class TaggedPathDictTest(unittest.TestCase):
    def test_enumerate_paths(self):
        tagged_paths = TaggedPathDict()

        self.assertEqual(
            ['/system/lib/libc.so'],
            list(tagged_paths._enumerate_paths('/system/lib/libc.so')))

        self.assertEqual(
            ['/system/lib/libc.so', '/system/lib64/libc.so'],
            list(tagged_paths._enumerate_paths('/system/${LIB}/libc.so')))

        self.assertEqual(
            ['/system/lib/vndk/libutils.so',
             '/system/lib64/vndk/libutils.so'],
            list(tagged_paths._enumerate_paths(
                '/system/${LIB}/vndk${VNDK_VER}/libutils.so')))

        tagged_paths = TaggedPathDict(['current', '27', '28'])

        self.assertEqual(
            ['/system/lib/vndk/libutils.so',
             '/system/lib64/vndk/libutils.so',
             '/system/lib/vndk-27/libutils.so',
             '/system/lib64/vndk-27/libutils.so',
             '/system/lib/vndk-28/libutils.so',
             '/system/lib64/vndk-28/libutils.so'],
            list(tagged_paths._enumerate_paths(
                '/system/${LIB}/vndk${VNDK_VER}/libutils.so')))

        self.assertEqual(
            ['/system/lib/vndk/libutils.so',
             '/system/lib/vndk-27/libutils.so',
             '/system/lib/vndk-28/libutils.so'],
            list(tagged_paths._enumerate_paths(
                '/system/lib/vndk${VNDK_VER}/libutils.so')))


    def test_load_from_csv_empty(self):
        try:
            TaggedPathDict().load_from_csv(StringIO(''))
        except StopIteration:
            self.fail('empty file should be considered as a valid input')


    def test_load_from_csv_with_header(self):
        fp = StringIO('Path,Tag\nliba.so,system-only\n')
        d = TaggedPathDict()
        d.load_from_csv(fp)
        self.assertIn('liba.so', d.system_only)

        fp = StringIO('Tag,Path\nsystem-only,liba.so\n')
        d = TaggedPathDict()
        d.load_from_csv(fp)
        self.assertIn('liba.so', d.system_only)


    def test_load_from_csv_without_header(self):
        fp = StringIO('liba.so,system-only\n')
        d = TaggedPathDict()
        d.load_from_csv(fp)
        self.assertIn('liba.so', d.system_only)


    def _check_test_data_loaded(self, d):
        # Paths
        self.assertIn('/system/lib/lib_ll_ndk.so', d.ll_ndk)
        self.assertIn('/system/lib/lib_ll_ndk_private.so', d.ll_ndk_private)
        self.assertIn('/system/lib/lib_vndk_sp.so', d.vndk_sp)
        self.assertIn('/system/lib/lib_vndk_sp_private.so', d.vndk_sp_private)
        self.assertIn('/system/lib/lib_vndk.so', d.vndk)
        self.assertIn('/system/lib/lib_vndk_private.so', d.vndk_private)
        self.assertIn('/system/lib/lib_system_only.so', d.system_only)
        self.assertIn('/system/lib/lib_system_only_rs.so', d.system_only_rs)
        self.assertIn('/vendor/lib/lib_sp_hal.so', d.sp_hal)
        self.assertIn('/vendor/lib/lib_sp_hal_dep.so', d.sp_hal_dep)
        self.assertIn('/vendor/lib/lib_vendor_only.so', d.vendor_only)
        self.assertIn('/system_ext/lib/lib_system_ext_only.so',
                      d.system_ext_only)
        self.assertIn('/product/lib/lib_product_only.so', d.product_only)
        self.assertIn('/system/lib/lib_remove.so', d.remove)

        # Aliases
        self.assertIn('/system/lib/lib_hl_ndk.so', d.system_only)
        self.assertIn('/system/lib/lib_vndk_sp_both.so', d.vndk_sp)
        self.assertIn('/system/lib/lib_vndk_sp_hal.so', d.vndk_sp)


    def test_load_from_csv_tags(self):
        fp = StringIO(_TEST_DATA)
        d = TaggedPathDict()
        d.load_from_csv(fp)
        self._check_test_data_loaded(d)


    def test_create_from_csv(self):
        d = TaggedPathDict.create_from_csv(StringIO(_TEST_DATA))
        self._check_test_data_loaded(d)


    def test_create_from_csv_path(self):
        with tempfile.NamedTemporaryFile('w+') as f:
            f.write(_TEST_DATA)
            f.flush()

            d = TaggedPathDict.create_from_csv_path(f.name)
            self._check_test_data_loaded(d)


    def test_get_path_tag(self):
        fp = StringIO(_TEST_DATA)
        d = TaggedPathDict()
        d.load_from_csv(fp)

        self.assertEqual('ll_ndk', d.get_path_tag('/system/lib/lib_ll_ndk.so'))
        self.assertEqual('ll_ndk_private',
                         d.get_path_tag('/system/lib/lib_ll_ndk_private.so'))
        self.assertEqual('vndk_sp',
                         d.get_path_tag('/system/lib/lib_vndk_sp.so'))
        self.assertEqual('vndk_sp_private',
                         d.get_path_tag('/system/lib/lib_vndk_sp_private.so'))
        self.assertEqual('vndk', d.get_path_tag('/system/lib/lib_vndk.so'))
        self.assertEqual('vndk_private',
                         d.get_path_tag('/system/lib/lib_vndk_private.so'))
        self.assertEqual('system_only',
                         d.get_path_tag('/system/lib/lib_system_only.so'))
        self.assertEqual('system_only_rs',
                         d.get_path_tag('/system/lib/lib_system_only_rs.so'))
        self.assertEqual('sp_hal',
                         d.get_path_tag('/vendor/lib/lib_sp_hal.so'))
        self.assertEqual('sp_hal_dep',
                         d.get_path_tag('/vendor/lib/lib_sp_hal_dep.so'))
        self.assertEqual('vendor_only',
                         d.get_path_tag('/vendor/lib/lib_vendor_only.so'))
        self.assertEqual('remove',
                         d.get_path_tag('/system/lib/lib_remove.so'))

        # Aliases
        self.assertEqual('system_only',
                         d.get_path_tag('/system/lib/lib_hl_ndk.so'))
        self.assertEqual('vndk_sp',
                         d.get_path_tag('/system/lib/lib_vndk_sp_hal.so'))
        self.assertEqual('vndk_sp',
                         d.get_path_tag('/system/lib/lib_vndk_sp_both.so'))
        self.assertEqual('vndk_private',
                         d.get_path_tag('/system/lib/lib_vndk_indirect.so'))
        self.assertEqual('vndk_sp',
                         d.get_path_tag('/system/lib/lib_vndk_sp_indirect.so'))
        self.assertEqual('vndk_sp_private',
                         d.get_path_tag('/system/lib/' +
                                        'lib_vndk_sp_indirect_private.so'))
        self.assertEqual('system_only',
                         d.get_path_tag('/system/lib/lib_fwk_only.so'))
        self.assertEqual('system_only_rs',
                         d.get_path_tag('/system/lib/lib_fwk_only_rs.so'))
        self.assertEqual('vendor_only',
                         d.get_path_tag('/vendor/lib/lib_vendor_only.so'))

        # Unmatched paths
        self.assertEqual('system_only',
                         d.get_path_tag('/system/lib/unknown.so'))
        self.assertEqual('system_only',
                         d.get_path_tag('/data/lib/unknown.so'))
        self.assertEqual('vendor_only',
                         d.get_path_tag('/vendor/lib/unknown.so'))


    def _check_path_visibility(self, d, all_paths, from_paths, visible_paths):
        for from_path in from_paths:
            for to_path in all_paths:
                self.assertEqual(d.is_path_visible(from_path, to_path),
                                 to_path in visible_paths)


    def test_is_path_visible(self):
        fp = StringIO(_TEST_DATA)
        d = TaggedPathDict()
        d.load_from_csv(fp)

        # Collect path universe set.
        all_paths = set()
        for tag in TaggedPathDict.TAGS:
            all_paths |= getattr(d, tag)
        all_paths -= _TEST_DATA_ALIAS_PATHS

        # LL-NDK
        from_paths = {
            '/system/lib/lib_ll_ndk.so',
            '/system/lib/lib_ll_ndk_private.so',
        }
        visible_paths = {
            '/system/lib/lib_ll_ndk.so',
            '/system/lib/lib_ll_ndk_private.so',
        }
        self._check_path_visibility(d, all_paths, from_paths, visible_paths)

        # VNDK-SP
        from_paths = {
            '/system/lib/lib_vndk_sp.so',
            '/system/lib/lib_vndk_sp_private.so',
        }
        visible_paths = {
            '/system/lib/lib_ll_ndk.so',
            '/system/lib/lib_vndk_sp.so',
            '/system/lib/lib_vndk_sp_private.so',
            '/system/lib/lib_system_only_rs.so',
        }
        self._check_path_visibility(d, all_paths, from_paths, visible_paths)

        # VNDK
        from_paths = {
            '/system/lib/lib_vndk.so',
        }
        visible_paths = {
            '/system/lib/lib_ll_ndk.so',
            '/system/lib/lib_vndk_sp.so',
            '/system/lib/lib_vndk_sp_private.so',
            '/system/lib/lib_vndk.so',
            '/system/lib/lib_vndk_private.so',
        }
        self._check_path_visibility(d, all_paths, from_paths, visible_paths)

        # SYSTEM-ONLY
        from_paths = {
            '/system/lib/lib_system_only.so',
            '/system/lib/lib_system_only_rs.so',
        }
        visible_paths = {
            '/system/lib/lib_ll_ndk.so',
            '/system/lib/lib_ll_ndk_private.so',
            '/system/lib/lib_vndk_sp.so',
            '/system/lib/lib_vndk_sp_private.so',
            '/system/lib/lib_vndk.so',
            '/system/lib/lib_vndk_private.so',
            '/system/lib/lib_system_only.so',
            '/system/lib/lib_system_only_rs.so',
            '/vendor/lib/lib_sp_hal.so',
            '/system_ext/lib/lib_system_ext_only.so',
        }
        self._check_path_visibility(d, all_paths, from_paths, visible_paths)

        # SP-HAL
        from_paths = {
            '/vendor/lib/lib_sp_hal.so',
            '/vendor/lib/lib_sp_hal_dep.so',
        }
        visible_paths = {
            '/system/lib/lib_ll_ndk.so',
            '/system/lib/lib_vndk_sp.so',
            '/vendor/lib/lib_sp_hal.so',
            '/vendor/lib/lib_sp_hal_dep.so',
        }
        self._check_path_visibility(d, all_paths, from_paths, visible_paths)

        # VENDOR-ONLY
        from_paths = {
            '/vendor/lib/lib_vendor_only.so',
        }
        visible_paths = {
            '/system/lib/lib_ll_ndk.so',
            '/system/lib/lib_vndk_sp.so',
            '/system/lib/lib_vndk.so',
            '/vendor/lib/lib_sp_hal.so',
            '/vendor/lib/lib_sp_hal_dep.so',
            '/vendor/lib/lib_vendor_only.so',
        }
        self._check_path_visibility(d, all_paths, from_paths, visible_paths)


class MockSPLibResult(object):
    def __init__(self, sp_hal, sp_hal_dep):
        self.sp_hal = sp_hal
        self.sp_hal_dep = sp_hal_dep


class MockELFLinkData(object):
    def __init__(self, path):
        self.path = path


class MockELFGraph(object):
    def __init__(self):
        self.lib_pt = [dict() for i in range(NUM_PARTITIONS)]


    def add(self, path):
        partition = PT_VENDOR if path.startswith('/vendor') else PT_SYSTEM
        lib = MockELFLinkData(path)
        self.lib_pt[partition][path] = lib
        return lib


    def compute_sp_lib(self, generic_refs=None):
        vendor_libs = self.lib_pt[PT_VENDOR].values()
        return MockSPLibResult(
            set(v for v in vendor_libs if 'lib_sp_hal.so' in v.path),
            set(v for v in vendor_libs if 'lib_sp_hal_dep.so' in v.path))


class TaggedLibDictTest(unittest.TestCase):
    def setUp(self):
        self.tagged_paths = TaggedPathDict.create_from_csv(StringIO(_TEST_DATA))

        self.graph = MockELFGraph()

        self.lib_ll_ndk = self.graph.add('/system/lib/lib_ll_ndk.so')
        self.lib_ll_ndk_private = \
            self.graph.add('/system/lib/lib_ll_ndk_private.so')

        self.lib_vndk_sp = self.graph.add('/system/lib/lib_vndk_sp.so')
        self.lib_vndk_sp_private = \
            self.graph.add('/system/lib/lib_vndk_sp_private.so')

        self.lib_vndk = self.graph.add('/system/lib/lib_vndk.so')

        self.lib_system_only = \
            self.graph.add('/system/lib/lib_system_only.so')
        self.lib_system_only_rs = \
            self.graph.add('/system/lib/lib_system_only_rs.so')

        self.lib_sp_hal = self.graph.add('/vendor/lib/lib_sp_hal.so')
        self.lib_sp_hal_dep = self.graph.add('/vendor/lib/lib_sp_hal_dep.so')

        self.lib_vendor_only = self.graph.add('/vendor/lib/lib_vendor_only.so')

        self.tagged_libs = TaggedLibDict.create_from_graph(
            self.graph, self.tagged_paths)


    def test_create_from_graph(self):
        self.assertIn(self.lib_ll_ndk, self.tagged_libs.ll_ndk)
        self.assertIn(self.lib_ll_ndk_private,
                      self.tagged_libs.ll_ndk_private)
        self.assertIn(self.lib_vndk_sp, self.tagged_libs.vndk_sp)
        self.assertIn(self.lib_vndk_sp_private,
                      self.tagged_libs.vndk_sp_private)

        self.assertIn(self.lib_vndk, self.tagged_libs.vndk)

        self.assertIn(self.lib_system_only, self.tagged_libs.system_only)
        self.assertIn(self.lib_system_only_rs, self.tagged_libs.system_only_rs)

        self.assertIn(self.lib_sp_hal, self.tagged_libs.sp_hal)
        self.assertIn(self.lib_sp_hal_dep, self.tagged_libs.sp_hal_dep)
        self.assertIn(self.lib_vendor_only, self.tagged_libs.vendor_only)


    def test_get_path_tag(self):
        d = self.tagged_libs

        self.assertEqual('ll_ndk', d.get_path_tag(self.lib_ll_ndk))
        self.assertEqual('ll_ndk_private',
                         d.get_path_tag(self.lib_ll_ndk_private))
        self.assertEqual('vndk_sp', d.get_path_tag(self.lib_vndk_sp))
        self.assertEqual('vndk_sp_private',
                         d.get_path_tag(self.lib_vndk_sp_private))
        self.assertEqual('vndk', d.get_path_tag(self.lib_vndk))
        self.assertEqual('system_only', d.get_path_tag(self.lib_system_only))
        self.assertEqual('system_only_rs',
                         d.get_path_tag(self.lib_system_only_rs))
        self.assertEqual('sp_hal', d.get_path_tag(self.lib_sp_hal))
        self.assertEqual('sp_hal_dep', d.get_path_tag(self.lib_sp_hal_dep))
        self.assertEqual('vendor_only', d.get_path_tag(self.lib_vendor_only))

        # Unmatched paths
        tag = d.get_path_tag(MockELFLinkData('/system/lib/unknown.so'))
        self.assertEqual('system_only', tag)
        tag = d.get_path_tag(MockELFLinkData('/data/lib/unknown.so'))
        self.assertEqual('system_only', tag)
        tag = d.get_path_tag(MockELFLinkData('/vendor/lib/unknown.so'))
        self.assertEqual('vendor_only', tag)
