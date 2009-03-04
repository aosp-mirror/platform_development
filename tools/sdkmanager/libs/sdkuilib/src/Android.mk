# Copyright 2008 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files)

# no resources yet.
# LOCAL_JAVA_RESOURCE_DIRS := resources

LOCAL_JAVA_LIBRARIES := \
	sdklib \
	swt \
	org.eclipse.jface_3.2.0.I20060605-1400 \
	org.eclipse.equinox.common_3.2.0.v20060603 \
	org.eclipse.core.commands_3.2.0.I20060605-1400
	
LOCAL_MODULE := sdkuilib

include $(BUILD_HOST_JAVA_LIBRARY)

