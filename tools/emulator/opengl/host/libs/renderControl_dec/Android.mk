
LOCAL_PATH := $(call my-dir)

### renderControl Decoder ###########################################
include $(CLEAR_VARS)

emulatorOpengl := $(LOCAL_PATH)/../../..
EMUGEN := $(BUILD_OUT_EXECUTABLES)/emugen

LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := lib_renderControl_dec
LOCAL_SRC_FILES := 
#LOCAL_CFLAGS += -DDEBUG_PRINTOUT -O0 -g
intermediates := $(local-intermediates-dir)

LOCAL_STATIC_LIBRARIES := \
	libOpenglCodecCommon \
    liblog
LOCAL_C_INCLUDES += $(emulatorOpengl)/shared/OpenglCodecCommon \
                    $(emulatorOpengl)/host/include/libOpenglRender \
                    $(emulatorOpengl)/system/renderControl_enc

#we use only *_dec.h as a sentinel for the other generated headers
GEN := $(intermediates)/renderControl_dec.cpp $(intermediates)/renderControl_dec.h
$(GEN): PRIVATE_PATH := $(LOCAL_PATH)
$(GEN): PRIVATE_CUSTOM_TOOL := $(EMUGEN) -D $(intermediates) -i $(emulatorOpengl)/system/renderControl_enc renderControl
$(GEN): $(EMUGEN) \
	$(emulatorOpengl)/system/renderControl_enc/renderControl.attrib \
	$(emulatorOpengl)/system/renderControl_enc/renderControl.in \
	$(emulatorOpengl)/system/renderControl_enc/renderControl.types
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(GEN)
include $(BUILD_HOST_SHARED_LIBRARY)
