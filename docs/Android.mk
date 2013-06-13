# This makefile builds the online developer site, gms javadocs, and gcm javadocs.
# It does the following things, which should be ran in order:
#
# 1. Builds the GMS Javadocs from the directory specified by GMS_CORE_PATH. (online-gms-ref-docs)
# 2. Renames, deletes and modifies files to clean the GMS Core build up (setup-gms-ref)
# 3. Builds the GCM Javadocs from vendor/unbundled_google/libs/gcm (online-gcm-ref-docs)
# 4. Renames, deletes, and modifies files to clean the GCM build up (setup-gcm-ref)
# 5. Builds the developer site with 'make online-sdk-docs' (online-sdk-docs)
# 6. Merges the GMS, GCM, and developer site together in out/target/common/docs/online-sdk
#    for staging (stage-gms-ref, stage-gcm-ref)
#
# Note: You can run all the targets individually or call 'make all-docs'

# If the staging server looks good:
# 1. Run 'make add-gms-ref add-gcm-ref' to copy the appropriate files to the right locations.
# This copies the GCM and GMS output from their respective out/target/common/docs dirs
# to frameworks/base/docs/html/reference of the branch you ran this in.
# 2. Run 'repo status' in frameworks/base to see all the modified files
# 3. Run 'git add' and 'git commit' as normal.

####################### GCM Javadocs ########################

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
GMS_CORE_PATH:=../froyo-ub-gcore-cheddar-release
GMS_LOCAL_MODULE:=google-play-services-dac
GCM_LOCAL_MODULE:=online-gcm-ref
GCM_PATH=../../vendor/unbundled_google/libs/gcm

LOCAL_MODULE_CLASS=DOCS
gcm_docs_src_files += \
        $(call all-java-files-under, $(GCM_PATH)/gcm-client/src) \
        $(call all-java-files-under, $(GCM_PATH)/gcm-server/src)
LOCAL_SRC_FILES := $(gcm_docs_src_files)
LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR:=build/tools/droiddoc/templates-sdk
LOCAL_MODULE := online-gcm-ref
LOCAL_DROIDDOC_OPTIONS:= \
        -toroot / \
        -gcmref \
        -hdf android.whichdoc online \

include $(BUILD_DROIDDOC)

.PHONY: build-stage-docs online-gms-ref-docs setup-gms-ref online-gcm-ref-docs setup-gcm-ref online-sdk-docs stage-gms-ref stage-gcm-ref

clean-all:
# Deletes the previous GCM Javadoc build
	$(hide) rm -rf $(GCM_PATH)/$(OUT_DOCS)/$(GCM_LOCAL_MODULE)
	$(hide) rm -f $(GCM_PATH)/$(OUT_DOCS)/$(GCM_LOCAL_MODULE)-timestamp
# Deletes the previous GMS Core Javadoc build
	$(hide) rm -rf $(GMS_CORE_PATH)/$(OUT_DOCS)/$(GMS_LOCAL_MODULE)
	$(hide) rm -f $(GMS_CORE_PATH)/$(OUT_DOCS)/$(GMS_LOCAL_MODULE)-timestamp
# Deletes the previous online-sdk build
	$(hide) rm -rf $(OUT_DOCS)/online-sdk
	$(hide) rm -f $(OUT_DOCS)/online-sdk-timestamp


# Cleanup the GCM build output
setup-gcm-ref:
	$(hide) rm -f $(OUT_DOCS)/$(GCM_LOCAL_MODULE)/timestamp.js \
	$(OUT_DOCS)/$(GCM_LOCAL_MODULE)/reference/hierarchy.html \
	$(OUT_DOCS)/$(GCM_LOCAL_MODULE)/reference/classes.html \
	$(OUT_DOCS)/$(GCM_LOCAL_MODULE)/reference/package-list \
	$(OUT_DOCS)/$(GCM_LOCAL_MODULE)/reference/index.html

	$(hide) sed 's/DATA/GCM_DATA/' $(OUT_DOCS)/$(GCM_LOCAL_MODULE)/reference/lists.js > \
	$(OUT_DOCS)/$(GCM_LOCAL_MODULE)/reference/gcm_lists.js
	$(hide) rm -f $(OUT_DOCS)/$(GCM_LOCAL_MODULE)/reference/lists.js

