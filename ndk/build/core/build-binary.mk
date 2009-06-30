# Copyright (C) 2008 The Android Open Source Project
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

# we expect the 'my' variable to be defined, either to
# 'HOST_' or 'TARGET_', and this allows us to call the
# appropriate compiler with $($(my)CC)
#
$(call assert-defined,my)

# LOCAL_MAKEFILE must also exist and name the Android.mk that
# included the module build script.
#
$(call assert-defined,LOCAL_MAKEFILE)

include $(BUILD_SYSTEM)/build-module.mk

# list of generated object files
LOCAL_OBJECTS :=

# always define ANDROID when building binaries
#
LOCAL_CFLAGS := -DANDROID $(LOCAL_CFLAGS)

#
# Add the default system shared libraries to the build
#
ifndef LOCAL_IS_HOST_MODULE
  ifeq ($(LOCAL_SYSTEM_SHARED_LIBRARIES),none)
    LOCAL_SHARED_LIBRARIES += $(TARGET_DEFAULT_SYSTEM_SHARED_LIBRARIES)
  else
    LOCAL_SHARED_LIBRARIES += $(LOCAL_SYSTEM_SHARED_LIBRARIES)
  endif
endif


#
# Check LOCAL_CPP_EXTENSION, use '.cpp' by default
#
LOCAL_CPP_EXTENSION := $(strip $(LOCAL_CPP_EXTENSION))
ifeq ($(LOCAL_CPP_EXTENSION),)
  LOCAL_CPP_EXTENSION := .cpp
else
  ifneq ($(words $(LOCAL_CPP_EXTENSION)),1)
    $(call __ndk_info, LOCAL_CPP_EXTENSION in $(LOCAL_MAKEFILE) must be one word only, not '$(LOCAL_CPP_EXTENSION)')
    $(call __ndk_error, Aborting)
  endif
endif

#
# If LOCAL_ALLOW_UNDEFINED_SYMBOLS, the linker will allow the generation
# of a binary that uses undefined symbols.
#
ifeq ($(strip $(LOCAL_ALLOW_UNDEFINED_SYMBOLS)),)
  LOCAL_LDFLAGS := $(LOCAL_LDFLAGS) $($(my)NO_UNDEFINED_LDFLAGS)
endif

#
# The original Android build system allows you to use the .arm prefix
# to a source file name to indicate that it should be defined in either
# 'thumb' or 'arm' mode, depending on the value of LOCAL_ARM_MODE
#
# First, check LOCAL_ARM_MODE, it should be empty, 'thumb' or 'arm'
# We make the default 'thumb'
#
LOCAL_ARM_MODE := $(strip $(LOCAL_ARM_MODE))
ifeq ($(LOCAL_ARM_MODE),)
  LOCAL_ARM_MODE := thumb
else
  ifneq ($(words $(LOCAL_ARM_MODE)),1)
      $(call __ndk_info,   LOCAL_ARM_MODE in $(LOCAL_MAKEFILE) must be one word, not '$(LOCAL_ARM_MODE)')
      $(call __ndk_error, Aborting)
  endif
  # check that LOCAL_ARM_MODE is defined to either 'arm' or 'thumb'
  $(if $(filter-out thumb arm, $(LOCAL_ARM_MODE)),\
      $(call __ndk_info,   LOCAL_ARM_MODE must be defined to either 'arm' or 'thumb' in $(LOCAL_MAKEFILE) not '$(LOCAL_ARM_MODE)')\
      $(call __ndk_error, Aborting)\
  )
endif

LOCAL_ARM_TEXT_arm   = arm$(space)$(space)
LOCAL_ARM_TEXT_thumb = thumb

LOCAL_ARM_CFLAGS := $(TARGET_$(LOCAL_ARM_MODE)_$(LOCAL_BUILD_MODE)_CFLAGS)
LOCAL_ARM_TEXT   := $(LOCAL_ARM_TEXT_$(LOCAL_ARM_MODE))

# As a special case, the original Android build system
# allows one to specify that certain source files can be
# forced to build in ARM mode by using a '.arm' suffix
# after the extension, e.g.
#
#  LOCAL_SRC_FILES := foo.c.arm
#
# to build source file $(LOCAL_PATH)/foo.c as ARM
#

#
# Build C source files into .o
#

ifeq ($(LOCAL_ARM_MODE),arm)
    arm_sources   := $(LOCAL_SRC_FILES)
else
    arm_sources   := $(filter %.arm,$(LOCAL_SRC_FILES))
    thumb_sources := $(filter-out %.arm,$(LOCAL_SRC_FILES))
endif

# First, build the 'thumb' sources
#
LOCAL_ARM_MODE := thumb

$(foreach src,$(filter %.c,$(thumb_sources)), $(call compile-c-source,$(src)))
$(foreach src,$(filter %.S,$(thumb_sources)), $(call compile-s-source,$(src)))

$(foreach src,$(filter %$(LOCAL_CPP_EXTENSION),$(thumb_sources)),\
    $(call compile-cpp-source,$(src)))

# Then, the 'ARM' ones
#
LOCAL_ARM_MODE := arm
arm_sources := $(arm_sources:%.arm=%)

$(foreach src,$(filter %.c,$(arm_sources)), $(call compile-c-source,$(src)))
$(foreach src,$(filter %.S,$(arm_sources)), $(call compile-s-source,$(src)))

$(foreach src,$(filter %$(LOCAL_CPP_EXTENSION),$(arm_sources)),\
    $(call compile-cpp-source,$(src)))

#
# The compile-xxx-source calls updated LOCAL_OBJECTS and LOCAL_DEPENDENCY_DIRS
#
ALL_DEPENDENCY_DIRS += $(sort $(LOCAL_DEPENDENCY_DIRS))
CLEAN_OBJS_DIRS     += $(LOCAL_OBJS_DIR)

#
# Handle the static and shared libraries this module depends on
#
LOCAL_STATIC_LIBRARIES := $(call strip-lib-prefix,$(LOCAL_STATIC_LIBRARIES))
LOCAL_SHARED_LIBRARIES := $(call strip-lib-prefix,$(LOCAL_SHARED_LIBRARIES))

static_libraries := $(call map,static-library-path,$(LOCAL_STATIC_LIBRARIES))
shared_libraries := $(call map,shared-library-path,$(LOCAL_SHARED_LIBRARIES)) \
                    $(TARGET_PREBUILT_SHARED_LIBRARIES)

$(LOCAL_BUILT_MODULE): $(static_libraries) $(shared_libraries)

# If LOCAL_LDLIBS contains anything like -l<library> then
# prepend a -L$(SYSROOT)/usr/lib to it to ensure that the linker
# looks in the right location
#
ifneq ($(filter -l%,$(LOCAL_LDLIBS)),)
    LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib $(LOCAL_LDLIBS)
endif

$(LOCAL_BUILT_MODULE): PRIVATE_STATIC_LIBRARIES := $(static_libraries)
$(LOCAL_BUILT_MODULE): PRIVATE_SHARED_LIBRARIES := $(shared_libraries)
$(LOCAL_BUILT_MODULE): PRIVATE_OBJECTS          := $(LOCAL_OBJECTS)

$(LOCAL_BUILT_MODULE): PRIVATE_LDFLAGS := $(TARGET_LDFLAGS) $(LOCAL_LDFLAGS)
$(LOCAL_BUILT_MODULE): PRIVATE_LDLIBS  := $(LOCAL_LDLIBS) $(TARGET_LDLIBS)

$(LOCAL_BUILT_MODULE): PRIVATE_NAME := $(notdir $(LOCAL_BUILT_MODULE))
