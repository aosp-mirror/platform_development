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

# Assemble the Platform Development Kit (PDK)
# (TODO) Figure out why $(ACP) builds with target ndk but not pdk_docs
# (TODO) Copy over index.html from templates instead of from generatedDocs 
# (TODO) Build doxygen (depend on latest version)

pdk:
	@echo "Package: $@ has targets ndk, pdk_docs and pdk_all"

pdk_all: ndk pdk_docs
	@echo "Package: $^"

LOCAL_PATH := $(call my-dir)

#-------------------------------------------------------------------------------
# Make the Native Development Kit (Code examples)
#   Allows vendors to build shared libraries without entire source tree.
# This include adds /ndk to LOCAL_PATH, so can't use it afterwards...
include $(LOCAL_PATH)/ndk/Ndk.mk


#-------------------------------------------------------------------------------
# Make the Plaftorm Development Kit Documentation.
#   Doxygenize the header files to create html docs in the generatedDocs dir.
#   Copy the template files and the generated html to the docs dir and zip 
#   everything up to the distribution directory.


# Workspace directory
pdk_docs_intermediates := $(call intermediates-dir-for,PACKAGING,pdkdocs)

# Source directories for templates, config & header files
pdk_templates_dir := development/pdk/docs
pdk_config_dir := development/pdk/doxygen_config
pdk_docsfile_dir := $(pdk_config_dir)/docsfiles
pdk_legacy_hardware_dir := hardware/libhardware_legacy/include/hardware_legacy
pdk_camera_dir := frameworks/base/include/ui

# Destination directory for docs (templates + doxygenated headers)
pdk_docs_dest_dir := $(pdk_docs_intermediates)/docs

# Working directory for source to be doxygenated
pdk_doxy_source_dir := $(pdk_docs_intermediates)/sources

# Working directory for html, et al. after doxygination
pdk_generated_source_dir := $(pdk_docs_intermediates)/generatedDocs/html

# Working directory for .dox files
pdk_doxy_docsfiles_dir := $(pdk_docs_intermediates)/docsfiles

# Doxygen version to use, so we can override it on the command line
# doxygen 1.4.6 working, the latest version get-apt installable on goobuntu.
# (TODO) doxygen 1.5.6 generated source files not displayable
# doxygen_version='~pubengdocs/shared/doxy/doxygen.1.5.6.kcc'
#   for latest version of doxygen on linux
doxygen_version = doxygen

#------------------------------------------------------------------------------- 
# Header files to doxygenize. 
#   Add new header files to document here, also adjust the templates to have 
#   descriptions for the new headers and point to the new doxygen created html.
pdk_headers := \
    $(pdk_legacy_hardware_dir)/AudioHardwareInterface.h \
    $(pdk_legacy_hardware_dir)/gps.h \
    $(pdk_legacy_hardware_dir)/wifi.h \
    $(pdk_camera_dir)/CameraHardwareInterface.h

# Create a rule to copy the list of PDK headers to be doxyginated.
# copy-one-header defines the actual rule.
$(foreach header,$(pdk_headers), \
  $(eval _chFrom := $(header)) \
  $(eval _chTo :=  $(pdk_doxy_source_dir)/$(notdir $(header))) \
  $(eval $(call copy-one-header,$(_chFrom),$(_chTo))) \
  $(eval all_copied_pdk_headers: $(_chTo)) \
 )
_chFrom :=
_chTo :=


#-------------------------------------------------------------------------------
# Assemble all the necessary doxygen config files and the sources into the
#   working directories

pdk_templates := $(shell find $(pdk_templates_dir) -type f)

# Create a rule to copy the list of PDK doc templates.
# copy-one-file defines the actual rule.
$(foreach template,$(pdk_templates), \
  $(eval _chFrom := $(template)) \
  $(eval _chTo :=  $(pdk_docs_dest_dir)/$(notdir $(template))) \
  $(eval $(call copy-one-header,$(_chFrom),$(_chTo))) \
  $(eval all_copied_pdk_templates: $(_chTo)) \
 )
_chFrom :=
_chTo :=

