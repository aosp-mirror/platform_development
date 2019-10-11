#
# Copyright (C) 2016 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# For testing, we build multiple apk files with different versions.

LOCAL_PATH:= $(call my-dir)

#============================================================================
# Base version (10)
#============================================================================

include $(CLEAR_VARS)

LOCAL_USE_AAPT2 := true

LOCAL_PACKAGE_NAME := ShortcutSample

LOCAL_AAPT_FLAGS += --version-code 10

LOCAL_MODULE_TAGS := samples tests

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_STATIC_ANDROID_LIBRARIES = androidx.legacy_legacy-support-v4

LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

#============================================================================
# Version 11.
#============================================================================

include $(CLEAR_VARS)

LOCAL_USE_AAPT2 := true

LOCAL_PACKAGE_NAME := ShortcutSample11

LOCAL_AAPT_FLAGS += --version-code 11

LOCAL_MODULE_TAGS := samples tests

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_STATIC_ANDROID_LIBRARIES = androidx.legacy_legacy-support-v4

LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

#============================================================================
# Version 12.
#============================================================================

include $(CLEAR_VARS)

LOCAL_USE_AAPT2 := true

LOCAL_PACKAGE_NAME := ShortcutSample12

LOCAL_AAPT_FLAGS += --version-code 12

LOCAL_MODULE_TAGS := samples tests

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_STATIC_ANDROID_LIBRARIES = androidx.legacy_legacy-support-v4

LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

#============================================================================
# Version 11, backup disabled.
#============================================================================

include $(CLEAR_VARS)

LOCAL_USE_AAPT2 := true

LOCAL_PACKAGE_NAME := ShortcutSample11nb

LOCAL_AAPT_FLAGS += --version-code 11
LOCAL_MANIFEST_FILE := noback/AndroidManifest.xml

LOCAL_MODULE_TAGS := samples tests

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_STATIC_ANDROID_LIBRARIES = androidx.legacy_legacy-support-v4

LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

#============================================================================
# Version 12, backup disabled.
#============================================================================

include $(CLEAR_VARS)

LOCAL_USE_AAPT2 := true

LOCAL_PACKAGE_NAME := ShortcutSample12nb

LOCAL_AAPT_FLAGS += --version-code 12
LOCAL_MANIFEST_FILE := noback/AndroidManifest.xml

LOCAL_MODULE_TAGS := samples tests

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_STATIC_ANDROID_LIBRARIES = androidx.legacy_legacy-support-v4

LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

