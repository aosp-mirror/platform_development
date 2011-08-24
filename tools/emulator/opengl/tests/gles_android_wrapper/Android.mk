LOCAL_PATH := $(call my-dir)

#### libGLESv1_CM_emul.so
$(call emugl-begin-shared-library,libGLESv1_CM_emul)
$(call emugl-import,libGLESv1_enc)
$(call emugl-gen-wrapper,$(EMUGL_PATH)/system/GLESv1_enc,gl)
$(call emugl-set-shared-library-subpath,egl)

LOCAL_SRC_FILES += glesv1_emul_ifc.cpp

$(call emugl-end-module)

emulatorOpengl := $(LOCAL_PATH)/../..
logTag := -DLOG_TAG=\"eglWrapper\"
EMUGEN = $(BUILD_OUT_EXECUTABLES)/emugen
## comment for no debug
#debugFlags = -g -O0

#### libGLESv2_CM_emul.so
$(call emugl-begin-shared-library, libGLESv2_emul)
$(call emugl-import,libGLESv2_enc)
$(call emugl-gen-wrapper,$(EMUGL_PATH)/system/GLESv2_enc,gl2)
LOCAL_SRC_FILES += glesv2_emul_ifc.cpp
$(call emugl-set-shared-library-subpath,egl)
$(call emugl-end-module)

##### libEGL_emul.so ###########

# THE FOLLOWING DOESN'T WORK YET
#
# $(call emugl-begin-shared-library,libEGL_emul)
# $(call emugl-import,libut_rendercontrol_enc libGLESv1_enc libGLESv2_enc libOpenglSystemCommon)
#
# $(call emugl-set-shared-library-subpath,egl)
# LOCAL_CFLAGS += $(logTag)
#
# LOCAL_SRC_FILES :=  \
#         egl.cpp \
#         egl_dispatch.cpp \
#         ServerConnection.cpp \
#         ThreadInfo.cpp
#
# $(call emugl-end-module)

include $(CLEAR_VARS)

LOCAL_SRC_FILES :=  \
        egl.cpp \
        egl_dispatch.cpp \
        ServerConnection.cpp \
        ThreadInfo.cpp

# add additional depencies to ensure that the generated code that we depend on
# is generated
LOCAL_ADDITIONAL_DEPENDENCIES := \
	$(TARGET_OUT_SHARED_LIBRARIES)/libut_rendercontrol_enc$(TARGET_SHLIB_SUFFIX) \
	$(TARGET_OUT_SHARED_LIBRARIES)/libGLESv1_enc$(TARGET_SHLIB_SUFFIX) \
	$(TARGET_OUT_SHARED_LIBRARIES)/libGLESv2_enc$(TARGET_SHLIB_SUFFIX) \
	$(TARGET_OUT_SHARED_LIBRARIES)/egl/libGLESv1_CM_emul$(TARGET_SHLIB_SUFFIX) \
	$(TARGET_OUT_SHARED_LIBRARIES)/egl/libGLESv2_emul$(TARGET_SHLIB_SUFFIX)



LOCAL_C_INCLUDES := $(emulatorOpengl)/shared/OpenglCodecCommon \
        $(emulatorOpengl)/host/include/libOpenglRender \
        $(emulatorOpengl)/system/OpenglSystemCommon \
		$(call intermediates-dir-for, SHARED_LIBRARIES, libut_rendercontrol_enc) \
		$(call intermediates-dir-for, SHARED_LIBRARIES, libGLESv1_enc) \
		$(call intermediates-dir-for, SHARED_LIBRARIES, libGLESv2_enc) \
		$(call intermediates-dir-for, SHARED_LIBRARIES, libGLESv1_CM_emul) \
		$(call intermediates-dir-for, SHARED_LIBRARIES, libGLESv2_emul) \
        $(emulatorOpengl)/system/GLESv1_enc \
        $(emulatorOpengl)/system/GLESv2_enc \
        $(emulatorOpengl)/tests/ut_rendercontrol_enc


LOCAL_CFLAGS := $(logTag)
LOCAL_CFLAGS += $(debugFlags)


LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/egl
LOCAL_MODULE := libEGL_emul
LOCAL_MODULE_TAGS := debug
LOCAL_PRELINK_MODULE := false

#LOCAL_LDLIBS := -lpthread -ldl
LOCAL_SHARED_LIBRARIES := libdl \
	libcutils \
	libGLESv1_enc \
	libGLESv2_enc \
	libOpenglSystemCommon \
	libut_rendercontrol_enc

LOCAL_STATIC_LIBRARIES := libOpenglCodecCommon

include $(BUILD_SHARED_LIBRARY)

#### egl.cfg ####

# Ensure that this file is only copied to emulator-specific builds.
# Other builds are device-specific and will provide their own
# version of this file to point to the appropriate HW EGL libraries.
#
ifneq (,$(filter full full_x86 sdk sdk_x86,$(TARGET_PRODUCT)))
ifeq (,$(BUILD_EMULATOR_OPENGL_DRIVER))
include $(CLEAR_VARS)

LOCAL_MODULE := egl.cfg
LOCAL_SRC_FILES := $(LOCAL_MODULE)

LOCAL_MODULE_PATH := $(TARGET_OUT)/lib/egl
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE_CLASS := ETC

include $(BUILD_PREBUILT)
endif # building 'real' driver BUILD_EMULATOR_OPENGL_DRIVER
endif # TARGET_PRODUCT in 'full sdk full_x86 sdk_x86'

#### gles_emul.cfg ####
include $(CLEAR_VARS)

LOCAL_MODULE := gles_emul.cfg
LOCAL_SRC_FILES := $(LOCAL_MODULE)

LOCAL_MODULE_PATH := $(TARGET_OUT)/etc
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE_CLASS := ETC

include $(BUILD_PREBUILT)




