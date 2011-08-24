LOCAL_PATH := $(call my-dir)

$(call emugl-begin-host-static-library,libGLESv2_dec)
$(call emugl-import, libOpenglCodecCommon libOpenglOsUtils)
$(call emugl-gen-decoder,$(EMUGL_PATH)/system/GLESv2_enc,gl2)

# For gl2_types.h !
$(call emugl-export,C_INCLUDES,$(EMUGL_PATH)/system/GLESv2_enc)
$(call emugl-export,C_INCLUDES,$(LOCAL_PATH))

#For gl debbuging
#$(call emugl-export,CFLAGS,-DCHECK_GL_ERROR)
#$(call emugl-export,CFLAGS,-DDEBUG_PRINTOUT)

LOCAL_SRC_FILES := GL2Decoder.cpp

$(call emugl-end-module)
