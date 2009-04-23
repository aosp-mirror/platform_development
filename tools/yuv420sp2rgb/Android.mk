# Copyright 2005 Google Inc. All Rights Reserved.
#
# Android.mk for yuv420sp2rgb 
#

LOCAL_PATH:= $(call my-dir)

ifeq ($(TARGET_ARCH),arm)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := yuv420sp2rgb.c cmdline.c debug.c

LOCAL_MODULE := yuv420sp2rgb

include $(BUILD_HOST_EXECUTABLE)
endif
