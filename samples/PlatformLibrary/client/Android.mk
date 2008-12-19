#
# Copyright (C) 2008 The Android Open Source Project
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

# This makefile is an example of writing an application that will link against
# a custom shared library included with an Android system.

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := samples

# This is the target being built.
LOCAL_PACKAGE_NAME := PlatformLibraryClient

# Only compile source java files in this apk.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

# Link against the current Android SDK.
LOCAL_SDK_VERSION := current

# Also link against our own custom library.
LOCAL_JAVA_LIBRARIES := com.example.android.platform_library

include $(BUILD_PACKAGE)
