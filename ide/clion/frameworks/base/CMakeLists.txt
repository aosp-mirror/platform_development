cmake_minimum_required(VERSION 3.6)
project(frameworks_base)

include("${CMAKE_CURRENT_SOURCE_DIR}/../../CMakeLists.txt")

# HWUI
try_add_subdir(libs/hwui/libhwui)
try_add_subdir(libs/hwui/hwuimacro)
try_add_subdir(libs/hwui/hwuimicro)
try_add_subdir(libs/hwui/hwui_unit_tests)

# JNI
try_add_subdir(core/jni/libandroid_runtime)
