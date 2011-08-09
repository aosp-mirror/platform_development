# This build script corresponds to a library containing many definitions
# common to both the guest and the host. They relate to
#
LOCAL_PATH := $(call my-dir)

### CodecCommon  guest ##############################################
$(call emugl-begin-static-library,libOpenglCodecCommon)

LOCAL_SRC_FILES := \
        GLClientState.cpp \
        GLSharedGroup.cpp \
        glUtils.cpp \
        TcpStream.cpp \
        TimeUtils.cpp

LOCAL_CFLAGS += -DLOG_TAG=\"eglCodecCommon\"

$(call emugl-export,SHARED_LIBRARIES,libcutils libutils)
$(call emugl-export,C_INCLUDES,$(LOCAL_PATH))
$(call emugl-end-module)

### OpenglCodecCommon  host ##############################################
$(call emugl-begin-host-static-library,libOpenglCodecCommon)

LOCAL_SRC_FILES := \
        GLClientState.cpp \
        glUtils.cpp \
        TcpStream.cpp \
        TimeUtils.cpp

$(call emugl-export,STATIC_LIBRARIES,libcutils)
$(call emugl-export,C_INCLUDES,$(LOCAL_PATH))
$(call emugl-end-module)

