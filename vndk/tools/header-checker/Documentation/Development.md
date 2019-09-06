# Development

## Checkout source tree

To checkout the source tree, run the following commands:

    $ mkdir aosp-clang-tools

    $ cd aosp-clang-tools

    $ repo init \
          -u persistent-https://android.googlesource.com/platform/manifest \
          -b clang-tools

    $ repo sync


## Build instructions

To build `header-abi-dumper`, `header-abi-linker` and `header-abi-diff`:

    $ OUT_DIR=out \
          development/vndk/tools/header-checker/android/build-prebuilts.sh


## Alternative build instructions

If you have a full AOSP master branch source tree, you may build the tools
with:

    $ source build/envsetup.sh

    $ lunch aosp_arm64-userdebug

    $ cd development/vndk/tools/header-checker

    $ source android/envsetup.sh

    $ mm
