#!/usr/bin/env python3

import argparse
import itertools
import os
import re

import ninja


def _parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('input_file', help='input ninja file')
    parser.add_argument('--encoding', default='utf-8',
                        help='ninja file encoding')
    parser.add_argument('--ninja-deps', help='.ninja_deps file')

    return parser.parse_args()


def main():
    args = _parse_args()

    system_out_pattern = re.compile('out/target/product/[^/]+/system/')
    vendor_src_pattern = re.compile('(?:vendor)|(?:device)/')

    manifest = ninja.Parser().parse(
        args.input_file, args.encoding, args.ninja_deps)

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
        # Check whether the input files are matched by vendor_src_pattern first.
        gen_paths = []
        paths = itertools.chain(
            build.explicit_ins, build.implicit_ins, build.depfile_implicit_ins)
        for path in paths:
            if vendor_src_pattern.match(path):
                return True
            if path.startswith('out/'):
                gen_paths.append(path)

        # Check whether the input files transitively depend on
        # vendor_src_pattern.
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
            matched = bool(vendor_src_pattern.match(path))
        outs_from_vendor_cache[out_path] = matched
        return matched

    bad_paths = [
        path for path in outs
        if is_from_vendor(path) and system_out_pattern.match(path)]

    bad_paths.sort()

    for path in bad_paths:
        print(path)


if __name__ == '__main__':
    main()
