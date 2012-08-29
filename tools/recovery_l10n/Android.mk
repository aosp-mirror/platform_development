# Copyright 2012 Google Inc. All Rights Reserved.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := RecoveryLocalizer
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

include $(BUILD_PACKAGE)
