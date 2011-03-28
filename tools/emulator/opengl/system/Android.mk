
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


### GLESv1_enc Encoder ###########################################
include $(CLEAR_VARS)


LOCAL_SRC_FILES := \
        GLESv1_enc/GLEncoder.cpp \
        GLESv1_enc/GLEncoderUtils.cpp

LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libGLESv1_enc
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

glesv1_intermediates := $(local-intermediates-dir)

LOCAL_PRELINK_MODULE := false
LOCAL_CFLAGS += -DLOG_TAG=\"egl_GLESv1_enc\"
LOCAL_C_INCLUDES +=  \
    $(LOCAL_PATH)/OpenglCodecCommon \
    $(LOCAL_PATH)/GLESv1_enc $(glesv1_intermediates)

LOCAL_STATIC_LIBRARIES := \
        libOpenglCodecCommon
LOCAL_SHARED_LIBRARIES := libcutils

EMUGEN := $(HOST_OUT_EXECUTABLES)/emugen

GEN_GL := \
	$(glesv1_intermediates)/gl_entry.cpp \
	$(glesv1_intermediates)/gl_enc.cpp \
	$(glesv1_intermediates)/gl_enc.h

$(GEN_GL) : PRIVATE_PATH := $(LOCAL_PATH)
$(GEN_GL) : PRIVATE_CUSTOM_TOOL := \
        $(EMUGEN) -E $(glesv1_intermediates) -i $(PRIVATE_PATH)/GLESv1_enc gl
$(GEN_GL) : $(EMUGEN) \
        $(LOCAL_PATH)/GLESv1_enc/gl.attrib \
        $(LOCAL_PATH)/GLESv1_enc/gl.in \
        $(LOCAL_PATH)/GLESv1_enc/gl.types
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(GEN_GL)
include $(BUILD_SHARED_LIBRARY)

