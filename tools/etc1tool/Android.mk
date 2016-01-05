# Copyright 2009 Google Inc. All Rights Reserved.
#
# Android.mk for etc1tool 
#

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := etc1tool.cpp

LOCAL_STATIC_LIBRARIES := \
	libhost \
	libexpat \
	libpng \
	libETC1

# Statically link libz for MinGW (Win SDK under Linux),
# and dynamically link for all others.
LOCAL_STATIC_LIBRARIES_windows := libz
LOCAL_LDLIBS_darwin := -lz
LOCAL_LDLIBS_linux := -lrt -lz

LOCAL_MODULE := etc1tool
LOCAL_MODULE_HOST_OS := darwin linux windows

include $(BUILD_HOST_EXECUTABLE)
