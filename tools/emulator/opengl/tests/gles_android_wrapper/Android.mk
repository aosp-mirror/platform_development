LOCAL_PATH := $(call my-dir)

emulatorOpengl := $(LOCAL_PATH)/../..
logTag := -DLOG_TAG=\"eglWrapper\"
EMUGEN = $(BUILD_OUT_EXECUTABLES)/emugen
## comment for no debug
#debugFlags = -g -O0

#### libGLESv1_CM_emul.so
include $(CLEAR_VARS)



LOCAL_MODULE := libGLESv1_CM_emul
LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/egl
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_SRC_FILES := glesv1_emul_ifc.cpp

LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS := debug
LOCAL_SHARED_LIBRARIES := libdl libcutils
LOCAL_CFLAGS += $(debugFlags)

LOCAL_C_INCLUDES += \
	$(emulatorOpengl)/system/GLESv1_enc \
	$(emulatorOpengl)/shared/OpenglCodecCommon

glesv1_emul_intermediates := $(local-intermediates-dir)

GEN_GLESv1_emul := \
	$(glesv1_emul_intermediates)/gl_wrapper_entry.cpp \
	$(glesv1_emul_intermediates)/gl_wrapper_context.cpp
$(GEN_GLESv1_emul) : PRIVATE_PATH := $(LOCAL_PATH)
$(GEN_GLESv1_emul) : PRIVATE_CUSTOM_TOOL := \
	$(EMUGEN) -W $(glesv1_emul_intermediates) -i $(emulatorOpengl)/system/GLESv1_enc gl
$(GEN_GLESv1_emul) : $(EMUGEN) \
	$(emulatorOpengl)/system/GLESv1_enc/gl.in \
	$(emulatorOpengl)/system/GLESv1_enc/gl.attrib \
	$(emulatorOpengl)/system/GLESv1_enc/gl.types
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(GEN_GLESv1_emul)

include $(BUILD_SHARED_LIBRARY)

#### libGLESv2_CM_emul.so
include $(CLEAR_VARS)

LOCAL_MODULE := libGLESv2_emul
LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/egl
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_SRC_FILES := glesv2_emul_ifc.cpp

LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS := debug
LOCAL_SHARED_LIBRARIES := libdl libcutils
LOCAL_CFLAGS += $(debugFlags)

LOCAL_C_INCLUDES += \
	$(emulatorOpengl)/system/GLESv2_enc \
	$(emulatorOpengl)/shared/OpenglCodecCommon

glesv2_emul_intermediates := $(local-intermediates-dir)

GEN_GLESv2_emul := \
	$(glesv2_emul_intermediates)/gl2_wrapper_entry.cpp \
	$(glesv2_emul_intermediates)/gl2_wrapper_context.cpp

$(GEN_GLESv2_emul) : PRIVATE_PATH := $(LOCAL_PATH)
$(GEN_GLESv2_emul) : PRIVATE_CUSTOM_TOOL := \
	$(EMUGEN) -W $(glesv2_emul_intermediates) -i $(emulatorOpengl)/system/GLESv2_enc gl2
$(GEN_GLESv2_emul) : $(EMUGEN) \
	$(emulatorOpengl)/system/GLESv2_enc/gl2.in \
	$(emulatorOpengl)/system/GLESv2_enc/gl2.attrib \
	$(emulatorOpengl)/system/GLESv2_enc/gl2.types
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(GEN_GLESv2_emul)

include $(BUILD_SHARED_LIBRARY)


##### libEGL_emul.so ###########
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
	libut_rendercontrol_enc \
    libOpenglSystemCommon

LOCAL_STATIC_LIBRARIES := libOpenglCodecCommon libqemu

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




