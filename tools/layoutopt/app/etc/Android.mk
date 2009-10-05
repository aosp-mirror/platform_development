# Copyright 2009 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_PREBUILT_EXECUTABLES := layoutopt
include $(BUILD_HOST_PREBUILT)

