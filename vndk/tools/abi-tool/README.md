VNDK ABI Tool
=============

This is the wrapper command to collect ABI reference dumps from the binaries
with debug information.


## Usage

First, lunch the product target:

    $ cd ${AOSP_DIR}
    $ source build/envsetup.sh
    $ lunch ${YOUR_TARGET_NAME}

Second, build `vndk-vtable-dumper`:

    $ croot
    $ cd development/vndk/tools/vtable-dumper
    $ mm -j${NUM_CORES}

Third, run `vndk_abi_tool.py` with VNDK library list file:

    $ croot
    $ cd development/vndk/tools/abi-tool
    $ ./vndk_abi_tool.py --vndk-list=${VNDK_LIBRARY_LIST_FILE}

The content of `${VNDK_LIBRARY_LIST_FILE}` should contain VNDK library names
(one name per line.)  For example, if the VNDK library set contains
`libjpeg.so` and `libpng.so`, then `${VNDK_LIBRARY_LIST_FILE}` will be:

    libjpeg.so
    libpng.so

You can skip `--vndk-list` as well.  In that case, `vndk_abi_tool.py` will
generate ABI dumps for all shared libraries.
