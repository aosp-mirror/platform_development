LOCAL_PATH:=$(call my-dir)

$(call emugl-begin-host-executable,emulator_test_renderer)
$(call emugl-import,libOpenglRender event_injector)

LOCAL_SRC_FILES := main.cpp

PREBUILT := $(HOST_PREBUILT_TAG)
LOCAL_SDL_CONFIG ?= prebuilt/$(PREBUILT)/sdl/bin/sdl-config
LOCAL_SDL_CFLAGS := $(shell $(LOCAL_SDL_CONFIG) --cflags)
LOCAL_SDL_LDLIBS := $(filter-out %.a %.lib,$(shell $(LOCAL_SDL_CONFIG) --static-libs))

LOCAL_CFLAGS += $(LOCAL_SDL_CFLAGS) -g -O0
LOCAL_LDLIBS += $(LOCAL_SDL_LDLIBS)

ifeq ($(HOST_OS),windows)
LOCAL_LDLIBS += -lws2_32
endif

LOCAL_STATIC_LIBRARIES += libSDL libSDLmain

$(call emugl-end-module)
