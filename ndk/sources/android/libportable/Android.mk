#
# Copyright (C) 2012 The Android Open Source Project
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

#=====================================================================
# Device Shared Library libportable
#=====================================================================

include $(CLEAR_VARS)

LOCAL_MODULE := libportable
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

LOCAL_C_INCLUDES := $(LOCAL_PATH)/common/include

# Uncomment the next line to easily enable Lib-Portable logging during development.
# LOCAL_CFLAGS += -DLOG_NDEBUG=0

libportable_src_files = \
  $(patsubst $(LOCAL_PATH)/%,%,$(wildcard $(LOCAL_PATH)/arch-$(TARGET_ARCH)/*.c)) \
  $(patsubst $(LOCAL_PATH)/%,%,$(wildcard $(LOCAL_PATH)/arch-$(TARGET_ARCH)/*.S))

LOCAL_SRC_FILES := \
  $(libportable_src_files)

LOCAL_WHOLE_STATIC_LIBRARIES += cpufeatures
LOCAL_SHARED_LIBRARIES += liblog libdl

include $(BUILD_SHARED_LIBRARY)
