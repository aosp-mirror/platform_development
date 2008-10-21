# Copyright 2007 The Android Open Source Project

#
# Build instructions for simulator LD_PRELOAD wrapper.
#
ifneq ($(TARGET_ARCH),arm)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	BitVector.c \
	DevAudio.c \
	DevConsoleTty.c \
	DevEvent.c \
	DevFb.c \
	DevLog.c \
	DevPower.c \
	DevVibrator.c \
	FakeDev.c \
	Init.c \
	Intercept.c \
	Log.c \
	SimMgr.c

LOCAL_C_INCLUDES += prebuilt/common/esd

LOCAL_MODULE := libwrapsim

# Relying on other Android libraries is probably a bad idea, since any
# library or system calls they make could lead to recursive behavior.
#
#LOCAL_SHARED_LIBRARIES +=

LOCAL_LDLIBS += -lpthread -ldl -lesd

include $(BUILD_SHARED_LIBRARY)



#
# Build instructions for simulator runtime launch wrapper.
#
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	LaunchWrapper.c

LOCAL_MODULE := launch-wrapper
include $(BUILD_EXECUTABLE)

endif
# ifneq ($(TARGET_ARCH),arm)
