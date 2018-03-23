List System Files from Vendor Source
====================================

`list_system_file_from_vendor_source.py` helps users to list the vendor source
files or vendor prebuilts that will be installed into the system image.

This tool lists all *possible* file that *may* be installed to the system
image.  Some listed files are not actually installed because they are not
specified in `PRODUCT_PACKAGES`.


## Usage

First, build an Android target:

```
lunch aosp_sailfish-userdebug
make -j8
```

Then, in `${ANDROID_BUILD_TOP}`, run `list_system_file_from_vendor_source.py`:

```
./development/vndk/tools/sourcedr/sourcedr/list_system_file_from_vendor_source.py \
    --ninja-deps out/.ninja_deps \
    out/combined-sailfish.ninja \
    | tee files.txt
```

It may take for 3-5 mintues.
