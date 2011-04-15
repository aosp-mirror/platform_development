# Building this module breaks the Linux build because
# libxcb.so is not installed in the i686-linux-glibc2.7-4.4.3
# prebuilt sysroot. Since rebuilding it will take some time, here's a
# quick fix to unbreak it.
#
ifneq (,$(BUILD_EMULATOR_OPENGL))

LOCAL_PATH:=$(call my-dir)

# ut_renderer test program ###########################

include $(CLEAR_VARS)

ifeq ($(HOST_OS), linux)

emulatorOpengl := $(LOCAL_PATH)/../..

LOCAL_MODULE := ut_renderer
LOCAL_MODULE_TAGS := debug

# add additional depencies to ensure that the generated code that we depend on
# is generated
LOCAL_ADDITIONAL_DEPENDENCIES := \
	$(HOST_OUT_SHARED_LIBRARIES)/libut_rendercontrol_dec$(HOST_SHLIB_SUFFIX) \
	$(HOST_OUT_SHARED_LIBRARIES)/libGLESv1_dec$(HOST_SHLIB_SUFFIX)

LOCAL_SRC_FILES := ut_renderer.cpp \
        RenderingThread.cpp \
	ReadBuffer.cpp \
	Renderer.cpp \
	RendererContext.cpp \
	RendererSurface.cpp \
	X11Windowing.cpp 

# define PVR_WAR to support imgtec PVR opengl-ES implementation
#
# specifically this MACRO enables code that work arounds a bug 
# in the implementation where glTextureParameter(...,GL_TEXTURE_RECT,...)
# is called would cause a crash if the texture dimensions have not been 
# defined yet.

LOCAL_CFLAGS := -DPVR_WAR 
#LOCAL_CFLAGS += -g -O0

LOCAL_C_INCLUDES := $(emulatorOpengl)/shared/OpenglCodecCommon \
        $(emulatorOpengl)/host/include/libOpenglRender \
		$(call intermediates-dir-for, SHARED_LIBRARIES, libut_rendercontrol_dec, HOST) \
		$(call intermediates-dir-for, SHARED_LIBRARIES, libGLESv1_dec, HOST) \
        $(emulatorOpengl)/host/libs/GLESv1_dec \
        $(emulatorOpengl)/system/GLESv1_enc \
        $(emulatorOpengl)/tests/ut_rendercontrol_enc

LOCAL_SHARED_LIBRARIES := libut_rendercontrol_dec libGLESv1_dec libEGL_host_wrapper
LOCAL_STATIC_LIBRARIES := \
    libOpenglCodecCommon \
    libcutils

LOCAL_LDLIBS := -lpthread -lX11 -lrt
include $(BUILD_HOST_EXECUTABLE)

endif # HOST_OS == linux

endif # BUILD_EMULATOR_OPENGL
