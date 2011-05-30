
LOCAL_PATH := $(call my-dir)

ifeq ($(HOST_OS),darwin)
include $(CLEAR_VARS)


LOCAL_LDLIBS := -Wl,-framework,AppKit

LOCAL_SRC_FILES :=  setup_gl.m



LOCAL_CFLAGS := -g -O0
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libMac_view


include $(BUILD_HOST_STATIC_LIBRARY)
endif
