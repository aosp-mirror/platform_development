LOCAL_PATH := $(call my-dir)
emulatorOpengl := $(LOCAL_PATH)/../..

### GLESv1 implementation ###########################################
include $(CLEAR_VARS)

# add additional depencies to ensure that the generated code that we depend on
# is generated
LOCAL_ADDITIONAL_DEPENDENCIES := \
	$(TARGET_OUT_SHARED_LIBRARIES)/lib_renderControl_enc$(TARGET_SHLIB_SUFFIX) \
	$(TARGET_OUT_SHARED_LIBRARIES)/libGLESv1_enc$(TARGET_SHLIB_SUFFIX)

LOCAL_SRC_FILES := \
        gl.cpp


LOCAL_PRELINK_MODULE := false
LOCAL_CFLAGS += -DLOG_TAG=\"GLES_emulation\" -DGL_GLEXT_PROTOTYPES
LOCAL_C_INCLUDES +=  \
        $(emulatorOpengl)/host/include/libOpenglRender \
        $(emulatorOpengl)/shared/OpenglCodecCommon \
        $(emulatorOpengl)/system/OpenglSystemCommon \
        $(emulatorOpengl)/system/GLESv1_enc \
        $(emulatorOpengl)/system/renderControl_enc \
		$(call intermediates-dir-for, SHARED_LIBRARIES, lib_renderControl_enc) \
		$(call intermediates-dir-for, SHARED_LIBRARIES, libGLESv1_enc)

LOCAL_MODULE_TAGS := debug
LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/egl
LOCAL_MODULE := libGLESv1_CM_emulation
LOCAL_MODULE_CLASS := SHARED_LIBRARIES


LOCAL_STATIC_LIBRARIES := \
    libOpenglCodecCommon  \
	libqemu

LOCAL_SHARED_LIBRARIES := \
    libcutils \
	libutils \
	libdl \
    libOpenglSystemCommon \
	libGLESv1_enc	\
	lib_renderControl_enc


include $(BUILD_SHARED_LIBRARY)
