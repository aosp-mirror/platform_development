LOCAL_PATH := $(call my-dir)

emulatorOpengl := $(LOCAL_PATH)/../..
EMUGEN := $(HOST_OUT_EXECUTABLES)/emugen
#### renderControl  ####
include $(CLEAR_VARS)

LOCAL_SRC_FILES :=  
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := lib_renderControl_enc
LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

rc_intermediates := $(local-intermediates-dir)

LOCAL_C_INCLUDES += $(emulatorOpengl)/shared/OpenglCodecCommon \
                    $(emulatorOpengl)/host/include/libOpenglRender

LOCAL_STATIC_LIBRARIES := \
        libOpenglCodecCommon
LOCAL_SHARED_LIBRARIES := libcutils

LOCAL_CFLAGS += -DDEBUG_PRINTOUT -O0 -g

RC_GEN := \
	$(rc_intermediates)/renderControl_enc.cpp \
	$(rc_intermediates)/renderControl_enc.h

$(RC_GEN) : PRIVATE_PATH = $(LOCAL_PATH)
$(RC_GEN) : PRIVATE_CUSTOM_TOOL := \
        $(EMUGEN) -i $(PRIVATE_PATH) -E $(rc_intermediates) renderControl
$(RC_GEN) : $(EMUGEN) \
        $(LOCAL_PATH)/renderControl.in \
        $(LOCAL_PATH)/renderControl.attrib \
        $(LOCAL_PATH)/renderControl.types
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(RC_GEN)
include $(BUILD_SHARED_LIBRARY)

