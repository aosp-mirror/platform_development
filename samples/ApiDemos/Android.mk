LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := samples tests

# Only compile source java files in this apk.
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += \
        src/com/example/android/apis/app/IRemoteService.aidl \
        src/com/example/android/apis/app/IRemoteServiceCallback.aidl \
        src/com/example/android/apis/app/ISecondary.aidl \

LOCAL_JAVA_LIBRARIES := telephony-common mms-common

LOCAL_STATIC_JAVA_LIBRARIES = android-support-v4

LOCAL_PACKAGE_NAME := ApiDemos

LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
