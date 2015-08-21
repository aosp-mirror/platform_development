LOCAL_PATH := $(call my-dir)

# ===== SDK source.property files =====

# Add all files to be generated from the source.prop templates to the SDK pre-requisites
ALL_SDK_FILES += $(patsubst \
                   $(TOPDIR)development/sdk/%_source.prop_template, \
                   $(HOST_OUT)/development/sdk/%_source.properties, \
                   $(wildcard $(TOPDIR)development/sdk/*_source.prop_template)) \
                 $(patsubst \
                   $(TOPDIR)development/samples/%_source.prop_template, \
                   $(HOST_OUT)/development/samples/%_source.properties, \
                   $(wildcard $(TOPDIR)development/samples/*_source.prop_template)) \
                 $(patsubst \
                   $(TOPDIR)development/sys-img/%_source.prop_template, \
                   $(HOST_OUT)/development/sys-img-$(TARGET_CPU_ABI)/%_source.properties, \
                   $(wildcard $(TOPDIR)development/sys-img/*_source.prop_template))

# Rule to convert a source.prop template into the desired source.property
# This needs to vary based on the CPU ABI for the system-image files.
# Rewritten variables:
# - ${PLATFORM_VERSION}          e.g. "1.0"
# - ${PLATFORM_SDK_VERSION}      e.g. "3", aka the API level
# - ${PLATFORM_VERSION_CODENAME} e.g. "REL" (transformed into "") or "Cupcake"
# - ${TARGET_ARCH}               e.g. "arm", "x86", "mips" and their 64-bit variants.
# - ${TARGET_CPU_ABI}            e.g. "armeabi", "x86", "mips" and their 64-bit variants.
$(HOST_OUT)/development/sys-img-$(TARGET_CPU_ABI)/%_source.properties : $(TOPDIR)development/sys-img/%_source.prop_template
	@echo Generate $@
	$(hide) mkdir -p $(dir $@)
	$(hide) sed \
		-e 's/$${PLATFORM_VERSION}/$(PLATFORM_VERSION)/' \
		-e 's/$${PLATFORM_SDK_VERSION}/$(PLATFORM_SDK_VERSION)/' \
		-e 's/$${PLATFORM_VERSION_CODENAME}/$(subst REL,,$(PLATFORM_VERSION_CODENAME))/' \
		-e 's/$${TARGET_ARCH}/$(TARGET_ARCH)/' \
		-e 's/$${TARGET_CPU_ABI}/$(TARGET_CPU_ABI)/' \
		$< > $@ && sed -i -e '/^AndroidVersion.CodeName=\s*$$/d' $@

$(HOST_OUT)/development/sdk/%_source.properties : $(TOPDIR)development/sdk/%_source.prop_template
	@echo Generate $@
	$(hide) mkdir -p $(dir $@)
	$(hide) sed \
		-e 's/$${PLATFORM_VERSION}/$(PLATFORM_VERSION)/' \
		-e 's/$${PLATFORM_SDK_VERSION}/$(PLATFORM_SDK_VERSION)/' \
		-e 's/$${PLATFORM_VERSION_CODENAME}/$(subst REL,,$(PLATFORM_VERSION_CODENAME))/' \
		$< > $@ && sed -i -e '/^AndroidVersion.CodeName=\s*$$/d' $@

$(HOST_OUT)/development/samples/%_source.properties : $(TOPDIR)development/samples/%_source.prop_template
	@echo Generate $@
	$(hide) mkdir -p $(dir $@)
	$(hide) sed\
		-e 's/$${PLATFORM_VERSION}/$(PLATFORM_VERSION)/' \
		-e 's/$${PLATFORM_SDK_VERSION}/$(PLATFORM_SDK_VERSION)/' \
		-e 's/$${PLATFORM_VERSION_CODENAME}/$(subst REL,,$(PLATFORM_VERSION_CODENAME))/' \
		$< > $@ && sed -i -e '/^AndroidVersion.CodeName=\s*$$/d' $@


# ===== SDK jar file of stubs =====
# A.k.a the "current" version of the public SDK (android.jar inside the SDK package).
sdk_stub_name := android_stubs_current
stub_timestamp := $(OUT_DOCS)/api-stubs-timestamp
include $(LOCAL_PATH)/build_android_stubs.mk

.PHONY: android_stubs
android_stubs: $(full_target)

# The real rules create a javalib.jar that contains a classes.dex file.  This
# code is never going to be run anywhere, so just make a copy of the file.
# The package installation stuff doesn't know about this file, so nobody will
# ever be able to write a rule that installs it to a device.
$(dir $(full_target))javalib.jar: $(full_target)
	$(hide)$(ACP) $< $@

# android.jar is what we put in the SDK package.
android_jar_intermediates := $(TARGET_OUT_COMMON_INTERMEDIATES)/PACKAGING/android_jar_intermediates
android_jar_full_target := $(android_jar_intermediates)/android.jar

$(android_jar_full_target): $(full_target)
	@echo Package SDK Stubs: $@
	$(hide)mkdir -p $(dir $@)
	$(hide)$(ACP) $< $@

ALL_SDK_FILES += $(android_jar_full_target)

# ====================================================

# The uiautomator stubs
ALL_SDK_FILES += $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/android_uiautomator_intermediates/javalib.jar

# org.apache.http.legacy.jar stubs
ALL_SDK_FILES += $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/org.apache.http.legacy_intermediates/javalib.jar

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
    android-support-annotations \
    android-support-v4 \
    android-support-v7-appcompat \
    android-support-v7-cardview \
    android-support-v7-gridlayout \
    android-support-v7-mediarouter \
    android-support-v7-palette \
    android-support-v7-recyclerview \
    android-support-v13 \
    android-support-v17-leanback \
    android-support-multidex \
    android-support-multidex-instrumentation \
    android-support-design \
    android-support-percent \
    android-support-customtabs

$(foreach lib, $(ANDROID_SUPPORT_LIBRARIES), $(eval $(call _package_sdk_library,$(lib))))

# ======= Lint API XML ===========

ALL_SDK_FILES += $(HOST_OUT)/development/sdk/generated-api-versions.xml

api_gen_jar := $(TOPDIR)prebuilts/tools/common/api-generator/api-generator-22.9.3.jar
api_gen_deps := \
  $(TOPDIR)prebuilts/tools/common/kxml2-tools/kxml2-2.3.0.jar \
  $(TOPDIR)prebuilts/tools/common/asm-tools/asm-4.0.jar \
  $(TOPDIR)prebuilts/tools/common/asm-tools/asm-tree-4.0.jar \
  $(TOPDIR)prebuilts/devtools/tools/lib/common.jar
api_gen_classpath := $(subst $(space),:,$(api_gen_jar) $(api_gen_deps))


$(HOST_OUT)/development/sdk/generated-api-versions.xml: $(android_jar_full_target)
	java -cp $(api_gen_classpath) \
	  com.android.apigenerator.Main \
	  --pattern $(TOPDIR)prebuilts/tools/common/api-versions/android-%/android.jar \
	  --pattern $(TOPDIR)prebuilts/sdk/%/android.jar \
	  --current-version $(PLATFORM_SDK_VERSION) \
	  --current-codename $(PLATFORM_VERSION_CODENAME) \
	  --current-jar $(android_jar_full_target) \
	  $@


# ============ System SDK ============
sdk_stub_name := android_system_stubs_current
stub_timestamp := $(OUT_DOCS)/system-api-stubs-timestamp
include $(LOCAL_PATH)/build_android_stubs.mk

.PHONY: android_system_stubs
android_system_stubs: $(full_target)

# Build and store the android_system.jar.
$(call dist-for-goals,sdk win_sdk,$(full_target):android_system.jar)
