LOCAL_PATH := $(call my-dir)

emulatorOpengl := $(LOCAL_PATH)/../..
#### ut_rendercontrol  ####
include $(CLEAR_VARS)

LOCAL_SRC_FILES :=  
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libut_rendercontrol_enc
LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

ut_intermediates := $(local-intermediates-dir)

LOCAL_C_INCLUDES += \
    $(emulatorOpengl)/shared/OpenglCodecCommon \
    $(emulatorOpengl)/host/include/libOpenglRender

LOCAL_STATIC_LIBRARIES := \
        libOpenglCodecCommon
LOCAL_SHARED_LIBRARIES := libcutils

UT_GEN := \
	$(ut_intermediates)/ut_rendercontrol_enc.cpp \
	$(ut_intermediates)/ut_rendercontrol_enc.h

$(UT_GEN) : PRIVATE_PATH = $(LOCAL_PATH)
$(UT_GEN) : PRIVATE_CUSTOM_TOOL := \
        $(EMUGEN) -i $(PRIVATE_PATH) -E $(ut_intermediates) ut_rendercontrol
$(UT_GEN) : $(EMUGEN) \
        $(LOCAL_PATH)/ut_rendercontrol.in \
        $(LOCAL_PATH)/ut_rendercontrol.attrib \
        $(LOCAL_PATH)/ut_rendercontrol.types
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(UT_GEN)
include $(BUILD_SHARED_LIBRARY)

