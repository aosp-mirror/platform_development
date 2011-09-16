LOCAL_PATH := $(call my-dir)

$(call emugl-begin-shared-library,lib_renderControl_enc)
$(call emugl-gen-encoder,$(LOCAL_PATH),renderControl)
$(call emugl-export,C_INCLUDES,$(LOCAL_PATH))
$(call emugl-import,libOpenglCodecCommon)
$(call emugl-end-module)
