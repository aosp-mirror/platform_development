#!/usr/bin/env python3

from sourcedr.data_utils import (data_exist, load_data, merge, save_data)
import sourcedr.codesearch

import re


class ReviewDB(object):
    def __init__(self, codesearch):
        self._cs = codesearch

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
