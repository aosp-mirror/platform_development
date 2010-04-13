# Copyright 2009 Google Inc. All Rights Reserved.
#
# Android.mk for etc1tool 
#

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := etc1tool.cpp

LOCAL_C_INCLUDES += external/libpng
LOCAL_C_INCLUDES += external/zlib
LOCAL_C_INCLUDES += build/libs/host/include
LOCAL_C_INCLUDES += frameworks/base/opengl/include

#LOCAL_WHOLE_STATIC_LIBRARIES := 
LOCAL_STATIC_LIBRARIES := \
	libhost \
	libutils \
	libcutils \
	libexpat \
	libpng \
	libETC1

ifeq ($(HOST_OS),linux)
LOCAL_LDLIBS := -lz -lrt
endif

ifeq ($(HOST_OS),windows)
ifeq ($(strip $(USE_CYGWIN)),)
LOCAL_LDLIBS := -lz -lws2_32
endif
ifneq ($(strip $(USE_MINGW)),)
LOCAL_STATIC_LIBRARIES += libz
endif
endif

LOCAL_MODULE := etc1tool

include $(BUILD_HOST_EXECUTABLE)
