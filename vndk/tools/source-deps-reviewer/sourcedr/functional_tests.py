#!/usr/bin/env python3

from sourcedr.data_utils import *
from sourcedr.preprocess import prepare
from sourcedr.server import *

from flask import Flask, jsonify, render_template, request
from flask_testing import LiveServerTestCase, TestCase
from urllib.request import urlopen
import flask_testing
import os
import unittest

app.config['TESTING'] = True

class TestPreprocess(unittest.TestCase):
    def test_prepare(self):
        remove_data()
        prepare(android_root='sourcedr/test', pattern='dlopen', is_regex=False)
        self.assertTrue( os.path.exists(data_path) )

class TestViews(TestCase):
    def create_app(self):
        return app

    def setUp(self):
        prepare(android_root='sourcedr/test', pattern='dlopen', is_regex=False)

    def tearDown(self):
        remove_data()

    def test_get_file(self):
        test_arg = 'sourcedr/test/example.c'
        response = self.client.get('/get_file',
                                   query_string=dict(path=test_arg))
        ret = response.json['result']
        with open(test_arg, 'r') as f:
            self.assertEqual(ret, f.read())

    def test_load_file(self):
        test_arg = os.path.abspath('./sourcedr/test/dlopen/test.c:10')
        response = self.client.get('/load_file',
                                   query_string=dict(path=test_arg))
        deps = json.loads(response.json['deps'])
        codes = json.loads(response.json['codes'])
        with open(data_path, 'r') as f:
            cdata = json.load(f)

        self.assertEqual(deps, cdata[test_arg][0])
        self.assertEqual(codes, cdata[test_arg][1])

    def test_save_all(self):
        test_arg = {
            'path': 'test_path:test_line_no',
            'deps': json.dumps(['this_is_a_test.so']),
            'codes': json.dumps(['arr_0', 'arr_1'])
        }
        response = self.client.get('/save_all', query_string=test_arg)
        cdata = load_data()
        self.assertEqual(['this_is_a_test.so'],  cdata[test_arg['path']][0])
        self.assertEqual(['arr_0', 'arr_1'], cdata[test_arg['path']][1])

if __name__ == '__main__':
    unittest.main()
