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
# Install all the files needed to build the pndk.
#   We build three versions of the pndk
#       (1) The full version, with source.
#       (2) The full version, without source.
#       (3) A JNI-only version, with source.
#
#   We make five sets of trees:
#		(A) Common files used in all versions of the pndk
#   (B) Common files used in the full versions of the pndk
#		(C) Files used in the standard pndk (no source files included)
#		(D) Files used in both the JNI-only and full-with-source version
#		(E) Files used in just the full-with-source version
#
#   Each pndk version is created by combining the appropriate trees:
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

# Source trees for the pndk
samples_src_dir := $(LOCAL_PATH)/samples
sample_src_dir := $(samples_src_dir)/sample
samplejni_src_dir := $(samples_src_dir)/samplejni
config_src_dir := $(LOCAL_PATH)/config
kernel_common_src_dir := $(KERNEL_HEADERS_COMMON)
kernel_arch_src_dir := $(KERNEL_HEADERS_ARCH)
bionic_src_dir := bionic
jni_src_dir := $(JNI_H_INCLUDE)

# Workspace directory
pndk_intermediates := $(call intermediates-dir-for,PACKAGING,pndk)

# Common destination trees for the pndk
pndk_common_tree := $(pndk_intermediates)/common
pndk_common_dest_dir := $(pndk_common_tree)/pndk
samplejni_dest_dir := $(pndk_common_dest_dir)/samples/samplejni
config_dest_dir := $(pndk_common_dest_dir)/config
kernel_dest_dir := $(pndk_common_dest_dir)/include/kernel/include
gcc_dest_dir := $(pndk_common_dest_dir)/toolchain
jni_dest_dir := $(pndk_common_dest_dir)/include/nativehelper

# Common-full destination trees for the pndk
pndk_common_full_tree := $(pndk_intermediates)/common_full
pndk_common_full_dest_dir := $(pndk_common_full_tree)/pndk
sample_dest_dir := $(pndk_common_full_dest_dir)/samples/sample

# Destination trees without source for the standard pndk (without source)
pndk_no_src_tree := $(pndk_intermediates)/no_src
pndk_no_src_dest_dir := $(pndk_no_src_tree)/pndk
bionic_no_src_dest_dir := $(pndk_no_src_dest_dir)/include/bionic

# Destination trees including source for the pndk with source
pndk_src_tree := $(pndk_intermediates)/with_src
pndk_src_dest_dir := $(pndk_src_tree)/pndk
bionic_src_dest_dir := $(pndk_src_dest_dir)/include/bionic

# Destinations of all common files (not picked up by tree rules below)
pndk_common_dest_files := $(pndk_common_dest_dir)/Android_PNDK_README.html \
		$(pndk_common_dest_dir)/config/armelf.x \
		$(pndk_common_dest_dir)/config/armelflib.x \
		$(pndk_common_dest_dir)/lib/crtbegin_dynamic.o \
		$(pndk_common_dest_dir)/lib/crtend_android.o \
		$(pndk_common_dest_dir)/lib/libc.so \
		$(pndk_common_dest_dir)/lib/libm.so 
    
# Destinations of files used by the full, non-jni-only configurations
pndk_common_full_dest_files := \
		$(pndk_common_full_dest_dir)/lib/libdl.so \
		$(pndk_common_full_dest_dir)/lib/libstdc++.so

# Install common files outside common trees
$(pndk_common_dest_dir)/Android_PNDK_README.html: $(LOCAL_PATH)/Android_PNDK_README.html | $(ACP)
	@echo "pndk Android_PNDK_README.html: from $? to $@"
	$(copy-file-to-target)

$(pndk_common_dest_dir)/config/armelf.x: $(BUILD_SYSTEM)/armelf.x | $(ACP)
	@echo "pndk config: $@"
	$(copy-file-to-target)

$(pndk_common_dest_dir)/config/armelflib.x: $(BUILD_SYSTEM)/armelflib.x | $(ACP)
	@echo "pndk config: $@"
	$(copy-file-to-target)

$(pndk_common_dest_dir)/lib/%: $(TARGET_OUT_INTERMEDIATE_LIBRARIES)/% | $(ACP)
	@echo "pndk lib: $@"
	$(copy-file-to-target)

# Install common_full files outside common trees
$(pndk_common_full_dest_dir)/lib/%: $(TARGET_OUT_INTERMEDIATE_LIBRARIES)/% | $(ACP)
	@echo "pndk lib full: $@"
	$(copy-file-to-target)

# Install files in common trees
listvar := pndk_common_dest_files
$(call define-tree-copy-rules,$(samplejni_src_dir),$(samplejni_dest_dir),$(listvar))
$(call define-tree-copy-rules,$(config_src_dir),$(config_dest_dir),$(listvar))
$(call define-tree-copy-rules,$(kernel_common_src_dir),$(kernel_dest_dir),$(listvar))
$(call define-tree-copy-rules,$(kernel_arch_src_dir),$(kernel_dest_dir),$(listvar))
$(call define-tree-copy-rules,$(jni_src_dir),$(jni_dest_dir),$(listvar), -name jni.h)

