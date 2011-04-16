LOCAL_PATH:=$(call my-dir)

# test opengl renderer driver ###########################
include $(CLEAR_VARS)

emulatorOpengl := $(LOCAL_PATH)/../..

LOCAL_MODULE := emulator_test_renderer
LOCAL_MODULE_TAGS := debug

LOCAL_SRC_FILES := \
    main.cpp

PREBUILT := $(HOST_PREBUILT_TAG)
SDL_CONFIG ?= prebuilt/$(PREBUILT)/sdl/bin/sdl-config
SDL_CFLAGS := $(shell $(SDL_CONFIG) --cflags)
SDL_LDLIBS := $(filter-out %.a %.lib,$(shell $(SDL_CONFIG) --static-libs))

LOCAL_CFLAGS += $(SDL_CFLAGS) -g -O0
LOCAL_LDLIBS += $(SDL_LDLIBS)

LOCAL_C_INCLUDES := $(emulatorOpengl)/host/include \
                    $(emulatorOpengl)/host/include/libOpenglRender \
                    $(emulatorOpengl)/shared/OpenglCodecCommon \
                    $(emulatorOpengl)/host/libs/libOpenglRender

LOCAL_SHARED_LIBRARIES := libOpenglRender \
        libGLESv1_dec \
        lib_renderControl_dec

LOCAL_STATIC_LIBRARIES += libSDL libSDLmain

include $(BUILD_HOST_EXECUTABLE)
