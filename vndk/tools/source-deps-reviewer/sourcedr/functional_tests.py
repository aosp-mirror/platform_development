#!/usr/bin/env python3

import json
import os
import tempfile
import unittest

import flask_testing

from sourcedr.codesearch import CodeSearch
from sourcedr.map import (
    load_build_dep_file_from_path, load_review_data,
    link_build_dep_and_review_data)
from sourcedr.project import Project
from sourcedr.review_db import ReviewDB
from sourcedr.server import create_app


ANDROID_ROOT = os.path.join(os.path.dirname(__file__), '..', 'sourcedr', 'test')
PROJECT_DIR = 'unittest_sourcedr_data'


class ReviewDBTest(unittest.TestCase):
    def setUp(self):
        self.csearch_index_path = 'csearchindex'
        self.review_db_path = ReviewDB.DEFAULT_NAME

    def tearDown(self):
        os.remove(self.csearch_index_path)
        os.remove(self.review_db_path)

    def test_preprocess(self):
        codesearch = CodeSearch(ANDROID_ROOT, self.csearch_index_path)
        codesearch.build_index()
        review_db = ReviewDB(ReviewDB.DEFAULT_NAME, codesearch)
        review_db.find(patterns=['dlopen'], is_regexs=[False])
        self.assertTrue(os.path.exists(ReviewDB.DEFAULT_NAME))


class ViewTest(flask_testing.TestCase):
    def create_app(self):
        self.tmp_dir = tempfile.TemporaryDirectory(prefix='test_sourcedr_data-')

        project = Project(ANDROID_ROOT, self.tmp_dir.name)
        project.update_csearch_index(True)
        self.project = project

        app = create_app(project)
        app.config['TESTING'] = True
        self.app = app
        return app

    def setUp(self):
        review_db = self.project.review_db
        review_db.find(patterns=['dlopen'], is_regexs=[False])

    def tearDown(self):
        self.tmp_dir.cleanup()

    def test_get_file(self):
        test_arg = 'example.c'
        response = self.client.get('/get_file',
                                   query_string=dict(path=test_arg))
        ret = response.json['result']
        with open(os.path.join(ANDROID_ROOT, test_arg), 'r') as f:
            self.assertEqual(ret, f.read())

    def test_load_file(self):
        test_arg = 'dlopen/test.c'
        test_arg += ':10:    handle = dlopen("libm.so.6", RTLD_LAZY);'
        response = self.client.get('/load_file',
                                   query_string=dict(path=test_arg))
        deps = json.loads(response.json['deps'])
        codes = json.loads(response.json['codes'])
        with open(self.project.review_db.path, 'r') as f:
            cdata = json.load(f)

        self.assertEqual(deps, cdata[test_arg][0])
        self.assertEqual(codes, cdata[test_arg][1])

    def test_save_all(self):
        label = os.path.abspath('sourcedr/test/dlopen/test.c')
        label += ':10:    handle = dlopen("libm.so.6", RTLD_LAZY);'
        test_arg = {
            'label': label,
            'deps': json.dumps(['this_is_a_test.so']),
            'codes': json.dumps(['arr_0', 'arr_1'])
        }
        response = self.client.get('/save_all', query_string=test_arg)
        cdata = ReviewDB(self.project.review_db.path, None).data
        self.assertEqual(['this_is_a_test.so'],  cdata[test_arg['label']][0])
        self.assertEqual(['arr_0', 'arr_1'], cdata[test_arg['label']][1])


class MapTest(unittest.TestCase):
    DEP_PATH = os.path.join('sourcedr', 'test', 'map', 'build_dep.json')
    REVIEW_DB_PATH = os.path.join('sourcedr', 'test', 'map', 'data.json')

    def test_load_build_dep_file(self):
        dep = load_build_dep_file_from_path(self.DEP_PATH)

        self.assertIn('liba.so', dep)
        self.assertIn('libb.so', dep)
        self.assertIn('libc.so', dep)

        self.assertSetEqual({'a.h', 'a1.c', 'a1.o', 'a2.c', 'a2.o'},
                            dep['liba.so'])
        self.assertSetEqual({'a.h', 'b.c', 'b.o'}, dep['libb.so'])
        self.assertSetEqual(set(), dep['libc.so'])

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
