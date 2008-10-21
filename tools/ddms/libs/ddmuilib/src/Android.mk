# Copyright 2007 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_JAVA_RESOURCE_DIRS := resources

LOCAL_JAVA_LIBRARIES := \
	ddmlib \
	swt \
	org.eclipse.jface_3.2.0.I20060605-1400 \
	org.eclipse.equinox.common_3.2.0.v20060603 \
	org.eclipse.core.commands_3.2.0.I20060605-1400 \
	jcommon-1.0.12 \
	jfreechart-1.0.9 \
	jfreechart-1.0.9-swt
	
LOCAL_MODULE := ddmuilib

include $(BUILD_HOST_JAVA_LIBRARY)

