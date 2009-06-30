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

LOCAL_BUILD_SCRIPT := BUILD_EXECUTABLE
LOCAL_MODULE_CLASS := EXECUTABLE
LOCAL_MAKEFILE     := $(local-makefile)

$(call check-defined-LOCAL_MODULE,$(LOCAL_BUILD_SCRIPT))
$(call check-LOCAL_MODULE,$(LOCAL_MAKEFILE))

# only adjust the build if this module is needed by the current app
ifneq ($(filter $(LOCAL_MODULE),$(NDK_APP_MODULES)),)

# we are building target objects
my := TARGET_

LOCAL_BUILT_MODULE := $(call executable-path,$(LOCAL_MODULE))
LOCAL_OBJS_DIR     := $(TARGET_OBJS)/$(LOCAL_MODULE)

include $(BUILD_SYSTEM)/build-binary.mk

$(LOCAL_BUILT_MODULE): $(LOCAL_OBJECTS)
	@ mkdir -p $(dir $@)
	@ echo "Executable     : $(PRIVATE_NAME)"
	$(hide) $(cmd-build-executable)

ALL_EXECUTABLES += $(LOCAL_BUILT_MODULE)

include $(BUILD_SYSTEM)/install-binary.mk

endif # filter LOCAL_MODULE in NDK_APP_MODULES
