LOCAL_PATH := $(call my-dir)

$(call emugl-begin-host-static-library,event_injector)

LOCAL_SRC_FILES := \
    EventInjector.cpp \
    sockets.c \
    emulator-console.c \
    iolooper-select.c

$(call emugl-export,C_INCLUDES,$(LOCAL_PATH))

$(call emugl-end-module)
