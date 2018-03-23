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
    out/combined-sailfish.ninja \
    --ninja-deps out/.ninja_deps \
    | tee files.txt
```

Be patient.  It may take for 3-5 mintues.


## Re-use the Parsed Dependency Graph

`list_system_file_from_vendor_source.py` spends a lot of time parsing ninja
files.  It is desirable to keep the parsed dependency graph for further
investigation.  You can create a parsed dependency graph with:

```
./development/vndk/tools/sourcedr/sourcedr/ninja.py pickle \
    out/combined-sailfish.ninja \
    --ninja-deps out/.ninja_deps \
    -o sailfish.pickle
```

And then, load dependency graph with:

```
./development/vndk/tools/sourcedr/sourcedr/list_system_file_from_vendor_source.py \
    sailfish.pickle
```
