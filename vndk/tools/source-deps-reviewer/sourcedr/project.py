#!/usr/bin/env python3

"""SourceDR project configurations and databases.

`Project` class holds configuration files, review databases, pattern databases,
and `codesearch` index files.
"""

import contextlib
import os

from sourcedr.codesearch import CodeSearch
from sourcedr.data_utils import (init_pattern, load_pattern, patterns_exist)
from sourcedr.review_db import ReviewDB


class Project(object):
    """SourceDR project configuration files and databases.
    """

    def __init__(self, android_root, project_dir):
        self.android_root = os.path.abspath(android_root)
        self.project_dir = os.path.abspath(project_dir)
        self._tmp_dir = os.path.join(self.project_dir, 'tmp')

        self.csearch_index_path = os.path.join(self._tmp_dir, 'csearchindex')
        self.codesearch = CodeSearch(self.android_root, self.csearch_index_path)
        self.codesearch.add_default_filters()

        self.review_db = ReviewDB(self.codesearch)


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

        # TODO: Remove patterns_exist() and load_pattern() after refactoring
        # pattern database code.
        if not patterns_exist():
            init_pattern('\\bdlopen\\b', is_regex=True)
        patterns, is_regexs = load_pattern()

        self.review_db.find(patterns, is_regexs)
