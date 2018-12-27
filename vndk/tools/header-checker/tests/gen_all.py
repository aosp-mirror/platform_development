#!/usr/bin/env python3

import os
import re
import sys

import_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
import_path = os.path.abspath(os.path.join(import_path, 'utils'))
sys.path.insert(1, import_path)

from utils import run_header_abi_dumper
from module import Module
from test import INPUT_DIR
from test import EXPECTED_DIR
from test import make_and_copy_reference_dumps

FILE_EXTENSIONS = ['h', 'hpp', 'hxx', 'cpp', 'cc', 'c']


def main():
    patt = re.compile(
        '^.*\\.(?:' +
        '|'.join('(?:' + re.escape(ext) + ')' for ext in FILE_EXTENSIONS) +
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
            output_content = run_header_abi_dumper(input_path)

            os.makedirs(os.path.dirname(output_path), exist_ok=True)
            with open(output_path, 'w') as f:
                f.write(output_content)
    modules = Module.get_test_modules()
    for module in modules:
        print('Created abi dump at', make_and_copy_reference_dumps(module))

    return 0


if __name__ == '__main__':
    sys.exit(main())
