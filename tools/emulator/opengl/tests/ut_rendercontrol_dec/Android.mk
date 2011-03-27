
LOCAL_PATH := $(call my-dir)

### ut_rendercontrol Decoder ###########################################
include $(CLEAR_VARS)

emulatorOpengl := $(LOCAL_PATH)/../..
EMUGEN := $(HOST_OUT_EXECUTABLES)/emugen

LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libut_rendercontrol_dec
LOCAL_SRC_FILES := 
#LOCAL_CFLAGS += -DDEBUG_PRINTOUT -O0 -g
intermediates := $(local-intermediates-dir)

LOCAL_STATIC_LIBRARIES := \
	libOpenglCodecCommon \
    liblog
LOCAL_C_INCLUDES += $(emulatorOpengl)/system/OpenglCodecCommon $(emulatorOpengl)/tests/ut_rendercontrol_enc

#we use only *_dec.h as a sentinel for the other generated headers
GEN := $(intermediates)/ut_rendercontrol_dec.cpp $(intermediates)/ut_rendercontrol_dec.h
$(GEN): PRIVATE_PATH := $(LOCAL_PATH)
$(GEN): PRIVATE_CUSTOM_TOOL := $(EMUGEN) -D $(intermediates) -i $(emulatorOpengl)/tests/ut_rendercontrol_enc ut_rendercontrol
$(GEN): $(EMUGEN) \
	$(emulatorOpengl)/tests/ut_rendercontrol_enc/ut_rendercontrol.attrib \
	$(emulatorOpengl)/tests/ut_rendercontrol_enc/ut_rendercontrol.in \
	$(emulatorOpengl)/tests/ut_rendercontrol_enc/ut_rendercontrol.types
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(GEN)
include $(BUILD_HOST_SHARED_LIBRARY)
