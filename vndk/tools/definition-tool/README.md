VNDK Definition Tool
====================

VNDK definition tool was designed to classify all shared libraries in the
system partition and give suggestions to copy necessary libraries to the vendor
partition.

## Usage

To run VNDK definition tool, you will need three inputs:

1. The system and vendor image for your target
2. Android Treble reference image
3. Eligible VNDK list from Google (e.g. eligible-list-v3.0.csv)

The high-level overview of the command line usage is:

    $ python3 ./vndk_definition_tool.py vndk \
        --system "/path/to/your/product_out/system" \
        --vendor "/path/to/your/product_out/vendor" \
        --aosp-system "/path/to/aosp/generic/system" \
        --tag-file "eligible-list-v3.0.csv"

This command will print several lines such as:

    extra_vndk_sp_indirect: libexample1.so
    extra_vndk_sp_indirect: libexample2.so
    vndk_ext: libexample3.so
    vndk_ext: libexample4.so

This output implies:

1. `libexample1.so` and `libexample2.so` should be copied into
   `/vendor/lib[64]/vndk-sp`.

2. `libexample3.so` and `libexample4.so` should be copied into
   `/vendor/lib[64]`.


# Boilerplates

There are some boilerplates in `templates` directory that can automate the
process to copy shared libraries.

If the output tagged some shared libraries with `extra_vndk_sp_indirect`, then
copy `templates/extra_vndk_sp_indirect.txt` to an Android.mk and substitute
`##_EXTRA_VNDK_SP_INDIRECT_##` with library names (without `.so`).

If the output tagged some shared libraries with `vndk_ext`, then copy
`templates/vndk_ext.txt` to an Android.mk and substitute `##_VNDK_EXT_##` with
library names (without `.so`).

These boilerplates only define the modules to copy shared libraries.
Developers have to add those modules to the `PRODUCT_PACKAGES` variable in
their `device.mk`.  For example, in the example mentioned above, following
`PRODUCT_PACKAGES` changes are necessary for that target:

    PRODUCT_PACKAGES += libexample1.vndk-sp-ext
    PRODUCT_PACKAGES += libexample2.vndk-sp-ext
    PRODUCT_PACKAGES += libexample3.vndk-ext
    PRODUCT_PACKAGES += libexample4.vndk-ext


## Implicit Dependencies

If there are implicit dependencies, such as `dlopen()`, we can specify them in
a dependency file and load the dependency file with `--load-extra-deps`.  The
dependency file format is simple: (a) each line stands for a dependency, and
(b) the file before the colon depends on the file after the colon.  For
example, `libart.so` depends on `libart-compiler.so`:

    /system/lib64/libart.so: /system/lib64/libart-compiler.so

And then, run VNDK definition tool with:

    $ python3 vndk_definition_tool.py vndk \
        --system ${ANDROID_PRODUCT_OUT}/system \
        --vendor ${ANDROID_PRODUCT_OUT}/vendor \
        --aosp-system ${ANDROID_PRODUCT_OUT}/../generic_arm64_a \
        --tag-file eligible-list-v3.0.csv \
        --load-extra-deps dlopen.dep


## Remarks

To run VNDK definition tool against an image (`.img`), run the following
command to mount the images and run `vndk_definition_tool.py` with `sudo`:

    $ simg2img system.img system.raw.img

    $ simg2img vendor.img vendor.raw.img

    $ mkdir system

    $ mkdir vendor

    $ sudo mount -o loop,ro system.raw.img system

    $ sudo mount -o loop,ro vendor.raw.img vendor

    $ sudo python3 vndk_definition_tool.py vndk \
        --system system \
        --vendor vendor \
        --aosp-system /path/to/aosp/generic/system \
        --tag-file eligible-list-v3.0.csv
