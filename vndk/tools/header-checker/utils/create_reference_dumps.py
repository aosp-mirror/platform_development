#!/usr/bin/env python3

import os
import re
import sys
import argparse

from utils import make_library
from utils import find_lib_lsdump
from utils import get_build_var
from utils import AOSP_DIR
from utils import read_output_content
from utils import copy_reference_dump

class Target(object):
    def __init__(self, has_2nd):
        extra = '_2ND' if has_2nd else ''
        self.arch = get_build_var('TARGET{}_ARCH'.format(extra))
        self.arch_variant = get_build_var('TARGET{}_ARCH_VARIANT'.format(extra))
        self.cpu_variant = \
            get_build_var('TARGET{}_CPU_VARIANT'.format(extra))

def get_vndk_libs(vndk_list_path):
    with open(vndk_list_path, 'r') as f:
        return f.read().splitlines()

def create_source_abi_reference_dumps(soong_dir, vndk_libs, args):
    ref_dump_dir_stem = os.path.join(args.ref_dump_dir, args.version)
    ref_dump_dir_insertion = 'source-based'
    num_libs_copied = 0
    for vndk_lib in vndk_libs:
        if args.make_libs:
            make_library(vndk_lib)
        for target in [Target(True), Target(False)]:
            arch_lsdump_path = find_lib_lsdump(vndk_lib, target.arch,
                                               target.arch_variant,
                                               target.cpu_variant)
            # Copy the contents of the lsdump into it's corresponding
            # reference  directory.
            num_libs_copied += copy_reference_dump(arch_lsdump_path,
                                                   ref_dump_dir_stem,
                                                   ref_dump_dir_insertion,
                                                   target.arch)
    return num_libs_copied


def main():
    # Parse command line options.
    parser = argparse.ArgumentParser()
    parser.add_argument('--version', help='VNDK version')
    parser.add_argument('--vndk-list', help='file containing list of vndk \
                        libraries')
    parser.add_argument('-ref-dump-dir', help='directory to copy reference abi \
                        dumps into')
    parser.add_argument('-make-libs', action ="store_true", default = False,
                        help='make libraries before copying dumps')
    args = parser.parse_args()
    num_processed = 0
    soong_dir = os.path.join(AOSP_DIR, 'out', 'soong', '.intermediates')
    num_processed += create_source_abi_reference_dumps(soong_dir,\
          get_vndk_libs(args.vndk_list), args)
    print()
    print('msg: Processed', num_processed, 'libraries')
if __name__ == '__main__':
    main()
