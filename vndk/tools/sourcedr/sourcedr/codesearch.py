#!/usr/bin/env python3

"""Code indexing and searching utilities.

This module will build n-gram file index with codesearch_ and use the index as
a bloom filter to find the regular expression pattern.

In addition, language-specific filters are provided to ignore matchings in
string literals or comments in the source code.

.. _codesearch: https://github.com/google/codesearch
"""

import collections
import os
import re
import subprocess


class ClikeFilter(object):
    def __init__(self, skip_literals=True, skip_comments=True):
        self.file_exts = (b'.c', b'.cpp', b'.cc', b'.cxx', b'.h', b'.hpp',
                          b'.hxx', b'.java')
        self.skip_literals = skip_literals
        self.skip_comments = skip_comments

    def process(self, code):
        if self.skip_comments:
            # Remove // comments.
            code = re.sub(b'//[^\\r\\n]*[\\r\\n]', b'', code)
            # Remove matched /* */ comments.
            code = re.sub(b'/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/', b'', code)
        if self.skip_literals:
            # Remove matching quotes.
            code = re.sub(b'"(?:\\\\?.)*?"', b'', code)
            code = re.sub(b'\'(?:\\\\?.)*?\'', b'', code)
        return code

    def get_span(self, code):
        span = []
        if self.skip_comments:
            # Remove // comments.
            p = re.compile(b'//[^\\r\\n]*[\\r\\n]')
            for m in p.finditer(code):
                span.append(m.span())
            # Remove matched /* */ comments.
            p = re.compile(b'/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/')
            for m in p.finditer(code):
                span.append(m.span())
        if self.skip_literals:
            # Remove matching quotes.
            p = re.compile(b'"(?:\\\\?.)*?"')
            for m in p.finditer(code):
                span.append(m.span())
            p = re.compile(b'\'(?:\\\\?.)*?\'')
            for m in p.finditer(code):
                span.append(m.span())
        return span

class PyFilter(object):
    def __init__(self, skip_literals=True, skip_comments=True):
        self.file_exts = (b'.py',)
        self.skip_literals = skip_literals
        self.skip_comments = skip_comments

    def process(self, code):
        if self.skip_comments:
            # Remove # comments
            code = re.sub(b'#[^\\r\\n]*[\\r\\n]', b'', code)
        if self.skip_literals:
            # Remove matching quotes.
            code = re.sub(b'"(?:\\\\?.)*?"', b'', code)
            code = re.sub(b'\'(?:\\\\?.)*?\'', b'', code)
        return code

    def get_span(self, code):
        span = []
        if self.skip_comments:
            # Remove # comments.
            p = re.compile(b'#[^\\r\\n]*[\\r\\n]')
            for m in p.finditer(code):
                span.append(m.span())
        if self.skip_literals:
            # Remove matching quotes.
            p = re.compile(b'"(?:\\\\?.)*?"')
            for m in p.finditer(code):
                span.append(m.span())
            p = re.compile(b'\'(?:\\\\?.)*?\'')
            for m in p.finditer(code):
                span.append(m.span())
        return span

class AssemblyFilter(object):
    def __init__(self, skip_literals=True, skip_comments=True):
        self.file_exts = (b'.s', b'.S')
        self.skip_literals = skip_literals
        self.skip_comments = skip_comments

    def process(self, code):
        if self.skip_comments:
            # Remove @ comments
            code = re.sub(b'@[^\\r\\n]*[\\r\\n]', b'', code)
            # Remove // comments.
            code = re.sub(b'//[^\\r\\n]*[\\r\\n]', b'', code)
            # Remove matched /* */ comments.
            code = re.sub(b'/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/', b'', code)
        return code

    def get_span(self, code):
        span = []
        if self.skip_comments:
            # Remove # comments.
            p = re.compile(b'@[^\\r\\n]*[\\r\\n]')
            for m in p.finditer(code):
                span.append(m.span())
            # Remove // comments
            p = re.compile(b'//[^\\r\\n]*[\\r\\n]')
            for m in p.finditer(code):
                span.append(m.span())
            # Remove matched /* */ comments
            p = re.compile(b'/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/')
            for m in p.finditer(code):
                span.append(m.span())
        return span

class MkFilter(object):
    def __init__(self, skip_literals=True, skip_comments=True):
        self.file_exts = (b'.mk',)
        self.skip_literals = skip_literals
        self.skip_comments = skip_comments

    def process(self, code):
        if self.skip_comments:
            # Remove # comments
            code = re.sub(b'#[^\\r\\n]*[\\r\\n]', b'', code)
        return code

    def get_span(self, code):
        span = []
        if self.skip_comments:
            # Remove # comments.
            p = re.compile(b'#[^\\r\\n]*[\\r\\n]')
            for m in p.finditer(code):
                span.append(m.span())
        return span

class BpFilter(object):
    def __init__(self, skip_literals=True, skip_comments=True):
        self.file_exts = (b'.bp',)
        self.skip_literals = skip_literals
        self.skip_comments = skip_comments

    def process(self, code):
        if self.skip_comments:
            # Remove // comments
            code = re.sub(b'//[^\\r\\n]*[\\r\\n]', b'', code)
        return code

    def get_span(self, code):
        span = []
        if self.skip_comments:
            # Remove // comments.
            p = re.compile(b'//[^\\r\\n]*[\\r\\n]')
            for m in p.finditer(code):
                span.append(m.span())
        return span

