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


# We're moving the emulator-specific platform libs to
# development.git/tools/emulator/. The following test is to ensure
# smooth builds even if the tree contains both versions.
#
ifndef BUILD_EMULATOR_SENSORS_MODULE
BUILD_EMULATOR_SENSORS_MODULE := true

LOCAL_PATH := $(call my-dir)

ifneq ($(TARGET_PRODUCT),sim)
# HAL module implemenation stored in
# hw/<SENSORS_HARDWARE_MODULE_ID>.<ro.hardware>.so
include $(CLEAR_VARS)

LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/hw
LOCAL_SHARED_LIBRARIES := liblog libcutils
LOCAL_SRC_FILES := sensors_qemu.c
ifeq ($(TARGET_PRODUCT),vbox_x86)
LOCAL_MODULE := sensors.vbox_x86
else
LOCAL_MODULE := sensors.goldfish
endif
LOCAL_MODULE_TAGS := debug
include $(BUILD_SHARED_LIBRARY)
endif

endif # BUILD_EMULATOR_SENSORS_MODULE
