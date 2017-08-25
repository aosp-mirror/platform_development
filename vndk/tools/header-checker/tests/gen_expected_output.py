#!/usr/bin/env python3

import sys
from utils import run_header_abi_dumper

def main():
    sys.stdout.write(run_header_abi_dumper(sys.argv[1], True, sys.argv[2:]))
    return 0

if __name__ == '__main__':
    sys.exit(main())
