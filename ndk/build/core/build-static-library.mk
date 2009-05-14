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

# this file is included from Android.mk files to build a target-specific
# executable program
#

LOCAL_BUILD_SCRIPT := BUILD_STATIC_LIBRARY
LOCAL_MODULE_CLASS := STATIC_LIBRARY
LOCAL_MAKEFILE     := $(local-makefile)

$(call check-defined-LOCAL_MODULE,$(LOCAL_BUILD_SCRIPT))
$(call check-LOCAL_MODULE,$(LOCAL_MAKEFILE))

# only adjust the build if this module is needed by the current app
ifneq ($(filter $(LOCAL_MODULE),$(NDK_APP_MODULES)),)

# we are building target objects
my := TARGET_

LOCAL_BUILT_MODULE := $(call static-library-path,$(LOCAL_MODULE))
LOCAL_OBJS_DIR     := $(TARGET_OBJS)/$(LOCAL_MODULE)

include $(BUILD_SYSTEM)/build-binary.mk

static_libraries := $(call map,static-library-path,$(LOCAL_STATIC_LIBRARIES))
shared_libraries := $(call map,shared-library-path,$(LOCAL_SHARED_LIBRARIES)) \
                    $(TARGET_PREBUILT_SHARED_LIBRARIES)

$(LOCAL_BUILT_MODULE): $(static_libraries) $(shared_libraries)

LOCAL_LDLIBS := $(_module_libs) $(LOCAL_LDLIBS)

$(LOCAL_BUILT_MODULE): PRIVATE_STATIC_LIBRARIES := $(static_libraries)
$(LOCAL_BUILT_MODULE): PRIVATE_SHARED_LIBRARIES := $(shared_libraries)
$(LOCAL_BUILT_MODULE): PRIVATE_OBJECTS          := $(LOCAL_OBJECTS)

$(LOCAL_BUILT_MODULE): PRIVATE_LDFLAGS := $(TARGET_LDFLAGS) $(LOCAL_LDFLAGS)
$(LOCAL_BUILT_MODULE): PRIVATE_LDLIBS  := $(LOCAL_LDLIBS) $(TARGET_LDLIBS)

$(LOCAL_BUILT_MODULE): PRIVATE_NAME := $(notdir $(LOCAL_BUILT_MODULE))
$(LOCAL_BUILT_MODULE): PRIVATE_SRC  := $(LOCAL_BUILT_MODULE)

$(LOCAL_BUILT_MODULE): $(LOCAL_OBJECTS)
	@ mkdir -p $(dir $@)
	@ echo "StaticLibrary  : $(PRIVATE_NAME)"
	$(hide) rm -rf $@
	$(hide) $(cmd-build-static-library)

ALL_STATIC_LIBRARIES += $(LOCAL_BUILT_MODULE)

endif # filter LOCAL_MODULE in NDK_APP_MODULES
