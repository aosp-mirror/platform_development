LOCAL_PATH := $(call my-dir)

host_OS_SRCS :=
host_common_LDLIBS :=

ifeq ($(HOST_OS),linux)
    host_OS_SRCS = EglX11Api.cpp
    host_common_LDLIBS += -lX11 -lGL -ldl -lpthread
endif

ifeq ($(HOST_OS),darwin)
    host_OS_SRCS = EglMacApi.cpp \
                   MacNative.m   \
                   MacPixelFormatsAttribs.m

    host_common_LDLIBS += -Wl,-framework,AppKit
endif

ifeq ($(HOST_OS),windows)
    host_OS_SRCS = EglWindowsApi.cpp
    host_common_LDLIBS += -lopengl32 -lgdi32
endif

host_common_SRC_FILES :=      \
     $(host_OS_SRCS)          \
     ThreadInfo.cpp           \
     EglImp.cpp               \
     EglConfig.cpp            \
     EglContext.cpp           \
     EglGlobalInfo.cpp        \
     EglValidate.cpp          \
     EglSurface.cpp           \
     EglWindowSurface.cpp     \
     EglPbufferSurface.cpp    \
     EglPixmapSurface.cpp     \
     EglThreadInfo.cpp        \
     EglDisplay.cpp           \
     ClientAPIExts.cpp

### EGL host implementation ########################
$(call emugl-begin-host-shared-library,libEGL_translator)
$(call emugl-import,libGLcommon)

LOCAL_LDLIBS += $(host_common_LDLIBS)
LOCAL_SRC_FILES := $(host_common_SRC_FILES)

$(call emugl-end-module)

### EGL host implementation, 64-bit ########################
$(call emugl-begin-host-shared-library,lib64EGL_translator)
$(call emugl-import,lib64GLcommon)

LOCAL_LDLIBS += $(host_common_LDLIBS) -m64
LOCAL_SRC_FILES := $(host_common_SRC_FILES)

$(call emugl-end-module)

