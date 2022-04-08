# Copyright 2008 The Android Open Source Project
#
LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_MODULE := monkeylib
LOCAL_MODULE_STEM := monkey
include $(BUILD_JAVA_LIBRARY)

################################################################
include $(CLEAR_VARS)
LOCAL_MODULE := monkey
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_SRC_FILES := monkey
LOCAL_REQUIRED_MODULES := monkeylib
include $(BUILD_PREBUILT)
