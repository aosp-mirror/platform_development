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
    $(call __ndk_log, LOCAL_CPP_EXTENSION in $(LOCAL_MAKEFILE) must be one word only, not '$(LOCAL_CPP_EXTENSION)')
    $(call __ndk_error, Aborting)
  endif
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
      $(call __ndk_log,   LOCAL_ARM_MODE in $(LOCAL_MAKEFILE) must be one word, not '$(LOCAL_ARM_MODE)')
      $(call __ndk_error, Aborting)
  endif
  # check that LOCAL_ARM_MODE is defined to either 'arm' or 'thumb'
  $(if $(filter-out thumb arm, $(LOCAL_ARM_MODE)),\
      $(call __ndk_log,   LOCAL_ARM_MODE must be defined to either 'arm' or 'thumb' in $(LOCAL_MAKEFILE), not '$(LOCAL_ARM_MODE)')\
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

# XXX: TODO: support LOCAL_ARM_MODE

arm_sources := $(LOCAL_SRC_FILES:%.arm)

c_sources := $(filter %.c, \
                 $(LOCAL_SRC_FILES) \
                 $(arm_sources:%.arm=%))

s_sources := $(filter %.S, \
                 $(LOCAL_SRC_FILES) \
                 $(arm_sources:%.arm=%))

cpp_sources := $(filter %$(LOCAL_CPP_EXTENSION), \
                   $(LOCAL_SRC_FILES) \
                   $(arm_sources:%.arm=%))

#
# The following will update LOCAL_OBJECTS and LOCAL_DEPENDENCY_DIRS
#
$(foreach src,$(c_sources),   $(call compile-c-source,$(src)))
$(foreach src,$(s_sources),   $(call compile-s-source,$(src)))
$(foreach src,$(cpp_sources), $(call compile-cpp-source,$(src)))

LOCAL_DEPENDENCY_DIRS := $(sort $(LOCAL_DEPENDENCY_DIRS))
CLEAN_OBJS_DIRS       += $(LOCAL_OBJS_DIR)
