#!/usr/bin/env python3

from __future__ import print_function

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import unittest

from vndk_definition_tool import BA_WARN, BannedLibDict


class BannedLibDictTest(unittest.TestCase):
    def test_add(self):
        d = BannedLibDict()
        d.add('libfoo.so', 'test', BA_WARN)
        x = d.get('libfoo.so')

        self.assertIsNotNone(x)
        self.assertEqual('libfoo.so', x.name)
        self.assertEqual('test', x.reason)
        self.assertEqual(BA_WARN, x.action)

    def test_get(self):
        d = BannedLibDict.create_default()
        self.assertIsNotNone(d.get('libbinder.so'))
        self.assertIsNotNone(d.get('libselinux.so'))
        self.assertIsNone(d.get('libc.so'))

    def test_is_banned(self):
        d = BannedLibDict.create_default()
        self.assertTrue(d.is_banned('/system/lib/libbinder.so'))
        self.assertTrue(d.is_banned('/system/lib/libselinux.so'))
        self.assertTrue(d.is_banned('/system/lib64/libbinder.so'))
        self.assertTrue(d.is_banned('/system/lib64/libselinux.so'))
        self.assertFalse(d.is_banned('/system/lib64/libc.so'))


if __name__ == '__main__':
    unittest.main()

