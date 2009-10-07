# Copyright 2009 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_JAVA_RESOURCE_DIRS := resources

LOCAL_MODULE := uix
LOCAL_JAVA_LIBRARIES := \
	groovy-all-1.6.5

include $(BUILD_HOST_JAVA_LIBRARY)
