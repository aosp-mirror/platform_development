# Copyright 2007 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_JAR_MANIFEST := ../etc/manifest.txt
LOCAL_JAVA_LIBRARIES := \
	androidprefs \
	sdklib \
	sdkuilib
LOCAL_MODULE := sdkmanager

include $(BUILD_HOST_JAVA_LIBRARY)

