cmake_minimum_required(VERSION 3.6)
project(surfaceflinger)
set(ANDROID_ROOT ../../../../../../../../..)
set(SURFACEFLINGER_ROOT ${ANDROID_ROOT}/frameworks/native/services/surfaceflinger)

set(CMAKE_C_COMPILER "${ANDROID_ROOT}/prebuilts/clang/host/linux-x86/clang-3289846/bin/clang")
set(CMAKE_CXX_COMPILER "${ANDROID_ROOT}/prebuilts/clang/host/linux-x86/clang-3289846/bin/clang++")

set(SOURCE_FILES
  ${SURFACEFLINGER_ROOT}/DisplayHardware/FramebufferSurface.cpp
  ${SURFACEFLINGER_ROOT}/DisplayHardware/PowerHAL.cpp
  ${SURFACEFLINGER_ROOT}/DisplayHardware/VirtualDisplaySurface.cpp
  ${SURFACEFLINGER_ROOT}/DisplayHardware/HWC2On1Adapter.cpp
  ${SURFACEFLINGER_ROOT}/Effects/Daltonizer.cpp
  ${SURFACEFLINGER_ROOT}/EventLog/EventLog.cpp
  ${SURFACEFLINGER_ROOT}/RenderEngine/Description.cpp
  ${SURFACEFLINGER_ROOT}/RenderEngine/GLES20RenderEngine.cpp
  ${SURFACEFLINGER_ROOT}/RenderEngine/GLExtensions.cpp
  ${SURFACEFLINGER_ROOT}/RenderEngine/Mesh.cpp
  ${SURFACEFLINGER_ROOT}/RenderEngine/Program.cpp
  ${SURFACEFLINGER_ROOT}/RenderEngine/ProgramCache.cpp
  ${SURFACEFLINGER_ROOT}/RenderEngine/RenderEngine.cpp
  ${SURFACEFLINGER_ROOT}/RenderEngine/Texture.cpp
  ${SURFACEFLINGER_ROOT}/Client.cpp
  ${SURFACEFLINGER_ROOT}/DdmConnection.cpp
  ${SURFACEFLINGER_ROOT}/DisplayDevice.cpp
  ${SURFACEFLINGER_ROOT}/DispSync.cpp
  ${SURFACEFLINGER_ROOT}/EventControlThread.cpp
  ${SURFACEFLINGER_ROOT}/EventThread.cpp
  ${SURFACEFLINGER_ROOT}/FrameTracker.cpp
  ${SURFACEFLINGER_ROOT}/GpuService.cpp
  ${SURFACEFLINGER_ROOT}/Layer.cpp
  ${SURFACEFLINGER_ROOT}/LayerDim.cpp
  ${SURFACEFLINGER_ROOT}/LayerRejecter.cpp
  ${SURFACEFLINGER_ROOT}/main_surfaceflinger.cpp
  ${SURFACEFLINGER_ROOT}/MessageQueue.cpp
  ${SURFACEFLINGER_ROOT}/MonitoredProducer.cpp
  ${SURFACEFLINGER_ROOT}/SurfaceFlingerConsumer.cpp
  ${SURFACEFLINGER_ROOT}/SurfaceInterceptor.cpp
  ${SURFACEFLINGER_ROOT}/Transform.cpp
  )

set(USE_HWC2 1)

if (USE_HWC2)
  list(APPEND SOURCE_FILES "${SURFACEFLINGER_ROOT}/DisplayHardware/ComposerHal.cpp")
  list(APPEND SOURCE_FILES "${SURFACEFLINGER_ROOT}/SurfaceFlinger.cpp")
  list(APPEND SOURCE_FILES "${SURFACEFLINGER_ROOT}/DisplayHardware/HWC2.cpp")
  list(APPEND SOURCE_FILES "${SURFACEFLINGER_ROOT}/DisplayHardware/HWComposer.cpp")

  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DBYPASS_IHWC")
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DUSE_HWC2")
else()
  list(APPEND SOURCE_FILES "${SURFACEFLINGER_ROOT}/SurfaceFlinger_hwc1.cpp")
  list(APPEND SOURCE_FILES "${SURFACEFLINGER_ROOT}/DisplayHardware/HWComposer_hwc1.cpp")
  list(APPEND SOURCE_FILES "${SURFACEFLINGER_ROOT}/DisplayHardware/HWComposer_hwc1.cpp")
endif()

