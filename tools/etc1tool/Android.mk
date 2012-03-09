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
	libexpat \
	libpng \
	libETC1

ifeq ($(HOST_OS),linux)
LOCAL_LDLIBS += -lrt
endif

# Statically link libz for MinGW (Win SDK under Linux),
# and dynamically link for all others.
ifneq ($(strip $(USE_MINGW)),)
  LOCAL_STATIC_LIBRARIES += libz
else
  LOCAL_LDLIBS += -lz
endif

LOCAL_MODULE := etc1tool

include $(BUILD_HOST_EXECUTABLE)
