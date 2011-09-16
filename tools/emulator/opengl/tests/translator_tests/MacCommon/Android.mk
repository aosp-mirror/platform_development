LOCAL_PATH := $(call my-dir)

ifeq ($(HOST_OS),darwin)
$(call emugl-begin-host-static-library,libMac_view)

LIBMACVIEW_FRAMEWORKS := AppKit AudioToolbox AudioUnit
LIBMACVIEW_PREFIX := -Wl,-framework,

$(call emugl-export,LDLIBS,$(foreach _framework,$(LIBMACVIEW_FRAMEWORKS),$(LIBMACVIEW_PREFIX)$(_framework)))
LOCAL_SRC_FILES := setup_gl.m
LOCAL_CFLAGS += -g -O0
$(call emugl-end-module)
endif # HOST_OS == darwin
