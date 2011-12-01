# Makefile to build the SDK repository packages.

.PHONY: sdk_repo

SDK_REPO_DEPS :=
SDK_REPO_XML_ARGS :=

# Define the name of a package zip file to generate
# $1=OS (e.g. linux-x86, windows, etc)
# $2=sdk zip (e.g. out/host/linux.../android-eng-sdk.zip)
# $3=package to create (e.g. tools, docs, etc.)
#
define sdk-repo-pkg-zip
$(dir $(2))/sdk-repo-$(1)-$(3)-$(FILE_NAME_TAG).zip
endef

# Defines the rule to build an SDK repository package by zipping all
# the content of the given directory.
# E.g. given a folder out/host/linux.../sdk/android-eng-sdk/tools
# this generates an sdk-repo-linux-tools that contains tools/*
#
# $1=OS (e.g. linux-x86, windows, etc)
# $2=sdk zip (e.g. out/host/linux.../android-eng-sdk.zip)
# $3=package to create (e.g. tools, docs, etc.)
#
# The rule depends on the SDK zip file, which is defined by $2.
#
define mk-sdk-repo-pkg-1
$(call sdk-repo-pkg-zip,$(1),$(2),$(3)): $(2)
	@echo "Building SDK repository package $(3) from $(notdir $(2))"
	$(hide) cd $(basename $(2)) && \
			zip -9rq ../$(notdir $(call sdk-repo-pkg-zip,$(1),$(2),$(3))) $(3)/*
$(call dist-for-goals, sdk_repo, $(call sdk-repo-pkg-zip,$(1),$(2),$(3)))
SDK_REPO_XML_ARGS += $(3) $(1) \
	$(call sdk-repo-pkg-zip,$(1),$(2),$(3)):$(notdir $(call sdk-repo-pkg-zip,$(1),$(2),$(3)))
endef

# Defines the rule to build an SDK repository package when the
# package directory contains a single platform-related inner directory.
# E.g. given a folder out/host/linux.../sdk/android-eng-sdk/samples/android-N
# this generates an sdk-repo-linux-samples that contains android-N/*
#
# $1=OS (e.g. linux-x86, windows, etc)
# $2=sdk zip (e.g. out/host/linux.../android-eng-sdk.zip)
# $3=package to create (e.g. platforms, samples, etc.)
#
# The rule depends on the SDK zip file, which is defined by $2.
#
define mk-sdk-repo-pkg-2
$(call sdk-repo-pkg-zip,$(1),$(2),$(3)): $(2)
	@echo "Building SDK repository package $(3) from $(notdir $(2))"
	$(hide) cd $(basename $(2))/$(3) && \
			zip -9rq ../../$(notdir $(call sdk-repo-pkg-zip,$(1),$(2),$(3))) *
$(call dist-for-goals, sdk_repo, $(call sdk-repo-pkg-zip,$(1),$(2),$(3)))
SDK_REPO_XML_ARGS += $(3) $(1) \
	$(call sdk-repo-pkg-zip,$(1),$(2),$(3)):$(notdir $(call sdk-repo-pkg-zip,$(1),$(2),$(3)))
endef

# Defines the rule to build an SDK repository package when the
# package directory contains 3 levels from the sdk dir, for example
# to package SDK/extra/android/support or SDK/system-images/android-N/armeabi.
# Because we do not know the intermediary directory name, this only works
# if each directory contains a single sub-directory (e.g. sdk/$4/*/* must be
# unique.)
#
# $1=OS (e.g. linux-x86, windows, etc)
# $2=sdk zip (e.g. out/host/linux.../android-eng-sdk.zip)
# $3=package to create (e.g. system-images, support, etc.)
# $4=the root of directory to package in the sdk (e.g. extra/android).
#    this must be a 2-segment path, the last one can be *.
#
# The rule depends on the SDK zip file, which is defined by $2.
#
define mk-sdk-repo-pkg-3
$(call sdk-repo-pkg-zip,$(1),$(2),$(3)): $(2)
	@echo "Building SDK repository package $(3) from $(notdir $(2))"
	$(hide) cd $(basename $(2))/$(4) && \
			zip -9rq ../../../$(notdir $(call sdk-repo-pkg-zip,$(1),$(2),$(3))) *
$(call dist-for-goals, sdk_repo, $(call sdk-repo-pkg-zip,$(1),$(2),$(3)))
SDK_REPO_XML_ARGS += $(3) $(1) \
	$(call sdk-repo-pkg-zip,$(1),$(2),$(3)):$(notdir $(call sdk-repo-pkg-zip,$(1),$(2),$(3)))
endef

# Defines the rule to build an SDK sources package.
#
# $1=OS (e.g. linux-x86, windows, etc)
# $2=sdk zip (e.g. out/host/linux.../android-eng-sdk.zip)
# $3=package to create, must be "sources"
#
define mk-sdk-repo-sources
$(call sdk-repo-pkg-zip,$(1),$(2),$(3)): $(2) $(TOPDIR)development/sdk/source_source.properties
	@echo "Building SDK sources package"
	$(hide) $(TOPDIR)development/build/tools/mk_sources_zip.py --exec-zip \
			$(TOPDIR)development/sdk/source_source.properties \
			$(call sdk-repo-pkg-zip,$(1),$(2),$(3)) \
			$(TOPDIR).
$(call dist-for-goals, sdk_repo, $(call sdk-repo-pkg-zip,$(1),$(2),$(3)))
SDK_REPO_XML_ARGS += $(3) $(1) \
	$(call sdk-repo-pkg-zip,$(1),$(2),$(3)):$(notdir $(call sdk-repo-pkg-zip,$(1),$(2),$(3)))
