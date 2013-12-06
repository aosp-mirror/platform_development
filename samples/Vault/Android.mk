LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS)

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_SDK_VERSION := current

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_PACKAGE_NAME := Vault

include $(BUILD_PACKAGE)
