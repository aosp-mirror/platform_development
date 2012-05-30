# This build script corresponds to a small library containing
# OS-specific support functions for:
#   - thread-local storage
#   - dynamic library loading
#   - child process creation and wait  (probably not needed in guest)
#
LOCAL_PATH := $(call my-dir)

### Guest library ##############################################
$(call emugl-begin-static-library,libOpenglOsUtils)

    $(call emugl-export,C_INCLUDES,$(LOCAL_PATH))
    $(call emugl-export,LDLIBS,-ldl)

    LOCAL_SRC_FILES := \
        osProcessUnix.cpp \
        osThreadUnix.cpp \
        osDynLibrary.cpp

$(call emugl-end-module)
