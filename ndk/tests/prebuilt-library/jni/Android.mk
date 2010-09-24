LOCAL_PATH := $(call my-dir)

# Define BUILD_FOO=1 to rebuild libfoo.so from scratch, then
# copy obj/local/armeabi/libfoo.so to jni/libfoo.so
#
ifneq ($(BUILD_FOO),)

include $(CLEAR_VARS)
LOCAL_MODULE := foo
LOCAL_SRC_FILES := foo/foo.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/foo
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/foo
include $(BUILD_SHARED_LIBRARY)

else # not build libfoo.so, trying to use PREBUILT_SHARED_LIBRARY instead.

# Note: the module is named foo-prebuilt, but the library is libfool.so !
#
include $(CLEAR_VARS)
LOCAL_MODULE := foo-prebuilt
LOCAL_SRC_FILES := libfoo.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/foo
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := foo-user
LOCAL_SRC_FILES := foo-user.c
LOCAL_SHARED_LIBRARIES := foo-prebuilt
include $(BUILD_SHARED_LIBRARY)

endif
