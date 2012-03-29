LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := samples

LOCAL_SRC_FILES := $(call all-subdir-java-files)

# TODO: Change sdk version to 16
LOCAL_SDK_VERSION := current

LOCAL_PACKAGE_NAME := HelloSpellChecker

include $(BUILD_PACKAGE)
