LOCAL_PATH := $(call my-dir)

$(call emugl-begin-host-static-library,libGLESv1_dec)

$(call emugl-import, libOpenglCodecCommon libOpenglOsUtils)
$(call emugl-export,C_INCLUDES,$(LOCAL_PATH))

$(call emugl-gen-decoder,$(EMUGL_PATH)/system/GLESv1_enc,gl)

LOCAL_SRC_FILES := GLDecoder.cpp

# for gl_types.h !
$(call emugl-export,C_INCLUDES,$(EMUGL_PATH)/system/GLESv1_enc)

#For gl debbuging
#$(call emugl-export,CFLAGS,-DCHECK_GL_ERROR)
#$(call emugl-export,CFLAGS,-DDEBUG_PRINTOUT)

$(call emugl-end-module)
