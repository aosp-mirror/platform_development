#
# Copyright (C) 2009 The Android Open Source Project
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
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_JAR_MANIFEST := ../etc/manifest.txt
LOCAL_JAVA_LIBRARIES := \
	ddmlib \
	jython \
	xmlwriter \
	guavalib


LOCAL_MODULE := monkeyrunner

include $(BUILD_HOST_JAVA_LIBRARY)

# Build ext.jar
# ============================================================

ext_dirs := 	../../../../external/xmlwriter/src

ext_src_files := $(call all-java-files-under,$(ext_dirs))

# ====  the library  =========================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(ext_src_files)

LOCAL_NO_STANDARD_LIBRARIES := true
#LOCAL_JAVA_LIBRARIES := core
#LOCAL_STATIC_JAVA_LIBRARIES := libgoogleclient

LOCAL_MODULE := xmlwriter

include $(BUILD_HOST_JAVA_LIBRARY)
