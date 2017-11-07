#!/usr/bin/env python3

"""This command maps source file review results to compiled binaries.
"""

import argparse
import collections
import itertools
import json
import os
import sys

from sourcedr import ninja
from sourcedr.review_db import ReviewDB


def load_build_dep_graph(graph):
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


def load_build_dep_ninja(ninja_path, work_dir, ninja_deps=None):
    manifest = ninja.Parser().parse(ninja_path, 'utf-8', ninja_deps)
    graph = collections.defaultdict(set)
    for build in manifest.builds:
        for path in itertools.chain(build.explicit_outs, build.implicit_outs):
            ins = graph[path]
            ins.update(build.explicit_ins)
            ins.update(build.implicit_ins)
            ins.update(build.depfile_implicit_ins)
    return load_build_dep_graph(graph)


def load_build_dep_file(fp):
    return load_build_dep_graph(json.load(fp))


def load_build_dep_file_from_path(path):
    with open(path, 'r') as fp:
        return load_build_dep_file(fp)


def load_review_data(path):
    table = collections.defaultdict(list)
    review_db = ReviewDB(path, None)
    for key, item in review_db.data.items():
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
