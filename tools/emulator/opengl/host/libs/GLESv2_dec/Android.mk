LOCAL_PATH := $(call my-dir)

### GLESv2 Decoder ###########################################
include $(CLEAR_VARS)

emulatorOpengl := $(LOCAL_PATH)/../../..
EMUGEN := $(BUILD_OUT_EXECUTABLES)/emugen

LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libGLESv2_dec

intermediates := $(local-intermediates-dir)

LOCAL_SRC_FILES := \
        GL2Decoder.cpp

LOCAL_C_INCLUDES += \
    $(emulatorOpengl)/shared \
    $(emulatorOpengl)/shared/OpenglCodecCommon \
    $(emulatorOpengl)/host/include/libOpenglRender \
    $(emulatorOpengl)/system/GLESv2_enc

LOCAL_STATIC_LIBRARIES := \
        libOpenglCodecCommon \
	libOpenglOsUtils \
        liblog

# XXX - uncomment for debug
#LOCAL_CFLAGS := -DDEBUG_PRINTOUT -O0 -g
LOCAL_LDLIBS := -ldl


GEN := $(intermediates)/gl2_dec.cpp $(intermediates)/gl2_dec.h $(intermediates)/gl2_server_context.cpp

$(GEN) : PRIVATE_PATH := $(LOCAL_PATH)
$(GEN) : PRIVATE_CUSTOM_TOOL := $(EMUGEN) -D $(intermediates) -i $(emulatorOpengl)/system/GLESv2_enc gl2
$(GEN) : $(EMUGEN) \
        $(emulatorOpengl)/system/GLESv2_enc/gl2.attrib \
        $(emulatorOpengl)/system/GLESv2_enc/gl2.in \
        $(emulatorOpengl)/system/GLESv2_enc/gl2.types
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(GEN)
include $(BUILD_HOST_SHARED_LIBRARY)

