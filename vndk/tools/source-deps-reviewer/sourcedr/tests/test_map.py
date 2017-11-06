#!/usr/bin/env python3

import os
import unittest

from sourcedr.map import (
    link_build_dep_and_review_data, load_build_dep_file_from_path,
    load_build_dep_ninja, load_review_data)


TESTDATA_DIR = os.path.join(os.path.dirname(__file__), 'testdata')


class MapTest(unittest.TestCase):
    MAP_TESTDATA_DIR = os.path.join(TESTDATA_DIR, 'map')
    DEP_PATH = os.path.join(MAP_TESTDATA_DIR, 'build_dep.json')
    REVIEW_DB_PATH = os.path.join(MAP_TESTDATA_DIR, 'data.json')
    NINJA_PATH = os.path.join(MAP_TESTDATA_DIR, 'build.ninja')
    NINJA_DEP_PATH = os.path.join(MAP_TESTDATA_DIR, 'ninja_deps')


    def test_load_build_dep_file(self):
        dep = load_build_dep_file_from_path(self.DEP_PATH)

        self.assertIn('liba.so', dep)
        self.assertIn('libb.so', dep)
        self.assertIn('libc.so', dep)

        self.assertSetEqual({'a.h', 'a1.c', 'a1.o', 'a2.c', 'a2.o'},
                            dep['liba.so'])
        self.assertSetEqual({'a.h', 'b.c', 'b.o'}, dep['libb.so'])
        self.assertSetEqual({'c.c', 'c.o'}, dep['libc.so'])


    def test_load_build_dep_ninja(self):
        dep = load_build_dep_ninja(self.NINJA_PATH, self.MAP_TESTDATA_DIR,
                                   self.NINJA_DEP_PATH)

        self.assertIn('liba.so', dep)
        self.assertIn('libb.so', dep)
        self.assertIn('libc.so', dep)

        self.assertSetEqual({'a.h', 'a1.c', 'a1.o', 'a2.c', 'a2.o'},
                            dep['liba.so'])
        self.assertSetEqual({'a.h', 'b.c', 'b.o'}, dep['libb.so'])
        self.assertSetEqual({'c.c', 'c.o'}, dep['libc.so'])


    def test_load_review_data(self):
        data = load_review_data(self.REVIEW_DB_PATH)
        self.assertIn('a.h', data)
        self.assertEqual(['libx.so'], data['a.h'])


    def test_link_build_dep_and_review_data(self):
        dep = load_build_dep_file_from_path(self.DEP_PATH)
        data = load_review_data(self.REVIEW_DB_PATH)
        result = link_build_dep_and_review_data(dep, data)

        self.assertIn('liba.so', result)
        self.assertIn('libb.so', result)
        self.assertIn('libc.so', result)

        self.assertEqual(['libx.so'], result['liba.so'])
        self.assertEqual(['libx.so'], result['libb.so'])


if __name__ == '__main__':
    unittest.main()
