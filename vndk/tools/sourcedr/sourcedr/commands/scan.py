#!/usr/bin/env python3

"""`sourcedr scan` command."""


def init_argparse(parsers):
    """Initialize argument parser for `sourcedr scan`."""
    parsers.add_parser('scan', help='Scan all pattern occurrences')
    return run


def run(_):
    """Main function for `sourcedr scan`."""
    print('error: Need human review.  Run: `sourcedr review`')
