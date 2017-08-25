#!/usr/bin/env python3

from sourcedr.config import *
from sourcedr.data_utils import (
    init_pattern, load_data, save_data, save_pattern,
)
from subprocess import call
import collections
import json
import os
import re
import subprocess

class CodeSearch(object):
    def __init__(self, android_root, csearch_path='./csearchindex'):
        self.android_root = android_root
        android_root = os.path.expanduser(android_root)
        print('building csearchindex ...')
        self.d = dict(os.environ)
        self.d["CSEARCHINDEX"] = os.path.abspath(csearch_path)
        subprocess.call(['cindex', android_root], env=self.d)

    def sanitize_code(self, file_path, skip_literals=True, skip_comments=True):
        with open(file_path, 'rb') as f:
            code = f.read()

        clike = ['.c', '.cpp', '.cc', '.cxx', '.h', '.hpp', '.hxx', '.java']
        assembly = ['.s', '.S']
        python = ['.py']
        mk = ['.mk']

        if any(file_path.endswith(ext) for ext in clike):
            if skip_comments:
                # Remove // comments.
                code = re.sub(b'//[^\\r\\n]*$', b'', code)
                # Remove matched /* */ comments.
                code = re.sub(b'/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/', b'', code)
            if skip_literals:
                # Remove matching quotes.
                code = re.sub(b'"(?:[^"\\\\]|(?:\\\\.))*"', b'', code)
                code = re.sub(b'\'(?:[^\'\\\\]|(?:\\\\.))*\'', b'', code)

        if any(file_path.endswith(ext) for ext in assembly):
            if skip_comments:
                # Remove @ comments
                code = re.sub(b'@[^\\r\\n]*$', b'', code)
                # Remove // comments.
                code = re.sub(b'//[^\\r\\n]*$', b'', code)
                # Remove matched /* */ comments.
                code = re.sub(b'/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/', b'', code)

        if any(file_path.endswith(ext) for ext in python):
            if skip_comments:
                # Remove # comments
                code = re.sub(b'#[^\\r\\n]*$')
            if skip_literals:
                # Remove matching quotes.
                code = re.sub(b'"(?:[^"\\\\]|(?:\\\\.))*"', b'', code)
                code = re.sub(b'\'(?:[^\'\\\\]|(?:\\\\.))*\'', b'', code)

        if any(file_path.endswith(ext) for ext in mk):
            if skip_comments:
                # Remove # comments
                code = re.sub(b'#[^\\r\\n]*$')

        if file_path == 'Android.bp':
            if skip_comments:
                # Remove // comments.
                code = re.sub(b'//[^\\r\\n]*$', b'', code)

        return code

    def process_grep(self, raw_grep, pattern, is_regex, skip_literals,
                     skip_comments):
        patt = re.compile(b'([^:]+):(\\d+):(.*)$')

        kw_patt = pattern.encode('utf-8')
        if not is_regex:
            kw_patt = b'\\b' + re.escape(kw_patt) + b'\\b'
        kw_patt = re.compile(kw_patt)

        suspect = collections.defaultdict(list)

        for line in raw_grep.split(b'\n'):

            match = patt.match(line)
            if not match:
                continue

            file_path = match.group(1)
            line_no = match.group(2)
            code = match.group(3)

            file_name = os.path.basename(file_path)
            file_name_root, file_ext = os.path.splitext(file_name)


            # Check file name.
            if file_ext.lower() in FILE_EXT_BLACK_LIST:
                continue
            if file_name in FILE_NAME_BLACK_LIST:
                continue
            if any(patt in file_path for patt in PATH_PATTERN_BLACK_LIST):
                continue

            # Check matched line (quick filter).
            path = os.path.join(self.android_root, file_path.decode('utf-8'))
            if not kw_patt.search(self.sanitize_code(path)):
                continue

            suspect[file_path].append((file_path, line_no, code))

        suspect = sorted(suspect.items())

        processed = b''
        for file_path, entries in suspect:
            for ent in entries:
                processed += (ent[0] + b':' + ent[1] + b':' + ent[2] + b'\n')

        return processed

    def find(self, pattern, is_regex, skip_literals=True, skip_comments=True):
        if not is_regex:
            pattern = re.escape(pattern)
        try:
            raw_grep = subprocess.check_output(
                ['csearch', '-n', pattern],
                cwd=os.path.expanduser(self.android_root),
                env=self.d)
        except subprocess.CalledProcessError as e:
            if e.output == b'':
                print('nothing found')
        processed = self.process_grep(raw_grep, pattern, is_regex,
                                      skip_comments, skip_comments)
        self.to_json(processed)
        init_pattern(pattern)

    def add_pattern(self, pattern, is_regex,
                    skip_literals=True, skip_comments=True):
        if not is_regex:
            pattern = re.escape(pattern)
        try:
            raw_grep = subprocess.check_output(
                ['csearch', '-n', pattern],
                cwd=os.path.expanduser(self.android_root),
                env=self.d)
        except subprocess.CalledProcessError as e:
            if e.output == b'':
                print('nothing found')
        processed = self.process_grep(raw_grep, pattern, is_regex,
                                      skip_comments, skip_comments)
        self.add_to_json(processed)
        save_pattern(pattern)

    def to_json(self, processed):
        # Load all matched grep.
        data = {}
        patt = re.compile('([^:]+):(\\d+):(.*)$')
        suspect = set()
        for line in processed.decode('utf-8').split('\n'):
            match = patt.match(line)
            if not match:
                continue

            file_path = match.group(1)
            line_no = int(match.group(2))
            data[file_path + ':' + str(line_no)] = ([], [])

        save_data(data)

    def add_to_json(self,processed):
        # Load all matched grep.
        data = load_data()
        patt = re.compile('([^:]+):(\\d+):(.*)$')
        suspect = set()
        for line in processed.decode('utf-8').split('\n'):
            match = patt.match(line)
            if not match:
                continue

            file_path = match.group(1)
            line_no = int(match.group(2))
            data[file_path + ':' + str(line_no)] = ([], [])

        save_data(data)

if __name__ == '__main__':
    # Initialize a codeSearch engine for the directory 'test'
    engine = CodeSearch(android_root='sourcedr/test')
    # This sets up the search engine and save it to database
    engine.find(pattern='dlopen', is_regex=False)
    # This add a new pattern and save it to database
    engine.add_pattern(pattern='cosine', is_regex=False)
