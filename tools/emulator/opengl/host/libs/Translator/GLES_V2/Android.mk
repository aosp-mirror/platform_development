LOCAL_PATH := $(call my-dir)

### GLES_CM host implementation (On top of OpenGL) ########################
include $(CLEAR_VARS)

translator_path := $(LOCAL_PATH)/..
#exclude darwin builds
ifeq (, $(findstring $(HOST_OS), darwin))

LOCAL_SRC_FILES :=                    \
     GLESv2Imp.cpp                    \
     GLESv2Context.cpp                \
     GLESv2Validate.cpp               \
     ShaderParser.cpp                 \

LOCAL_C_INCLUDES += \
                 $(translator_path)/include \


LOCAL_SHARED_LIBRARIES := \
    libGLcommon

LOCAL_CFLAGS := -g -O0
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libGLES_V2_translator


include $(BUILD_HOST_SHARED_LIBRARY)

endif
