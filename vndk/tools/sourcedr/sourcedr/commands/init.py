#!/usr/bin/env python3

"""`sourcedr init` command."""

import os
import sys

from sourcedr.project import Project


def _is_dir_empty(path):
    """Determine whether the given path is an empty directory."""
    return len(os.listdir(path)) == 0


def init_argparse(parsers):
    """Initialize argument parser for `sourcedr init`."""
    parser = parsers.add_parser('init', help='Start a new review project')
    parser.add_argument('--project-dir', default='.')
    parser.add_argument('--android-root', required=True,
                        help='Android source tree root directory')
    return run


def run(args):
    """Main function for `sourcedr init`."""

    if args.project_dir == '.' and not _is_dir_empty(args.project_dir):
        print('error: Current working directory is not an empty directory.',
              file=sys.stderr)

    project_dir = os.path.expanduser(args.project_dir)
    source_dir = os.path.expanduser(args.android_root)

    Project.get_or_create_project_dir(project_dir, source_dir)