endef

# -----------------------------------------------------------------
# Rules for win_sdk

ifneq ($(WIN_SDK_ZIP),)

# docs, platforms and samples have nothing OS-dependent right now.
$(eval $(call mk-sdk-repo-pkg-1,windows,$(WIN_SDK_ZIP),tools))
$(eval $(call mk-sdk-repo-pkg-1,windows,$(WIN_SDK_ZIP),platform-tools))

SDK_REPO_DEPS += \
	$(call sdk-repo-pkg-zip,windows,$(WIN_SDK_ZIP),tools) \
        $(call sdk-repo-pkg-zip,windows,$(WIN_SDK_ZIP),platform-tools)

endif

# -----------------------------------------------------------------
# Rules for main host sdk

ifneq ($(filter sdk win_sdk,$(MAKECMDGOALS)),)

$(eval $(call mk-sdk-repo-pkg-1,$(HOST_OS),$(MAIN_SDK_ZIP),tools))
$(eval $(call mk-sdk-repo-pkg-1,$(HOST_OS),$(MAIN_SDK_ZIP),platform-tools))
$(eval $(call mk-sdk-repo-pkg-1,$(HOST_OS),$(MAIN_SDK_ZIP),docs))
$(eval $(call mk-sdk-repo-pkg-2,$(HOST_OS),$(MAIN_SDK_ZIP),platforms))
$(eval $(call mk-sdk-repo-pkg-2,$(HOST_OS),$(MAIN_SDK_ZIP),samples))
$(eval $(call mk-sdk-repo-pkg-3,$(HOST_OS),$(MAIN_SDK_ZIP),system-images,system-images/*))
$(eval $(call mk-sdk-repo-pkg-3,$(HOST_OS),$(MAIN_SDK_ZIP),support,extras/android))
$(eval $(call mk-sdk-repo-sources,$(HOST_OS),$(MAIN_SDK_ZIP),sources))

SDK_REPO_DEPS += \
		$(call sdk-repo-pkg-zip,$(HOST_OS),$(MAIN_SDK_ZIP),tools) \
		$(call sdk-repo-pkg-zip,$(HOST_OS),$(MAIN_SDK_ZIP),platform-tools) \
		$(call sdk-repo-pkg-zip,$(HOST_OS),$(MAIN_SDK_ZIP),docs) \
		$(call sdk-repo-pkg-zip,$(HOST_OS),$(MAIN_SDK_ZIP),platforms) \
		$(call sdk-repo-pkg-zip,$(HOST_OS),$(MAIN_SDK_ZIP),samples) \
		$(call sdk-repo-pkg-zip,$(HOST_OS),$(MAIN_SDK_ZIP),system-images) \
		$(call sdk-repo-pkg-zip,$(HOST_OS),$(MAIN_SDK_ZIP),support) \
		$(call sdk-repo-pkg-zip,$(HOST_OS),$(MAIN_SDK_ZIP),sources)

endif

# -----------------------------------------------------------------
# Rules for sdk addon

ifneq ($(ADDON_SDK_ZIP),)

# ADDON_SDK_ZIP is defined in build/core/tasks/sdk-addon.sh and is
# already packaged correctly. All we have to do is dist it with
# a different destination name.

RENAMED_ADDON_ZIP := $(ADDON_SDK_ZIP):$(notdir $(call sdk-repo-pkg-zip,$(HOST_OS),$(ADDON_SDK_ZIP),addon))

$(call dist-for-goals, sdk_repo, $(RENAMED_ADDON_ZIP))

# Also generate the addon.xml using the latest schema and the renamed addon zip

SDK_ADDON_XML := $(dir $(ADDON_SDK_ZIP))/addon.xml

SDK_ADDON_XSD := \
	$(lastword \
	  $(wildcard \
	    $(TOPDIR)sdk/sdkmanager/libs/sdklib/src/com/android/sdklib/repository/sdk-addon-*.xsd \
	))

$(SDK_ADDON_XML): $(ADDON_SDK_ZIP)
	$(hide) $(TOPDIR)development/build/tools/mk_sdk_repo_xml.sh \
		$(SDK_ADDON_XML) $(SDK_ADDON_XSD) add-on $(HOST_OS) $(RENAMED_ADDON_ZIP)

$(call dist-for-goals, sdk_repo, $(SDK_ADDON_XML))

endif

# -----------------------------------------------------------------
# Rules for the SDK Repository XML

SDK_REPO_XML := $(HOST_OUT)/sdk/repository.xml

ifneq ($(SDK_REPO_XML_ARGS),)

# Pickup the most recent xml schema
SDK_REPO_XSD := \
	$(lastword \
	  $(wildcard \
	    $(TOPDIR)sdk/sdkmanager/libs/sdklib/src/com/android/sdklib/repository/sdk-repository-*.xsd \
	))

$(SDK_REPO_XML): $(SDK_REPO_DEPS)
	$(hide) $(TOPDIR)development/build/tools/mk_sdk_repo_xml.sh \
		$(SDK_REPO_XML) $(SDK_REPO_XSD) $(SDK_REPO_XML_ARGS)

$(call dist-for-goals, sdk_repo, $(SDK_REPO_XML))

else

$(SDK_REPO_XML): ;

endif

# -----------------------------------------------------------------

sdk_repo: $(SDK_REPO_DEPS) $(SDK_REPO_XML)
	@echo "Packing of SDK repository done"

