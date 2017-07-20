#!/usr/bin/env python3

from __future__ import print_function

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import unittest

from vndk_definition_tool import ELFLinkData, PT_SYSTEM, PT_VENDOR


class ELFLinkDataTest(unittest.TestCase):
    def setUp(self):
        self.x = ELFLinkData(PT_SYSTEM, '/system/lib/libx.so', None, 0)
        self.y = ELFLinkData(PT_SYSTEM, '/system/lib/liby.so', None, 0)
        self.z = ELFLinkData(PT_SYSTEM, '/system/lib/libz.so', None, 0)
        self.w = ELFLinkData(PT_SYSTEM, '/system/lib/libw.so', None, 0)
        self.v = ELFLinkData(PT_VENDOR, '/vendor/lib/libv.so', None, 0)

        self.x.add_dep(self.y, ELFLinkData.NEEDED)
        self.x.add_dep(self.z, ELFLinkData.DLOPEN)

        self.z.add_dep(self.w, ELFLinkData.NEEDED)
        self.z.add_dep(self.w, ELFLinkData.DLOPEN)

    def test_add_dep_and_accessors(self):
        self.assertIn(self.y, self.x.dt_deps)
        self.assertIn(self.x, self.y.dt_users)
        self.assertNotIn(self.y, self.x.dl_deps)
        self.assertNotIn(self.x, self.y.dl_users)

        self.assertIn(self.z, self.x.dl_deps)
        self.assertIn(self.x, self.z.dl_users)
        self.assertNotIn(self.z, self.x.dt_deps)
        self.assertNotIn(self.x, self.z.dt_users)

    def test_remove_dep(self):
        self.assertIn(self.y, self.x.dt_deps)
        self.assertIn(self.x, self.y.dt_users)

        with self.assertRaises(KeyError):
            self.x.remove_dep(self.y, ELFLinkData.DLOPEN)
        self.assertIn(self.y, self.x.dt_deps)
        self.assertIn(self.x, self.y.dt_users)

        self.x.remove_dep(self.y, ELFLinkData.NEEDED)
        self.assertNotIn(self.y, self.x.dt_deps)
        self.assertNotIn(self.x, self.y.dt_users)

    def test_num_deps(self):
        self.assertEqual(2, self.x.num_deps)
        self.assertEqual(0, self.y.num_deps)
        self.assertEqual(0, self.w.num_deps)
        self.assertEqual(0, self.v.num_deps)

        # NEEDED and DLOPEN are counted twice.
        self.assertEqual(2, self.z.num_deps)

    def test_num_users(self):
        self.assertEqual(0, self.x.num_users)
        self.assertEqual(1, self.y.num_users)
        self.assertEqual(1, self.z.num_users)
        self.assertEqual(0, self.v.num_users)

        # NEEDED and DLOPEN are counted twice.
        self.assertEqual(2, self.w.num_users)

    def test_has_dep(self):
        self.assertTrue(self.x.has_dep(self.y))
        self.assertTrue(self.x.has_dep(self.z))
        self.assertFalse(self.x.has_dep(self.x))
        self.assertFalse(self.x.has_dep(self.w))

    def test_has_user(self):
        self.assertTrue(self.y.has_user(self.x))
        self.assertTrue(self.z.has_user(self.x))
        self.assertFalse(self.x.has_user(self.x))
        self.assertFalse(self.w.has_user(self.x))

    def test_is_system_lib(self):
        self.assertTrue(self.x.is_system_lib())
        self.assertFalse(self.v.is_system_lib())

    def test_get_dep_linked_symbols(self):
        self.x.linked_symbols['c'] = self.y
        self.x.linked_symbols['b'] = self.y
        self.x.linked_symbols['a'] = self.y

        self.x.linked_symbols['w'] = self.z
        self.x.linked_symbols['z'] = self.z
        self.x.linked_symbols['y'] = self.z
        self.x.linked_symbols['x'] = self.z

        self.assertEqual(['a', 'b', 'c'],
                         self.x.get_dep_linked_symbols(self.y))

        self.assertEqual(['w', 'x', 'y', 'z'],
                         self.x.get_dep_linked_symbols(self.z))


if __name__ == '__main__':
    unittest.main()
