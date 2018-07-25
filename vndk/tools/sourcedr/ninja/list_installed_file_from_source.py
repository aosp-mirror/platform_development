#!/usr/bin/env python3

import argparse
import itertools
import os
import posixpath
import re

try:
    import cPickle as pickle  # Python 2
except ImportError:
    import pickle  # Python 3

import ninja


def _parse_args():
    parser = argparse.ArgumentParser()

    # Ninja input file options
    parser.add_argument('input_file', help='input ninja file')
    parser.add_argument('--ninja-deps', help='.ninja_deps file')
    parser.add_argument('--cwd', help='working directory for ninja')
    parser.add_argument('--encoding', default='utf-8',
                        help='ninja file encoding')

    # Options
    parser.add_argument(
            '--out-dir', default='out', help='path to output directory')
    parser.add_argument(
            '--installed-filter', default='system',
            help='path filter for installed files (w.r.t. device root)')
    parser.add_argument(
            '--source-filter', default='vendor:device',
            help='path filter for source files (w.r.t. source root)')

    return parser.parse_args()


def main():
    args = _parse_args()

    out_dir = posixpath.normpath(args.out_dir)

    out_pattern = re.compile(re.escape(out_dir) + '/')

    installed_dirs = '|'.join('(?:' + re.escape(posixpath.normpath(path)) + ')'
                              for path in args.installed_filter.split(':'))

    installed_filter = re.compile(
        re.escape(out_dir) + '/target/product/[^/]+/' +
        '(?:' + installed_dirs + ')')

    source_filter = re.compile(
        '|'.join('(?:' + re.escape(posixpath.normpath(path)) + ')'
                 for path in args.source_filter.split(':')))

    manifest = ninja.load_manifest_from_args(args)

    # Build lookup map
    outs = {}
    for build in manifest.builds:
        for path in build.explicit_outs:
            outs[path] = build
        for path in build.implicit_outs:
            outs[path] = build

    # Compute transitive input files
    outs_from_vendor_cache = {}

    def _are_inputs_from_vendor(build):
        # Check whether the input files are matched by source_filter first.
        gen_paths = []
        paths = itertools.chain(
            build.explicit_ins, build.implicit_ins, build.depfile_implicit_ins)
        for path in paths:
            if source_filter.match(path):
                return True
            if out_pattern.match(path):
                gen_paths.append(path)

        # Check whether the input files transitively depend on source_filter.
        for path in gen_paths:
            if is_from_vendor(path):
                return True

        return False

    def is_from_vendor(out_path):
        matched = outs_from_vendor_cache.get(out_path, None)
        if matched is not None:
            return matched

        build = outs.get(out_path)
        if build:
            matched = _are_inputs_from_vendor(build)
        else:
            matched = bool(source_filter.match(path))
        outs_from_vendor_cache[out_path] = matched
        return matched

    matched_paths = [
        path for path in outs
        if installed_filter.match(path) and is_from_vendor(path)]

    matched_paths.sort()

    for path in matched_paths:
        print(path)


if __name__ == '__main__':
    main()
