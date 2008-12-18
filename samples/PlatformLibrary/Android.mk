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

# This makefile shows how to build your own shared library that can be
# shipped on the system of a phone, and included additional examples of
# including JNI code with the library and writing client applications against it.

LOCAL_PATH := $(call my-dir)

# the library
# ============================================================
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := samples

# This is the target being built.
LOCAL_MODULE:= com.example.android.platform_library

# Only compile source java files for the platform library.
LOCAL_SRC_FILES := $(call all-java-files-under, java)

include $(BUILD_JAVA_LIBRARY)

# ============================================================

# Also build all of the sub-targets under this one: the library's
# associated JNI code, and a sample client of the library.
include $(call all-makefiles-under,$(LOCAL_PATH))
