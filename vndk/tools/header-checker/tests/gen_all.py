#!/usr/bin/env python3

import os
import re
import sys

import_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
import_path = os.path.abspath(os.path.join(import_path, 'utils'))
sys.path.insert(1, import_path)

from utils import run_header_abi_dumper
from utils import copy_reference_dump_content
from module import Module

SCRIPT_DIR = os.path.abspath(os.path.dirname(__file__))
INPUT_DIR = os.path.join(SCRIPT_DIR, 'input')
EXPECTED_DIR = os.path.join(SCRIPT_DIR, 'expected')
REFERENCE_DUMP_DIR = os.path.join(SCRIPT_DIR, 'reference_dumps')

DEFAULT_CFLAGS = ['-x', 'c++', '-std=c++11']

FILE_EXTENSIONS = ['h', 'hpp', 'hxx', 'cpp', 'cc', 'c']

def make_and_copy_reference_dumps(module, default_cflags):
    lsdump_content = module.make_lsdump(default_cflags)
    copy_reference_dump_content(module.get_name(), lsdump_content,
                                REFERENCE_DUMP_DIR, '', module.get_arch())

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
            output_content = run_header_abi_dumper(input_path, True,
                                                   DEFAULT_CFLAGS)

            os.makedirs(os.path.dirname(output_path), exist_ok=True)
            with open(output_path, 'w') as f:
                f.write(output_content)
    modules = Module.get_test_modules()
    for module in modules:
        make_and_copy_reference_dumps(module, DEFAULT_CFLAGS)

    return 0

if __name__ == '__main__':
    sys.exit(main())
