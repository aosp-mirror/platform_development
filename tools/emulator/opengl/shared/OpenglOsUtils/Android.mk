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

host_common_SRC_FILES := osDynLibrary.cpp
host_common_LDLIBS :=

ifeq ($(HOST_OS),windows)
    host_common_SRC_FILES += \
        osProcessWin.cpp \
        osThreadWin.cpp
    host_common_LDLIBS += -lws2_32 -lpsapi
else
    host_common_SRC_FILES += \
        osProcessUnix.cpp \
        osThreadUnix.cpp
    host_common_LDLIBS += -ldl
endif

ifeq ($(HOST_OS),linux)
    host_common_LDLIBS += -lpthread -lrt
endif

### 32-bit host library ####
$(call emugl-begin-host-static-library,libOpenglOsUtils)
    $(call emugl-export,C_INCLUDES,$(LOCAL_PATH))
    LOCAL_SRC_FILES = $(host_common_SRC_FILES)
    $(call emugl-export,LDLIBS,$(host_common_LDLIBS))
$(call emugl-end-module)

### 64-bit host library ####
$(call emugl-begin-host-static-library,lib64OpenglOsUtils)
    $(call emugl-export,C_INCLUDES,$(LOCAL_PATH))
    LOCAL_SRC_FILES = $(host_common_SRC_FILES)
    $(call emugl-export,LDLIBS,$(host_common_LDLIBS))
    $(call emugl-export,CFLAGS,-m64)
$(call emugl-end-module)
