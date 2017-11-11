#!/usr/bin/env python3

"""`sourcedr review` command."""

import os

from sourcedr.project import Project
from sourcedr.server import create_app


def init_argparse(parsers):
    """Initialize argument parser for `sourcedr init`."""
    parser = parsers.add_parser('review', help='Open web-based review UI')
    parser.add_argument('--project-dir', default='.')
    parser.add_argument('--rebuild-csearch-index', action='store_true',
                        help='Re-build the existing csearch index file')
    return run


def run(args):
    """Main function for `sourcedr init`."""
    project_dir = os.path.expanduser(args.project_dir)

    project = Project(project_dir)
    project.update_csearch_index(args.rebuild_csearch_index)
    project.update_review_db()

    app = create_app(project)
    app.run()
