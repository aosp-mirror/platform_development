#
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

# Assemble the Native Development Kit
# Assembled using the generic build by default.
# (set in device/config/product_config.make)

# NOTE: gcc source is in a separate perforce tree which must be checked out to
# the same directory that the device code is checked out to; so that gcc can be
# copied from ../toolchain/compiler/gcc-4.2.1

# A macro to make rules to copy all newer files in a directory tree matching an
# optional find filter and add the files to a list variable for dependencies.
# Designed after copy_headers.make: Create a rule to copy each file;
# copy-one-file defines the actual rule.
# $(1): source directory tree root
# $(2): destination directory tree root
# $(3): list variable to append destination files to
# $(4): optional find(1) arguments
define define-tree-copy-rules
  $(eval _src_files := $(shell find $(1) -type f $(4))) \
  $(foreach _src, $(_src_files), \
    $(eval _dest := $(patsubst $(1)%,$(2)%,$(_src))) \
    $(eval $(3) := $($(3)) $(_dest)) \
    $(eval $(call copy-one-file,$(_src),$(_dest))) \
   )
endef

#-------------------------------------------------------------------------------
# Install all the files needed to build the ndk (both with and without source)
#   We make three sets of trees:
#			Common files used in both versions of the ndk
#			Files used in the standard ndk (no source files included)
#			Files used in the ndk-src (includes source files)
#   We do this as some partners want to recompile our libraries for optimization

LOCAL_PATH := $(my-dir)

# Source trees for the ndk
sample_src_dir := $(LOCAL_PATH)/sample
config_src_dir := $(LOCAL_PATH)/config
kernel_common_src_dir := $(KERNEL_HEADERS_COMMON)
kernel_arch_src_dir := $(KERNEL_HEADERS_ARCH)
bionic_src_dir := system/bionic
libm_src_dir := system/libm
libstdcpp_src_dir := system/libstdc++
gcc_src_dir := ../toolchain/compiler/gcc-4.2.1

# Workspace directory
ndk_intermediates := $(call intermediates-dir-for,PACKAGING,ndk)

# Common destination trees for the ndk
ndk_common_tree := $(ndk_intermediates)/common
ndk_common_dest_dir := $(ndk_common_tree)/ndk
sample_dest_dir := $(ndk_common_dest_dir)/sample
config_dest_dir := $(ndk_common_dest_dir)/config
kernel_dest_dir := $(ndk_common_dest_dir)/include/kernel/include
gcc_dest_dir := $(ndk_common_dest_dir)/toolchain

# Destination trees without source for the standard ndk (without source)
ndk_no_src_tree := $(ndk_intermediates)/no_src
ndk_no_src_dest_dir := $(ndk_no_src_tree)/ndk
bionic_no_src_dest_dir := $(ndk_no_src_dest_dir)/include/bionic
libm_no_src_dest_dir := $(ndk_no_src_dest_dir)/include/libm
libstdcpp_no_src_dest_dir := $(ndk_no_src_dest_dir)/include/libstdc++

# Destination trees including source for the ndk with source
ndk_src_tree := $(ndk_intermediates)/with_src
ndk_src_dest_dir := $(ndk_src_tree)/ndk
bionic_src_dest_dir := $(ndk_src_dest_dir)/include/bionic
libm_src_dest_dir := $(ndk_src_dest_dir)/include/libm
libstdcpp_src_dest_dir := $(ndk_src_dest_dir)/include/libstdc++

# Destinations of all common files (not picked up by tree rules below)
ndk_common_dest_files := $(ndk_common_dest_dir)/README \
		$(ndk_common_dest_dir)/config/armelf.x \
		$(ndk_common_dest_dir)/config/armelflib.x \
		$(ndk_common_dest_dir)/lib/crtbegin_dynamic.o \
		$(ndk_common_dest_dir)/lib/crtend_android.o \
		$(ndk_common_dest_dir)/lib/libc.so \
		$(ndk_common_dest_dir)/lib/libdl.so \
		$(ndk_common_dest_dir)/lib/libm.so \
		$(ndk_common_dest_dir)/lib/libstdc++.so

# Install common files outside common trees
$(ndk_common_dest_dir)/README: $(LOCAL_PATH)/README | $(ACP)
	@echo "NDK README: from $? to $@"
	$(copy-file-to-target)

$(ndk_common_dest_dir)/config/armelf.x: config/armelf.x | $(ACP)
	@echo "NDK config: $@"
	$(copy-file-to-target)

