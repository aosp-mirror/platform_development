# Test that LOCAL_CFLAGS works for both C and C++ sources
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := test-LOCAL_CFLAGS
LOCAL_SRC_FILES := test-LOCAL_CFLAGS-1.c \
                   test-LOCAL_CFLAGS-2.cpp \

LOCAL_CFLAGS    := -DBANANA=100

include $(BUILD_SHARED_LIBRARY)
