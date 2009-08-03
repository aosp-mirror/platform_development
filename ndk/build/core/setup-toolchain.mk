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

# this file is included repeatedly from build/core/main.mk and is used
# to setup the target toolchain for a given platform/abi combination.
#

$(call assert-defined,TARGET_TOOLCHAIN TARGET_PLATFORM TARGET_ARCH TARGET_ARCH_ABI)
$(call assert-defined,NDK_APPS)

TARGET_ABI := $(TARGET_PLATFORM)-$(TARGET_ARCH_ABI)

# setup sysroot-related variables. The SYSROOT point to a directory
# that contains all public header files for a given platform, plus
# some libraries and object files used for linking the generated
# target files properly.
#
SYSROOT := build/platforms/$(TARGET_PLATFORM)/arch-$(TARGET_ARCH_ABI)

TARGET_CRTBEGIN_STATIC_O  := $(SYSROOT)/usr/lib/crtbegin_static.o
TARGET_CRTBEGIN_DYNAMIC_O := $(SYSROOT)/usr/lib/crtbegin_dynamic.o
TARGET_CRTEND_O           := $(SYSROOT)/usr/lib/crtend_android.o

TARGET_PREBUILT_SHARED_LIBRARIES := libc libstdc++ libm
TARGET_PREBUILT_SHARED_LIBRARIES := $(TARGET_PREBUILT_SHARED_LIBRARIES:%=$(SYSROOT)/usr/lib/%.so)

# now call the toolchain-specific setup script
include $(NDK_TOOLCHAIN.$(TARGET_TOOLCHAIN).setup)

# compute NDK_APP_DEST as the destination directory for the generated files
NDK_APP_DEST := $(NDK_APP_PROJECT_PATH)/libs/$(TARGET_ABI_SUBDIR)

# free the dictionary of LOCAL_MODULE definitions
$(call modules-clear)

# now parse the Android.mk for the application
include $(NDK_APP_BUILD_SCRIPT)
