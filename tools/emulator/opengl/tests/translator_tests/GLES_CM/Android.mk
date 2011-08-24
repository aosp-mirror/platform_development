LOCAL_PATH:= $(call my-dir)

$(call emugl-begin-host-executable,triangleCM)
$(call emugl-import,libEGL_translator libGLES_CM_translator)

PREBUILT := $(HOST_PREBUILT_TAG)
SDL_CONFIG ?= prebuilt/$(PREBUILT)/sdl/bin/sdl-config
SDL_CFLAGS := $(shell $(SDL_CONFIG) --cflags)
SDL_LDLIBS := $(filter-out %.a %.lib,$(shell $(SDL_CONFIG) --static-libs))

LOCAL_SRC_FILES:= \
        triangleCM.cpp

LOCAL_CFLAGS += $(SDL_CFLAGS) -g -O0
LOCAL_LDLIBS += $(SDL_LDLIBS)

LOCAL_STATIC_LIBRARIES += libSDL libSDLmain

ifeq ($(HOST_OS),darwin)
$(call emugl-import,libMac_view)
endif

$(call emugl-end-module)
