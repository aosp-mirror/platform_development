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

# Build the unit tests.
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# Build the unit tests.
test_src_files := $(call find-subdir-files, *.cpp)

shared_libraries := \
        libz \
        liblog \
        libcutils \
        libutils \
        libstlport

static_libraries := \
        libgtest \
        libgtest_main

c_includes := \
    external/zlib \
    external/icu4c/common \
    bionic \
    bionic/libstdc++/include \
    external/gtest/include \
    external/stlport/stlport

module_tags := eng tests

$(foreach file,$(test_src_files), \
    $(eval include $(CLEAR_VARS)) \
    $(eval LOCAL_SHARED_LIBRARIES := $(shared_libraries)) \
    $(eval LOCAL_STATIC_LIBRARIES := $(static_libraries)) \
    $(eval LOCAL_C_INCLUDES := $(c_includes)) \
    $(eval LOCAL_SRC_FILES := $(file)) \
    $(eval LOCAL_MODULE := $(notdir $(file:%.cpp=%))) \
    $(eval LOCAL_MODULE_TAGS := $(module_tags)) \
    $(eval include $(BUILD_EXECUTABLE)) \
)
