cmake_minimum_required(VERSION 3.6)
project(hwui)

set(ROOT ${CMAKE_CURRENT_LIST_DIR}/../../../../)
set(ROOT_OUT ${CMAKE_CURRENT_LIST_DIR}/cmake_out)

# HWUI deps that we want source for
add_subdirectory(${ROOT}/frameworks/native/libs/gui/libgui-arm64-android ${ROOT_OUT}/libgui)
add_subdirectory(${ROOT}/frameworks/native/libs/ui/libui-arm64-android ${ROOT_OUT}/libui)
add_subdirectory(${ROOT}/external/skia/libskia-arm64-android ${ROOT_OUT}/libskia)

# HWUI
add_subdirectory(${ROOT}/frameworks/base/libs/hwui/libhwui-arm64-android ${ROOT_OUT}/libhwui)
add_subdirectory(${ROOT}/frameworks/base/libs/hwui/hwuimacro-arm64-android ${ROOT_OUT}/hwuimacro)
add_subdirectory(${ROOT}/frameworks/base/libs/hwui/hwuimicro-arm64-android ${ROOT_OUT}/hwuimicro)
add_subdirectory(${ROOT}/frameworks/base/libs/hwui/hwui_unit_tests-arm64-android ${ROOT_OUT}/hwuiunit)

# JNI
add_subdirectory(${ROOT}/frameworks/base/core/jni/libandroid_runtime-arm64-android ${ROOT_OUT}/jni)
