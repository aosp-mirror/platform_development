LOCAL_PATH := $(call my-dir)

### GLES_CM host implementation (On top of OpenGL) ########################
include $(CLEAR_VARS)

translator_path := $(LOCAL_PATH)/..

LOCAL_SRC_FILES :=                    \
     GLESv2Imp.cpp                    \
     GLESv2Context.cpp                \
     GLESv2Validate.cpp               \
     ShaderParser.cpp                 \

LOCAL_C_INCLUDES += \
                 $(translator_path)/include \

LOCAL_STATIC_LIBRARIES := \
    libGLcommon           \
    libOpenglOsUtils      \
    libutils              \
    libcutils

ifeq ($(HOST_OS),linux)
    LOCAL_LDLIBS := -lGL -ldl
endif

ifeq ($(HOST_OS),windows)
    LOCAL_LDLIBS := -lopengl32 -lgdi32
endif

LOCAL_CFLAGS := -g -O0
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libGLES_V2_translator


include $(BUILD_HOST_SHARED_LIBRARY)

