cmake_minimum_required(VERSION 3.6)
project("Android Resources")

set(ROOT ${CMAKE_CURRENT_LIST_DIR}/../../../../)
set(ROOT_OUT ${CMAKE_CURRENT_LIST_DIR}/cmake_out)
set(AAPT2_BASE ${ROOT}/frameworks/base/tools/aapt2)
set(IDMAP2_BASE ${ROOT}/frameworks/base/cmds/idmap2)
set(ANDROIDFW_BASE ${ROOT}/frameworks/base/libs/androidfw)
set(JNI_BASE ${ROOT}/frameworks/base/core/jni)
set(AAPT_BASE ${ROOT}/frameworks/base/tools/aapt)

# Set host arch
if(EXISTS ${AAPT2_BASE}/aapt2-x86_64-linux_glibc)
    message(NOTICE, "Applying HOST_ARCH x86_64-linux_glibc")
    set(HOST_ARCH x86_64-linux_glibc)
elseif(EXISTS ${AAPT2_BASE}/aapt2-x86-linux_glibc)
    message(NOTICE, "Applying HOST_ARCH x86-linux_glibc")
    set(HOST_ARCH x86-linux_glibc)
elseif(EXISTS ${AAPT2_BASE}/aapt2-x86_64-windows)
    message(NOTICE, "Applying HOST_ARCH x86_64-windows")
    set(HOST_ARCH x86_64-windows)
elseif(EXISTS ${AAPT2_BASE}/aapt2-x86-windows)
    message(NOTICE, "Applying HOST_ARCH x86-windows")
    set(HOST_ARCH x86-windows)
else()
    message(NOTICE, "Applying default HOST_ARCH x86_64-linux_glibc")
    set(HOST_ARCH x86_64-linux_glibc)
endif()

# Set target arch
if(EXISTS ${IDMAP2_BASE}/idmap2-arm64-android)
    message(NOTICE, "Applying TARGET_ARCH arm64-android")
    set(TARGET_ARCH arm64-android)
elseif(EXISTS ${IDMAP2_BASE}/idmap2-arm-android)
    message(NOTICE, "Applying TARGET_ARCH arm-android")
    set(TARGET_ARCH arm-android)
elseif(EXISTS ${IDMAP2_BASE}/idmap2-x86_64-linux_glibc)
    message(NOTICE, "Applying TARGET_ARCH x86_64-linux_glibc")
    set(TARGET_ARCH x86_64-linux_glibc)
elseif(EXISTS ${IDMAP2_BASE}/idmap2-x86-linux_glibc)
    message(NOTICE, "Applying TARGET_ARCH x86-linux_glibc")
    set(TARGET_ARCH x86-linux_glibc)
else()
    message(NOTICE, "Applying default TARGET_ARCH arm64-android")
    set(TARGET_ARCH arm64-android)
endif()

# aapt2
add_subdirectory(${AAPT2_BASE}/aapt2-${HOST_ARCH} ${ROOT_OUT}/aapt2)
add_subdirectory(${AAPT2_BASE}/aapt2_tests-${HOST_ARCH} ${ROOT_OUT}/aapt2_tests)
add_subdirectory(${AAPT2_BASE}/libaapt2-${HOST_ARCH} ${ROOT_OUT}/libaapt2)

# idmap2
add_subdirectory(${IDMAP2_BASE}/idmap2-${TARGET_ARCH} ${ROOT_OUT}/idmap2)
add_subdirectory(${IDMAP2_BASE}/idmap2d-${TARGET_ARCH} ${ROOT_OUT}/idmap2d)
add_subdirectory(${IDMAP2_BASE}/libidmap2-${TARGET_ARCH} ${ROOT_OUT}/libidmap2)
add_subdirectory(${IDMAP2_BASE}/libidmap2_protos-${TARGET_ARCH} ${ROOT_OUT}/libidmap2_protos)
add_subdirectory(${IDMAP2_BASE}/libidmap2daidl-${TARGET_ARCH} ${ROOT_OUT}/libidmap2daidl)
add_subdirectory(${IDMAP2_BASE}/idmap2_tests-${TARGET_ARCH} ${ROOT_OUT}/idmap2_tests)

# Android Runtime
add_subdirectory(${ANDROIDFW_BASE}/libandroidfw-${TARGET_ARCH} ${ROOT_OUT}/libandroidfw)
add_subdirectory(${ANDROIDFW_BASE}/libandroidfw_tests-${TARGET_ARCH} ${ROOT_OUT}/libandroidfw_tests)
add_subdirectory(${ANDROIDFW_BASE}/libandroidfw_benchmarks-${TARGET_ARCH} ${ROOT_OUT}/libandroidfw_benchmarks)
add_subdirectory(${ANDROIDFW_BASE}/libandroidfw_fuzzer_lib-${TARGET_ARCH} ${ROOT_OUT}/libandroidfw_fuzzer_lib)
add_subdirectory(${ANDROIDFW_BASE}/fuzz/resourcefile_fuzzer/resourcefile_fuzzer-${TARGET_ARCH}
        ${ROOT_OUT}/fuzz/resourcefile_fuzzer/resourcefile_fuzzer)

# JNI
add_subdirectory(${JNI_BASE}/libandroid_runtime-${TARGET_ARCH} ${ROOT_OUT}/jni)

# aapt
add_subdirectory(${AAPT_BASE}/aapt-${HOST_ARCH} ${ROOT_OUT}/aapt)
add_subdirectory(${AAPT_BASE}/libaapt-${HOST_ARCH} ${ROOT_OUT}/libaapt)
add_subdirectory(${AAPT_BASE}/libaapt_tests-${HOST_ARCH} ${ROOT_OUT}/libaapt_tests)
