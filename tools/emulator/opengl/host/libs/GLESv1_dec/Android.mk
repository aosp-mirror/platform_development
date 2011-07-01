LOCAL_PATH := $(call my-dir)

$(call emugl-begin-host-shared-library,libGLESv1_dec)

$(call emugl-import, libOpenglCodecCommon libOpenglOsUtils)
$(call emugl-export,C_INCLUDES,$(LOCAL_PATH))

$(call emugl-gen-decoder,$(EMUGL_PATH)/system/GLESv1_enc,gl)

LOCAL_SRC_FILES := GLDecoder.cpp

# for gl_types.h !
$(call emugl-export,C_INCLUDES,$(EMUGL_PATH)/system/GLESv1_enc)

$(call emugl-end-module)
