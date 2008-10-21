# Copyright 2006 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)

# host executable
#
include $(CLEAR_VARS)
LOCAL_SRC_FILES:= mksdcard.c
LOCAL_MODULE = mksdcard
include $(BUILD_HOST_EXECUTABLE)

