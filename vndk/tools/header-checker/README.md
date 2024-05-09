Header ABI Checker
===================

The header ABI checker consists of 3 tools:
[header-abi-dumper](#Header-ABI-Dumper),
[header-abi-linker](#Header-ABI-Linker), and
[header-abi-diff](#Header-ABI-Diff).  The first two commands generate ABI dumps
for shared libraries.  The third command compares the ABI dumps with the
prebuilt reference ABI dumps.

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

### Configuration
header-abi-diff reads a config file named `config.json`. The config file must
be placed in the dump directory, such as
`prebuilts/abi-dumps/platform/33/64/x86_64/source-based/config.json`.
The file consists of multiple sections. There are two types of sections: global
config section and library config section. Each library config section contains
flags for a specific version and a library. header-abi-diff chooses the library
config section by command line options `-target-version` and `-lib`.

#### Format
Here is an example of a config.json.
```json
{
  "global": {
    "flags": {
      "allow_adding_removing_weak_symbols": true,
    },
  },
  "libfoo": [
    {
      "target_version": "current",
      "flags": {
        "check_all_apis": true,
      },
    },
    {
      "target_version": "34",
      "ignore_linker_set_keys": [
        "_ZTI14internal_state",
      ],
      "flags": {
        "allow_extensions": true,
      }
    }
  ]
}
```

#### Library Config Section
A library config section includes members: "target_version",
"ignore_linker_set_keys" and "flags". header-abi-diff selects the config
section that matches the target version given by CLI.
Take above config as an example, if `-target-version 34` and `-lib libfoo` are
specified, the selected config section is:
```json
{
  "target_version": "34",
  "ignore_linker_set_keys": [
    "_ZTI14internal_state",
  ],
  "flags": {
    "allow_extensions": true,
  }
}
```

#### Flags

The config file and the header-abi-diff CLI support the same set of `flags`. If
a flag is present in both CLI and config sections, the library config section
takes priority, then the global config section and the CLI.

## Opt-in ABI check

Android build system runs the ABI checker automatically when it builds
particular libraries, such as NDK and VNDK. Developers can enable the ABI
check for common libraries by the following steps:

1. Set the ABI checker properties in Android.bp. For example,

   ```
   cc_library {
       name: "libfoo",
       ...
       target: {
           vendor: {
               header_abi_checker: {
                   enabled: true,
                   symbol_file: "map.txt",
                   ref_dump_dirs: ["abi-dumps"],
               },
           },
       },
   }
   ```

   `cc_library` modules and their `platform`, `product`, and `vendor` variants
   support `header_abi_checker`. The following are the commonly used
   properties of `header_abi_checker`:

   - `enabled` explicitly enables or disables the check.
   - `symbol_file` is the file containing the exported symbols.
   - `diff_flags` are the command line options for header-abi-diff.
   - `ref_dump_dirs` are the directories containing the dumps and
     [config files](#Configuration).

2. Follow the instructions in
   [Update Opt-in Reference ABI Dumps](#Update-Opt_in-Reference-ABI-Dumps)
   to generate ABI dumps in the `ref_dump_dirs`.

3. Verify that the ABI check is working.

   ```
   $ make libfoo.vendor
   $ find $ANDROID_BUILD_TOP/out/soong/.intermediates \
     -name libfoo.so.opt0.abidiff
   ```

## FAQ

### How to Resolve ABI Difference

The build system compares the source code with three sets of reference dumps:
**current version**, **opt-in**, and **previous version**. The ABI difference
is propagated as build errors. This section describes the common methods to
resolve them.

#### Update Reference ABI Dumps for Current Version

When the build system finds difference between the source code and the ABI
reference dumps for the **current version**, it instructs you to run
`create_reference_dumps.py` to update the dumps.

The command below updates the reference ABI dumps for all monitored libraries
on arm, arm64, x86, and x86_64 architectures:

```
$ python3 utils/create_reference_dumps.py
```

To update reference ABI dumps for a specific library, `libfoo` for example,
run the command below:

```
$ python3 utils/create_reference_dumps.py -l libfoo
```

For more command line options, run:

```
$ utils/create_reference_dumps.py --help
```

#### Update Opt-in Reference ABI Dumps

When the build system finds difference between the source code and the
**opt-in** ABI reference dumps, it instructs you to run
`create_reference_dumps.py` with `--ref-dump-dir` to update the dumps.

The command below updates the reference ABI dumps for a specific library:

```
$ python3 utils/create_reference_dumps.py -l libfoo \
  --ref-dump-dir /path/to/abi-dumps
```

You may specify `--product` if you don't want to create the ABI dumps for
all architectures. For example, with `--product aosp_arm`, the command creates
dumps for 32-bit arm only.

#### Configure Cross-Version ABI Check

When the build system finds incompatibility between the source code and the ABI
of the **previous version**, it instructs you to follow this document to
resolve it.

If the ABI difference is intended, you may configure the ABI tools to ignore
it. The following example shows how to make an exception for the ABI difference
in `libfoo` between the current source and the previous version, `33`:

1. Open `libfoo.so.33.abidiff` which is located in
   `$OUT_DIR/soong/.intermediates` or `$DIST_DIR/abidiffs`. Find out the
   `linker_set_key` of the type that has ABI difference. Here is a sample
   abidiff file:

   ```
   lib_name: "libfoo"
   arch: "x86_64"
   record_type_diffs {
     name: "bar"
     ...
     linker_set_key: "_ZTI3bar"
   }
   compatibility_status: INCOMPATIBLE
   ```

2. Find the reference dump directories by

   `find $ANDROID_BUILD_TOP/prebuilts/abi-dumps/*/33 -name libfoo.so.lsdump -exec dirname {} +`

   The command should show 6 directories for different architectures.

3. Create or edit `config.json` in every directory, for instance,

   `prebuilts/abi-dumps/ndk/33/64/x86_64/source-based/config.json`

   ```
   {
     "libfoo": [
       {
         "target_version": "34",
         "ignore_linker_set_keys": [
           "_ZTI3bar",
         ],
       },
     ],
   }
   ```

   The config above makes the ABI tools ignore the difference in type
   `_ZTI3bar` in `libfoo`. If the API level of this branch has been finalized
   (i.e., PLATFORM_VERSION_CODENAME=REL), `target_version` must be set to the
   API level. Otherwise, `target_version` must be set to
   **the previous finalized API level + 1** so that the config will continue
   being effective after finalization.

For more information about the config files, please refer to
[Configuration](#Configuration).

### How to Ignore Weak Symbol Difference

If you compile Android with a customized toolchain, it may produce different
weak symbols. You may make header-abi-diff ignore the weak symbols by adding
`config.json` to each reference dump directory. For example, the following
configuration makes header-abi-diff ignore weak symbols for all x86_64 NDK
libraries at API level 33:

`prebuilts/abi-dumps/ndk/33/64/x86_64/source-based/config.json`

```
{
  "global": {
    "flags": {
      "allow_adding_removing_weak_symbols": true,
    },
  },
}
```

To ignore weak symbols for a specific library, you can add extra flags to its
Android.bp. For example,

```
cc_library {
    header_abi_checker: {
        diff_flags: ["-allow-adding-removing-weak-symbols"],
    },
}
```

### How to Disable the ABI Check

You can disable the ABI check entirely by setting the environment variable
`SKIP_ABI_CHECKS`. For example,

`$ SKIP_ABI_CHECKS=true make`

You can disable the ABI check for a specific library by using the property
`enabled` in its Android.bp. For example,

```
cc_library {
    header_abi_checker: {
        enabled: false,
    },
}
```
