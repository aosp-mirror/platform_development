#!/usr/bin/env python3

import argparse

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('old', help='Location of compiled stable abi reference.')
    parser.add_argument('old_headers', help='Headers for old.')
    parser.add_argument('new', help='Location of compiled stable abi successor.')
    parser.add_argument('new_headers', help='Headers for new.')

    return parser.parse_args()

def main():
    args = parse_args()
    print(args.old, args.old_headers)
    print(args.new, args.new_headers)

if __name__ == "__main__":
    main()
