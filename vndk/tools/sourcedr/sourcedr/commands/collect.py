#!/usr/bin/env python3

"""`sourcedr collect` command."""

import json
import os

from sourcedr.project import Project
from sourcedr.map import (
    link_build_dep_and_review_data, load_build_dep_ninja, load_review_data)


def init_argparse(parsers):
    """Initialize argument parser for `sourcedr collect`."""
    parser = parsers.add_parser('collect', help='Open web-based review UI')
    parser.add_argument('input', help='Ninja file')
    parser.add_argument('--ninja-deps')
    parser.add_argument('--project-dir', default='.')
    parser.add_argument('-o', '--output', required=True)
    return run


def run(args):
    project_dir = os.path.expanduser(args.project_dir)
    project = Project(project_dir)

    # Load build dependency file
    try:
        dep = load_build_dep_ninja(args.input, project.source_dir,
                                   args.ninja_deps)
    except IOError:
        print('error: Failed to open build dependency file:', args.input,
              file=sys.stderr)
        sys.exit(1)

    # Load review data
    table = load_review_data(project.review_db.path)

    # Link build dependency file and review data
    res = link_build_dep_and_review_data(dep, table)

    # Write the output file
    with open(args.output, 'w') as f:
        json.dump(res, f, sort_keys=True, indent=4)

if __name__ == '__main__':
    main()
