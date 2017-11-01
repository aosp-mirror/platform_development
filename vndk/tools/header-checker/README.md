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

  $ cd $(ANDROID_BUILD_TOP); lunch aosp_arm64; make -j64
  $ python3 utils/create_reference_dumps.py --vndk-list vndk_list -ref-dump-dir \
    $(ANDROID_BUILD_TOP)/prebuilts/abi-dumps/vndk --version current

    --vndk_list : file containing a list of the libraries whose
    reference dumps are to be created, seperated by \n. For eg:
    vndk_list:
    libtinyxml2
    libziparchive
    libc++

    --version: This is the version of the vndk being built.

    This will create corresponding .lsdump files at
    $(ANDROID_BUILD_TOP)/prebuilts/abi-dumps/vndk/arm(64)/current/source-based

  gzip the reference dumps produced :
     $ cd $(ANDROID_BUILD_TOP)/prebuilts/abi-dumps/vndk/arm(64)/current/source-based;
     $ gzip *.lsdump;

  The same procedure should also be followed for ll-ndk libraries, listing them
  in a seperate file and running create_reference_dumps.py with -ref-dump-dir
  set to $(ANDROID_BUILD_TOP)/prebuilts/abi-dumps/ndk.

  On subsequent arm based builds, header-abi-diff will report warnings on
  abi-breakages and changes to exported apis.

