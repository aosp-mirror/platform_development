LOCAL_PATH:=$(call my-dir)

# OS X not supported for now
ifneq ($(HOST_OS),darwin)
# host renderer process ###########################
include $(CLEAR_VARS)

emulatorOpengl := $(LOCAL_PATH)/../..

LOCAL_MODULE := emulator_renderer
LOCAL_MODULE_TAGS := debug

LOCAL_SRC_FILES := \
    main.cpp

LOCAL_CFLAGS += -g -O0

LOCAL_C_INCLUDES := $(emulatorOpengl)/host/include \
                    $(emulatorOpengl)/host/include/libOpenglRender \
                    $(emulatorOpengl)/shared/OpenglOsUtils \
                    $(emulatorOpengl)/shared/OpenglCodecCommon \
                    $(emulatorOpengl)/host/libs/libOpenglRender

LOCAL_SHARED_LIBRARIES := libOpenglRender \
        libGLESv1_dec \
        libGLESv2_dec \
        lib_renderControl_dec

include $(BUILD_HOST_EXECUTABLE)
endif # HOST_OS != darwin
