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
# (TODO) Figure out why $(ACP) builds with target pndk but not pdk_docs

pdk:
	@echo "Package: $@ has targets pndk, pdk_docs and pdk_all"

pdk_all: pndk pdk_docs
	@echo "Package: $^"

LOCAL_PATH := $(call my-dir)

#-------------------------------------------------------------------------------
# Make the Native Development Kit (Code examples)
#   Allows vendors to build shared libraries without entire source tree.
# This include adds /pndk to LOCAL_PATH, so can't use it afterwards...
include $(LOCAL_PATH)/pndk/Pndk.mk


#-------------------------------------------------------------------------------
# Make the Plaftorm Development Kit Documentation.
#   Doxygenize the header files to create html docs in the generatedDocs dir.
#   Copy the appengine files, the template files and the generated html 
#   to the docs dir and zip everything up to the distribution directory.
#   Run javadocs/droiddocs/clearsilver on the generatedDocs dir to get the right
#   styles added to the html.


# Workspace directory
pdk_docs_intermediates := $(call intermediates-dir-for,PACKAGING,pdkdocs)

# Source directories for appengine, templates, config & header files
pdk_hosting_dir := development/pdk/hosting
pdk_templates_dir := development/pdk/docs
pdk_config_dir := development/pdk/doxygen_config
pdk_docsfile_dir := $(pdk_config_dir)/docsfiles
pdk_legacy_hardware_dir := hardware/libhardware_legacy/include/hardware_legacy
pdk_hardware_dir := hardware/libhardware/include/hardware
pdk_camera_dir := frameworks/base/include/camera

# Destination directory for docs (templates + doxygenated headers)
pdk_docs_dest_dir := $(pdk_docs_intermediates)/docs/porting
pdk_app_eng_root := $(pdk_docs_intermediates)/docs

# Working directory for source to be doxygenated
pdk_doxy_source_dir := $(pdk_docs_intermediates)/sources

# Working directory for html, et al. after doxygination
pdk_generated_source_dir := $(pdk_docs_intermediates)/generatedDocs/html

# Working directory for .dox files
pdk_doxy_docsfiles_dir := $(pdk_docs_intermediates)/docsfiles

# Doxygen version to use, so we can override it on the command line
# doxygen 1.5.6 working, the latest version get-apt installable on ghardy.
# with bug fix for </div> error.
doxygen_version = doxygen

#------------------------------------------------------------------------------- 
# Header files to doxygenize. 
#   Add new header files to document here, also adjust the templates to have 
#   descriptions for the new headers and point to the new doxygen created html.
pdk_headers := \
    $(pdk_legacy_hardware_dir)/AudioHardwareInterface.h \
    $(pdk_hardware_dir)/gps.h \
    $(pdk_legacy_hardware_dir)/wifi.h \
    $(pdk_camera_dir)/CameraHardwareInterface.h \
    $(pdk_hardware_dir)/sensors.h \
    $(pdk_hardware_dir)/lights.h

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
  $(eval _chTo :=  $(pdk_app_eng_root)/$(patsubst $(pdk_templates_dir)/%,%,$(template))) \
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

# All the files that we depend upon
all_pdk_docs_files := $(pdk_doxygen_config_override_file) \
    $(pdk_doxygen_config_file) $(pdk_docs_intermediates)/header.html \
    $(pdk_docs_intermediates)/footer.html $(pdk_doxy_docsfiles_dir)/groups.dox \
    $(pdk_doxy_docsfiles_dir)/main.dox all_copied_pdk_templates  \
    all_copied_pdk_headers

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
	# Fix a doxygen bug: in *-source.html file insert '</div>\n' after line 25
	# @$(pdk_hosting_dir)/edoxfix.sh $(pdk_generated_source_dir)
	@cp -fp $(pdk_generated_source_dir)/* $(pdk_docs_dest_dir)
	@rm $(pdk_generated_source_dir)/*


# ==== docs for the web (on the google app engine server) =======================
# Run javadoc/droiddoc/clearsilver to get the formatting right

# make droiddocs run after we make our doxygen docs
$(pdk_docs_intermediates)/pdk-timestamp: pdk_doxygen all_copied_pdk_templates
	@touch $(pdk_docs_intermediates)/pdk-timestamp

$(LOCAL_PATH)/pdk-timestamp: $(pdk_docs_intermediates)/pdk-timestamp

include $(CLEAR_VARS)

LOCAL_SRC_FILES := pdk-timestamp samples/samplejni/src/com/example/jniexample/JNIExample.java  
LOCAL_MODULE_CLASS := development/pdk/pndk/samples/samplejni/src/com/example/jniexample
LOCAL_DROIDDOC_HTML_DIR := ../../../$(pdk_app_eng_root)

LOCAL_MODULE := online-pdk

LOCAL_DROIDDOC_OPTIONS:= \
		-toroot / \
		-hdf android.whichdoc online \
		-hdf android.whichmodule $(LOCAL_MODULE)

LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR := build/tools/droiddoc/templates-pdk
LOCAL_DROIDDOC_CUSTOM_ASSET_DIR := assets

include $(BUILD_DROIDDOC)

# The docs output dir is:  out/target/common/docs/online-pdk
DOCS_OUT_DIR  := $(OUT_DOCS)/$(LOCAL_MODULE)

# Copy appengine server files for new system
$(OUT_DOCS)/app.yaml: $(pdk_hosting_dir)/app.yaml
	@echo "PDK: $@"
	$(copy-file-to-target-with-cp)

# Name the tar files
name := android_pdk_docs-$(REQUESTED_PRODUCT)
ifeq ($(TARGET_BUILD_TYPE),debug)
  name := $(name)_debug
endif
name := $(name)-$(BUILD_NUMBER)
pdk_docs_tarfile := $(pdk_docs_intermediates)/$(name).tar
pdk_docs_tarfile_zipped := $(pdk_docs_tarfile).gz

.PHONY: pdk pdk_docs pdk_doxygen all_copied_pdk_headers all_copied_pdk_templates pdk-timestamp

pdk_docs: $(pdk_docs_tarfile_zipped) $(pdk_docs_tarfile)
	@echo "PDK: Docs tarred and zipped"

# Put the pdk_docs zip files in the distribution directory
$(call dist-for-goals,pdk_docs,$(pdk_docs_tarfile_zipped))

# zip up tar files
%.tar.gz: %.tar
	@echo "PDK docs: zipped $<"
	$(hide) gzip -cf $< > $@

# tar up all the files to make the pdk docs.
$(pdk_docs_tarfile): $(DOCS_OUT_DIR)-timestamp $(OUT_DOCS)/app.yaml
	@echo "PDK docs: $@"
	@mkdir -p $(dir $@)
	@rm -f $@
	$(hide) tar rf $@ -C $(OUT_DOCS) $(LOCAL_MODULE) app.yaml

# Debugging reporting can go here, add it as a target to get output.
pdk_debug:
	@echo "You are here: $@"
	@echo "pdk headers copied: $(all_copied_pdk_headers)"
	@echo "pdk headers: $(pdk_headers)"
	@echo "pdk docs dest: $(pdk_docs_dest_dir)"
	@echo "config dest: $(pdk_doxygen_config_file)"
	@echo "config src: $(pdk_config_dir)/pdk_config.conf"
	@echo "pdk templates: $(pdk_templates_dir)"
