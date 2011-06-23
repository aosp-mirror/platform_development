LOCAL_PATH:=$(call my-dir)

ifeq ($(HOST_OS), linux)

$(call emugl-begin-host-executable,ut_renderer)
$(call emugl-import,libut_rendercontrol_dec libGLESv1_dec libGLESv2_dec libEGL_host_wrapper)

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

LOCAL_CFLAGS += -DPVR_WAR
#LOCAL_CFLAGS += -g -O0

LOCAL_LDLIBS += -lpthread -lX11 -lrt

$(call emugl-end-module)

endif # HOST_OS == linux
