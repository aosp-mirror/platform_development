LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_STATIC_JAVA_LIBRARIES := \
        androidx.annotation_annotation \
        androidx.collection_collection \
        androidx.arch.core_core-common \
        androidx.lifecycle_lifecycle-common \

LOCAL_STATIC_ANDROID_LIBRARIES := \
        androidx.lifecycle_lifecycle-runtime \
        androidx.percentlayout_percentlayout \
        androidx.transition_transition \
        androidx.core_core \
        androidx.legacy_legacy-support-core-ui \
        androidx.media_media \
        androidx.legacy_legacy-support-v13 \
        androidx.preference_preference \
        androidx.appcompat_appcompat \
        androidx.gridlayout_gridlayout \
        androidx.recyclerview_recyclerview

LOCAL_PACKAGE_NAME := DumpViewer
LOCAL_SDK_VERSION := 26

include $(BUILD_PACKAGE)
