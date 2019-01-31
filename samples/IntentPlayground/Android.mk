LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := samples tests

# Only compile source java files in this apk.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_STATIC_ANDROID_LIBRARIES := \
    com.google.android.material_material \
    androidx.appcompat_appcompat \
    androidx.recyclerview_recyclerview \
    androidx.lifecycle_lifecycle-livedata \
    androidx.lifecycle_lifecycle-viewmodel

LOCAL_USE_AAPT2 := true

LOCAL_PACKAGE_NAME := IntentPlayground

LOCAL_SDK_VERSION := current


include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
