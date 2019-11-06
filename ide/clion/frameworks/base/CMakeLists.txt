cmake_minimum_required(VERSION 3.6)
project(frameworks_base)

# HWUI
add_subdirectory(libs/hwui/libhwui-arm64-android)
add_subdirectory(libs/hwui/hwuimacro-arm64-android)
add_subdirectory(libs/hwui/hwuimicro-arm64-android)
add_subdirectory(libs/hwui/hwui_unit_tests-arm64-android)

# JNI
add_subdirectory(core/jni/libandroid_runtime-arm64-android)
add_subdirectory(core/jni/libandroid_graphics-arm64-android)
