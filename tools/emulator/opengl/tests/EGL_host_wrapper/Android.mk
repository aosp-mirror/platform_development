ifeq ($(HOST_OS),linux)

LOCAL_PATH := $(call my-dir)

$(call emugl-begin-host-static-library,libEGL_host_wrapper)

LOCAL_SRC_FILES :=  \
        egl.cpp \
        egl_dispatch.cpp

$(call emugl-export,LDLIBS,-ldl -pthread)
$(call emugl-export,C_INCLUDES,$(LOCAL_PATH))

$(call emugl-end-module)

endif # HOST_OS == linux
