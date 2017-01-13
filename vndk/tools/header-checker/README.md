# VNDK Header Checker

`header-checker` is a tool to check for ABI compliance.  First, we can create a
reference dump for each header file when we are preparing a formal release.
After the release, we can check the ABI compliance by comparing the information
in the reference dump and the latest header.

## Usage

Example 1:

    $ header-checker -g -r example1.ast tests/example1.h -- clang -x c++
