# Copyright (C) 2014 The Android Open Source Project
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

LOCAL_PATH := $(call my-dir)

supported_platforms := linux # For now... Needs to be tested on MacOS/Windows
cur_platform := $(filter $(HOST_OS),$(supported_platforms))

ifdef cur_platform

# Host executable

include $(CLEAR_VARS)
LOCAL_MODULE := btsnooz
LOCAL_SRC_FILES := btsnooz.cpp btsnooz_utils.cpp
LOCAL_C_INCLUDES := external/zlib system/bt
LOCAL_CFLAGS += -std=c++11 -W -Wall
LOCAL_STATIC_LIBRARIES := libz

ifeq ($(HOST_OS),linux)
  LOCAL_LDLIBS += -lresolv
endif

include $(BUILD_HOST_EXECUTABLE)

endif #cur_platform
