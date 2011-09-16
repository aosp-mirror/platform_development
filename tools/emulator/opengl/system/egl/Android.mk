ifneq (false,$(BUILD_EMULATOR_OPENGL_DRIVER))

LOCAL_PATH := $(call my-dir)

$(call emugl-begin-shared-library,libEGL_emulation)
$(call emugl-import,libOpenglSystemCommon)
$(call emugl-set-shared-library-subpath,egl)

LOCAL_CFLAGS += -DLOG_TAG=\"EGL_emulation\" -DEGL_EGLEXT_PROTOTYPES -DWITH_GLES2

LOCAL_SRC_FILES := \
    eglDisplay.cpp \
    egl.cpp \
    ClientAPIExts.cpp

LOCAL_SHARED_LIBRARIES += libdl

# Used to access the Bionic private OpenGL TLS slot
LOCAL_C_INCLUDES += bionic/libc/private

$(call emugl-end-module)

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
endif # TARGET_PRODUCT in 'full sdk full_x86 sdk_x86)

endif # BUILD_EMULATOR_OPENGL_DRIVER != false
