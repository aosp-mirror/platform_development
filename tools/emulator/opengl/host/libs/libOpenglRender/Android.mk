LOCAL_PATH := $(call my-dir)

### libOpenglRender #################################################
$(call emugl-begin-host-shared-library,libOpenglRender)

$(call emugl-import,libGLESv1_dec libGLESv2_dec lib_renderControl_dec libOpenglCodecCommon libOpenglOsUtils)

OS_SRCS:=

ifeq ($(HOST_OS),linux)
    OS_SRCS = NativeLinuxSubWindow.cpp
    LOCAL_LDLIBS += -lX11
endif

ifeq ($(HOST_OS),darwin)
    OS_SRCS = NativeMacSubWindow.m
    LOCAL_LDLIBS += -Wl,-framework,AppKit
endif

ifeq ($(HOST_OS),windows)
    OS_SRCS = NativeWindowsSubWindow.cpp
endif

LOCAL_SRC_FILES := \
    $(OS_SRCS)      \
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
$(call emugl-export,C_INCLUDES,$(LOCAL_PATH))

# use Translator's egl/gles headers
LOCAL_C_INCLUDES += $(EMUGL_PATH)/host/libs/Translator/include

LOCAL_STATIC_LIBRARIES += libutils liblog

#For gl debbuging
#$(call emugl-export,CFLAGS,-DCHECK_GL_ERROR)

$(call emugl-end-module)
