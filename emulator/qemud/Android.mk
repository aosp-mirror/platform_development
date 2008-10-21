# Copyright 2008 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	qemud.c


LOCAL_SHARED_LIBRARIES := \
	libcutils \

LOCAL_MODULE:= qemud

include $(BUILD_EXECUTABLE)
