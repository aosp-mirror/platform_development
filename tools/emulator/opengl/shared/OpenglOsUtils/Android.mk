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


### Host library ##############################################
$(call emugl-begin-host-static-library,libOpenglOsUtils)

    $(call emugl-export,C_INCLUDES,$(LOCAL_PATH))

    LOCAL_SRC_FILES := osDynLibrary.cpp

    ifeq ($(HOST_OS),windows)
        LOCAL_SRC_FILES += \
            osProcessWin.cpp \
            osThreadWin.cpp

        $(call emugl-export,LDLIBS,-lws2_32 -lpsapi)
    else
        LOCAL_SRC_FILES += \
            osProcessUnix.cpp \
            osThreadUnix.cpp

        $(call emugl-export,LDLIBS,-ldl)
    endif

    ifeq ($(HOST_OS),linux)
        $(call emugl-export,LDLIBS,-lpthread -lrt)
    endif

$(call emugl-end-module)
