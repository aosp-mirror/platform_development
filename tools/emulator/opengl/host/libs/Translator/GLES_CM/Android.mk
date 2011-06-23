LOCAL_PATH := $(call my-dir)

### GLES_CM host implementation (On top of OpenGL) ########################
$(call emugl-begin-host-shared-library,libGLES_CM_translator)

$(call emugl-import,libGLcommon)

LOCAL_SRC_FILES :=      \
     GLEScmImp.cpp      \
     GLEScmUtils.cpp    \
     TextureUtils.cpp   \
     GLEScmContext.cpp  \
     GLEScmValidate.cpp

$(call emugl-end-module)
