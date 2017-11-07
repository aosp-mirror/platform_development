#!/usr/bin/env python3

import os
import unittest

from sourcedr.codesearch import CodeSearch
from sourcedr.review_db import ReviewDB


TESTDATA_DIR = os.path.join(os.path.dirname(__file__), 'testdata')
ANDROID_DIR = os.path.join(TESTDATA_DIR, 'android_src')


class ReviewDBTest(unittest.TestCase):
    def setUp(self):
        self.csearch_index_path = 'csearchindex'
        self.review_db_path = ReviewDB.DEFAULT_NAME


    def tearDown(self):
        os.remove(self.csearch_index_path)
        os.remove(self.review_db_path)


    def test_preprocess(self):
        codesearch = CodeSearch(ANDROID_DIR, self.csearch_index_path)
        codesearch.build_index()
        review_db = ReviewDB(ReviewDB.DEFAULT_NAME, codesearch)
        review_db.find(patterns=['dlopen'], is_regexs=[False])
        self.assertTrue(os.path.exists(ReviewDB.DEFAULT_NAME))


if __name__ == '__main__':
    unittest.main()