# GLOBAL FLAGS:
# parameters: '${config.CommonGlobalIncludes} ${config.CommonGlobalSystemIncludes} ${config.Arm64IncludeFlags} ${config.CommonNativehelperInclude} -Iframeworks/native/libs/ui  ${config.Arm64ClangCflags} ${config.CommonClangGlobalCflags} ${config.DeviceClangGlobalCflags} ${config.Arm64ClangCortexA53Cflags} -Isystem/libhidl/base/include -Isystem/core/base/include -Isystem/libhidl/transport/include -Isystem/core/base/include -Isystem/libhidl/base/include -Isystem/core/base/include -Iout/soong/.intermediates/system/libhidl/transport/manager/1.0/android.hidl.manager@1.0_genc++_headers/gen -Iout/soong/.intermediates/system/libhidl/transport/base/1.0/android.hidl.base@1.0_genc++_headers/gen -Isystem/libhwbinder/include -Isystem/core/base/include -Isystem/libhidl/base/include -Isystem/core/base/include -Isystem/libhidl/transport/include -Isystem/core/base/include -Isystem/libhidl/base/include -Isystem/core/base/include -Iout/soong/.intermediates/system/libhidl/transport/manager/1.0/android.hidl.manager@1.0_genc++_headers/gen -Iout/soong/.intermediates/system/libhidl/transport/base/1.0/android.hidl.base@1.0_genc++_headers/gen -Isystem/libhwbinder/include -Isystem/core/base/include -Iout/soong/.intermediates/hardware/interfaces/graphics/common/1.0/android.hardware.graphics.common@1.0_genc++_headers/gen -Isystem/libhidl/base/include -Isystem/core/base/include -Isystem/libhidl/transport/include -Isystem/core/base/include -Isystem/libhidl/base/include -Isystem/core/base/include -Iout/soong/.intermediates/system/libhidl/transport/manager/1.0/android.hidl.manager@1.0_genc++_headers/gen -Iout/soong/.intermediates/system/libhidl/transport/base/1.0/android.hidl.base@1.0_genc++_headers/gen -Isystem/libhwbinder/include -Isystem/core/base/include -Iout/soong/.intermediates/system/libhidl/transport/base/1.0/android.hidl.base@1.0_genc++_headers/gen -Iout/soong/.intermediates/hardware/interfaces/graphics/allocator/2.0/android.hardware.graphics.allocator@2.0_genc++_headers/gen -Isystem/libhidl/base/include -Isystem/core/base/include -Isystem/libhidl/transport/include -Isystem/core/base/include -Isystem/libhidl/base/include -Isystem/core/base/include -Iout/soong/.intermediates/system/libhidl/transport/manager/1.0/android.hidl.manager@1.0_genc++_headers/gen -Iout/soong/.intermediates/system/libhidl/transport/base/1.0/android.hidl.base@1.0_genc++_headers/gen -Isystem/libhwbinder/include -Isystem/core/base/include -Isystem/libhidl/base/include -Isystem/core/base/include -Isystem/libhidl/transport/include -Isystem/core/base/include -Isystem/libhidl/base/include -Isystem/core/base/include -Iout/soong/.intermediates/system/libhidl/transport/manager/1.0/android.hidl.manager@1.0_genc++_headers/gen -Iout/soong/.intermediates/system/libhidl/transport/base/1.0/android.hidl.base@1.0_genc++_headers/gen -Isystem/libhwbinder/include -Isystem/core/base/include -Isystem/libhidl/base/include -Isystem/core/base/include -Isystem/libhidl/transport/include -Isystem/core/base/include -Isystem/libhidl/base/include -Isystem/core/base/include -Iout/soong/.intermediates/system/libhidl/transport/manager/1.0/android.hidl.manager@1.0_genc++_headers/gen -Iout/soong/.intermediates/system/libhidl/transport/base/1.0/android.hidl.base@1.0_genc++_headers/gen -Isystem/libhwbinder/include -Isystem/core/base/include -Iout/soong/.intermediates/hardware/interfaces/graphics/common/1.0/android.hardware.graphics.common@1.0_genc++_headers/gen -Isystem/libhidl/base/include -Isystem/core/base/include -Isystem/libhidl/transport/include -Isystem/core/base/include -Isystem/libhidl/base/include -Isystem/core/base/include -Iout/soong/.intermediates/system/libhidl/transport/manager/1.0/android.hidl.manager@1.0_genc++_headers/gen -Iout/soong/.intermediates/system/libhidl/transport/base/1.0/android.hidl.base@1.0_genc++_headers/gen -Isystem/libhwbinder/include -Isystem/core/base/include -Iout/soong/.intermediates/system/libhidl/transport/base/1.0/android.hidl.base@1.0_genc++_headers/gen -Iout/soong/.intermediates/hardware/interfaces/graphics/allocator/2.0/android.hardware.graphics.allocator@2.0_genc++_headers/gen -Isystem/libhidl/base/include -Isystem/core/base/include -Isystem/libhidl/transport/include -Isystem/core/base/include -Isystem/libhidl/base/include -Isystem/core/base/include -Iout/soong/.intermediates/system/libhidl/transport/manager/1.0/android.hidl.manager@1.0_genc++_headers/gen -Iout/soong/.intermediates/system/libhidl/transport/base/1.0/android.hidl.base@1.0_genc++_headers/gen -Isystem/libhwbinder/include -Isystem/core/base/include -Iout/soong/.intermediates/hardware/interfaces/graphics/common/1.0/android.hardware.graphics.common@1.0_genc++_headers/gen -Isystem/libhidl/base/include -Isystem/core/base/include -Isystem/libhidl/transport/include -Isystem/core/base/include -Isystem/libhidl/base/include -Isystem/core/base/include -Iout/soong/.intermediates/system/libhidl/transport/manager/1.0/android.hidl.manager@1.0_genc++_headers/gen -Iout/soong/.intermediates/system/libhidl/transport/base/1.0/android.hidl.base@1.0_genc++_headers/gen -Isystem/libhwbinder/include -Isystem/core/base/include -Iout/soong/.intermediates/system/libhidl/transport/base/1.0/android.hidl.base@1.0_genc++_headers/gen -Iout/soong/.intermediates/hardware/interfaces/graphics/mapper/2.0/android.hardware.graphics.mapper@2.0_genc++_headers/gen -Isystem/core/base/include -Isystem/libhidl/base/include -Isystem/core/base/include -Isystem/libhidl/transport/include -Isystem/core/base/include -Isystem/libhidl/base/include -Isystem/core/base/include -Iout/soong/.intermediates/system/libhidl/transport/manager/1.0/android.hidl.manager@1.0_genc++_headers/gen -Iout/soong/.intermediates/system/libhidl/transport/base/1.0/android.hidl.base@1.0_genc++_headers/gen -Isystem/core/libsync/include -Iexternal/libcxx/include -Iexternal/libcxxabi/include'
# variable ${config.CommonGlobalIncludes} = '-Isystem/core/include -Isystem/media/audio/include -Ihardware/libhardware/include -Ihardware/libhardware_legacy/include -Ihardware/ril/include -Ilibnativehelper/include -Iframeworks/native/include -Iframeworks/native/opengl/include'
# parameters: '-Isystem/core/include -Isystem/media/audio/include -Ihardware/libhardware/include -Ihardware/libhardware_legacy/include -Ihardware/ril/include -Ilibnativehelper/include -Iframeworks/native/include -Iframeworks/native/opengl/include'
# variable ${config.CommonGlobalSystemIncludes} = '-isystem frameworks/av/include'
# parameters: '-isystem frameworks/av/include'
# variable ${config.Arm64IncludeFlags} = '-isystem bionic/libc/arch-arm64/include -isystem bionic/libc/include -isystem bionic/libc/kernel/uapi -isystem bionic/libc/kernel/uapi/asm-arm64 -isystem bionic/libc/kernel/android/uapi'
# parameters: '-isystem bionic/libc/arch-arm64/include -isystem bionic/libc/include -isystem bionic/libc/kernel/uapi -isystem bionic/libc/kernel/uapi/asm-arm64 -isystem bionic/libc/kernel/android/uapi'
# variable ${config.CommonNativehelperInclude} = '-Ilibnativehelper/include/nativehelper'
# parameters: '-Ilibnativehelper/include/nativehelper'
# variable ${config.Arm64ClangCflags} = '-fno-exceptions -Wno-multichar -fno-strict-aliasing -fstack-protector-strong -ffunction-sections -fdata-sections -funwind-tables -Wa,--noexecstack -Werror=format-security -D_FORTIFY_SOURCE=2 -fno-short-enums -no-canonical-prefixes -Werror=pointer-to-int-cast -Werror=int-to-pointer-cast -Werror=implicit-function-declaration -DNDEBUG -O2 -g -Wstrict-aliasing=2'
# parameters: '-fno-exceptions -Wno-multichar -fno-strict-aliasing -fstack-protector-strong -ffunction-sections -fdata-sections -funwind-tables -Wa,--noexecstack -Werror=format-security -D_FORTIFY_SOURCE=2 -fno-short-enums -no-canonical-prefixes -Werror=pointer-to-int-cast -Werror=int-to-pointer-cast -Werror=implicit-function-declaration -DNDEBUG -O2 -g -Wstrict-aliasing=2'
# variable ${config.CommonClangGlobalCflags} = '-DANDROID -fmessage-length=0 -W -Wall -Wno-unused -Winit-self -Wpointer-arith -DNDEBUG -UDEBUG -fdebug-prefix-map=/proc/self/cwd= -D__compiler_offsetof=__builtin_offsetof -Werror=int-conversion -Wno-reserved-id-macro -Wno-format-pedantic -Wno-unused-command-line-argument -fcolor-diagnostics -Wno-expansion-to-defined'
# parameters: '-DANDROID -fmessage-length=0 -W -Wall -Wno-unused -Winit-self -Wpointer-arith -DNDEBUG -UDEBUG -fdebug-prefix-map=/proc/self/cwd= -D__compiler_offsetof=__builtin_offsetof -Werror=int-conversion -Wno-reserved-id-macro -Wno-format-pedantic -Wno-unused-command-line-argument -fcolor-diagnostics -Wno-expansion-to-defined'
# variable ${config.DeviceClangGlobalCflags} = '-Werror=return-type -Werror=non-virtual-dtor -Werror=address -Werror=sequence-point -Werror=date-time -nostdlibinc'
# parameters: '-Werror=return-type -Werror=non-virtual-dtor -Werror=address -Werror=sequence-point -Werror=date-time -nostdlibinc'
# variable ${config.Arm64ClangCortexA53Cflags} = '-mcpu=cortex-a53'
# parameters: '-mcpu=cortex-a53'
include_directories(SYSTEM "${ANDROID_ROOT}/bionic/libc/arch-arm64/include")
include_directories(SYSTEM "${ANDROID_ROOT}/frameworks/av/include")
include_directories(SYSTEM "${ANDROID_ROOT}/bionic/libc/include")
include_directories(SYSTEM "${ANDROID_ROOT}/bionic/libc/kernel/uapi")
include_directories(SYSTEM "${ANDROID_ROOT}/bionic/libc/kernel/uapi/asm-arm64")
include_directories(SYSTEM "${ANDROID_ROOT}/bionic/libc/kernel/android/uapi")
include_directories("${ANDROID_ROOT}/libnativehelper/include/nativehelper")
include_directories("${ANDROID_ROOT}/system/libhidl/base/include")
include_directories("${ANDROID_ROOT}/out/soong/.intermediates/system/libhidl/transport/base/1.0/android.hidl.base@1.0_genc++_headers/gen")
include_directories("${ANDROID_ROOT}/system/libhwbinder/include")
include_directories("${ANDROID_ROOT}/external/libcxx/include")
include_directories("${ANDROID_ROOT}/external/libcxxabi/include")
include_directories("${ANDROID_ROOT}/hardware/libhardware/include")
include_directories("${ANDROID_ROOT}/hardware/libhardware_legacy/include")
include_directories("${ANDROID_ROOT}/frameworks/native/include")
include_directories("${ANDROID_ROOT}/system/core/base/include")
include_directories("${ANDROID_ROOT}/out/soong/.intermediates/system/libhidl/transport/manager/1.0/android.hidl.manager@1.0_genc++_headers/gen")
include_directories("${ANDROID_ROOT}/out/soong/.intermediates/hardware/interfaces/graphics/mapper/2.0/android.hardware.graphics.mapper@2.0_genc++_headers/gen")
include_directories("${ANDROID_ROOT}/system/core/include")
include_directories("${ANDROID_ROOT}/frameworks/native/libs/ui")
include_directories("${ANDROID_ROOT}/out/soong/.intermediates/hardware/interfaces/graphics/common/1.0/android.hardware.graphics.common@1.0_genc++_headers/gen")
include_directories("${ANDROID_ROOT}/system/core/libsync/include")
include_directories("${ANDROID_ROOT}/libnativehelper/include")
include_directories("${ANDROID_ROOT}/system/media/audio/include")
include_directories("${ANDROID_ROOT}/hardware/ril/include")
include_directories("${ANDROID_ROOT}/system/libhidl/transport/include")
include_directories("${ANDROID_ROOT}/out/soong/.intermediates/hardware/interfaces/graphics/allocator/2.0/android.hardware.graphics.allocator@2.0_genc++_headers/gen")
include_directories("${ANDROID_ROOT}/frameworks/native/opengl/include")
include_directories("${ANDROID_ROOT}/hardware/interfaces/graphics/composer/2.1/default")
include_directories("${ANDROID_ROOT}/out/soong/.intermediates/hardware/interfaces/graphics/composer/2.1/android.hardware.graphics.composer@2.1_genc++_headers/gen")
include_directories("${ANDROID_ROOT}/frameworks/native/services/surfaceflinger")
include_directories("${ANDROID_ROOT}/system/libfmq/include")

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fno-exceptions")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-multichar")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fno-strict-aliasing")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fstack-protector-strong")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffunction-sections")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fdata-sections")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -funwind-tables")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wa,--noexecstack")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Werror=format-security")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -D_FORTIFY_SOURCE=2")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fno-short-enums")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -no-canonical-prefixes")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Werror=pointer-to-int-cast")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Werror=int-to-pointer-cast")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Werror=implicit-function-declaration")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNDEBUG")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -g")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wstrict-aliasing=2")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DANDROID")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fmessage-length=0")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -W")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-unused")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Winit-self")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wpointer-arith")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNDEBUG")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -UDEBUG")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fdebug-prefix-map=/proc/self/cwd=")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -D__compiler_offsetof=__builtin_offsetof")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Werror=int-conversion")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-reserved-id-macro")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-format-pedantic")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-unused-command-line-argument")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fcolor-diagnostics")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-expansion-to-defined")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Werror=return-type")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Werror=non-virtual-dtor")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Werror=address")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Werror=sequence-point")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Werror=date-time")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -nostdlibinc")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-exceptions")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-multichar")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-strict-aliasing")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fstack-protector-strong")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffunction-sections")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fdata-sections")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -funwind-tables")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wa,--noexecstack")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Werror=format-security")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D_FORTIFY_SOURCE=2")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-short-enums")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -no-canonical-prefixes")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Werror=pointer-to-int-cast")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Werror=int-to-pointer-cast")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Werror=implicit-function-declaration")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DNDEBUG")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wstrict-aliasing=2")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DANDROID")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fmessage-length=0")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -W")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-unused")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Winit-self")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wpointer-arith")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DNDEBUG")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -UDEBUG")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fdebug-prefix-map=/proc/self/cwd=")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D__compiler_offsetof=__builtin_offsetof")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Werror=int-conversion")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-reserved-id-macro")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-format-pedantic")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-unused-command-line-argument")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fcolor-diagnostics")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-expansion-to-defined")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Werror=return-type")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Werror=non-virtual-dtor")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Werror=address")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Werror=sequence-point")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Werror=date-time")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlibinc")

