LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := eng

LOCAL_JAVA_LIBRARIES := android.test.runner telephony-common

LOCAL_SRC_FILES := $(call all-subdir-java-files) \
                src/com/android/development/IRemoteService.aidl \

LOCAL_PACKAGE_NAME := Development
LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)
