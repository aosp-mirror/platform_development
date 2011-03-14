# Copyright 2008 The Android Open Source Project

# We're moving the emulator-specific platform libs to
# development.git/tools/emulator/. The following test is to ensure
# smooth builds even if the tree contains both versions.
#
ifndef BUILD_EMULATOR_QEMUD
BUILD_EMULATOR_QEMUD := true

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	qemud.c


LOCAL_SHARED_LIBRARIES := \
	libcutils \

LOCAL_MODULE:= qemud
LOCAL_MODULE_TAGS := debug

include $(BUILD_EXECUTABLE)

endif # BUILD_EMULATOR_QEMUD