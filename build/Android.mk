LOCAL_PATH := $(call my-dir)

# ===== SDK source.property files =====

# Add all files to be generated from the source.prop templates to the SDK pre-requisites
sdk_props := $(patsubst \
               $(TOPDIR)development/sdk/%_source.prop_template, \
               $(HOST_OUT)/development/sdk/%_source.properties, \
               $(wildcard $(TOPDIR)development/sdk/*_source.prop_template))
sample_props := $(patsubst \
                  $(TOPDIR)development/samples/%_source.prop_template, \
                  $(HOST_OUT)/development/samples/%_source.properties, \
                  $(wildcard $(TOPDIR)development/samples/*_source.prop_template))
sys_img_props := $(patsubst \
                   $(TOPDIR)development/sys-img/%_source.prop_template, \
                   $(HOST_OUT)/development/sys-img-$(TARGET_CPU_ABI)/%_source.properties, \
                   $(wildcard $(TOPDIR)development/sys-img/*_source.prop_template))
ALL_SDK_FILES += $(sdk_props) $(sample_props) $(sys_img_props)

# Rule to convert a source.prop template into the desired source.property
# This needs to vary based on the CPU ABI for the system-image files.
# Rewritten variables:
# - ${PLATFORM_VERSION}          e.g. "1.0"
# - ${PLATFORM_SDK_VERSION}      e.g. "3", aka the API level
# - ${PLATFORM_VERSION_CODENAME} e.g. "REL" (transformed into "") or "Cupcake"
# - ${TARGET_ARCH}               e.g. "arm", "x86", "mips" and their 64-bit variants.
# - ${TARGET_CPU_ABI}            e.g. "armeabi", "x86", "mips" and their 64-bit variants.
$(sys_img_props) : $(HOST_OUT)/development/sys-img-$(TARGET_CPU_ABI)/%_source.properties : $(TOPDIR)development/sys-img/%_source.prop_template
	@echo Generate $@
	$(hide) mkdir -p $(dir $@)
	$(hide) sed \
		-e 's/$${PLATFORM_VERSION}/$(PLATFORM_VERSION)/' \
		-e 's/$${PLATFORM_SDK_VERSION}/$(PLATFORM_SDK_VERSION)/' \
		-e 's/$${PLATFORM_VERSION_CODENAME}/$(subst REL,,$(PLATFORM_VERSION_CODENAME))/' \
		-e 's/$${TARGET_ARCH}/$(TARGET_ARCH)/' \
		-e 's/$${TARGET_CPU_ABI}/$(TARGET_CPU_ABI)/' \
		$< > $@ && sed -i -e '/^AndroidVersion.CodeName=\s*$$/d' $@

$(sdk_props) : $(HOST_OUT)/development/sdk/%_source.properties : $(TOPDIR)development/sdk/%_source.prop_template
	@echo Generate $@
	$(hide) mkdir -p $(dir $@)
	$(hide) sed \
		-e 's/$${PLATFORM_VERSION}/$(PLATFORM_VERSION)/' \
		-e 's/$${PLATFORM_SDK_VERSION}/$(PLATFORM_SDK_VERSION)/' \
		-e 's/$${PLATFORM_VERSION_CODENAME}/$(subst REL,,$(PLATFORM_VERSION_CODENAME))/' \
		$< > $@ && sed -i -e '/^AndroidVersion.CodeName=\s*$$/d' $@

$(sample_props) : $(HOST_OUT)/development/samples/%_source.properties : $(TOPDIR)development/samples/%_source.prop_template
	@echo Generate $@
	$(hide) mkdir -p $(dir $@)
	$(hide) sed\
		-e 's/$${PLATFORM_VERSION}/$(PLATFORM_VERSION)/' \
		-e 's/$${PLATFORM_SDK_VERSION}/$(PLATFORM_SDK_VERSION)/' \
		-e 's/$${PLATFORM_VERSION_CODENAME}/$(subst REL,,$(PLATFORM_VERSION_CODENAME))/' \
		$< > $@ && sed -i -e '/^AndroidVersion.CodeName=\s*$$/d' $@


# ===== SDK jar file of stubs =====
# A.k.a the "current" version of the public SDK (android.jar inside the SDK package).
full_target := $(call intermediates-dir-for,JAVA_LIBRARIES,android_stubs_current,,COMMON)/classes.jar
full_src_target := $(OUT_DOCS)/api-stubs-docs-stubs.srcjar

.PHONY: android_stubs
android_stubs: $(full_target) $(full_src_target)

# android.jar is what we put in the SDK package.
android_jar_intermediates := $(TARGET_OUT_COMMON_INTERMEDIATES)/PACKAGING/android_jar_intermediates
android_jar_full_target := $(android_jar_intermediates)/android.jar
android_jar_src_target := $(android_jar_intermediates)/android-stubs-src.jar

$(android_jar_full_target): $(full_target)
	@echo Package SDK Stubs: $@
	$(copy-file-to-target)

$(android_jar_src_target): $(full_src_target)
	@echo Package SDK Stubs Source: $@
	$(hide)mkdir -p $(dir $@)
	$(hide)$(ACP) $< $@

ALL_SDK_FILES += $(android_jar_full_target)
ALL_SDK_FILES += $(android_jar_src_target)

# ====================================================

# The uiautomator stubs
ALL_SDK_FILES += $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/android_uiautomator_intermediates/classes.jar

# org.apache.http.legacy.jar stubs
ALL_SDK_FILES += $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/org.apache.http.legacy.stubs_intermediates/classes.jar

# test stubs
ALL_SDK_FILES += $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/android.test.mock.stubs_intermediates/classes.jar
ALL_SDK_FILES += $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/android.test.base.stubs_intermediates/classes.jar
ALL_SDK_FILES += $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/android.test.runner.stubs_intermediates/classes.jar

# core-lambda-stubs
ALL_SDK_FILES += $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/core-lambda-stubs_intermediates/classes.jar

# shrinkedAndroid.jar for multidex support
ALL_SDK_FILES += $(HOST_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/shrinkedAndroid_intermediates/shrinkedAndroid.jar

# $(1): the Java library name
define _package_sdk_library
$(eval _psm_build_module := $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/$(1)_intermediates/classes.jar)
$(eval _psm_packaging_target := $(TARGET_OUT_COMMON_INTERMEDIATES)/PACKAGING/$(1)_intermediates/$(1).jar)
$(_psm_packaging_target) : $(_psm_build_module)
	@echo "Package $(1).jar: $$@"
	$$(copy-file-to-target)
	@# Delete resource generated classes from the jar files.
	$(hide) zip -d $$@ "*/R.class" "*/R\$$$$*.class" "*/Manifest.class" "*/Manifest\$$$$*.class" >/dev/null 2>&1 || true

ALL_SDK_FILES += $(_psm_packaging_target)
$(eval _psm_build_module :=)
$(eval _psm_packaging_target :=)
endef

# ======= Lint API XML ===========

ALL_SDK_FILES += $(HOST_OUT)/development/sdk/generated-api-versions.xml

api_gen_jar := $(TOPDIR)prebuilts/tools/common/api-generator/api-generator-26.3.0.jar
api_gen_deps := \
  $(TOPDIR)prebuilts/tools/common/m2/repository/net/sf/kxml/kxml2/2.3.0/kxml2-2.3.0.jar \
  $(TOPDIR)prebuilts/tools/common/m2/repository/org/ow2/asm/asm/5.0.4/asm-5.0.4.jar \
  $(TOPDIR)prebuilts/tools/common/m2/repository/org/ow2/asm/asm-tree/5.0.4/asm-tree-5.0.4.jar \
  $(TOPDIR)prebuilts/tools/common/m2/repository/com/google/guava/guava/17.0/guava-17.0.jar
api_gen_classpath := $(subst $(space),:,$(api_gen_jar) $(api_gen_deps))


$(HOST_OUT)/development/sdk/generated-api-versions.xml: $(android_jar_full_target)
	$(JAVA) -cp $(api_gen_classpath) \
	  com.android.apigenerator.Main \
	  --pattern $(TOPDIR)prebuilts/tools/common/api-versions/android-%/android.jar \
	  --pattern $(TOPDIR)prebuilts/sdk/%/public/android.jar \
	  --current-version $(PLATFORM_SDK_VERSION) \
	  --current-codename $(PLATFORM_VERSION_CODENAME) \
	  --current-jar $(android_jar_full_target) \
	  $@

# ============ System SDK ============
full_target := $(call intermediates-dir-for,JAVA_LIBRARIES,android_system_stubs_current,,COMMON)/classes.jar

.PHONY: android_system_stubs
android_system_stubs: $(full_target)

# Build and store the android_system.jar.
$(call dist-for-goals,sdk win_sdk,$(full_target):android_system.jar)

# ============ Test SDK ============
full_target := $(call intermediates-dir-for,JAVA_LIBRARIES,android_test_stubs_current,,COMMON)/classes.jar

.PHONY: android_test_stubs
android_test_stubs: $(full_target)

# Build and store the android_test.jar.
$(call dist-for-goals,sdk win_sdk,$(full_target):android_test.jar)
