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
# Install all the files needed to build the ndk.
#   We build three versions of the NDK
#       (1) The full version, with source.
#       (2) The full version, without source.
#       (3) A JNI-only version, with source.
#
#   We make five sets of trees:
#		(A) Common files used in all versions of the NDK
#   (B) Common files used in the full versions of the NDK
#		(C) Files used in the standard ndk (no source files included)
#		(D) Files used in both the JNI-only and full-with-source version
#		(E) Files used in just the full-with-source version
#
#   Each NDK version is created by combining the appropriate trees:
#
#            (A)   (B)  (C)  (D)  (E)
#       (1)  yes   yes       yes  yes
#       (2)  yes   yes  yes
#       (3)  yes             yes
#
# Source is provided for partners who want to recompile our libraries for optimization.
# The JNI-only version is provided for partners that want to create shared
# libraries that can be packaged with APK files and called from Java code.

LOCAL_PATH := $(call my-dir)

# Source trees for the ndk
samples_src_dir := $(LOCAL_PATH)/samples
sample_src_dir := $(samples_src_dir)/sample
samplejni_src_dir := $(samples_src_dir)/samplejni
config_src_dir := $(LOCAL_PATH)/config
kernel_common_src_dir := $(KERNEL_HEADERS_COMMON)
kernel_arch_src_dir := $(KERNEL_HEADERS_ARCH)
bionic_src_dir := bionic
jni_src_dir := $(JNI_H_INCLUDE)

# Workspace directory
ndk_intermediates := $(call intermediates-dir-for,PACKAGING,ndk)

# Common destination trees for the ndk
ndk_common_tree := $(ndk_intermediates)/common
ndk_common_dest_dir := $(ndk_common_tree)/ndk
samplejni_dest_dir := $(ndk_common_dest_dir)/samples/samplejni
config_dest_dir := $(ndk_common_dest_dir)/config
kernel_dest_dir := $(ndk_common_dest_dir)/include/kernel/include
gcc_dest_dir := $(ndk_common_dest_dir)/toolchain
jni_dest_dir := $(ndk_common_dest_dir)/include/nativehelper

# Common-full destination trees for the ndk
ndk_common_full_tree := $(ndk_intermediates)/common_full
ndk_common_full_dest_dir := $(ndk_common_full_tree)/ndk
sample_dest_dir := $(ndk_common_full_dest_dir)/samples/sample

# Destination trees without source for the standard ndk (without source)
ndk_no_src_tree := $(ndk_intermediates)/no_src
ndk_no_src_dest_dir := $(ndk_no_src_tree)/ndk
bionic_no_src_dest_dir := $(ndk_no_src_dest_dir)/include/bionic

# Destination trees including source for the ndk with source
ndk_src_tree := $(ndk_intermediates)/with_src
ndk_src_dest_dir := $(ndk_src_tree)/ndk
bionic_src_dest_dir := $(ndk_src_dest_dir)/include/bionic

# Destinations of all common files (not picked up by tree rules below)
ndk_common_dest_files := $(ndk_common_dest_dir)/Android_NDK_README.html \
		$(ndk_common_dest_dir)/config/armelf.x \
		$(ndk_common_dest_dir)/config/armelflib.x \
		$(ndk_common_dest_dir)/lib/crtbegin_dynamic.o \
		$(ndk_common_dest_dir)/lib/crtend_android.o \
		$(ndk_common_dest_dir)/lib/libc.so \
		$(ndk_common_dest_dir)/lib/libm.so 
    
# Destinations of files used by the full, non-jni-only configurations
ndk_common_full_dest_files := \
		$(ndk_common_full_dest_dir)/lib/libdl.so \
		$(ndk_common_full_dest_dir)/lib/libstdc++.so

# Install common files outside common trees
$(ndk_common_dest_dir)/Android_NDK_README.html: $(LOCAL_PATH)/Android_NDK_README.html | $(ACP)
	@echo "NDK Android_NDK_README.html: from $? to $@"
	$(copy-file-to-target)

$(ndk_common_dest_dir)/config/armelf.x: $(BUILD_SYSTEM)/armelf.x | $(ACP)
	@echo "NDK config: $@"
	$(copy-file-to-target)

$(ndk_common_dest_dir)/config/armelflib.x: $(BUILD_SYSTEM)/armelflib.x | $(ACP)
	@echo "NDK config: $@"
	$(copy-file-to-target)

$(ndk_common_dest_dir)/lib/%: $(TARGET_OUT_INTERMEDIATE_LIBRARIES)/% | $(ACP)
	@echo "NDK lib: $@"
	$(copy-file-to-target)

# Install common_full files outside common trees
$(ndk_common_full_dest_dir)/lib/%: $(TARGET_OUT_INTERMEDIATE_LIBRARIES)/% | $(ACP)
	@echo "NDK lib full: $@"
	$(copy-file-to-target)

