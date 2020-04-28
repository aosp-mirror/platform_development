LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := samples

# Only compile source java files in this apk.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := BusinessCard

LOCAL_SDK_VERSION := current

LOCAL_DEX_PREOPT := false

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)
