
LOCAL_PATH := $(call my-dir)
emulatorOpengl := $(LOCAL_PATH)/../..

### OpenglCodecCommon ##############################################

include $(CLEAR_VARS)

OpenglCodecCommon := \
        GLClientState.cpp \
        glUtils.cpp \
        TcpStream.cpp \
        TimeUtils.cpp

LOCAL_SRC_FILES :=  $(OpenglCodecCommon)

LOCAL_C_INCLUDES += $(emulatorOpengl)/host/include/libOpenglRender 

LOCAL_CFLAGS += -DLOG_TAG=\"eglCodecCommon\"
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libOpenglCodecCommon

include $(BUILD_STATIC_LIBRARY)

### OpenglCodecCommon  host ##############################################
include $(CLEAR_VARS)

LOCAL_SRC_FILES :=  $(OpenglCodecCommon)

LOCAL_C_INCLUDES += $(emulatorOpengl)/host/include/libOpenglRender 

LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libOpenglCodecCommon
LOCAL_PRELINK_MODULE := false

# XXX - enable the next line for host debugging - JR
# LOCAL_CFLAGS := -O0 -g
include $(BUILD_HOST_STATIC_LIBRARY)
