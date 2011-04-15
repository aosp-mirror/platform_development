LOCAL_PATH := $(call my-dir)

##### libGLES_emul.so ###########
include $(CLEAR_VARS)

LOCAL_SRC_FILES :=  \
        egl.cpp \
        egl_dispatch.cpp \
        gles.cpp \
        gles_dispatch.cpp \
        ServerConnection.cpp \
        ThreadInfo.cpp

# add additional depencies to ensure that the generated code that we depend on
# is generated
LOCAL_ADDITIONAL_DEPENDENCIES := \
	$(TARGET_OUT_SHARED_LIBRARIES)/libut_rendercontrol_enc$(TARGET_SHLIB_SUFFIX) \
	$(TARGET_OUT_SHARED_LIBRARIES)/libGLESv1_enc$(TARGET_SHLIB_SUFFIX)


emulatorOpengl := $(LOCAL_PATH)/../..

LOCAL_C_INCLUDES := $(emulatorOpengl)/shared/OpenglCodecCommon \
        $(emulatorOpengl)/host/include/libOpenglRender \
		$(call intermediates-dir-for, SHARED_LIBRARIES, libut_rendercontrol_enc) \
		$(call intermediates-dir-for, SHARED_LIBRARIES, libGLESv1_enc) \
        $(emulatorOpengl)/system/GLESv1_enc \
        $(emulatorOpengl)/tests/ut_rendercontrol_enc 


LOCAL_CFLAGS := -DLOG_TAG=\"eglWrapper\"
LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/egl
LOCAL_MODULE := libGLES_emul
LOCAL_MODULE_TAGS := debug
LOCAL_PRELINK_MODULE := false

#LOCAL_LDLIBS := -lpthread -ldl
LOCAL_SHARED_LIBRARIES := libdl libcutils libGLESv1_enc libut_rendercontrol_enc
LOCAL_STATIC_LIBRARIES := libOpenglCodecCommon

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

#### gles_emul.cfg ####
include $(CLEAR_VARS)

LOCAL_MODULE := gles_emul.cfg
LOCAL_SRC_FILES := $(LOCAL_MODULE)

LOCAL_MODULE_PATH := $(TARGET_OUT)/etc
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE_CLASS := ETC

include $(BUILD_PREBUILT)