# Install files in common trees
listvar := ndk_common_dest_files
$(call define-tree-copy-rules,$(samplejni_src_dir),$(samplejni_dest_dir),$(listvar))
$(call define-tree-copy-rules,$(config_src_dir),$(config_dest_dir),$(listvar))
$(call define-tree-copy-rules,$(kernel_common_src_dir),$(kernel_dest_dir),$(listvar))
$(call define-tree-copy-rules,$(kernel_arch_src_dir),$(kernel_dest_dir),$(listvar))
$(call define-tree-copy-rules,$(jni_src_dir),$(jni_dest_dir),$(listvar), -name jni.h)

# Install files common to the full builds but not the JNI build
listvar := ndk_common_full_dest_files
$(call define-tree-copy-rules,$(sample_src_dir),$(sample_dest_dir),$(listvar))

# Install files without sources
listvar := ndk_no_src_dest_files
$(call define-tree-copy-rules,$(bionic_src_dir),$(bionic_no_src_dest_dir),$(listvar),-name '*.h')

# Install files including sources
listvar := ndk_with_src_dest_files
$(call define-tree-copy-rules,$(bionic_src_dir),$(bionic_src_dest_dir),$(listvar))


#-------------------------------------------------------------------------------
# Create the multiple versions of the ndk:
# ndk_no_src           all files without source
# ndk_with_source      all files with source
# ndk_jni_with_source  just files for building JNI shared libraries with source.

# Name the tar files
name := android_ndk-$(TARGET_PRODUCT)
ifeq ($(TARGET_BUILD_TYPE),debug)
  name := $(name)_debug
endif
name := $(name)-$(BUILD_NUMBER)
ndk_tarfile := $(ndk_intermediates)/$(name).tar
ndk_tarfile_zipped := $(ndk_tarfile).gz
ndk_with_src_tarfile := $(ndk_intermediates)/$(name)-src.tar
ndk_with_src_tarfile_zipped := $(ndk_with_src_tarfile).gz
ndk_jni_with_src_tarfile := $(ndk_intermediates)/$(name)-jni-src.tar
ndk_jni_with_src_tarfile_zipped := $(ndk_jni_with_src_tarfile).gz

.PHONY: ndk ndk_with_src ndk_no_src ndk_jni_with_src ndk_debug

ndk: ndk_no_src ndk_with_src ndk_jni_with_src
ndk_no_src: $(ndk_tarfile_zipped)
ndk_with_src: $(ndk_with_src_tarfile_zipped)
ndk_jni_with_src: $(ndk_jni_with_src_tarfile_zipped)

# Put the ndk zip files in the distribution directory
$(call dist-for-goals,ndk,$(ndk_tarfile_zipped))
$(call dist-for-goals,ndk,$(ndk_with_src_tarfile_zipped))
$(call dist-for-goals,ndk,$(ndk_jni_with_src_tarfile_zipped))

# zip up tar files
%.tar.gz: %.tar
	@echo "NDK: zipped $<"
	$(hide) gzip -cf $< > $@

# tar up the files without our sources to make the ndk.
$(ndk_tarfile): $(ndk_common_dest_files) $(ndk_common_full_dest_files) $(ndk_no_src_dest_files)
	@echo "NDK: $@"
	@mkdir -p $(dir $@)
	@rm -f $@
	$(hide) tar rf $@ -C $(ndk_common_tree) ndk
	$(hide) tar rf $@ -C $(ndk_common_full_tree) ndk
	$(hide) tar rf $@ -C $(ndk_no_src_tree) ndk

# tar up the full sources to make the ndk with sources.
$(ndk_with_src_tarfile): $(ndk_common_dest_files) $(ndk_common_full_dest_files) $(ndk_with_src_dest_files) $(ndk_full_with_src_dest_files)
	@echo "NDK: $@"
	@mkdir -p $(dir $@)
	@rm -f $@
	$(hide) tar rf $@ -C $(ndk_common_tree) ndk
	$(hide) tar rf $@ -C $(ndk_common_full_tree) ndk
	$(hide) tar rf $@ -C $(ndk_src_tree) ndk
	
# tar up the sources to make the ndk with JNI support.
$(ndk_jni_with_src_tarfile): $(ndk_common_dest_files) $(ndk_with_src_dest_files)
	@echo "NDK: $@"
	@mkdir -p $(dir $@)
	@rm -f $@
	$(hide) tar rf $@ -C $(ndk_common_tree) ndk
	$(hide) tar rf $@ -C $(ndk_src_tree) ndk

# Debugging reporting can go here, add it as a target to get output.
ndk_debug: ndk
	@echo "You are here: $@"
	@echo "ndk tar file:          $(ndk_tarfile_zipped)"
	@echo "ndk_with_src tar file: $(ndk_with_src_tarfile_zipped)"
	@echo "ndk_jni_with_src tar file: $(ndk_jni_with_src_tarfile_zipped)"
	@echo "ndk_files:             $(ndk_no_src_dest_files)"
	@echo "ndk_with_src files:    $(ndk_with_src_dest_files)"
	@echo "ndk_full_with_src files:    $(ndk_full_with_src_dest_files)"
	@echo "ndk_common_files:      $(ndk_common_dest_files)"
	@echo "ndk_common_full_dest_files:      $(ndk_common_full_dest_files)"

