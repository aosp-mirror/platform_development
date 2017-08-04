#!/usr/bin/env python3

from sourcedr.config import *
from sourcedr.data_utils import *

from subprocess import call
import collections
import json
import os
import re
import subprocess

def sanitize_code_unmatched(code):
    # Remove unmatched quotes until EOL.
    code = re.sub(b'"[^"]*$', b'', code)
    # Remove unmatched C comments.
    code = re.sub(b'/\\*.*$', b'', code)
    return code

def sanitize_code_matched(code):
    # Remove matching quotes.
    code = re.sub(b'"(?:[^"\\\\]|(?:\\\\.))*"', b'', code)
    # Remove C++ comments.
    code = re.sub(b'//[^\\r\\n]*$', b'', code)
    # Remove matched C comments.
    code = re.sub(b'/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/', b'', code)
    return code

def sanitize_code(code):
    return sanitize_code_unmatched(sanitize_code_matched(code))

def to_json(processed):
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

def add_to_json(processed):
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

def process_grep(raw_grep, android_root, pattern, is_regex):
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
        if not kw_patt.search(sanitize_code(code)):
            continue

        suspect[file_path].append((file_path, line_no, code))

    suspect = sorted(suspect.items())

    processed = b''
    for file_path, entries in suspect:
        path = os.path.join(android_root, file_path.decode('utf-8'))
        with open(path, 'rb') as f:
            code = sanitize_code_matched(f.read())
            if not kw_patt.search(sanitize_code(code)):
                print('deep-filter:', file_path.decode('utf-8'))
                continue

        for ent in entries:
            processed += (ent[0] + b':' + ent[1] + b':' + ent[2] + b'\n')

    return processed

def prepare(android_root, pattern, is_regex):
    android_root = os.path.expanduser(android_root)
    print('building csearchindex ...')
    subprocess.call(['cindex', android_root])
    if not is_regex:
        pattern = re.escape(pattern)
    try:
        raw_grep = subprocess.check_output(['csearch', '-n', pattern],
                                           cwd=os.path.expanduser(android_root))
    except subprocess.CalledProcessError as e:
        if e.output == b'':
            print('nothing found')

    to_json(process_grep(raw_grep, android_root, pattern, is_regex))
    init_pattern(pattern)

def add_pattern(android_root, pattern, is_regex):
    if not is_regex:
        pattern = re.escape(pattern)
    try:
        raw_grep = subprocess.check_output(['csearch', '-n', pattern],
                                           cwd=os.path.expanduser(android_root))
    except subprocess.CalledProcessError as e:
        if e.output == b'':
            print('nothing found')
        return

    add_to_json(process_grep(raw_grep, android_root, pattern, is_regex))

if __name__ == '__main__':
    prepare(android_root='test', pattern='dlopen', is_regex=False)
    add_pattern(android_root='test', pattern='dlopen', is_regex=False)
