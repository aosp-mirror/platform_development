LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_PACKAGE_NAME := AtscTvInput
LOCAL_MODULE_TAGS := samples
LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)
