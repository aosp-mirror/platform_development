LOCAL_PATH := $(call my-dir)

ifneq ($(HOST_OS),darwin)

### libOpenglRender #################################################
$(call emugl-begin-host-shared-library,libOpenglRender)

$(call emugl-import,libGLESv1_dec libGLESv2_dec lib_renderControl_dec libOpenglCodecCommon libOpenglOsUtils)

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

$(call emugl-export,C_INCLUDES,$(EMUGL_PATH)/host/include)

# use Translator's egl/gles headers
LOCAL_C_INCLUDES += $(EMUGL_PATH)/host/libs/Translator/include

LOCAL_STATIC_LIBRARIES += libutils liblog

$(call emugl-end-module)

endif # HOST_OS != darwin
