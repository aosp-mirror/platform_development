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

# this file is used to prepare the NDK to build with the arm-eabi-4.2.1
# toolchain any number of source files
#
# its purpose is to define (or re-define) templates used to build
# various sources into target object files, libraries or executables.
#
# Note that this file may end up being parsed several times in future
# revisions of the NDK.
#

TOOLCHAIN_NAME   := arm-eabi-4.2.1
TOOLCHAIN_PREFIX := $(HOST_PREBUILT)/$(TOOLCHAIN_NAME)/bin/arm-eabi-

TARGET_CFLAGS.common := \
    -I$(SYSROOT)/usr/include \
    -march=armv5te -mtune=xscale \
    -msoft-float -fpic \
    -mthumb-interwork \
    -ffunction-sections \
    -funwind-tables \
    -fstack-protector \
    -fno-short-enums \
    -D__ARM_ARCH_5__ -D__ARM_ARCH_5T__ \
    -D__ARM_ARCH_5E__ -D__ARM_ARCH_5TE__ \


TARGET_arm_release_CFLAGS :=  -O2 \
                              -fomit-frame-pointer \
                              -fstrict-aliasing    \
                              -funswitch-loops     \
                              -finline-limit=300

TARGET_thumb_release_CFLAGS := -mthumb \
                               -Os \
                               -fomit-frame-pointer \
                               -fno-strict-aliasing \
                               -finline-limit=64

# When building for debug, compile everything as arm.
TARGET_arm_debug_CFLAGS := $(TARGET_arm_release_CFLAGS) \
                           -fno-omit-frame-pointer \
                           -fno-strict-aliasing

TARGET_thumb_debug_CFLAGS := $(TARGET_thumb_release_CFLAGS) \
                             -marm \
                             -fno-omit-frame-pointer

# This function will be called to determine the target CFLAGS used to build
# a C or Assembler source file, based on its tags.
#
# NOTE: ARM Advanced SIMD (a.k.a. NEON) is not supported with this toolchain.
#
TARGET-process-src-files-tags = \
$(eval __arm_sources := $(call get-src-files-with-tag,arm)) \
$(eval __thumb_sources := $(call get-src-files-without-tag,arm)) \
$(eval __debug_sources := $(call get-src-files-with-tag,debug)) \
$(eval __release_sources := $(call get-src-files-without-tag,debug)) \
$(call set-src-files-target-cflags, \
    $(call set_intersection,$(__arm_sources),$(__debug_sources)), \
    $(TARGET_arm_debug_CFLAGS)) \
$(call set-src-files-target-cflags,\
    $(call set_intersection,$(__arm_sources),$(__release_sources)),\
    $(TARGET_arm_release_CFLAGS)) \
$(call set-src-files-target-cflags,\
    $(call set_intersection,$(__arm_sources),$(__debug_sources)),\
    $(TARGET_arm_debug_CFLAGS)) \
$(call set-src-files-target-cflags,\
    $(call set_intersection,$(__thumb_sources),$(__release_sources)),\
    $(TARGET_thumb_release_CFLAGS)) \
$(call set-src-files-target-cflags,\
    $(call set_intersection,$(__thumb_sources),$(__debug_sources)),\
    $(TARGET_thumb_debug_CFLAGS)) \
$(call set-src-files-text,$(__arm_sources),arm$(space)$(space)) \
$(call set-src-files-text,$(__thumb_sources),thumb)

TARGET_CC       := $(TOOLCHAIN_PREFIX)gcc
TARGET_CFLAGS   := $(TARGET_CFLAGS.common)


TARGET_CXX      := $(TOOLCHAIN_PREFIX)g++
TARGET_CXXFLAGS := $(TARGET_CFLAGS.common) -fno-exceptions -fno-rtti

TARGET_LD      := $(TOOLCHAIN_PREFIX)ld
TARGET_LDFLAGS :=

TARGET_AR      := $(TOOLCHAIN_PREFIX)ar
TARGET_ARFLAGS := crs

TARGET_LIBGCC := $(shell $(TARGET_CC) -mthumb-interwork -print-libgcc-file-name)
TARGET_LDLIBS := -Wl,-rpath-link=$(SYSROOT)/usr/lib

# These flags are used to ensure that a binary doesn't reference undefined
# flags.
TARGET_NO_UNDEFINED_LDFLAGS := -Wl,--no-undefined

# The ABI-specific sub-directory that the SDK tools recognize for
# this toolchain's generated binaries
TARGET_ABI_SUBDIR := armeabi

# NOTE: Ensure that TARGET_LIBGCC is placed after all private objects
#       and static libraries, but before any other library in the link
#       command line when generating shared libraries and executables.
#
#       This ensures that all libgcc.a functions required by the target
#       will be included into it, instead of relying on what's available
#       on other libraries like libc.so, which may change between system
#       releases due to toolchain or library changes.
#
define cmd-build-shared-library
$(TARGET_CC) \
    -nostdlib -Wl,-soname,$(notdir $@) \
    -Wl,-shared,-Bsymbolic \
    $(PRIVATE_OBJECTS) \
    -Wl,--whole-archive \
    $(PRIVATE_WHOLE_STATIC_LIBRARIES) \
    -Wl,--no-whole-archive \
    $(PRIVATE_STATIC_LIBRARIES) \
    $(TARGET_LIBGCC) \
    $(PRIVATE_SHARED_LIBRARIES) \
    $(PRIVATE_LDFLAGS) \
    $(PRIVATE_LDLIBS) \
    -o $@
endef

define cmd-build-executable
$(TARGET_CC) \
    -nostdlib -Bdynamic \
    -Wl,-dynamic-linker,/system/bin/linker \
    -Wl,--gc-sections \
    -Wl,-z,nocopyreloc \
    $(TARGET_CRTBEGIN_DYNAMIC_O) \
    $(PRIVATE_OBJECTS) \
    $(PRIVATE_STATIC_LIBRARIES) \
    $(TARGET_LIBGCC) \
    $(PRIVATE_SHARED_LIBRARIES) \
    $(PRIVATE_LDFLAGS) \
    $(PRIVATE_LDLIBS) \
    $(TARGET_CRTEND_O) \
    -o $@
endef

define cmd-build-static-library
$(TARGET_AR) $(TARGET_ARFLAGS) $@ $(PRIVATE_OBJECTS)
endef

cmd-strip = $(TOOLCHAIN_PREFIX)strip --strip-debug $1
