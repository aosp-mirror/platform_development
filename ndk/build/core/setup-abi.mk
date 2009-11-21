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

# this file is included multiple times by build/core/setup-app.mk
#

$(call ndk_log,Building application '$(NDK_APP_NAME)' for ABI '$(TARGET_ARCH_ABI)')

TARGET_ARCH := arm

TARGET_OUT  := $(NDK_APP_OUT)/$(_app)/$(TARGET_ARCH_ABI)
TARGET_OBJS := $(TARGET_OUT)/objs

TARGET_GDB_SETUP := $(TARGET_OUT)/setup.gdb

include $(BUILD_SYSTEM)/setup-toolchain.mk
