# Copyright 2007 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_JAVA_RESOURCE_DIRS := resources

LOCAL_JAR_MANIFEST := ../etc/manifest.txt
LOCAL_JAVA_LIBRARIES := \
	androidprefs \
	sdkstats \
	ddmlib \
	ddmuilib \
	swt \
	org.eclipse.jface_3.4.2.M20090107-0800 \
	org.eclipse.equinox.common_3.4.0.v20080421-2006 \
	org.eclipse.core.commands_3.4.0.I20080509-2000
LOCAL_MODULE := ddms

include $(BUILD_HOST_JAVA_LIBRARY)

