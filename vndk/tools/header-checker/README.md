VNDK Header Checker
===================

The VNDK header checker consists of 3 tools:
[header-abi-dumper](#Header-ABI-Dumper),
[header-abi-linker](#Header-ABI-Linker), and
[header-abi-diff](#Header-ABI-Diff).  The first two commands generate ABI dumps
for shared libraries.  The third command compares the ABI dumps with the
reference ABI dumps in [prebuilts/abi-dumps].  If there are no ABI dumps under
[prebuilts/abi-dumps], follow the instructions in
[Create Reference ABI Dumps](#Create-Reference-ABI-Dumps) to create one.

[prebuilts/abi-dumps]: https://android.googlesource.com/platform/prebuilts/abi-dumps


## Header ABI Dumper

`header-abi-dumper` dumps the ABIs (including classes, functions, variables,
etc) defined in a C/C++ source file.

The `-I` command line option controls the scope of ABIs that must be dumped.
If `-I <path-to-export-include-dir>` is specified, the generated ABI dump will
only include the classes, the functions, and the variables that are defined in
the header files under the exported include directories.

### Usage

```
header-abi-dumper -o <dump-file> <source_file> \
    -I <export-include-dir-1> \
    -I <export-include-dir-2> \
    ... \
    -- \
    <cflags>
```

For more command line options, run `header-abi-dumper --help`.


## Header ABI Linker

`header-abi-linker` links several ABI dumps produced by `header-abi-dumper`.
This tool combines all the ABI information present in the input ABI dump files
and prunes the irrelevant ABI dumps.

### Usage

```
header-abi-linker -o <linked-abi-dump> \
    <abi-dump1> <abi-dump2> <abi-dump3> ... \
    -so <path to so file> \
    -v <path to version script>
```

For more command line options, run `header-abi-linker --help`.


## Header ABI Diff

`header-abi-diff` compares two header ABI dumps produced by
`header-abi-dumper`.  It produces a report outlining all the differences
between the ABIs exposed by the two dumps.

### Usage

```
header-abi-diff -old <old-abi-dump> -new <new-abi-dump> -o <report>
```

For more command line options, run `header-abi-diff --help`.

### Return Value

* `0`: Compatible
* `1`: Changes to APIs unreferenced by symbols in the `.dynsym` table
* `4`: Compatible extension
* `8`: Incompatible
* `16`: ELF incompatible (Some symbols in the `.dynsym` table, not exported by
  public headers, were removed.)


## Create Reference ABI Dumps

`utils/create_reference_dumps.py` may be used to create reference ABI dumps.

#For VNDK libraries

For example, the command below creates reference ABI dumps for all VNDK shared
libraries on arm, arm64, x86, and x86_64 architectures:

```
$ python3 create_reference_dumps.py
```

To create reference ABI dumps for a specific library, run the command below:

```
$ python3 create_reference_dumps.py -l libfoo
```

This will create reference dumps for `libfoo`, assuming `libfoo` is a VNDK
library.

# For LLNDK libraries

```
$ python3 create_reference_dumps.py -l libfoo --llndk
```
This will create reference dumps for `libfoo`, assuming `libfoo` is an LLNDK
library.


For more command line options, run `utils/create_reference_dumps.py --help`.
