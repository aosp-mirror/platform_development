LOCAL_PATH := $(call my-dir)

### GLES_CM host implementation (On top of OpenGL) ########################
include $(CLEAR_VARS)

translator_path := $(LOCAL_PATH)/..

LOCAL_SRC_FILES :=      \
     GLEScmImp.cpp      \
     GLEScmUtils.cpp    \
     TextureUtils.cpp   \
     GLEScmContext.cpp  \
     GLEScmValidate.cpp 

LOCAL_C_INCLUDES += \
                 $(translator_path)/include \
                 $(translator_path)/../../../shared

LOCAL_STATIC_LIBRARIES := \
    libGLcommon           \
    libOpenglOsUtils      \
    libutils              \
    libcutils

LOCAL_CFLAGS := -g -O0
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libGLES_CM_translator

ifeq ($(HOST_OS),linux)
    LOCAL_LDLIBS := -lGL -ldl
endif

ifeq ($(HOST_OS),windows)
    LOCAL_LDLIBS := -lopengl32 -lgdi32
endif

include $(BUILD_HOST_SHARED_LIBRARY)