# CFLAGS:
# parameters: '-target aarch64-linux-android -B${config.Arm64GccRoot}/aarch64-linux-android/bin -DANDROID_STRICT -fPIC -D_USING_LIBCXX'
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -target")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} aarch64-linux-android")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -B${config.Arm64GccRoot}/aarch64-linux-android/bin")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DANDROID_STRICT")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -D_USING_LIBCXX")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -target")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} aarch64-linux-android")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -B${config.Arm64GccRoot}/aarch64-linux-android/bin")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DANDROID_STRICT")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D_USING_LIBCXX")

# C ONLY FLAGS:
# parameters: '-std=gnu99 ${config.CommonGlobalConlyflags}'
# variable ${config.CommonGlobalConlyflags} = ''
# parameters: ''
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=gnu99")

# CPP FLAGS:
# parameters: '-std=gnu++14 ${config.CommonClangGlobalCppflags} -Weverything -Werror -Wno-exit-time-destructors -Wno-global-constructors -Wno-c++98-compat-pedantic -Wno-float-equal -Wno-four-char-constants -Wno-padded -fno-rtti ${config.Arm64ClangCppflags}'
# variable ${config.CommonClangGlobalCppflags} = '-Wsign-promo -Wno-inconsistent-missing-override -Wno-null-dereference'
# parameters: '-Wsign-promo -Wno-inconsistent-missing-override -Wno-null-dereference'
# variable ${config.Arm64ClangCppflags} = '-fvisibility-inlines-hidden'
# parameters: '-fvisibility-inlines-hidden'
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++14")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wsign-promo")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-inconsistent-missing-override")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-null-dereference")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Weverything")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Werror")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-exit-time-destructors")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-global-constructors")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-c++98-compat-pedantic")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-float-equal")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-four-char-constants")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-padded")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-rtti")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fvisibility-inlines-hidden")

add_executable(surfaceflinger ${SOURCE_FILES})
