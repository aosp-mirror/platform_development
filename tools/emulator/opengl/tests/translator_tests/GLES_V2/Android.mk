LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

translator_path := $(LOCAL_PATH)/../../../host/libs/Translator

PREBUILT := $(HOST_PREBUILT_TAG)
SDL_CONFIG ?= prebuilt/$(PREBUILT)/sdl/bin/sdl-config
SDL_CFLAGS := $(shell $(SDL_CONFIG) --cflags)
SDL_LDLIBS := $(filter-out %.a %.lib,$(shell $(SDL_CONFIG) --static-libs))

LOCAL_SRC_FILES:= \
        triangleV2.cpp


LOCAL_SHARED_LIBRARIES := \
    libEGL_translator     \
    libGLES_V2_translator

LOCAL_CFLAGS += $(SDL_CFLAGS) -g -O0
LOCAL_LDLIBS += $(SDL_LDLIBS)


LOCAL_MODULE:= triangleV2
LOCAL_MODULE_TAGS := debug
LOCAL_STATIC_LIBRARIES += libSDL libSDLmain

ifeq ($(HOST_OS),darwin)

LOCAL_LDLIBS += -Wl,-framework,AudioToolbox -Wl,-framework,AudioUnit
LOCAL_C_INCLUDES += \
                 $(translator_path)/include \
                 $(LOCAL_PATH)/../MacCommon
LOCAL_STATIC_LIBRARIES += libMac_view

endif

include $(BUILD_HOST_EXECUTABLE)

