# Copyright 2007 The Android Open Source Project
#
# Copies files into the directory structure described by a manifest

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	line_endings.c

LOCAL_MODULE := line_endings

include $(BUILD_HOST_EXECUTABLE)

