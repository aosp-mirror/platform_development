LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := samples tests

# Only compile source java files in this apk.
LOCAL_SRC_FILES := \
    $(call all-java-files-under, src) \
    src/com/example/android/apis/app/IRemoteService.aidl \
    src/com/example/android/apis/app/IRemoteServiceCallback.aidl \
    src/com/example/android/apis/app/ISecondary.aidl \

LOCAL_STATIC_ANDROID_LIBRARIES += \
    android-support-v4

LOCAL_USE_AAPT2 := true

LOCAL_PACKAGE_NAME := ApiDemos

LOCAL_SDK_VERSION := current

LOCAL_DEX_PREOPT := false

LOCAL_COMPATIBILITY_SUITE := device-tests

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
