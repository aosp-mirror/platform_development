# Copyright 2008 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_JAVA_LIBRARIES := \
	androidprefs

LOCAL_MODULE := jarutils

include $(BUILD_HOST_JAVA_LIBRARY)

