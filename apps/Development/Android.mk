LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := android.test.runner.stubs telephony-common org.apache.http.legacy

LOCAL_SRC_FILES := $(call all-subdir-java-files) \
                src/com/android/development/IRemoteService.aidl \

LOCAL_PACKAGE_NAME := Development
LOCAL_LICENSE_KINDS := SPDX-license-identifier-Apache-2.0
LOCAL_LICENSE_CONDITIONS := notice
LOCAL_NOTICE_FILE  := $(LOCAL_PATH)/NOTICE
LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)
