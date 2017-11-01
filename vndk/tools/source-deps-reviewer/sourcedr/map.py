#!/usr/bin/env python3

"""This command maps source file review results to compiled binaries.
"""

import argparse
import collections
import json
import sys

from sourcedr.data_utils import load_data


def load_build_dep_file(fp):
    graph = json.load(fp)

    # Collect all shared libraries
    shared_libs = set()
    for key, value in graph.items():
        if key.split('.')[-1] == 'so':
            shared_libs.add(key)
        for v in value:
            if v.split('.')[-1] == 'so':
                shared_libs.add(v)

    # Collect transitive closures
    dep = {}
    for s in shared_libs:
        visited = set()
        stack = [s]
        while stack:
            v = stack.pop()
            if v not in visited:
                visited.add(v)
                try:
                    stack.extend(x for x in graph[v]
                                 if x not in visited and not x.endswith('.so')
                                 and not x.endswith('.toc'))
                except KeyError:
                    pass
        visited.remove(s)
        dep[s] = visited

    return dep


def load_build_dep_file_from_path(path):
    with open(path, 'r') as fp:
        return load_build_dep_file(fp)


def load_review_data():
    table = collections.defaultdict(list)
    data = load_data()
    for key, item in data.items():
        table[key.split(':')[0]] += item[0]
    return table


def link_build_dep_and_review_data(dep, table):
    res = collections.defaultdict(list)
    for out, ins in dep.items():
        try:
            res[out] += table[out]
        except KeyError:
            pass

        for in_file in ins:
            try:
                res[out] += table[in_file]
            except KeyError:
                pass
    return res


def main():
    # Parse arguments
    parser = argparse.ArgumentParser()
    parser.add_argument('input', help='Build dependency file')
    parser.add_argument('-o', '--output', required=True)
    args = parser.parse_args()

    # Load build dependency file
    try:
        dep = load_build_dep_file_from_path(args.input)
    except IOError:
        print('error: Failed to open build dependency file:', args.input,
              file=sys.stderr)
        sys.exit(1)

    # Load review data
    table = load_review_data()

    # Link build dependency file and review data
    res = link_build_dep_and_review_data(dep, table)

    # Write the output file
    with open(args.output, 'w') as f:
        json.dump(res, f, sort_keys=True, indent=4)

if __name__ == '__main__':
    main()
