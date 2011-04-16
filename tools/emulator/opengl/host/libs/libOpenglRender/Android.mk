LOCAL_PATH := $(call my-dir)

### libOpenglRender #################################################
include $(CLEAR_VARS)

emulatorOpengl := $(LOCAL_PATH)/../../..

LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libOpenglRender
LOCAL_ADDITIONAL_DEPENDENCIES := \
	$(HOST_OUT_SHARED_LIBRARIES)/lib_renderControl_dec$(HOST_SHLIB_SUFFIX) \
	$(HOST_OUT_SHARED_LIBRARIES)/libGLESv1_dec$(HOST_SHLIB_SUFFIX)

LOCAL_SRC_FILES := \
    render_api.cpp \
    ColorBuffer.cpp \
    EGLDispatch.cpp \
    FBConfig.cpp \
    FrameBuffer.cpp \
    GLDispatch.cpp \
    GL2Dispatch.cpp \
    RenderContext.cpp \
    WindowSurface.cpp \
    RenderControl.cpp \
    ThreadInfo.cpp \
    RenderThread.cpp \
    ReadBuffer.cpp \
    RenderServer.cpp

LOCAL_C_INCLUDES += \
    $(emulatorOpengl)/host/include \
    $(emulatorOpengl)/shared/OpenglCodecCommon \
    $(emulatorOpengl)/shared/OpenglOsUtils \
    $(emulatorOpengl)/host/include/libOpenglRender \
    $(emulatorOpengl)/host/libs/GLESv1_dec \
    $(emulatorOpengl)/system/GLESv1_enc \
    $(emulatorOpengl)/system/renderControl_enc \
	$(call intermediates-dir-for, SHARED_LIBRARIES, libGLESv1_dec, HOST) \
	$(call intermediates-dir-for, SHARED_LIBRARIES, lib_renderControl_dec, HOST)

LOCAL_STATIC_LIBRARIES := \
        libOpenglCodecCommon \
        libOpenglOsUtils \
        libcutils \
        libutils \
        liblog

LOCAL_SHARED_LIBRARIES := \
        libGLESv1_dec \
        lib_renderControl_dec

ifeq ($(HOST_OS),windows)
    LOCAL_LDLIBS := -lws2_32
endif

ifeq ($(HOST_OS),linux)
    LOCAL_LDLIBS := -ldl -lpthread -lrt
endif

# XXX - uncomment for debug
#LOCAL_CFLAGS := -O0 -g

include $(BUILD_HOST_SHARED_LIBRARY)
