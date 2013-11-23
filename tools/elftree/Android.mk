# Copyright (C) 2010 The Android Open Source Project
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

supported_platforms := linux-x86
cur_platform := $(filter $(HOST_OS)-$(HOST_ARCH),$(supported_platforms))

ifdef cur_platform

# Target executable
# TODO: Requires libelf for target

#include $(CLEAR_VARS)
#LOCAL_MODULE := $(module)
#LOCAL_SRC_FILES := $(src_files)
#LOCAL_C_INCLUDES := $(c_includes)
#LOCAL_SHARED_LIBRARIES := $(shared_libraries)
#LOCAL_STATIC_LIBRARIES := $(static_libraries)
#LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
#LOCAL_LDLIBS +=
#include $(BUILD_EXECUTABLE)

# Host executable

include $(CLEAR_VARS)
LOCAL_MODULE := elftree
LOCAL_SRC_FILES := elftree.c
LOCAL_C_INCLUDES := external/elfutils/0.153/libelf
# to fix compatibility issues in elf headers across different platforms
LOCAL_CFLAGS += \
	-include external/elfutils/0.153/host-$(HOST_OS)-fixup/AndroidFixup.h
LOCAL_STATIC_LIBRARIES := libelf
include $(BUILD_HOST_EXECUTABLE)

endif #cur_platform
