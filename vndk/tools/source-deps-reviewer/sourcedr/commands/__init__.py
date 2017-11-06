#!/usr/bin/env python3

"""Parser for command line options."""

import argparse
import sys

from sourcedr.commands import collect, init, scan, review


def main():
    """Register sub-commands, parse command line options, and delegate."""

    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest='subcmd')
    commands = {}

    def _register_subcmd(name, init_argparse):
        commands[name] = init_argparse(subparsers)

    _register_subcmd('init', init.init_argparse)
    _register_subcmd('scan', scan.init_argparse)
    _register_subcmd('review', review.init_argparse)
    _register_subcmd('collect', collect.init_argparse)

    args = parser.parse_args()

    try:
        func = commands[args.subcmd]
    except KeyError:
        parser.print_help()
        sys.exit(1)

    sys.exit(func(args))


if __name__ == '__main__':
    main()
