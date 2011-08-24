LOCAL_PATH := $(call my-dir)

$(call emugl-begin-host-static-library,lib_renderControl_dec)
$(call emugl-import,libOpenglCodecCommon)
$(call emugl-gen-decoder,$(EMUGL_PATH)/system/renderControl_enc,renderControl)
# For renderControl_types.h
$(call emugl-export,C_INCLUDES,$(EMUGL_PATH)/system/renderControl_enc)
$(call emugl-end-module)
