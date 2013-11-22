LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE:= ndk_helper
LOCAL_SRC_FILES:= JNIHelper.cpp interpolator.cpp tapCamera.cpp gestureDetector.cpp perfMonitor.cpp vecmath.cpp GLContext.cpp shader.cpp gl3stub.c

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
LOCAL_EXPORT_LDLIBS    := -llog -landroid -lEGL -lGLESv2

LOCAL_STATIC_LIBRARIES := cpufeatures android_native_app_glue


ifneq ($(filter %armeabi-v7a,$(TARGET_ARCH_ABI)),)
LOCAL_CFLAGS += -mhard-float -D_NDK_MATH_NO_SOFTFP=1
LOCAL_EXPORT_CFLAGS += -mhard-float -D_NDK_MATH_NO_SOFTFP=1
LOCAL_EXPORT_LDLIBS += -lm_hard
ifeq (,$(filter -fuse-ld=mcld,$(APP_LDFLAGS) $(LOCAL_LDFLAGS)))
LOCAL_EXPORT_LDFLAGS += -Wl,--no-warn-mismatch
endif
endif

include $(BUILD_STATIC_LIBRARY)

#$(call import-module,android/native_app_glue)
#$(call import-module,android/cpufeatures)