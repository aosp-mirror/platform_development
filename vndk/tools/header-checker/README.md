# VNDK Header Abi Dumper

`header-abi-dumper` is a tool to dump the abi of a source file. The abi dumped
 belonging to a source file. The abi dumped, maybe be filtered to include only
 the abi information contained in public headers by specifying -I <path to
 public header dir> with the invocation of header-abi-dumper.

## Usage
 header-abi-dumper -o <dump-file> <source_file> -I <export-include-dir-1> -I
 <export-include-dir-2>.. -- <cflags>
 For options : header-abi-dumper --help

# VNDK Header Abi Linker

`header-abi-linker` is a tool to link abi dumps produced by header-abi-dumper.
 This tool combines all the abi information present in the dump files passed to
 it.

## Usage
 header-abi-linker -o <linked-abi-dump> <abi-dump1> <abi-dump2> <abi-dump3> ...
 -so <path to so file> -v <path to version script>
 For options : header-abi-linker --help

# VNDK Header Abi Diff

`header-abi-diff` is a tool which compares two header abi dumps produced by
 header-abi-dumper. It produces a report outlining all the differences in the
 abi's exposed by the two dumps.

# Return Value
 0: Compatible
 1: Changes to APIs unreferenced by symbols in the .dynsym table
 4: Compatible Extension
 8: Incompatible
 16: Elf Incompatible : some symbols in the .dynsym table, not exported by
                        public headers, were removed.

## Usage
 header-abi-diff -old <old-abi-dump> -new <new-abi-dump> -o <report>
 For options : header-abi-diff --help

# Creating reference abi-dumps
  utils/create_reference_dumps.py may be used to create reference abi dumps.
  For options, refer to utils/create_reference_dumps.py --help.

  As an example, these steps could be followed to create reference abi-dumps :

  $python3 create_reference_dumps.py;

  This creates reference dumps for arm, arm64, x86, x86_64 for all vndk shared
  libraries.

  If one wanted to create references for a specific library, they could do it
  with the following command:

  $python3 create_reference_dumps.py -l libfoo, this will create reference dumps

  for libfoo, assuming libfoo is a vndk / llndk library.

