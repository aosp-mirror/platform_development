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

# We build two apps from the same source

LOCAL_PATH:= $(call my-dir)

# === App 1 ===
include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := ShortcutDemo

LOCAL_MODULE_TAGS := samples tests

LOCAL_AAPT_FLAGS += --rename-manifest-package com.example.android.pm.shortcutdemo

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += $(call all-java-files-under, ../common/src)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/../common/res
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res1

LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

# === App 2 ===
include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := ShortcutDemo2

LOCAL_MODULE_TAGS := samples tests

LOCAL_AAPT_FLAGS += --rename-manifest-package com.example.android.pm.shortcutdemo2

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += $(call all-java-files-under, ../common/src)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/../common/res
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res2

LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)
