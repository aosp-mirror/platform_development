# How to make your build VNDK compliant

## Enable VNDK flag
In your device's BoardConfig.mk, set BOARD_VNDK_VERSION := current.

Set that device's lunch combo and compile

    make -j32 >log 2>&1


## Fixing Build errors
The resulting errors will be mainly of 2 types:

### Copy headers not allowed
Vendor modules are not allowed to use LOCAL_COPY_HEADERS. They need to export
their headers via BUILD_HEADER_LIBRARY. Modules will import this library via
LOCAL_HEADER_LIBRARIES. 

Here is an example on how to do that:
* Lets call the offending module libabc. Open libabc's Android.mk
* Note all the headers that are being copied by libabc
* Create a local dir called include (or inc). Add symlinks to every file that is
   being copied. If all the files are in the same folder, the include dir itself
   will be a symlink to that folder
* In Android.mk, remove all lines with copy headers


    - LOCAL_COPY_HEADERS_TO := dir
    - LOCAL_COPY_HEADERS := file1
    - LOCAL_COPY_HEADERS := file2
    - ....

* Replace above lines with


    + LOCAL_EXPORT_HEADER_LIBRARY_HEADERS := libabc_headers


* Create the module_headers lib outside the definition of current module


    + include $(CLEAR_VARS)
    + LOCAL_MODULE := libabc_headers
    + LOCAL_EXPORT_C_INCLUDE_DIRS := $(LOCAL_PATH)/include
    + include $(BUILD_HEADER_LIBRARY)

Note: - and + are code lines in patch format

### Headers not found
* Once all copy header violations are cleaned up, make will start throwing lots of
   "file not found" errors. These are due to 2 reasons:

   * Modules relying on copy headers are not finding those headers anymore due
   to above changes

   * VNDK build rules remove global includes from the path. So dirs like
   system/core/include, frameworks/av/include or hardware/libhardware/include
   will no longer be offered in include path
* Fix them using the **parse_and_fix_errors.sh** script. Customize it according to
   your needs.
 

