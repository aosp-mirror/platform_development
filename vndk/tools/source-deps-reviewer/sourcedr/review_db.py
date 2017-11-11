#!/usr/bin/env python3

import json
import os
import re


class ReviewDB(object):
    DEFAULT_NAME = 'review_db.json'


    @classmethod
    def get_default_path(cls, project_dir):
        return os.path.join(project_dir, cls.DEFAULT_NAME)


    def __init__(self, path, codesearch):
        self.path = path
        self._cs = codesearch
        try:
            self.data = self._load_data()
        except FileNotFoundError:
            self.data = {}


    # patterns and is_regexs are lists
    def find(self, patterns, is_regexs):
        # they shouldn't be empty
        assert patterns and is_regexs
        processed = b''
        for pattern, is_regex in zip(patterns, is_regexs):
            if not is_regex:
                pattern = re.escape(pattern)
            raw_grep = self._cs.raw_grep(pattern)
            if raw_grep == b'':
                continue
            processed += self._cs.process_grep(raw_grep, pattern, is_regex)
        self.to_json(processed)


    def add_pattern(self, pattern, is_regex):
        if not is_regex:
            pattern = re.escape(pattern)
        raw_grep = self._cs.raw_grep(pattern)
        if raw_grep == b'':
            return
        processed = self._cs.process_grep(raw_grep, pattern, is_regex)
        self.add_to_json(processed)


    def to_json(self, processed):
        data = {}
        patt = re.compile('([^:]+):(\\d+):(.*)$')
        for line in processed.decode('utf-8').split('\n'):
            match = patt.match(line)
            if not match:
                continue
            data[line] = ([], [])

        # if old data exists, perform merge
        if os.path.exists(self.path):
            data.update(self._load_data())

        self._save_data(data)
        self.data = self._load_data()


    def add_to_json(self, processed):
        # Load all matched grep.
        data = self._load_data()
        patt = re.compile('([^:]+):(\\d+):(.*)$')
        for line in processed.decode('utf-8').split('\n'):
            match = patt.match(line)
            if not match:
                continue
            data[line] = ([], [])

        self._save_data(data)
        self.data = self._load_data()


    def add_label(self, label, deps, codes):
        self.data[label] = (deps, codes)
        self._save_data(self.data)


    def _save_data(self, data):
        with open(self.path, 'w') as data_fp:
            json.dump(data, data_fp, sort_keys=True, indent=4)


    def _load_data(self):
        with open(self.path, 'r') as data_fp:
            return json.load(data_fp)
