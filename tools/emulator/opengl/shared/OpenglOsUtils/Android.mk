LOCAL_PATH := $(call my-dir)
emulatorOpengl := $(LOCAL_PATH)/../..

### OpenglOsUtils ##############################################

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
        osProcessUnix.cpp \
        osThreadUnix.cpp \
        osDynLibrary.cpp

LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libOpenglOsUtils

include $(BUILD_STATIC_LIBRARY)

### OpenglOsUtils  host ##############################################
include $(CLEAR_VARS)

ifneq ($(HOST_OS),windows)

    LOCAL_SRC_FILES := \
        osProcessUnix.cpp \
        osThreadUnix.cpp \
        osDynLibrary.cpp

    LOCAL_LDLIBS := -ldl

else # !linux

    LOCAL_SRC_FILES := \
        osProcessWin.cpp \
        osThreadWin.cpp \
        osDynLibrary.cpp

endif # windows

LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libOpenglOsUtils

include $(BUILD_HOST_STATIC_LIBRARY)
