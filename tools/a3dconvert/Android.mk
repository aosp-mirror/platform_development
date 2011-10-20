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

LOCAL_PATH := $(call my-dir)

# Host executable
include $(CLEAR_VARS)
LOCAL_MODULE := a3dconvert
LOCAL_MODULE_TAGS := optional
LOCAL_CFLAGS += -DANDROID_RS_SERIALIZE
# Needed for colladaDom
LOCAL_CFLAGS += -DNO_BOOST -DDOM_INCLUDE_TINYXML -DNO_ZAE

LOCAL_SRC_FILES := \
    a3dconvert.cpp \
    ObjLoader.cpp \
    ColladaConditioner.cpp \
    ColladaGeometry.cpp \
    ColladaLoader.cpp

# Needed to maintain libRS dependencies
intermediates := $(call intermediates-dir-for,STATIC_LIBRARIES,libRS,HOST,)
librs_generated_headers := \
    $(intermediates)/rsgApiStructs.h \
    $(intermediates)/rsgApiFuncDecl.h
LOCAL_GENERATED_SOURCES := $(librs_generated_headers)

LOCAL_C_INCLUDES += external/collada/include
LOCAL_C_INCLUDES += external/collada/include/1.4
LOCAL_C_INCLUDES += frameworks/base/libs/rs
LOCAL_C_INCLUDES += $(intermediates)

LOCAL_LDLIBS := -ldl -lpthread
LOCAL_STATIC_LIBRARIES += libRS libutils libcutils
LOCAL_STATIC_LIBRARIES += colladadom libtinyxml libpcrecpp libpcre
include $(BUILD_HOST_EXECUTABLE)
