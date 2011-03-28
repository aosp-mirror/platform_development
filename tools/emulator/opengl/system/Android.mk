
LOCAL_PATH := $(call my-dir)

### OpenglCodecCommon ##############################################

include $(CLEAR_VARS)

OpenglCodecCommon := \
        OpenglCodecCommon/GLClientState.cpp \
        OpenglCodecCommon/glUtils.cpp \
        OpenglCodecCommon/TcpStream.cpp

LOCAL_SRC_FILES :=  $(OpenglCodecCommon)

LOCAL_CFLAGS += -DLOG_TAG=\"eglCodecCommon\"
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libOpenglCodecCommon

include $(BUILD_STATIC_LIBRARY)

### OpenglCodecCommon  host ##############################################
include $(CLEAR_VARS)

LOCAL_SRC_FILES :=  $(OpenglCodecCommon)

LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libOpenglCodecCommon
LOCAL_PRELINK_MODULE := false

# XXX - enable the next line for host debugging - JR
# LOCAL_CFLAGS := -O0 -g
include $(BUILD_HOST_STATIC_LIBRARY)

