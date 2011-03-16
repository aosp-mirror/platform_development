
LOCAL_PATH:=$(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := debug
LOCAL_SRC_FILES := ApiGen.cpp EntryPoint.cpp main.cpp strUtils.cpp TypeFactory.cpp
LOCAL_MODULE := emugen

include $(BUILD_HOST_EXECUTABLE)
