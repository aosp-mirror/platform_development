LOCAL_PATH := $(call my-dir)

### EGL host implementation ########################
$(call emugl-begin-host-static-library,libGLcommon)

$(call emugl-import,libOpenglOsUtils)

translator_path := $(LOCAL_PATH)/..

LOCAL_SRC_FILES :=           \
     GLDispatch.cpp          \
     GLutils.cpp             \
     GLEScontext.cpp         \
     GLESvalidate.cpp        \
     GLESpointer.cpp         \
     GLESbuffer.cpp          \
     DummyGLfuncs.cpp        \
     RangeManip.cpp          \
     objectNameManager.cpp


ifeq ($(HOST_OS),linux)
#    $(call emugl-export,LDFLAGS,-Wl,--whole-archive)
    $(call emugl-export,LDLIBS,-lGL -ldl)
endif

ifeq ($(HOST_OS),windows)
    $(call emugl-export,LDLIBS,-lopengl32 -lgdi32)
endif

$(call emugl-export,C_INCLUDES,$(LOCAL_PATH)/../include $(EMUGL_PATH)/shared)
$(call emugl-export,STATIC_LIBRARIES, libcutils libutils liblog)

$(call emugl-end-module)
