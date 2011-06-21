ifneq (,$(BUILD_EMULATOR_OPENGL_DRIVER))

LOCAL_PATH := $(call my-dir)
emulatorOpengl := $(LOCAL_PATH)/../..

### EGL implementation ###########################################
include $(CLEAR_VARS)

# add additional depencies to ensure that the generated code that we depend on
# is generated
LOCAL_ADDITIONAL_DEPENDENCIES := \
	$(TARGET_OUT_SHARED_LIBRARIES)/lib_renderControl_enc$(TARGET_SHLIB_SUFFIX) \
	$(TARGET_OUT_SHARED_LIBRARIES)/libGLESv1_enc$(TARGET_SHLIB_SUFFIX)

LOCAL_SRC_FILES := \
        eglDisplay.cpp \
        egl.cpp


LOCAL_PRELINK_MODULE := false
LOCAL_CFLAGS += -DLOG_TAG=\"EGL_emulation\" -DEGL_EGLEXT_PROTOTYPES
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
LOCAL_MODULE := libEGL_emulation
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_PRELINK_MODULE := false

LOCAL_STATIC_LIBRARIES := \
    libOpenglCodecCommon  \
	libqemu

LOCAL_SHARED_LIBRARIES := \
    libcutils \
	libutils \
	libdl	\
    libGLESv1_enc \
    libOpenglSystemCommon \
    lib_renderControl_enc


include $(BUILD_SHARED_LIBRARY)

#### egl.cfg ####

# Ensure that this file is only copied to emulator-specific builds.
# Other builds are device-specific and will provide their own
# version of this file to point to the appropriate HW EGL libraries.
#
ifneq (,$(filter full full_x86 sdk sdk_x86,$(TARGET_PRODUCT)))
include $(CLEAR_VARS)

LOCAL_MODULE := egl.cfg
LOCAL_SRC_FILES := $(LOCAL_MODULE)

LOCAL_MODULE_PATH := $(TARGET_OUT)/lib/egl
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE_CLASS := ETC

include $(BUILD_PREBUILT)
endif # TARGET_PRODUCT in 'full sdk full_x86 sdk_x86'

endif # of ifneq (,$(BUILD_EMULATOR_OPENGL_DRIVER))
