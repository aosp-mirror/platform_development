# A small sample used to demonstrate static C++ destructors
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libtest1
LOCAL_SRC_FILES := libtest1.cpp
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := test_dlclose_destruction
LOCAL_SRC_FILES := main.c
include $(BUILD_EXECUTABLE)
