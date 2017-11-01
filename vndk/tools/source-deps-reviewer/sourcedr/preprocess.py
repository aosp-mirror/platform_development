#!/usr/bin/env python3

from sourcedr.data_utils import (data_exist, load_data, merge, save_data)
import sourcedr.codesearch

import re


class CodeSearch(sourcedr.codesearch.CodeSearch):
    @staticmethod
    def create_default(android_root, index_path='csearchindex'):
        cs = CodeSearch(android_root, index_path)
        cs.add_default_filters()
        return cs

    # patterns and is_regexs are lists
    def find(self, patterns, is_regexs):
        # they shouldn't be empty
        assert patterns and is_regexs
        processed = b''
        for pattern, is_regex in zip(patterns, is_regexs):
            if not is_regex:
                pattern = re.escape(pattern)
            raw_grep = self.raw_grep(pattern)
            if raw_grep == b'':
                continue
            processed += self.process_grep(raw_grep, pattern, is_regex)
        self.to_json(processed)

    def add_pattern(self, pattern, is_regex):
        if not is_regex:
            pattern = re.escape(pattern)
        raw_grep = self.raw_grep(pattern)
        if raw_grep == b'':
            return
        processed = self.process_grep(raw_grep, pattern, is_regex)
        self.add_to_json(processed)

    def to_json(self, processed):
        data = {}
        suspect = set()
        patt = re.compile('([^:]+):(\\d+):(.*)$')
        for line in processed.decode('utf-8').split('\n'):
            match = patt.match(line)
            if not match:
                continue
            data[line] = ([], [])

        # if old data exists, perform merge
        if data_exist():
            old_data = load_data()
            data = merge(old_data, data)

        save_data(data)

    def add_to_json(self,processed):
        # Load all matched grep.
        data = load_data()
        patt = re.compile('([^:]+):(\\d+):(.*)$')
        for line in processed.decode('utf-8').split('\n'):
            match = patt.match(line)
            if not match:
                continue
            data[line] = ([], [])

        save_data(data)

if __name__ == '__main__':
    # Initialize a codeSearch engine for the directory 'test'
    engine = CodeSearch.create_default('sourcedr/test', 'csearchindex')
    # Build the index file for the directory
    engine.build_index()
    # This sets up the search engine and save it to database
    engine.find(patterns=['dlopen'], is_regexs=[False])
