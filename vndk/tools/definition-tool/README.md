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

    vndk-sp: libexample1.so
    vndk-sp-ext: libexample2.so
    extra-vendor-libs: libexample3.so

The output implies:

1. `libexample1.so` should be copied to `/system/lib[64]/vndk-sp`.
2. `libexample2.so` should be copied to `/vendor/lib[64]/vndk-sp`.
3. `libexample3.so` should be copied to `/vendor/lib[64]`.


# Makefile Boilerplates

There are some boilerplates in `templates` directory that can automate the
process to copy shared libraries.  Please copy a boilerplate, rename it as
`Android.mk`, and replace the placeholders with corresponding values:

* `##_VNDK_SP_##` should be replaced by library names tagged with `vndk_sp`.

* `##_VNDK_SP_EXT_##` should be replaced by library names tagged with
  `vndk_sp_ext`.

* `##_EXTRA_VENDOR_LIBS_##` should be replaced by library names tagged with
  `extra_vendor_libs`.

* `$(YOUR_DEVICE_NAME)` has to be replaced by your own device product name.

VNDK definition tool can fill in the library names and generate an `Android.mk`
when the `--output-format=make` is specified:

    $ python3 ./vndk_definition_tool.py vndk \
        --system "/path/to/your/product_out/system" \
        --vendor "/path/to/your/product_out/vendor" \
        --aosp-system "/path/to/aosp/generic/system" \
        --tag-file "eligible-list-v3.0.csv" \
        --output-format=make

These boilerplates only define the modules to copy shared libraries.
Developers have to add the phony package name to `PRODUCT_PACKAGES` variable in
the `device.mk` for their devices.

    PRODUCT_PACKAGES += $(YOUR_DEVICE_NAME)-vndk


## Ignore Subdirectories

Some devices keep their vendor modules in `/system/vendor`.  To run VNDK
definition tool for those devices, we have to skip `/system/vendor` and specify
it with `--vendor` option.  For example:

    python3 vndk_definition_tool.py vndk \
        --system ${ANDROID_PRODUCT_OUT}/system \
        --system-dir-igored vendor \
        --vendor ${ANDROID_PRODUCT_OUT}/system/vendor \
        # ...


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
