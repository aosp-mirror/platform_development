# Test that LOCAL_CPPFLAGS only works for C++ sources
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := test-LOCAL_CPPFLAGS
LOCAL_SRC_FILES := test-LOCAL_CPPFLAGS-1.c \
                   test-LOCAL_CPPFLAGS-2.cpp \

LOCAL_CFLAGS   := -DBANANA=200

# Note, the -UBANANA is only there to prevent a warning
# the test works well without it.
LOCAL_CPPFLAGS := -UBANANA -DBANANA=300

include $(BUILD_SHARED_LIBRARY)
