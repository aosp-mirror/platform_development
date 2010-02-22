/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#ifndef CPU_FEATURES_H
#define CPU_FEATURES_H

#include <stdint.h>

typedef enum {
    ANDROID_CPU_FAMILY_UNKNOWN = 0,
    ANDROID_CPU_FAMILY_ARM,
    ANDROID_CPU_FAMILY_X86,

    ANDROID_CPU_FAMILY_MAX  /* do not remove */

} AndroidCpuFamily;

/* Return family of the device's CPU */
extern AndroidCpuFamily   android_getCpuFamily(void);

enum {
    ANDROID_CPU_ARM_FEATURE_ARMv7 = (1 << 0),
    ANDROID_CPU_ARM_FEATURE_VFPv3 = (1 << 1),
    ANDROID_CPU_ARM_FEATURE_NEON  = (1 << 2),
};

extern uint64_t    android_getCpuFeatures(void);

#endif /* CPU_FEATURES_H */