$(ndk_common_dest_dir)/config/armelflib.x: config/armelflib.x | $(ACP)
	@echo "NDK config: $@"
	$(copy-file-to-target)

$(ndk_common_dest_dir)/lib/%: $(TARGET_OUT_INTERMEDIATE_LIBRARIES)/% | $(ACP)
	@echo "NDK lib: $@"
	$(copy-file-to-target)
# (TODO): libandroid_rumtime libnativehelper

# Install files in common trees
listvar := ndk_common_dest_files
$(call define-tree-copy-rules,$(sample_src_dir),$(sample_dest_dir),$(listvar))
$(call define-tree-copy-rules,$(config_src_dir),$(config_dest_dir),$(listvar))
$(call define-tree-copy-rules,$(kernel_common_src_dir),$(kernel_dest_dir),$(listvar))
$(call define-tree-copy-rules,$(kernel_arch_src_dir),$(kernel_dest_dir),$(listvar))
$(call define-tree-copy-rules,$(gcc_src_dir),$(gcc_dest_dir),$(listvar))
# TODO: jni
# TODO: gdb

# Install files without sources
listvar := ndk_no_src_dest_files
$(call define-tree-copy-rules,$(bionic_src_dir),$(bionic_no_src_dest_dir),$(listvar),-name '*.h')
$(call define-tree-copy-rules,$(libm_src_dir),$(libm_no_src_dest_dir),$(listvar),-name '*.h')
# Pull out files from only the include directory in libstdc++
$(call define-tree-copy-rules,$(libstdcpp_src_dir),$(libstdcpp_no_src_dest_dir),$(listvar),-regex '.*/include/.*')

# Install files including sources
listvar := ndk_with_src_dest_files
$(call define-tree-copy-rules,$(bionic_src_dir),$(bionic_src_dest_dir),$(listvar))
$(call define-tree-copy-rules,$(libm_src_dir),$(libm_src_dest_dir),$(listvar))
$(call define-tree-copy-rules,$(libstdcpp_src_dir),$(libstdcpp_src_dest_dir),$(listvar))

#-------------------------------------------------------------------------------
# Create the two versions of the ndk (with and without source)

# Name the tar files
name := android_ndk-$(REQUESTED_PRODUCT)
ifeq ($(TARGET_BUILD_TYPE),debug)
  name := $(name)_debug
endif
name := $(name)-$(BUILD_NUMBER)
ndk_tarfile := $(ndk_intermediates)/$(name).tar
ndk_tarfile_zipped := $(ndk_tarfile).gz
ndk_with_src_tarfile := $(ndk_intermediates)/$(name)-src.tar
ndk_with_src_tarfile_zipped := $(ndk_with_src_tarfile).gz

.PHONY: ndk ndk_with_src ndk_no_src ndk_debug

ndk: ndk_no_src ndk_with_src
ndk_no_src: $(ndk_tarfile_zipped)
ndk_with_src: $(ndk_with_src_tarfile_zipped)

# Put the ndk zip files in the distribution directory
$(call dist-for-goals,pdk,$(ndk_tarfile_zipped))
$(call dist-for-goals,pdk,$(ndk_with_src_tarfile_zipped))

# zip up tar files
%.tar.gz: %.tar
	@echo "NDK: zipped $<"
	$(hide) gzip -cf $< > $@

# tar up the files without our sources to make the ndk.
$(ndk_tarfile): $(ndk_common_dest_files) $(ndk_no_src_dest_files)
	@echo "NDK: $@"
	@mkdir -p $(dir $@)
	@rm -f $@
	$(hide) tar rf $@ -C $(ndk_common_tree) ndk
	$(hide) tar rf $@ -C $(ndk_no_src_tree) ndk

# tar up the full sources to make the ndk with sources.
$(ndk_with_src_tarfile): $(ndk_common_dest_files) $(ndk_with_src_dest_files)
	@echo "NDK: $@"
	@mkdir -p $(dir $@)
	@rm -f $@
	$(hide) tar rf $@ -C $(ndk_common_tree) ndk
	$(hide) tar rf $@ -C $(ndk_src_tree) ndk

# Debugging reporting can go here, add it as a target to get output.
ndk_debug: ndk
	@echo "You are here: $@"
	@echo "ndk_with_src tar file: $(ndk_with_src_tarfile_zipped)"
	@echo "ndk tar file:          $(ndk_tarfile_zipped)"
	@echo "ndk_with_src files:    $(ndk_with_src_dest_files)"
	@echo "ndk_files:             $(ndk_no_src_dest_files)"
	@echo "ndk_common_files:      $(ndk_common_dest_files)"

