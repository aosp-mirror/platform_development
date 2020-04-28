LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := OBJViewer

LOCAL_MODULE_TAGS := tests

# currently disabled because of API changes. won't be fixed for 1.0
#include $(BUILD_PACKAGE)
