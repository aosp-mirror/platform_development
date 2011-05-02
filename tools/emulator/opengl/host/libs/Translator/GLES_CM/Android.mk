LOCAL_PATH := $(call my-dir)

### GLES_CM host implementation (On top of OpenGL) ########################
include $(CLEAR_VARS)

translator_path := $(LOCAL_PATH)/..
#exclude darwin builds
ifeq (, $(findstring $(HOST_OS), darwin))

LOCAL_SRC_FILES :=    \
     GLDispatch.cpp   \
     GLEScontext.cpp  \
     GLESimp.cpp      \
     GLESpointer.cpp  \
     GLESvalidate.cpp \
     GLESutils.cpp    \
     GLESbuffer.cpp   \
     TextureUtils.cpp \
     RangeManip.cpp

LOCAL_C_INCLUDES += \
                 $(translator_path)/include

LOCAL_STATIC_LIBRARIES := \
    libGLcommon           \
    libcutils

LOCAL_CFLAGS := -g -O0
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libGLES_CM_translator
LOCAL_LDLIBS := -lGL

include $(BUILD_HOST_SHARED_LIBRARY)

endif
