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

# this file is included repeatedly from Android.mk files in order to clean
# the module-specific variables from the environment,

NDK_LOCAL_VARS := \
  LOCAL_MODULE \
  LOCAL_SRC_FILES \
  LOCAL_C_INCLUDES \
  LOCAL_CFLAGS \
  LOCAL_CXXFLAGS \
  LOCAL_CPPFLAGS \
  LOCAL_LDFLAGS \
  LOCAL_LDLIBS \
  LOCAL_ARFLAGS \
  LOCAL_CPP_EXTENSION \
  LOCAL_STATIC_LIBRARIES \
  LOCAL_STATIC_WHOLE_LIBRARIES \
  LOCAL_SHARED_LIBRARIES \
  LOCAL_MAKEFILE \
  LOCAL_NO_UNDEFINED_SYMBOLS \
  LOCAL_ARM_MODE \

$(call clear-vars, $(NDK_LOCAL_VARS))

