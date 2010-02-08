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
