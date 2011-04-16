LOCAL_PATH := $(call my-dir)
emulatorOpengl := $(LOCAL_PATH)/../..

### OpenglSystemCommon ##############################################
include $(CLEAR_VARS)

# add additional depencies to ensure that the generated code that we depend on
# is generated
LOCAL_ADDITIONAL_DEPENDENCIES := \
	$(TARGET_OUT_SHARED_LIBRARIES)/lib_renderControl_enc$(TARGET_SHLIB_SUFFIX) \
	$(TARGET_OUT_SHARED_LIBRARIES)/libGLESv1_enc$(TARGET_SHLIB_SUFFIX)

LOCAL_SRC_FILES := \
        HostConnection.cpp \
        ThreadInfo.cpp

LOCAL_C_INCLUDES += \
        $(emulatorOpengl)/host/include/libOpenglRender \
        $(emulatorOpengl)/shared/OpenglCodecCommon \
        $(emulatorOpengl)/system/GLESv1_enc \
        $(emulatorOpengl)/system/renderControl_enc \
		$(call intermediates-dir-for, SHARED_LIBRARIES, lib_renderControl_enc) \
		$(call intermediates-dir-for, SHARED_LIBRARIES, libGLESv1_enc)

LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libOpenglSystemCommon

include $(BUILD_STATIC_LIBRARY)
