LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_STATIC_JAVA_LIBRARIES := guavalib

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_MODULE:= idegen

include $(BUILD_HOST_JAVA_LIBRARY)

