#!/usr/bin/env python3

"""SourceDR project configurations and databases.

`Project` class holds configuration files, review databases, pattern databases,
and `codesearch` index files.
"""

import contextlib
import os

from sourcedr.codesearch import CodeSearch
from sourcedr.pattern_db import PatternDB
from sourcedr.review_db import ReviewDB


class Project(object):
    """SourceDR project configuration files and databases.
    """

    def __init__(self, android_root, project_dir):
        self.android_root = os.path.abspath(android_root)
        self.project_dir = os.path.abspath(project_dir)
        self._tmp_dir = os.path.join(self.project_dir, 'tmp')

        os.makedirs(self.project_dir, exist_ok=True)
        os.makedirs(self._tmp_dir, exist_ok=True)

        self.csearch_index_path = os.path.join(self._tmp_dir, 'csearchindex')
        self.codesearch = CodeSearch(self.android_root, self.csearch_index_path)
        self.codesearch.add_default_filters()

        review_db_path = os.path.join(self.project_dir, ReviewDB.DEFAULT_NAME)
        self.review_db = ReviewDB(review_db_path, self.codesearch)

        pattern_db_path = os.path.join(self.project_dir, PatternDB.DEFAULT_NAME)
        if not os.path.exists(pattern_db_path):
            PatternDB.create_default_database(pattern_db_path)
        self.pattern_db = PatternDB(pattern_db_path)


    def update_csearch_index(self, remove_existing_index):
        """Create or update codesearch index."""

        if os.path.exists(self.csearch_index_path):
            if not remove_existing_index:
                return
            with contextlib.suppress(FileNotFoundError):
                os.remove(self.csearch_index_path)
        os.makedirs(os.path.dirname(self.csearch_index_path), exist_ok=True)
        self.codesearch.build_index()


    def update_review_db(self):
        """Update the entries in the review database."""
        patterns, is_regexs = self.pattern_db.load()
        self.review_db.find(patterns, is_regexs)
