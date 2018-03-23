List Installed Files from Source Directories
============================================

`list_installed_file_from_source.py` helps users to list the vendor source
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

Then, in `${ANDROID_BUILD_TOP}`, run `list_installed_file_from_source.py`:

```
./development/vndk/tools/sourcedr/sourcedr/list_installed_file_from_source.py \
    out/combined-sailfish.ninja \
    --ninja-deps out/.ninja_deps \
    | tee files.txt
```

Be patient.  It may take for 3-5 mintues.


## Re-use the Parsed Dependency Graph

`list_installed_file_from_source.py` spends a lot of time parsing ninja
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
./development/vndk/tools/sourcedr/sourcedr/list_installed_file_from_source.py \
    sailfish.pickle
```


## Filters

By default, `list_installed_file_from_source.py` lists the files that are from
`device` or `vendor` directories and installed to the system partition.  This
can be tweaked with `--installed-filter` and `--source-filter`:

* `--installed-filter` filters the paths of the files that may be installed to
  the device.  The specified path must be relative to the file system root of
  the device.  The default value is `system`.

* `--source-filter` filters the paths of the source files.  The paths must be
  relative to Android source tree root (i.e. `${ANDROID_BUILD_TOP}`).  Multiple
  paths may be specified and separated by semicolons.  The default value is
  `device:vendor`.


### Examples

List the files from `device/google` and installed to `/system`:

```
./development/vndk/tools/sourcedr/sourcedr/list_installed_file_from_source.py \
    sailfish.pickle \
    --source-filter device/google \
    --installed-filter system
```


List the files from `device/google` and installed into `/system/lib64` with:

```
./development/vndk/tools/sourcedr/sourcedr/list_installed_file_from_source.py \
    sailfish.pickle \
    --source-filter device/google \
    --installed-filter system/lib64
```


List the files from `frameworks/base` or `frameworks/native` and installed
into `/system/lib` or `/system/lib64` with:

```
./development/vndk/tools/sourcedr/sourcedr/list_installed_file_from_source.py \
    sailfish.pickle \
    --source-filter frameworks/base:frameworks/native \
    --installed-filter system/lib:system/lib64
```
