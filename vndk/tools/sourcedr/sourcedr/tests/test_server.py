#!/usr/bin/env python3

import json
import os
import tempfile
import unittest

import flask_testing

from sourcedr.project import Project
from sourcedr.review_db import ReviewDB
from sourcedr.server import create_app


TESTDATA_DIR = os.path.join(os.path.dirname(__file__), 'testdata')
ANDROID_DIR = os.path.join(TESTDATA_DIR, 'android_src')


class ViewTest(flask_testing.TestCase):
    def create_app(self):
        self.tmp_dir = tempfile.TemporaryDirectory(prefix='test_sourcedr_')
        project = Project.get_or_create_project_dir(
                self.tmp_dir.name, ANDROID_DIR)
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
        with open(os.path.join(ANDROID_DIR, test_arg), 'r') as f:
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


if __name__ == '__main__':
    unittest.main()
