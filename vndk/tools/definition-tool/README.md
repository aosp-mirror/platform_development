VNDK Definition Tool
====================

## Usage

Create a generic reference symbols from AOSP build:

    $ python3 vndk_definition_tool.py create-generic-ref \
        -o generic_arm64/system \
        ${OUT_DIR_COMMON_BASE}/target/product/generic_arm64/system

Run the VNDK definition tool with:

    $ python3 vndk_definition_tool.py vndk \
        --system ${ANDROID_PRODUCT_OUT}/system \
        --vendor ${ANDROID_PRODUCT_OUT}/vendor \
        --load-generic-refs generic_arm64

This command will print 7 different lists:

* **vndk-stable**

  - This list contains the pre-defined vndk-stable libraries.

  - The libraries with long-term API/ABI stability/compatibility commitment.

  - The libraries will be installed to /system/lib[64]/vndk-stable

* **sp-hal**

  - This list contains the pre-defined SP-HAL libraries and their dependencies.

  - The libraries will be installed to /vendor/lib[64]/sameprocess

* **vndk-core**

  - This list contains the shared libraries used by both the framework and
    vendor code.

  - The libraries must be either intact or inward-customized.

  - The libraries will be installed to /system/lib[64]/vndk-$FWK

* **vndk-indirect**

  - This list contains the shared libraries which are indirectly used by
    aforementioned vndk-core but not directly used by vendor code.

  - The libraries must be either intact or inward-customized.

  - The libraries will be installed to /system/lib[64]/vndk-$FWK

* **vndk-fwd-ext**

  - This list contains the vndk-core/vndk-indirect overlays for *the framework*.

  - The libraries must be either outward-customized or extended.  In other
    words, the libraries in this list might use or define non-AOSP APIs.

  - The libraries will be installed to /system/lib[64]/vndk-$FWK-ext

* **vndk-vnd-ext**

  - This list contains the vndk-core overlays for *vendor code*.

  - The libraries must be either outward-customized or extended.  In other
    words, the libraries in this list might use or define non-AOSP APIs.

  - The libraries will be installed to /vendor/lib[64]/vndk-$VND-ext

- **extra-vendor**

  - This list contains the extra libraries that have to be copied from
    /system/lib[64] to /vendor/lib[64].

  - The libraries in this list are usually the non-AOSP dependencies of
    vndk-vnd-ext or other vendor code.

  - The libraries will be installed to /vendor/lib[64]


# Sub Directory Tagging

If there are some sub directory under system partition must be treated as
vendor files, then specify such directory with: `--system-dir-as-vendor`.

Conversely, if there are some sub directory under vendor partition must be
treated as system files, then specify such directory with:
`--vendor-dir-as-system`.

For example, if the device does not have an independent `vendor` partition (but
with a `vendor` folder in the `system` partition), then run this command:

    $ python3 vndk_definition_tool.py vndk \
        --system ${ANDROID_PRODUCT_OUT}/system \
        --system-dir-as-vendor vendor \
        --load-generic-refs generic_arm64

For example, if `/system/bin/hw`, `/system/lib/hw`, and `/system/lib64/hw` are
containing vendor files, then run this command:

    $ python3 vndk_definition_tool.py vndk \
        --system ${ANDROID_PRODUCT_OUT}/system \
        --system-dir-as-vendor bin/hw \
        --system-dir-as-vendor lib/hw \
        --system-dir-as-vendor lib64/hw \
        --vendor ${ANDROID_PRODUCT_OUT}/vendor \
        --load-generic-refs generic_arm64


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
        --load-generic-refs generic_arm64 \
        --load-extra-deps dlopen.dep


# Partition for VNDK with Outward Customization
An outward-customized VNDK library can be put on both system and vendor
partition.  VNDK definition tool will assume such library will be installed
into /system/lib[64]/vndk-$FWK-ext by default.  Use following options to change
the default behavior.

* `--outward-customization-default-partition=[system*|vendor|both]`

  This option specifies the default destination for outward-customized VNDK
  libraries.  The default value is the system partition.

* `--outward-customization-for-system=[lib]`

  This option specifies the library that should be installed to the system
  partition if it is an outward-customized VNDK library.

* `--outward-customization-for-vendor=[lib]`

  This option specifies the library that should be installed to the vendor
  partition if it is an outward-customized VNDK library.


## Warnings

### Incorrect Partition

If you specify `--warn-incorrect-partition` command line option, then VNDK
definition tool will emit warnings when:

1. A framework library is only used by vendor binaries.

2. A vendor library is only used by framework binaries.

This allows people to review the correct partition for the module.  For example,

    warning: /system/lib/libtinyxml.so: This is a framework library with
    vendor-only usages.

    warning: /system/lib64/libtinyxml.so: This is a framework library with
    vendor-only usages.

These warnings suggest that `libtinyxml.so` might be better to move to vendor
partition.


### VNDK Extension

If you specify `--load-generic-refs`, then you may see some warnings:

    warning: /system/lib/libhardware_legacy.so: This is a VNDK extension and
    must be moved to vendor partition.

This warning indicates that the library is not in the generic reference or the
library contains some symbols that are not available in generic build.  It must
be installed into vendor partition instead.  As the result, it will be included
in vndk-ext.


## Example

We can run this against Nexus 6p Factory Image:

    $ unzip angler-nmf26f-factory-ef607244.zip

    $ cd angler-nmf26f

    $ unzip image-angler-nmf26f.zip

    $ simg2img system.img system.raw.img

    $ simg2img vendor.img vendor.raw.img

    $ mkdir system

    $ mkdir vendor

    $ sudo mount -o loop,ro system.raw.img system

    $ sudo mount -o loop,ro vendor.raw.img vendor

    $ sudo python3 vndk_definition_tool.py vndk \
        --system system --vendor vendor

We can run this against latest Android build:

    $ python3 vndk_definition_tool.py vndk \
        --system ${ANDROID_PRODUCT_OUT}/system \
        --system-dir-as-vendor bin/hw \
        --system-dir-as-vendor lib/hw \
        --system-dir-as-vendor lib64/hw \
        --vendor ${ANDROID_PRODUCT_OUT}/vendor


## Same-Process HAL

VNDK Definition Tool supports the same-process HAL definition as well.  To find
same-process HALs, run `sp-hal` subcommand:

    $ python3 vndk_definition_tool.py sp-hal \
        --system ${ANDROID_PRODUCT_OUT}/system \
        --vendor ${ANDROID_PRODUCT_OUT}/vendor

To find the dependencies of same-process HALs, add `--closure` option:

    $ python3 vndk_definition_tool.py sp-hal \
        --system ${ANDROID_PRODUCT_OUT}/system \
        --vendor ${ANDROID_PRODUCT_OUT}/vendor
        --closure


## Python 2 Support

Since `vndk_definition_tool.py` runs 3x faster with Python 3, the shebang is
specifying `python3` by default.  To run `vndk_definition_tool.py` with
python2, run the following command:

    $ python vndk_definition_tool.py [options]
