# VNDK Header Abi Dumper

`header-abi-dumper` is a tool to dump the abi of a source. The Abi dumped
 belonging to a source file is filtered by dumping only the Abi contained in a
 set of header files exposed through the "export_include_dirs" directory(ies).

## Usage
 header-abi-dumper -o <dump-file> <source_file> -I <export-include-dir-1> -I
 <export-include-dir-2>.. -- <cflags>

# VNDK Header Abi Linker

`header-abi-linker` is a tool to link abi dumps produced by header-abi-dumper.
 This tool combines all the abi information present in the dump files passed to
 it.

## Usage
 header-abi-linker -o <linked-abi-dump> <abi-dump1> <abi-dump2> <abi-dump3> ...

# VNDK Header Abi Diff

`header-abi-diff` is a tool which compares two header abi dumps produced by
 header-abi-dumper. It produces a report outlining all the differences in the
 abi's exposed by the two dumps.

# Return Value
 1: InCompatible
 0: Compatible or Compatible Extension.


## Usage
 header-abi-diff -old <old-abi-dump> -new <new-abi-dump> -o <report>

