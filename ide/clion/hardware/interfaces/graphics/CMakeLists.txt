cmake_minimum_required(VERSION 3.6)
project(hardware_graphics)

include("${CMAKE_CURRENT_SOURCE_DIR}/../../../CMakeLists.txt")

try_add_subdir(allocator/aidl/vts/VtsHalGraphicsAllocatorAidl_TargetTest)
try_add_subdir(allocator/aidl/android.hardware.graphics.allocator-V2-ndk)
try_add_subdir(composer/aidl/android.hardware.graphics.composer3-V2-ndk)
try_add_subdir(composer/aidl/vts/VtsHalGraphicsComposer3_TargetTest)
try_add_subdir(common/aidl/android.hardware.graphics.common-V5-ndk)
try_add_subdir(mapper/stable-c/libimapper_providerutils_tests)
try_add_subdir(mapper/stable-c/VtsHalGraphicsMapperStableC_TargetTest)
try_add_subdir(mapper/4.0/vts/functional/VtsHalGraphicsMapperV4_0TargetTest)