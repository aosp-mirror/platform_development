#!/usr/bin/env python3

import os
import re
import sys

from utils import run_header_checker

SCRIPT_DIR = os.path.abspath(os.path.dirname(__file__))
INPUT_DIR = os.path.join(SCRIPT_DIR, 'input')
EXPECTED_DIR = os.path.join(SCRIPT_DIR, 'expected')

DEFAULT_CFLAGS = ['-x', 'c++', '-std=c++11']

FILE_EXTENSIONS = ['h', 'hpp', 'hxx', 'cpp', 'cc', 'c']

def main():
    patt = re.compile(
        '^.*\\.(?:' + \
        '|'.join('(?:' + re.escape(ext) + ')' for ext in FILE_EXTENSIONS) + \
        ')$')

    input_dir_prefix_len = len(INPUT_DIR) + 1
    for base, dirnames, filenames in os.walk(INPUT_DIR):
        for filename in filenames:
            if not patt.match(filename):
                print('ignore:', filename)
                continue

            input_path = os.path.join(base, filename)
            input_rel_path = input_path[input_dir_prefix_len:]
            output_path = os.path.join(EXPECTED_DIR, input_rel_path)

            print('generating', output_path, '...')
            output_content = run_header_checker(input_path, DEFAULT_CFLAGS)

            os.makedirs(os.path.dirname(output_path), exist_ok=True)
            with open(output_path, 'w') as f:
                f.write(output_content)
    return 0

if __name__ == '__main__':
    sys.exit(main())
