#
# Install a list of test definitions on device
#

# where to install the sample files on the device
#
local_target_dir := $(TARGET_OUT_DATA)/testinfo
LOCAL_PATH := $(call my-dir)

########################
include $(CLEAR_VARS)
LOCAL_MODULE := test_defs.xml
LOCAL_MODULE_TAGS := tests
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(local_target_dir)
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := coverage_targets.xml
LOCAL_MODULE_TAGS := tests
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(local_target_dir)
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)
