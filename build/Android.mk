# ===== SDK source.property files =====

# Add all files to be generated from the source.prop templates to the SDK pre-requisites
ALL_SDK_FILES += $(patsubst \
                   $(TOPDIR)development/sdk/%_source.prop_template, \
                   $(HOST_OUT)/development/sdk/%_source.properties, \
                   $(wildcard $(TOPDIR)development/sdk/*_source.prop_template)) \
                 $(patsubst \
                   $(TOPDIR)development/samples/%_source.prop_template, \
                   $(HOST_OUT)/development/samples/%_source.properties, \
                   $(wildcard $(TOPDIR)development/samples/*_source.prop_template))

# Rule to convert a source.prop template into the desired source.property
# Rewritten variables:
# - ${PLATFORM_VERSION}          e.g. "1.0"
# - ${PLATFORM_SDK_VERSION}      e.g. "3", aka the API level
# - ${PLATFORM_VERSION_CODENAME} e.g. "REL" (transformed into "") or "Cupcake"
$(HOST_OUT)/development/sdk/%_source.properties : $(TOPDIR)development/sdk/%_source.prop_template
	@echo Generate $@
	$(hide) mkdir -p $(dir $@)
	$(hide) sed -e 's/$${PLATFORM_VERSION}/$(PLATFORM_VERSION)/' \
		 -e 's/$${PLATFORM_SDK_VERSION}/$(PLATFORM_SDK_VERSION)/' \
		 -e 's/$${PLATFORM_VERSION_CODENAME}/$(subst REL,,$(PLATFORM_VERSION_CODENAME))/' \
		 $< > $@ && sed -i -e '/^AndroidVersion.CodeName=\s*$$/d' $@

$(HOST_OUT)/development/samples/%_source.properties : $(TOPDIR)development/samples/%_source.prop_template
	@echo Generate $@
	$(hide) mkdir -p $(dir $@)
	$(hide) sed -e 's/$${PLATFORM_VERSION}/$(PLATFORM_VERSION)/' \
		 -e 's/$${PLATFORM_SDK_VERSION}/$(PLATFORM_SDK_VERSION)/' \
		 -e 's/$${PLATFORM_VERSION_CODENAME}/$(subst REL,,$(PLATFORM_VERSION_CODENAME))/' \
		 $< > $@ && sed -i -e '/^AndroidVersion.CodeName=\s*$$/d' $@


# ===== Android Support/Compatibility Library =====

LOCAL_PATH := $(call my-dir)

# The source files for this library are _all_ generated, something we don't do
# anywhere else, and the rules don't support.  Aditionally, the depenencies on
# these files don't really matter, because they are all generated as part of
# building the docs.  So for the dependency, we just use the
# api-stubs-timestamp file, which is the $@ of the droiddoc rule.
# We also need to depend on framework-res.apk, in order to pull the
# resource files out of there for aapt.
#
# Normally the package rule runs aapt, which includes the resource,
# but we're not running that in our package rule so just copy in the
# resource files here.
intermediates := $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/android_stubs_current_intermediates
full_target := $(intermediates)/classes.jar
src_dir := $(intermediates)/src
classes_dir := $(intermediates)/classes
framework_res_package := $(call intermediates-dir-for,APPS,framework-res,,COMMON)/package-export.apk

$(full_target): PRIVATE_SRC_DIR := $(src_dir)
$(full_target): PRIVATE_INTERMEDIATES_DIR := $(intermediates)
$(full_target): PRIVATE_CLASS_INTERMEDIATES_DIR := $(classes_dir)
$(full_target): PRIVATE_FRAMEWORK_RES_PACKAGE := $(framework_res_package)

$(full_target): $(OUT_DOCS)/api-stubs-timestamp $(framework_res_package)
	@echo Compiling SDK Stubs: $@
	$(hide) rm -rf $(PRIVATE_CLASS_INTERMEDIATES_DIR)
	$(hide) mkdir -p $(PRIVATE_CLASS_INTERMEDIATES_DIR)
	$(hide) find $(PRIVATE_SRC_DIR) -name "*.java" > \
        $(PRIVATE_INTERMEDIATES_DIR)/java-source-list
	$(hide) $(TARGET_JAVAC) -encoding ascii -bootclasspath "" \
			-g $(xlint_unchecked) \
			-extdirs "" -d $(PRIVATE_CLASS_INTERMEDIATES_DIR) \
			\@$(PRIVATE_INTERMEDIATES_DIR)/java-source-list \
		|| ( rm -rf $(PRIVATE_CLASS_INTERMEDIATES_DIR) ; exit 41 )
	$(hide) if [ ! -f $(PRIVATE_FRAMEWORK_RES_PACKAGE) ]; then \
		echo Missing file $(PRIVATE_FRAMEWORK_RES_PACKAGE); \
		rm -rf $(PRIVATE_CLASS_INTERMEDIATES_DIR); \
		exit 1; \
	fi;
	$(hide) unzip -qo $(PRIVATE_FRAMEWORK_RES_PACKAGE) -d $(PRIVATE_CLASS_INTERMEDIATES_DIR)
	$(hide) (cd $(PRIVATE_CLASS_INTERMEDIATES_DIR) && rm -rf classes.dex META-INF)
	$(hide) mkdir -p $(dir $@)
	$(hide) jar -cf $@ -C $(PRIVATE_CLASS_INTERMEDIATES_DIR) .
	$(hide) jar -u0f $@ -C $(PRIVATE_CLASS_INTERMEDIATES_DIR) resources.arsc

.PHONY: android_stubs
android_stubs: $(full_target)

# The real rules create a javalib.jar that contains a classes.dex file.  This
# code is never going to be run anywhere, so just make a copy of the file.
# The package installation stuff doesn't know about this file, so nobody will
# ever be able to write a rule that installs it to a device.
$(dir $(full_target))javalib.jar: $(full_target)
	$(hide)$(ACP) $< $@


android_jar_intermediates := $(TARGET_OUT_COMMON_INTERMEDIATES)/PACKAGING/android_jar_intermediates
android_jar_full_target := $(android_jar_intermediates)/android.jar

$(android_jar_full_target): $(full_target)
	@echo Package SDK Stubs: $@
	$(hide)mkdir -p $(dir $@)
	$(hide)$(ACP) $< $@

ALL_SDK_FILES += $(android_jar_full_target)

# The uiautomator stubs
ALL_SDK_FILES += $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/android_uiautomator_intermediates/javalib.jar

# $(1): the Java library name
define _package_sdk_library
$(eval _psm_build_module := $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/$(1)_intermediates/javalib.jar)
$(eval _psm_packaging_target := $(TARGET_OUT_COMMON_INTERMEDIATES)/PACKAGING/$(1)_intermediates/$(1).jar)
$(_psm_packaging_target) : $(_psm_build_module) | $(ACP)
	@echo "Package $(1).jar: $$@"
	$(hide) mkdir -p $$(dir $$@)
	$(hide) $(ACP) $$< $$@

ALL_SDK_FILES += $(_psm_packaging_target)
$(eval _psm_build_module :=)
$(eval _psm_packaging_target :=)
endef

ANDROID_SUPPORT_LIBRARIES := \
    android-support-v4 \
    android-support-v7-gridlayout \
    android-support-v7-appcompat \
    android-support-v7-mediarouter \
    android-support-v13

$(foreach lib, $(ANDROID_SUPPORT_LIBRARIES), $(eval $(call _package_sdk_library,$(lib))))
