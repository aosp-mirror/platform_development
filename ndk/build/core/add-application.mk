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

# this script is used to record an application definition in the
# NDK build system, before performing any build whatsoever.
#
# It is included repeatedly from build/core/main.mk and expects a
# variable named '_application_mk' which points to a given Application.mk
# file that will be included here. The latter must define a few variables
# to describe the application to the build system, and the rest of the
# code here will perform book-keeping and basic checks
#

$(call assert-defined, _application_mk)
$(call ndk_log,Parsing $(_application_mk))

$(call clear-vars, $(NDK_APP_VARS))

include $(_application_mk)

$(call check-required-vars,$(NDK_APP_VARS_REQUIRED),$(_application_mk))

_dir  := $(patsubst %/,%,$(dir $(_application_mk)))
_name := $(notdir $(_dir))
_map  := NDK_APP.$(_name)

# strip the 'lib' prefix in front of APP_MODULES modules
APP_MODULES := $(call strip-lib-prefix,$(APP_MODULES))

# check that APP_OPTIM, if defined, is either 'release' or 'debug'
APP_OPTIM := $(strip $(APP_OPTIM))
$(if $(filter-out release debug,$(APP_OPTIM)),\
  $(call __ndk_info, The APP_OPTIM defined in $(_application_mk) must only be 'release' or 'debug')\
  $(call __ndk_error,Aborting)\
)

ifndef APP_OPTIM
    $(call ndk_log,  Defaulted to APP_OPTIM=release)
    APP_OPTIM := release
endif

# check whether APP_PLATFORM is defined. If not, look for default.properties in
# the $(APP_PROJECT_PATH) and extract the value with awk's help. If nothing is here,
# revert to the default value (i.e. "android-3").
#
# NOTE: APP_PLATFORM is an experimental feature for now.
#
APP_PLATFORM := $(strip $(APP_PLATFORM))
ifndef APP_PLATFORM
    _local_props := $(strip $(wildcard $(APP_PROJECT_PATH)/default.properties))
    ifdef _local_props
        APP_PLATFORM := $(strip $(shell $(HOST_AWK) -f $(BUILD_SYSTEM)/extract-platform.awk < $(_local_props)))
        $(call ndk_log,  Found APP_PLATFORM=$(APP_PLATFORM) in $(_local_props))
    else
        APP_PLATFORM := android-3
        $(call ndk_log,  Defaulted to APP_PLATFORM=$(APP_PLATFORM))
    endif
endif

# Check that the value of APP_PLATFORM corresponds to a known platform
# If not, we're going to use the max supported platform value.
#
_bad_platform := $(strip $(filter-out $(NDK_ALL_PLATFORMS),$(APP_PLATFORM)))
ifdef _bad_platform
    $(call __ndk_info,Application $(_name) targets unknown platform '$(_bad_platform)')
    APP_PLATFORM := android-$(NDK_MAX_PLATFORM_LEVEL)
    $(call __ndk_info,Switching to $(APP_PLATFORM))
endif

# If APP_BUILD_SCRIPT is defined, check that the file exists.
# If undefined, look in $(APP_PROJECT_PATH)/jni/Android.mk
#
APP_BUILD_SCRIPT := $(strip $(APP_BUILD_SCRIPT))
ifdef APP_BUILD_SCRIPT
    _build_script := $(strip $(wildcard $(APP_BUILD_SCRIPT)))
    ifndef _build_script
        $(call __ndk_info,Your APP_BUILD_SCRIPT points to an unknown file: $(APP_BUILD_SCRIPT))
        $(call __ndk_error,Aborting...)
    endif
    APP_BUILD_SCRIPT := $(_build_script)
    $(call ndk_log,  Using build script $(APP_BUILD_SCRIPT))
else
    _build_script := $(strip $(wildcard $(APP_PROJECT_PATH)/jni/Android.mk))
    ifndef _build_script
        $(call __ndk_info,There is no Android.mk under $(APP_PROJECT_PATH)/jni)
        $(call __ndk_info,If this is intentional, please define APP_BUILD_SCRIPT to point)
        $(call __ndk_info,to a valid NDK build script.)
        $(call __ndk_error,Aborting...)
    endif
    APP_BUILD_SCRIPT := $(_build_script)
    $(call ndk_log,  Defaulted to APP_BUILD_SCRIPT=$(APP_BUILD_SCRIPT))
endif

$(if $(call get,$(_map),defined),\
  $(call __ndk_info,Weird, the application $(_name) is already defined by $(call get,$(_map),defined))\
  $(call __ndk_error,Aborting)\
)

$(call set,$(_map),defined,$(_application_mk))

# Record all app-specific variable definitions
$(foreach __name,$(NDK_APP_VARS),\
  $(call set,$(_map),$(__name),$($(__name)))\
)

# Record the Application.mk for debugging
$(call set,$(_map),Application.mk,$(_application_mk))

NDK_ALL_APPS += $(_name)
