
LOCAL_PATH := $(call my-dir)

### EGL host implementation ########################
include $(CLEAR_VARS)

translator_path := $(LOCAL_PATH)/..

LOCAL_SRC_FILES :=           \
     GLDispatch.cpp          \
     GLutils.cpp             \
     GLEScontext.cpp         \
     GLESvalidate.cpp        \
     GLESpointer.cpp         \
     GLESbuffer.cpp          \
     DummyGLfuncs.cpp        \
     RangeManip.cpp          \
     objectNameManager.cpp


LOCAL_C_INCLUDES += \
                 $(translator_path)/include \
                 $(translator_path)/../../../shared

LOCAL_STATIC_LIBRARIES := \
    libOpenglOsUtils      \
    libutils              \
    libcutils

LOCAL_CFLAGS := -g -O0
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libGLcommon
ifeq ($(HOST_OS),linux)
    LOCAL_LDFLAGS := -Wl,--whole-archive
    LOCAL_LDLIBS := -lGL -ldl
endif

ifeq ($(HOST_OS),windows)
    LOCAL_LDLIBS := -lopengl32 -lgdi32
endif

include $(BUILD_HOST_STATIC_LIBRARY)
