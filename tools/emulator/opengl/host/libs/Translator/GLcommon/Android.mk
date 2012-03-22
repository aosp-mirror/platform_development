LOCAL_PATH := $(call my-dir)

host_common_SRC_FILES :=     \
     GLDispatch.cpp          \
     GLutils.cpp             \
     GLEScontext.cpp         \
     GLESvalidate.cpp        \
     GLESpointer.cpp         \
     GLESbuffer.cpp          \
     DummyGLfuncs.cpp        \
     RangeManip.cpp          \
     TextureUtils.cpp        \
     PaletteTexture.cpp      \
     etc1.cpp                \
     objectNameManager.cpp   \
     FramebufferData.cpp

host_GL_COMMON_LINKER_FLAGS :=
host_common_LDLIBS :=
host_common_LDFLAGS :=

ifeq ($(HOST_OS),linux)
#    host_common_LDFLAGS += -Wl,--whole-archive
    host_common_LDLIBS += -lGL -ldl
    host_common_LDFLAGS += -Wl,-Bsymbolic
endif

ifeq ($(HOST_OS),windows)
    host_common_LDLIBS += -lopengl32 -lgdi32
    host_common_LDFLAGS += -Wl,--add-stdcall-alias
endif


### EGL host implementation ########################

$(call emugl-begin-host-static-library,libGLcommon)

$(call emugl-import,libOpenglOsUtils)
translator_path := $(LOCAL_PATH)/..
LOCAL_SRC_FILES := $(host_common_SRC_FILES)
$(call emugl-export,LDLIBS,$(host_common_LDLIBS))
$(call emugl-export,LDFLAGS,$(host_common_LDFLAGS))
$(call emugl-export,C_INCLUDES,$(LOCAL_PATH)/../include $(EMUGL_PATH)/shared)
$(call emugl-export,STATIC_LIBRARIES, libcutils libutils liblog)

$(call emugl-end-module)


### EGL host implementation, 64-bit ################

$(call emugl-begin-host-static-library,lib64GLcommon)

$(call emugl-import,lib64OpenglOsUtils)
translator_path := $(LOCAL_PATH)/..
LOCAL_SRC_FILES := $(host_common_SRC_FILES)
$(call emugl-export,LDLIBS,$(host_common_LDLIBS))
$(call emugl-export,LDFLAGS,$(host_common_LDFLAGS))
$(call emugl-export,C_INCLUDES,$(LOCAL_PATH)/../include $(EMUGL_PATH)/shared)
$(call emugl-export,STATIC_LIBRARIES, lib64cutils lib64utils lib64log)

$(call emugl-end-module)
