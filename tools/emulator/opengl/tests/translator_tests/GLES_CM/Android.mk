ifneq ($(HOST_OS),darwin)
LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

#
# This is built on linux host only !!!
#
PREBUILT := $(HOST_PREBUILT_TAG)
SDL_CONFIG ?= prebuilt/$(PREBUILT)/sdl/bin/sdl-config
SDL_CFLAGS := $(shell $(SDL_CONFIG) --cflags)
SDL_LDLIBS := $(filter-out %.a %.lib,$(shell $(SDL_CONFIG) --static-libs))

LOCAL_SRC_FILES:= \
        triangleCM.cpp

LOCAL_SHARED_LIBRARIES := \
    libGLcommon           \
    libEGL_translator     \
    libGLES_CM_translator

LOCAL_CFLAGS += $(SDL_CFLAGS) -g -O0
LOCAL_LDLIBS += $(SDL_LDLIBS)

LOCAL_MODULE:= triangleCM
LOCAL_MODULE_TAGS := debug
LOCAL_STATIC_LIBRARIES += libSDL libSDLmain

include $(BUILD_HOST_EXECUTABLE)
endif
