#!/usr/bin/env python3

import argparse
import os
import unittest

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--verbose', '-v', action='store_true')
    args = parser.parse_args()

    verbosity = 2 if args.verbose else 1

    loader = unittest.TestLoader()
    tests = loader.discover(os.path.dirname(__file__), 'test_*.py')
    runner = unittest.runner.TextTestRunner(verbosity=verbosity)
    runner.run(tests)

if __name__ == '__main__':
    main()
