#!/usr/bin/env python3

import os
import unittest

from vndk_definition_tool import scandir


class ScanDirTest(unittest.TestCase):
    def test_scandir(self):
        testdata_dir = os.path.join(
            os.path.dirname(__file__), 'testdata', 'test_scandir')

        num_entries = 0
        for ent in scandir(testdata_dir):
            num_entries += 1

            self.assertEqual(ent.path, os.path.join(testdata_dir, ent.name))

            if ent.name == 'test_file':
                self.assertTrue(ent.is_file())
                self.assertFalse(ent.is_dir())
                self.assertFalse(ent.is_symlink())
            elif ent.name == 'test_dir':
                self.assertFalse(ent.is_file())
                self.assertTrue(ent.is_dir())
                self.assertFalse(ent.is_symlink())
            elif ent.name == 'link_test_file':
                self.assertTrue(ent.is_file())
                self.assertFalse(ent.is_file(follow_symlinks=False))
                self.assertFalse(ent.is_dir())
                self.assertFalse(ent.is_dir(follow_symlinks=False))
                self.assertTrue(ent.is_symlink())
            elif ent.name == 'link_test_dir':
                self.assertFalse(ent.is_file())
                self.assertFalse(ent.is_file(follow_symlinks=False))
                self.assertTrue(ent.is_dir())
                self.assertFalse(ent.is_dir(follow_symlinks=False))
                self.assertTrue(ent.is_symlink())
            elif ent.name == 'link_does_not_exist':
                self.assertFalse(ent.is_file())
                self.assertFalse(ent.is_file(follow_symlinks=False))
                self.assertFalse(ent.is_dir())
                self.assertFalse(ent.is_dir(follow_symlinks=False))
                self.assertTrue(ent.is_symlink())
            else:
                self.fail('unexpected filename: ' + ent.name)

        self.assertEqual(num_entries, 5)
