LOCAL_PATH := $(call my-dir)
emulatorOpengl := $(LOCAL_PATH)/../..

### GLESv1_enc Encoder ###########################################
include $(CLEAR_VARS)


LOCAL_SRC_FILES := \
        GLEncoder.cpp \
        GLEncoderUtils.cpp

LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libGLESv1_enc
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

glesv1_intermediates := $(local-intermediates-dir)

LOCAL_PRELINK_MODULE := false
LOCAL_CFLAGS += -DLOG_TAG=\"egl_GLESv1_enc\"
LOCAL_C_INCLUDES +=  \
    $(emulatorOpengl)/shared/OpenglCodecCommon \
    $(emulatorOpengl)/host/include/libOpenglRender \
    $(glesv1_intermediates)

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
        $(EMUGEN) -E $(glesv1_intermediates) -i $(PRIVATE_PATH) gl
$(GEN_GL) : $(EMUGEN) \
        $(LOCAL_PATH)/gl.attrib \
        $(LOCAL_PATH)/gl.in \
        $(LOCAL_PATH)//gl.types
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(GEN_GL)
include $(BUILD_SHARED_LIBRARY)
