
LOCAL_PATH := $(call my-dir)

### EGL host implementation ########################
include $(CLEAR_VARS)

translator_path := $(LOCAL_PATH)/..

LOCAL_SRC_FILES :=           \
     GLutils.cpp             \
     objectNameManager.cpp


LOCAL_C_INCLUDES += \
                 $(translator_path)/include

LOCAL_STATIC_LIBRARIES := \
    libutils              \
    libcutils

LOCAL_CFLAGS := -g -O0
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libGLcommon
ifeq ($(HOST_OS),linux)
    LOCAL_LDFLAGS := -Wl,--whole-archive
endif

include $(BUILD_HOST_SHARED_LIBRARY)
