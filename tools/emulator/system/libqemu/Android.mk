LOCAL_PATH := $(call my-dir)

# The main libqemud library
include $(CLEAR_VARS)
LOCAL_MODULE    := libqemu
LOCAL_SRC_FILES := libqemu.c
LOCAL_MODULE_TAGS := debug
include $(BUILD_STATIC_LIBRARY)

# Define BUILD_LIBQEMUD_TESTS to 'true' in your environment
# to generate the following test programs. You must be on Linux!
#
ifeq (true-linux,$(strip $(BUILD_LIBQEMU_TESTS)-$(HOST_OS)))
include $(LOCAL_PATH)/tests.mk
endif # BUILD_LIBQEMUD_TESTS == true
