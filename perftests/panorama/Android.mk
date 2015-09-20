local_target_dir := $(TARGET_OUT_DATA)/local/tmp

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/feature_mos/src \
    $(LOCAL_PATH)/feature_stab/src \
    $(LOCAL_PATH)/feature_stab/db_vlvm

LOCAL_SRC_FILES := benchmark.cpp \
	feature_mos/src/mosaic/ImageUtils.cpp \
    feature_mos/src/mosaic/Mosaic.cpp \
    feature_mos/src/mosaic/AlignFeatures.cpp \
    feature_mos/src/mosaic/Blend.cpp \
    feature_mos/src/mosaic/Pyramid.cpp \
    feature_mos/src/mosaic/trsMatrix.cpp \
    feature_mos/src/mosaic/Delaunay.cpp \
    feature_mos/src/mosaic_renderer/Renderer.cpp \
    feature_mos/src/mosaic_renderer/WarpRenderer.cpp \
    feature_mos/src/mosaic_renderer/SurfaceTextureRenderer.cpp \
    feature_mos/src/mosaic_renderer/YVURenderer.cpp \
    feature_mos/src/mosaic_renderer/FrameBuffer.cpp \
    feature_stab/db_vlvm/db_rob_image_homography.cpp \
    feature_stab/db_vlvm/db_feature_detection.cpp \
    feature_stab/db_vlvm/db_image_homography.cpp \
    feature_stab/db_vlvm/db_framestitching.cpp \
    feature_stab/db_vlvm/db_feature_matching.cpp \
    feature_stab/db_vlvm/db_utilities.cpp \
    feature_stab/db_vlvm/db_utilities_camera.cpp \
    feature_stab/db_vlvm/db_utilities_indexing.cpp \
    feature_stab/db_vlvm/db_utilities_linalg.cpp \
    feature_stab/db_vlvm/db_utilities_poly.cpp \
    feature_stab/src/dbreg/dbstabsmooth.cpp \
    feature_stab/src/dbreg/dbreg.cpp \
    feature_stab/src/dbreg/vp_motionmodel.c

LOCAL_CFLAGS := -O3 -DNDEBUG -Wno-unused-parameter -Wno-maybe-uninitialized
LOCAL_CPPFLAGS := -std=c++98
LOCAL_MODULE_TAGS := tests
LOCAL_MODULE := panorama_bench
LOCAL_MODULE_STEM_32 := panorama_bench
LOCAL_MODULE_STEM_64 := panorama_bench64
LOCAL_MULTILIB := both
LOCAL_MODULE_PATH := $(local_target_dir)
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk
LOCAL_FORCE_STATIC_EXECUTABLE := true
LOCAL_STATIC_LIBRARIES := libc libm

include $(BUILD_EXECUTABLE)