# Install files common to the full builds but not the JNI build
listvar := pndk_common_full_dest_files
$(call define-tree-copy-rules,$(sample_src_dir),$(sample_dest_dir),$(listvar))

# Install files without sources
listvar := pndk_no_src_dest_files
$(call define-tree-copy-rules,$(bionic_src_dir),$(bionic_no_src_dest_dir),$(listvar),-name '*.h')

# Install files including sources
listvar := pndk_with_src_dest_files
$(call define-tree-copy-rules,$(bionic_src_dir),$(bionic_src_dest_dir),$(listvar))


#-------------------------------------------------------------------------------
# Create the multiple versions of the pndk:
# pndk_no_src           all files without source
# pndk_with_source      all files with source
# pndk_jni_with_source  just files for building JNI shared libraries with source.

# Name the tar files
name := android_pndk-$(TARGET_PRODUCT)
ifeq ($(TARGET_BUILD_TYPE),debug)
  name := $(name)_debug
endif
name := $(name)-$(BUILD_NUMBER)
pndk_tarfile := $(pndk_intermediates)/$(name).tar
pndk_tarfile_zipped := $(pndk_tarfile).gz
pndk_with_src_tarfile := $(pndk_intermediates)/$(name)-src.tar
pndk_with_src_tarfile_zipped := $(pndk_with_src_tarfile).gz
pndk_jni_with_src_tarfile := $(pndk_intermediates)/$(name)-jni-src.tar
pndk_jni_with_src_tarfile_zipped := $(pndk_jni_with_src_tarfile).gz

.PHONY: pndk pndk_with_src pndk_no_src pndk_jni_with_src pndk_debug

pndk: pndk_no_src pndk_with_src pndk_jni_with_src
pndk_no_src: $(pndk_tarfile_zipped)
pndk_with_src: $(pndk_with_src_tarfile_zipped)
pndk_jni_with_src: $(pndk_jni_with_src_tarfile_zipped)

# Put the pndk zip files in the distribution directory
$(call dist-for-goals,pndk,$(pndk_tarfile_zipped))
$(call dist-for-goals,pndk,$(pndk_with_src_tarfile_zipped))
$(call dist-for-goals,pndk,$(pndk_jni_with_src_tarfile_zipped))

# zip up tar files
%.tar.gz: %.tar
	@echo "pndk: zipped $<"
	$(hide) gzip -cf $< > $@

# tar up the files without our sources to make the pndk.
$(pndk_tarfile): $(pndk_common_dest_files) $(pndk_common_full_dest_files) $(pndk_no_src_dest_files)
	@echo "pndk: $@"
	@mkdir -p $(dir $@)
	@rm -f $@
	$(hide) tar rf $@ -C $(pndk_common_tree) pndk
	$(hide) tar rf $@ -C $(pndk_common_full_tree) pndk
	$(hide) tar rf $@ -C $(pndk_no_src_tree) pndk

# tar up the full sources to make the pndk with sources.
$(pndk_with_src_tarfile): $(pndk_common_dest_files) $(pndk_common_full_dest_files) $(pndk_with_src_dest_files) $(pndk_full_with_src_dest_files)
	@echo "pndk: $@"
	@mkdir -p $(dir $@)
	@rm -f $@
	$(hide) tar rf $@ -C $(pndk_common_tree) pndk
	$(hide) tar rf $@ -C $(pndk_common_full_tree) pndk
	$(hide) tar rf $@ -C $(pndk_src_tree) pndk
	
# tar up the sources to make the pndk with JNI support.
$(pndk_jni_with_src_tarfile): $(pndk_common_dest_files) $(pndk_with_src_dest_files)
	@echo "pndk: $@"
	@mkdir -p $(dir $@)
	@rm -f $@
	$(hide) tar rf $@ -C $(pndk_common_tree) pndk
	$(hide) tar rf $@ -C $(pndk_src_tree) pndk

# Debugging reporting can go here, add it as a target to get output.
pndk_debug: pndk
	@echo "You are here: $@"
	@echo "pndk tar file:          $(pndk_tarfile_zipped)"
	@echo "pndk_with_src tar file: $(pndk_with_src_tarfile_zipped)"
	@echo "pndk_jni_with_src tar file: $(pndk_jni_with_src_tarfile_zipped)"
	@echo "pndk_files:             $(pndk_no_src_dest_files)"
	@echo "pndk_with_src files:    $(pndk_with_src_dest_files)"
	@echo "pndk_full_with_src files:    $(pndk_full_with_src_dest_files)"
	@echo "pndk_common_files:      $(pndk_common_dest_files)"
	@echo "pndk_common_full_dest_files:      $(pndk_common_full_dest_files)"

