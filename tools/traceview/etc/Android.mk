# Copyright 2007 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_PREBUILT_EXECUTABLES := traceview
include $(BUILD_HOST_PREBUILT)

