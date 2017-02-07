#!/usr/bin/env python3

import sys
from utils import run_header_checker

def main():
    sys.stdout.write(run_header_checker(sys.argv[1], sys.argv[2:]))
    return 0

if __name__ == '__main__':
    sys.exit(main())
