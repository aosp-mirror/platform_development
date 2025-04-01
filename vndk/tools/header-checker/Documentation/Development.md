# Development

## Build instructions

For Googlers, check out go/repo-init/main-clang-tools and run:

    $ OUT_DIR=out \
          development/vndk/tools/header-checker/android/build-prebuilts.sh

## Alternative build instructions

If you have a full source tree, you may build the tools with:

    $ source build/envsetup.sh

    $ lunch aosp_arm64-trunk_staging-userdebug

    $ cd development/vndk/tools/header-checker

    $ source android/envsetup.sh

    $ mm