class PathFilter(object):
    def __init__(self, file_ext_black_list=tuple(),
                 file_name_black_list=tuple(),
                 path_component_black_list=tuple()):
        self.file_ext_black_list = set(
                x.encode('utf-8') for x in file_ext_black_list)
        self.file_name_black_list = set(
                x.encode('utf-8') for x in file_name_black_list)
        self.path_component_black_list = set(
                x.encode('utf-8') for x in path_component_black_list)

    def should_skip(self, path):
        file_name = os.path.basename(path)
        file_ext = os.path.splitext(file_name)[1]

        if file_ext.lower() in self.file_ext_black_list:
            return True
        if file_name in self.file_name_black_list:
            return True
        return any(patt in path for patt in self.path_component_black_list)

class CodeSearch(object):
    DEFAULT_NAME = 'csearchindex'

    @classmethod
    def get_default_path(cls, project_dir):
        return os.path.join(project_dir, 'tmp', cls.DEFAULT_NAME)

    def __init__(self, root_dir, index_file_path, path_filter=None):
        self.path = os.path.abspath(index_file_path)
        self._root_dir = os.path.abspath(root_dir)
        self._env = dict(os.environ)
        self._env['CSEARCHINDEX'] = self.path
        self._filters = {}
        self._path_filter = PathFilter() if path_filter is None else path_filter

    def _run_cindex(self, options):
        subprocess.check_call(['cindex'] + options, env=self._env,
                              cwd=self._root_dir, stdout=subprocess.DEVNULL,
                              stderr=subprocess.DEVNULL)

    def _run_csearch(self, options):
        if not os.path.exists(self.path):
            raise ValueError('Failed to find ' + self.path)
        return subprocess.check_output(['csearch'] + options, env=self._env,
                                       cwd=self._root_dir,
                                       stderr=subprocess.DEVNULL)

    def add_filter(self, lang_filter):
        for ext in lang_filter.file_exts:
            self._filters[ext] = lang_filter

    def add_default_filters(self, skip_literals=True, skip_comments=True):
        self.add_filter(ClikeFilter(skip_literals, skip_comments))
        self.add_filter(AssemblyFilter(skip_literals, skip_comments))
        self.add_filter(PyFilter(skip_literals, skip_comments))
        self.add_filter(MkFilter(skip_literals, skip_comments))
        self.add_filter(BpFilter(skip_literals, skip_comments))

    def build_index(self, remove_existing_index=True):
        if remove_existing_index and os.path.exists(self.path):
            with contextlib.suppress(FileNotFoundError):
                os.remove(self.path)
        os.makedirs(os.path.dirname(self.path), exist_ok=True)
        self._run_cindex([self._root_dir])

    def _sanitize_code(self, file_path):
        with open(file_path, 'rb') as f:
            code = f.read()
        file_name = os.path.basename(file_path)
        f, ext = os.path.splitext(file_name)
        try:
            code = self._filters[ext].process(code)
        except KeyError:
            pass
        return code

    def _remove_prefix(self, raw_grep):
        ret = b''
        patt = re.compile(b'([^:]+):(\\d+):(.*)$')
        for line in raw_grep.split(b'\n'):
            match = patt.match(line)
            if not match:
                continue
            file_path = os.path.relpath(match.group(1),
                                        self._root_dir.encode('utf-8'))
            line_no = match.group(2)
            code = match.group(3)
            ret += file_path + b':' + line_no + b':' + code + b'\n'
        return ret

    def process_grep(self, raw_grep, pattern, is_regex):
        pattern = pattern.encode('utf-8')
        if not is_regex:
            pattern = re.escape(pattern)
        # Limit pattern not to match exceed a line
        # Since grep may get multiple patterns in a single entry
        pattern = re.compile(pattern + b'[^\\n\\r]*(?:\\n|\\r|$)')

        patt = re.compile(b'([^:]+):(\\d+):(.*)$')
        suspect = collections.defaultdict(list)
        for line in raw_grep.split(b'\n'):
            match = patt.match(line)
            if not match:
                continue

            file_path = match.group(1)
            line_no = match.group(2)
            code = match.group(3)

            if self._path_filter.should_skip(file_path):
                continue

            abs_file_path = os.path.join(self._root_dir.encode('utf-8'),
                                         file_path)
            # Check if any pattern can be found after sanitize_code
            if not pattern.search(self._sanitize_code(abs_file_path)):
                continue
            suspect[abs_file_path].append((file_path, line_no, code))

        suspect = sorted(suspect.items())

        processed = b''
        for file_path, entries in suspect:
            with open(file_path, 'rb') as f:
                code = f.read()
            # deep filter
            file_name = os.path.basename(file_path)
            f, ext = os.path.splitext(file_name)
            try:
                span = self._filters[ext].get_span(code)
            except KeyError:
                span = []

            matchers = [m for m in pattern.finditer(code)]
            for i, matcher in enumerate(matchers):
                if not span or all(span_ent[0] > matcher.start() or
                                   span_ent[1] <= matcher.start()
                                   for span_ent in span):
                    processed += (entries[i][0] + b':' +
                                  entries[i][1] + b':' +
                                  entries[i][2] + b'\n')

        return processed

    def raw_grep(self, pattern):
        try:
            return self._remove_prefix(self._run_csearch(['-n', pattern]))
        except subprocess.CalledProcessError as e:
            if e.output == b'':
                return b''
            raise

    def raw_search(self, pattern, is_regex):
        if not is_regex:
            pattern = re.escape(pattern)
        return self.raw_grep(pattern)
