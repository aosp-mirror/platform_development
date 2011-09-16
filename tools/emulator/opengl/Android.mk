# This is the top-level build file for the Android HW OpenGL ES emulation
# in Android.
#
# You must define BUILD_EMULATOR_OPENGL to 'true' in your environment to
# build the following files.
#
# Also define BUILD_EMULATOR_OPENGL_DRIVER to 'true' to build the gralloc
# stuff as well.
#
ifeq (true,$(BUILD_EMULATOR_OPENGL))

# Top-level for all modules
EMUGL_PATH := $(call my-dir)

# Directory containing common headers used by several modules
# This is always set to a module's LOCAL_C_INCLUDES
# See the definition of emugl-begin-module in common.mk
#
EMUGL_COMMON_INCLUDES := $(EMUGL_PATH)/host/include/libOpenglRender

# common cflags used by several modules
# This is always set to a module's LOCAL_CFLAGS
# See the definition of emugl-begin-module in common.mk
#
EMUGL_COMMON_CFLAGS := -DWITH_GLES2

# Uncomment the following line if you want to enable debug traces
# in the GLES emulation libraries.
# EMUGL_COMMON_CFLAGS += -DEMUGL_DEBUG=1

# Include common definitions used by all the modules included later
# in this build file. This contains the definition of all useful
# emugl-xxxx functions.
#
include $(EMUGL_PATH)/common.mk

# IMPORTANT: ORDER IS CRUCIAL HERE
#
# For the import/export feature to work properly, you must include
# modules below in correct order. That is, if module B depends on
# module A, then it must be included after module A below.
#
# This ensures that anything exported by module A will be correctly
# be imported by module B when it is declared.
#
# Note that the build system will complain if you try to import a
# module that hasn't been declared yet anyway.
#

# First, build the emugen host source-generation tool
#
# It will be used by other modules to generate wire protocol encode/decoder
# source files (see all emugl-gen-decoder/encoder in common.mk)
#
include $(EMUGL_PATH)/host/tools/emugen/Android.mk
include $(EMUGL_PATH)/shared/OpenglOsUtils/Android.mk
include $(EMUGL_PATH)/shared/OpenglCodecCommon/Android.mk

# System static libraries
include $(EMUGL_PATH)/system/GLESv1_enc/Android.mk
include $(EMUGL_PATH)/system/GLESv2_enc/Android.mk
include $(EMUGL_PATH)/system/renderControl_enc/Android.mk
include $(EMUGL_PATH)/system/OpenglSystemCommon/Android.mk

# System shared libraries
include $(EMUGL_PATH)/system/GLESv1/Android.mk
include $(EMUGL_PATH)/system/GLESv2/Android.mk

include $(EMUGL_PATH)/system/gralloc/Android.mk
include $(EMUGL_PATH)/system/egl/Android.mk

# Host static libraries
include $(EMUGL_PATH)/host/libs/GLESv1_dec/Android.mk
include $(EMUGL_PATH)/host/libs/GLESv2_dec/Android.mk
include $(EMUGL_PATH)/host/libs/renderControl_dec/Android.mk
include $(EMUGL_PATH)/tests/ut_rendercontrol_dec/Android.mk
include $(EMUGL_PATH)/host/libs/Translator/GLcommon/Android.mk
include $(EMUGL_PATH)/host/libs/Translator/GLES_CM/Android.mk
include $(EMUGL_PATH)/host/libs/Translator/GLES_V2/Android.mk
include $(EMUGL_PATH)/host/libs/Translator/EGL/Android.mk

# Host shared libraries
include $(EMUGL_PATH)/host/libs/libOpenglRender/Android.mk

# Host executables
include $(EMUGL_PATH)/host/renderer/Android.mk

# Host unit-test for the renderer.

include $(EMUGL_PATH)/tests/translator_tests/MacCommon/Android.mk
include $(EMUGL_PATH)/tests/translator_tests/GLES_CM/Android.mk
include $(EMUGL_PATH)/tests/translator_tests/GLES_V2/Android.mk

endif # BUILD_EMULATOR_OPENGL == true
