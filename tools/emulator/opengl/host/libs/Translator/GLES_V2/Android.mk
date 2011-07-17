LOCAL_PATH := $(call my-dir)

### GLES_CM host implementation (On top of OpenGL) ########################
$(call emugl-begin-host-shared-library,libGLES_V2_translator)
$(call emugl-import, libGLcommon)

LOCAL_SRC_FILES :=                    \
     GLESv2Imp.cpp                    \
     GLESv2Context.cpp                \
     GLESv2Validate.cpp               \
     ShaderParser.cpp                 \
     ProgramData.cpp

$(call emugl-end-module)
