LOCAL_PATH := $(call my-dir)
emulatorOpengl := $(LOCAL_PATH)/../..

### GLESv2_enc Encoder ###########################################
include $(CLEAR_VARS)


LOCAL_SRC_FILES := \
	GL2EncoderUtils.cpp \
        GL2Encoder.cpp 


LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libGLESv2_enc
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

glesv2_intermediates := $(local-intermediates-dir)

LOCAL_PRELINK_MODULE := false
LOCAL_CFLAGS += -DLOG_TAG=\"egl_GLESv2_enc\"
LOCAL_C_INCLUDES +=  \
    $(emulatorOpengl)/shared/OpenglCodecCommon \
    $(emulatorOpengl)/host/include/libOpenglRender \
    $(glesv2_intermediates)

LOCAL_STATIC_LIBRARIES := \
        libOpenglCodecCommon
LOCAL_SHARED_LIBRARIES := libcutils

EMUGEN := $(HOST_OUT_EXECUTABLES)/emugen

GEN_GL2 := \
	$(glesv2_intermediates)/gl2_entry.cpp \
	$(glesv2_intermediates)/gl2_enc.cpp \
	$(glesv2_intermediates)/gl2_enc.h

$(GEN_GL2) : PRIVATE_PATH := $(LOCAL_PATH)
$(GEN_GL2) : PRIVATE_CUSTOM_TOOL := \
        $(EMUGEN) -E $(glesv2_intermediates) -i $(PRIVATE_PATH) gl2
$(GEN_GL2) : $(EMUGEN) \
        $(LOCAL_PATH)/gl2.attrib \
        $(LOCAL_PATH)/gl2.in \
        $(LOCAL_PATH)/gl2.types
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(GEN_GL2)
include $(BUILD_SHARED_LIBRARY)