# Copies the output of the GCM build to the online-sdk out directory to view
# in staging
stage-gcm-ref:
	$(hide) cp -R $(OUT_DOCS)/$(GCM_LOCAL_MODULE)/reference \
	$(OUT_DOCS)/online-sdk

	$(hide) cp $(OUT_DOCS)/$(GCM_LOCAL_MODULE)/gcm_navtree_data.js \
	$(OUT_DOCS)/online-sdk/gcm_navtree_data.js

# Copies the output of the GCM build to the appropriate location in the repo
# for checking into Git
add-gcm-ref:
	$(hide) cp -R $(OUT_DOCS)/$(GCM_LOCAL_MODULE)/reference \
	frameworks/base/docs/html

	$(hide) cp $(OUT_DOCS)/$(GCM_LOCAL_MODULE)/gcm_navtree_data.js \
	frameworks/base/docs/html/gcm_navtree_data.js

####################### GMS Javadocs ########################

# Build the GMS Core Javadocs
online-gms-ref-docs:
# Copies the google_toc.cs to the GMS core branch so the javadocs build
# with the left navigation.
	$(hide) mkdir -p $(GMS_CORE_PATH)/frameworks/base/docs/html/google
	$(hide) cp frameworks/base/docs/html/google/google_toc.cs \
	$(GMS_CORE_PATH)/frameworks/base/docs/html/google

# Change to GMS core directory to run the build properly
# The ; and \ allow the commands to be run from GMS_CORE_PATH by chaining them
# If not, they will be run from wherever make was ran
	$(hide) cd $(GMS_CORE_PATH); \
	source build/envsetup.sh; \
	tapas GmsCore; \
	make google-play-services-dac-docs

# Cleanup the GMS Core build output
setup-gms-ref:
# Remove unneeded files
	$(hide) rm -f $(GMS_CORE_PATH)/$(OUT_DOCS)/$(GMS_LOCAL_MODULE)/timestamp.js \
	$(GMS_CORE_PATH)/$(OUT_DOCS)/$(GMS_LOCAL_MODULE)/reference/hierarchy.html \
	$(GMS_CORE_PATH)/$(OUT_DOCS)/$(GMS_LOCAL_MODULE)/reference/classes.html \
	$(GMS_CORE_PATH)/$(OUT_DOCS)/$(GMS_LOCAL_MODULE)/reference/package-list \
	$(GMS_CORE_PATH)/$(OUT_DOCS)/$(GMS_LOCAL_MODULE)/reference/index.html
# Rename files
	$(hide) sed 's/DATA/GMS_DATA/' $(GMS_CORE_PATH)/$(OUT_DOCS)/$(GMS_LOCAL_MODULE)/reference/lists.js > \
	$(GMS_CORE_PATH)/$(OUT_DOCS)/$(GMS_LOCAL_MODULE)/reference/gms_lists.js
	$(hide) rm -f $(GMS_CORE_PATH)/$(OUT_DOCS)/$(GMS_LOCAL_MODULE)/reference/lists.js

# Copies the output of the GMS Core build to the online-sdk out directory to view
# in staging
stage-gms-ref:
	$(hide) cp -R $(GMS_CORE_PATH)/$(OUT_DOCS)/$(GMS_LOCAL_MODULE)/reference \
	$(OUT_DOCS)/online-sdk

	$(hide) cp $(GMS_CORE_PATH)/$(OUT_DOCS)/$(GMS_LOCAL_MODULE)/gms_navtree_data.js \
	$(OUT_DOCS)/online-sdk/gms_navtree_data.js

# Copies the output of the GMS Core build to the appropriate location in the repo
# for checking into Git
add-gms-ref:
	$(hide) cp -R $(GMS_CORE_PATH)/$(OUT_DOCS)/$(GMS_LOCAL_MODULE)/reference \
	frameworks/base/docs/html

	$(hide) cp $(GMS_CORE_PATH)/$(OUT_DOCS)/$(GMS_LOCAL_MODULE)/gms_navtree_data.js \
	frameworks/base/docs/html/gms_navtree_data.js

# Builds the gms and gcm javadocs, then online-sdk docs, then copies the gms and gcm
# Javadocs over to the online-sdk out directory so you can stage the results.
# If all is good, just run "make add-gcm-ref add-gms-ref" to move the required
# files over to frameworks/base to do a git add/commit.
all-docs: clean-all online-gms-ref-docs setup-gms-ref online-gcm-ref-docs setup-gcm-ref online-sdk-docs stage-gms-ref stage-gcm-ref