# Copy newer doxygen config file (basic configs, should not change very often.)
pdk_doxygen_config_file := $(pdk_docs_intermediates)/pdk_config.conf
$(pdk_doxygen_config_file): $(pdk_config_dir)/pdk_config.conf
	@echo "PDK: $@"
	$(copy-file-to-target-with-cp)

# Copy newer doxygen override config file (may change these more often.)
pdk_doxygen_config_override_file := $(pdk_docs_intermediates)/overrideconfig.conf
$(pdk_doxygen_config_override_file): $(pdk_config_dir)/overrideconfig.conf
	@echo "PDK: $@"
	$(copy-file-to-target-with-cp)

# (TODO) Get the latest templates
# Copy newer doxygen html files.
$(pdk_docs_intermediates)/header.html: $(pdk_config_dir)/header.html
	@echo "PDK: $@"
	$(copy-file-to-target-with-cp)

$(pdk_docs_intermediates)/footer.html: $(pdk_config_dir)/footer.html
	@echo "PDK: $@"
	$(copy-file-to-target-with-cp)

# Copy newer doxygen .dox files
$(pdk_doxy_docsfiles_dir)/groups.dox: $(pdk_docsfile_dir)/groups.dox
	@echo "PDK: $@"
	$(copy-file-to-target-with-cp)

$(pdk_doxy_docsfiles_dir)/main.dox: $(pdk_docsfile_dir)/main.dox
	@echo "PDK: $@"
	$(copy-file-to-target-with-cp)

# Run doxygen and copy all output and templates to the final destination
# We replace index.html with a template file so don't use the generated one
pdk_doxygen: all_copied_pdk_headers $(pdk_doxygen_config_override_file) \
    $(pdk_doxygen_config_file) $(pdk_docs_intermediates)/header.html \
    $(pdk_docs_intermediates)/footer.html $(pdk_doxy_docsfiles_dir)/groups.dox \
    $(pdk_doxy_docsfiles_dir)/main.dox
	@echo "Files for Doxygination: $^"
	@mkdir -p $(pdk_generated_source_dir)
	@rm -f $(pdk_generated_source_dir)/*
	@cd $(pdk_docs_intermediates) && $(doxygen_version) pdk_config.conf
	@mkdir -p $(pdk_docs_dest_dir)
	@cd $(pdk_generated_source_dir) && chmod ug+rx *
	@rm -f $(pdk_generated_source_dir)/index.html
	@cp -fp $(pdk_generated_source_dir)/* $(pdk_docs_dest_dir)
  
# Name the tar files
name := android_pdk_docs-$(REQUESTED_PRODUCT)
ifeq ($(TARGET_BUILD_TYPE),debug)
  name := $(name)_debug
endif
name := $(name)-$(BUILD_NUMBER)
pdk_docs_tarfile := $(pdk_docs_intermediates)/$(name).tar
pdk_docs_tarfile_zipped := $(pdk_docs_tarfile).gz

.PHONY: pdk pdk_docs pdk_doxygen all_copied_pdk_headers all_copied_pdk_templates

pdk_docs: $(pdk_docs_tarfile_zipped)
	@echo "PDK: Docs tarred and zipped"

# Put the pdk_docs zip files in the distribution directory
$(call dist-for-goals,pdk_docs,$(pdk_docs_tarfile_zipped))

# zip up tar files
%.tar.gz: %.tar
	@echo "PDK: zipped $<"
	$(hide) gzip -cf $< > $@

# tar up all the files to make the pdk docs.
$(pdk_docs_tarfile): pdk_doxygen all_copied_pdk_templates
	@echo "PDK: $@"
	@mkdir -p $(dir $@)
	@rm -f $@
	$(hide) tar rf $@ -C $(pdk_docs_intermediates) docs 

# Debugging reporting can go here, add it as a target to get output.
pdk_debug:
	@echo "You are here: $@"
	@echo "pdk headers copied: $(all_copied_pdk_headers)"
	@echo "pdk headers: $(pdk_headers)"
	@echo "pdk docs dest: $(pdk_docs_dest_dir)"
	@echo "config dest: $(pdk_doxygen_config_file)"
	@echo "config src: $(pdk_config_dir)/pdk_config.conf"
	@echo "pdk templates: $(pdk_templates_dir)"
