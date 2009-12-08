# Copyright 2007 The Android Open Source Project

#
# Build instructions for simulator LD_PRELOAD wrapper.
#
ifeq ($(TARGET_SIMULATOR),true)

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
	SimMgr.c \
	SysPower.c \
	Util.c

LOCAL_MODULE := libwrapsim

# Relying on other Android libraries is probably a bad idea, since any
# library or system calls they make could lead to recursive behavior.
LOCAL_LDLIBS += -lpthread -ldl

ifeq ($(BUILD_SIM_WITHOUT_AUDIO),true)
LOCAL_CFLAGS += -DBUILD_SIM_WITHOUT_AUDIO=1
else
LOCAL_LDLIBS += -lasound
endif

include $(BUILD_SHARED_LIBRARY)



#
# Build instructions for simulator runtime launch wrapper.
#
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	LaunchWrapper.c

LOCAL_MODULE := launch-wrapper
include $(BUILD_EXECUTABLE)

endif # ifeq ($(TARGET_SIMULATOR),true)
