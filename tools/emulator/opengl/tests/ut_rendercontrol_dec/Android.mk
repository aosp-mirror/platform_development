LOCAL_PATH := $(call my-dir)

$(call emugl-begin-host-shared-library,libut_rendercontrol_dec)
$(call emugl-import, libOpenglCodecCommon)
$(call emugl-gen-decoder,$(EMUGL_PATH)/tests/ut_rendercontrol_enc,ut_rendercontrol)
$(call emugl-export,C_INCLUDES,$(EMUGL_PATH)/tests/ut_rendercontrol_enc)
$(call emugl-end-module)
