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

$(call clear-vars, $(NDK_APP_VARS))

include $(_application_mk)

$(call check-required-vars,$(NDK_APP_VARS_REQUIRED),$(_application_mk))

# strip the 'lib' prefix in front of APP_MODULES modules
APP_MODULES := $(call strip-lib-prefix,$(APP_MODULES))

# check that APP_OPTIM, if defined, is either 'release' or 'debug'
$(if $(filter-out release debug,$(APP_OPTIM)),\
  $(call __ndk_info, The APP_OPTIM defined in $(_application_mk) must only be 'release' or 'debug')\
  $(call __ndk_error,Aborting)\
)

_dir  := $(patsubst %/,%,$(dir $(_application_mk)))
_name := $(notdir $(_dir))
_app  := NDK_APP.$(_name)

$(if $(strip $(APP.$(_app).defined)),\
  $(call __ndk_info,Weird, the application $(_name) is already defined by $(APP.$(_app).defined))\
  $(call __ndk_error,Aborting)\
)

APP.$(_app).defined := $(_application_mk)

# Record all app-specific variable definitions
$(foreach __name,$(NDK_APP_VARS),\
  $(eval $(_app).$(__name) := $($(__name)))\
)

# Record the Application.mk for debugging
$(_app).Application.mk := $(_application_mk)

NDK_ALL_APPS += $(_name)
