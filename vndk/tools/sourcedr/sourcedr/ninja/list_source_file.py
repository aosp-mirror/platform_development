#!/usr/bin/env python3

"""List all source file of a installed module."""

import argparse
import itertools
import posixpath
import re

try:
    import cPickle as pickle  # Python 2
except ImportError:
    import pickle  # Python 3

import ninja


def _parse_args():
    """Parse the command line arguments."""

    parser = argparse.ArgumentParser()

    # Ninja input file options
    parser.add_argument('input_file', help='input ninja file')
    parser.add_argument('--ninja-deps', help='.ninja_deps file')
    parser.add_argument('--cwd', help='working directory for ninja')
    parser.add_argument('--encoding', default='utf-8',
                        help='ninja file encoding')

    # Options
    parser.add_argument(
            'installed_filter', nargs='+',
            help='path filter for installed files (w.r.t. device root)')
    parser.add_argument(
            '--out-dir', default='out', help='path to output directory')

    return parser.parse_args()


def collect_source_files(graph, start, out_dir_pattern, out_host_dir_pattern):
    """Collect the transitive dependencies of a target."""

    source_files = []

    # Extract the file name of the target file.  We need this file name to
    # allow the strip/copy build rules while leaving other shared libraries
    # alone.
    start_basename = posixpath.basename(start)

    # Collect all source files
    visited = {start}
    stack = [start]
    while stack:
        cur = stack.pop()

        if not out_dir_pattern.match(cur):
            source_files.append(cur)

        build = graph.get(cur)
        if build:
            for dep in itertools.chain(build.explicit_ins, build.implicit_ins,
                                       build.depfile_implicit_ins):
                # Skip the binaries for build process
                if dep.startswith('prebuilts/'):
                    continue
                if out_host_dir_pattern.match(dep):
                    continue

                # Skip the shared libraries
                if dep.endswith('.toc'):
                    continue
                if dep.endswith('.so'):
                    if posixpath.basename(dep) != start_basename:
                        continue

                if dep not in visited:
                    visited.add(dep)
                    stack.append(dep)

    return sorted(source_files)


def main():
    args = _parse_args()

    out_dir = posixpath.normpath(args.out_dir)
    out_dir_pattern = re.compile(re.escape(out_dir) + '/')
    out_host_dir_pattern = re.compile(re.escape(out_dir) + '/host/')
    out_product_dir = out_dir + '/target/product/[^/]+'

    def _normalize_path(path):
        if path.startswith(out_dir + '/target'):
            return path
        return posixpath.join(out_product_dir, path)

    installed_filter = [_normalize_path(path) for path in args.installed_filter]
    installed_filter = re.compile(
        '|'.join('(?:' + p + ')' for p in installed_filter))

    manifest = ninja.load_manifest_from_args(args)

    # Build lookup map
    graph = {}
    for build in manifest.builds:
        for path in build.explicit_outs:
            graph[path] = build
        for path in build.implicit_outs:
            graph[path] = build

    # Collect all matching outputs
    matched_files = [path for path in graph if installed_filter.match(path)]
    matched_files.sort()

    for path in matched_files:
        source_files = collect_source_files(
            graph, path, out_dir_pattern, out_host_dir_pattern)
        print(path)
        for dep in source_files:
            print('\t' + dep)
        print()


if __name__ == '__main__':
    main()
