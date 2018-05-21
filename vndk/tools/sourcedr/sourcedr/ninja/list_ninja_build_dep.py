#!/usr/bin/env python3

"""List all transitive build rules of a target."""

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
    parser.add_argument('target', help='build target')

    return parser.parse_args()


def collect_build_targets(graph, target):
    """Collect the transitive build targets."""

    # Search for the first target
    build = graph[target]

    # Collect all source files
    dfs = [build]

    visited = {build}
    stack = [build]
    while stack:
        build = stack.pop()
        for dep in itertools.chain(build.explicit_ins, build.implicit_ins,
                                   build.depfile_implicit_ins):
            dep = graph.get(dep)
            if not dep:
                continue
            if dep not in visited:
                visited.add(dep)
                dfs.append(dep)
                stack.append(dep)

    return dfs


def main():
    args = _parse_args()

    # Build lookup map
    manifest = ninja.load_manifest_from_args(args)
    graph = {}
    for build in manifest.builds:
        for path in build.explicit_outs:
            graph[path] = build
        for path in build.implicit_outs:
            graph[path] = build

    # List all transitive targets
    try:
        builds = collect_build_targets(graph, args.target)
    except KeyError:
        print('error: Failed to find the target {}'.format(arg.target),
              file=sys.stderr)
        sys.exit(1)

    # Print all targets
    for build in builds:
        print('build')
        for path in build.explicit_outs:
            print('  explicit_out:', path)
        for path in build.implicit_outs:
            print('  implicit_out:', path)
        for path in build.explicit_ins:
            print('  explicit_in:', path)
        for path in build.implicit_ins:
            print('  implicit_in:', path)
        for path in build.prerequisites:
            print('  prerequisites:', path)
        for path in build.depfile_implicit_ins:
            print('  depfile_implicit_in:', path)


if __name__ == '__main__':
    main()
