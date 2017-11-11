#!/usr/bin/env python3

"""Unit tests and functional tests runner."""

import argparse
import os
import unittest


TESTS_DIR = os.path.join(os.path.dirname(__file__), 'sourcedr', 'tests')


def main():
    """ Find and run unit tests and functional tests."""

    parser = argparse.ArgumentParser()
    parser.add_argument('--verbose', '-v', action='store_true')
    args = parser.parse_args()

    verbosity = 2 if args.verbose else 1

    loader = unittest.TestLoader()
    tests = loader.discover(TESTS_DIR, 'test_*.py')
    runner = unittest.runner.TextTestRunner(verbosity=verbosity)
    runner.run(tests)

if __name__ == '__main__':
    main()
