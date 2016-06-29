# Copyright 2006 The Android Open Source Project
#

LOCAL_PATH := $(call my-dir)

# the script
# ============================================================
include $(CLEAR_VARS)

LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := bugreport

include $(BUILD_SYSTEM)/base_rules.mk

$(LOCAL_BUILT_MODULE): $(LOCAL_PATH)/bugreport | $(ACP)
	@echo "Copy: $(PRIVATE_MODULE) ($@)"
	$(copy-file-to-new-target)
	$(hide) chmod 755 $@


# the java
# ============================================================
include $(CLEAR_VARS)

LOCAL_MODULE := BugReport
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_JAR_MANIFEST := manifest-library.mf
LOCAL_MODULE_TAGS := optional
LOCAL_JAVA_RESOURCE_DIRS := resources
LOCAL_STATIC_JAVA_LIBRARIES := \
    jsilver

include $(BUILD_HOST_JAVA_LIBRARY)

