# Build an SDK jar file out of the generated stubs
# Input variable:
#   sdk_stub_name: the name of the SDK stubs; the stub source code should have been generated to
#                  $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/$(sdk_stub_name)_intermediates.
#   stub_timestamp: the timestamp file we use as dependency of the generated source.
# Output variable:
#   full_target: the built classes.jar

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
intermediates := $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/$(sdk_stub_name)_intermediates
full_target := $(intermediates)/classes.jar
header_target := $(intermediates)/classes-header.jar
full_src_target = $(intermediates)/android-stubs-src.jar
src_dir := $(intermediates)/src
classes_dir := $(intermediates)/classes
framework_res_package := $(call intermediates-dir-for,APPS,framework-res,,COMMON)/package-export.apk

$(full_target) $(full_src_target): PRIVATE_SRC_DIR := $(src_dir)
$(full_target) $(full_src_target): PRIVATE_INTERMEDIATES_DIR := $(intermediates)
$(full_target): PRIVATE_FRAMEWORK_RES_PACKAGE := $(framework_res_package)

$(full_target): PRIVATE_CLASS_INTERMEDIATES_DIR := $(classes_dir)

$(full_src_target): $(stub_timestamp)
	@echo Packaging SDK Stub sources: $@
	$(hide) cd $(PRIVATE_INTERMEDIATES_DIR) && zip -rq $(notdir $@) $(notdir $(PRIVATE_SRC_DIR))

$(full_target): $(stub_timestamp) $(framework_res_package) $(ZIPTIME)
	@echo Compiling SDK Stubs: $@
	$(hide) rm -rf $(PRIVATE_CLASS_INTERMEDIATES_DIR)
	$(hide) mkdir -p $(PRIVATE_CLASS_INTERMEDIATES_DIR)
	$(hide) mkdir -p $(PRIVATE_CLASS_INTERMEDIATES_DIR)/NOTICES
	$(hide) if [ ! -f libcore/NOTICE ]; then \
	echo "Missing notice file : libcore/NOTICE"; \
	rm -rf $(PRIVATE_CLASS_INTERMEDIATES_DIR); \
	exit 1; \
	fi;
	$(hide) if [ ! -f libcore/ojluni/NOTICE ]; then \
	echo "Missing notice file : libcore/ojluni/NOTICE"; \
	rm -rf $(PRIVATE_CLASS_INTERMEDIATES_DIR); \
	exit 1; \
	fi;
	$(hide) $(ACP) libcore/NOTICE $(PRIVATE_CLASS_INTERMEDIATES_DIR)/NOTICES/libcore-NOTICE
	$(hide) $(ACP) libcore/ojluni/NOTICE $(PRIVATE_CLASS_INTERMEDIATES_DIR)/NOTICES/ojluni-NOTICE
	$(hide) find $(PRIVATE_SRC_DIR) -name "*.java" > \
        $(PRIVATE_INTERMEDIATES_DIR)/java-source-list
	$(hide) $(TARGET_JAVAC) -source 1.8 -target 1.8 -encoding UTF-8 -bootclasspath "" \
			-g -d $(PRIVATE_CLASS_INTERMEDIATES_DIR) \
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
	$(hide) jar -cf $@.tmp -C $(PRIVATE_CLASS_INTERMEDIATES_DIR) .
	$(hide) jar -u0f $@.tmp -C $(PRIVATE_CLASS_INTERMEDIATES_DIR) resources.arsc
	$(hide) $(ZIPTIME) $@.tmp
	$(hide) $(call commit-change-for-toc,$@)

.KATI_RESTAT: $(full_target)

$(eval $(call copy-one-file,$(full_target),$(header_target)))
