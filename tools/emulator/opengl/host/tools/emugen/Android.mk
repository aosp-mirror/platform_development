ifneq ($(HOST_OS),windows)

LOCAL_PATH:=$(call my-dir)

$(call emugl-begin-host-executable,emugen)

    LOCAL_SRC_FILES := \
        ApiGen.cpp \
        EntryPoint.cpp \
        main.cpp \
        strUtils.cpp \
        TypeFactory.cpp

$(call emugl-end-module)

# The location of the emugen host tool that is used to generate wire
# protocol encoders/ decoders. This variable is used by other emugl modules.
EMUGL_EMUGEN := $(LOCAL_BUILT_MODULE)

else # windows build

# on windows use the build host emugen executable
# (that will be the linux exeutable when using mingw build)
EMUGL_EMUGEN := $(BUILD_OUT_EXECUTABLES)/emugen

endif
