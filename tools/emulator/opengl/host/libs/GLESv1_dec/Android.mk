
LOCAL_PATH := $(call my-dir)

### GLESv1 Decoder ###########################################
include $(CLEAR_VARS)

emulatorOpengl := $(LOCAL_PATH)/../../..
EMUGEN := $(HOST_OUT_EXECUTABLES)/emugen

LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libGLESv1_dec

intermediates := $(local-intermediates-dir)

LOCAL_SRC_FILES := \
        GLDecoder.cpp

LOCAL_C_INCLUDES += \
    $(emulatorOpengl)/system/OpenglCodecCommon \
    $(emulatorOpengl)/system/GLESv1_enc

LOCAL_STATIC_LIBRARIES := \
        libOpenglCodecCommon \
        liblog

# XXX - uncomment for debug
#LOCAL_CFLAGS := -DDEBUG_PRINTOUT -O0 -g
LOCAL_LDLIBS := -ldl


GEN := $(intermediates)/gl_dec.cpp $(intermediates)/gl_dec.h

$(GEN) : PRIVATE_PATH := $(LOCAL_PATH)
$(GEN) : PRIVATE_CUSTOM_TOOL := $(EMUGEN) -D $(intermediates) -i $(emulatorOpengl)/system/GLESv1_enc gl
$(GEN) : $(EMUGEN) \
        $(emulatorOpengl)/system/GLESv1_enc/gl.attrib \
        $(emulatorOpengl)/system/GLESv1_enc/gl.in \
        $(emulatorOpengl)/system/GLESv1_enc/gl.types
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(GEN)
include $(BUILD_HOST_SHARED_LIBRARY)
