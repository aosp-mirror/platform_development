# Copyright (C) 2011 The Android Open Source Project
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

ifndef BUILD_EMULATOR_CAMERA_HAL
BUILD_EMULATOR_CAMERA_HAL := true

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/hw
LOCAL_CFLAGS += -fno-short-enums -DQEMU_HARDWARE
LOCAL_SHARED_LIBRARIES:= \
    libbinder \
    libutils \
    libcutils \
    libcamera_client \
    libui \

# JPEG conversion libraries and includes.
LOCAL_SHARED_LIBRARIES += \
	libjpeg \
	libskia \
	libandroid_runtime \

LOCAL_C_INCLUDES += external/jpeg \
					external/skia/include/core/ \
					frameworks/base/core/jni/android/graphics

LOCAL_SRC_FILES := \
	EmulatedCameraHal.cpp \
    EmulatedCameraFactory.cpp \
    EmulatedCamera.cpp \
	EmulatedCameraDevice.cpp \
	EmulatedQemuCamera.cpp \
	EmulatedQemuCameraDevice.cpp \
	EmulatedFakeCamera.cpp \
	EmulatedFakeCameraDevice.cpp \
	Converters.cpp \
	PreviewWindow.cpp \
	CallbackNotifier.cpp \
	QemuClient.cpp \
	JpegCompressor.cpp

ifeq ($(TARGET_PRODUCT),vbox_x86)
LOCAL_MODULE := camera.vbox_x86
else
LOCAL_MODULE := camera.goldfish
endif

LOCAL_MODULE_TAGS := debug
include $(BUILD_SHARED_LIBRARY)

endif # BUILD_EMULATOR_CAMERA_HAL
