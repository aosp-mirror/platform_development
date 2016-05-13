/*
 * Copyright (C) 2015 The Android Open Source Project
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
 */

/**
 * @addtogroup Camera
 * @{
 */

/**
 * @file NdkCameraMetadata.h
 */

/*
 * This file defines an NDK API.
 * Do not remove methods.
 * Do not change method signatures.
 * Do not change the value of constants.
 * Do not change the size of any of the classes defined in here.
 * Do not reference types that are not part of the NDK.
 * Do not #include files that aren't part of the NDK.
 */

#ifndef _NDK_CAMERA_METADATA_H
#define _NDK_CAMERA_METADATA_H

#include "NdkCameraError.h"
#include "NdkCameraMetadataTags.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct ACameraMetadata ACameraMetadata;

// Keep in sync with system/media/include/system/camera_metadata.h
enum {
    // Unsigned 8-bit integer (uint8_t)
    ACAMERA_TYPE_BYTE = 0,
    // Signed 32-bit integer (int32_t)
    ACAMERA_TYPE_INT32 = 1,
    // 32-bit float (float)
    ACAMERA_TYPE_FLOAT = 2,
    // Signed 64-bit integer (int64_t)
    ACAMERA_TYPE_INT64 = 3,
    // 64-bit float (double)
    ACAMERA_TYPE_DOUBLE = 4,
    // A 64-bit fraction (ACameraMetadata_rational)
    ACAMERA_TYPE_RATIONAL = 5,
    // Number of type fields
    ACAMERA_NUM_TYPES
};

typedef struct ACameraMetadata_rational {
    int32_t numerator;
    int32_t denominator;
} ACameraMetadata_rational;

typedef struct ACameraMetadata_entry {
    uint32_t tag;
    uint8_t  type;
    uint32_t count;
    union {
        uint8_t *u8;
        int32_t *i32;
        float   *f;
        int64_t *i64;
        double  *d;
        ACameraMetadata_rational* r;
    } data;
} ACameraMetadata_entry;

typedef struct ACameraMetadata_const_entry {
    uint32_t tag;
    uint8_t  type;
    uint32_t count;
    union {
        const uint8_t *u8;
        const int32_t *i32;
        const float   *f;
        const int64_t *i64;
        const double  *d;
        const ACameraMetadata_rational* r;
    } data;
} ACameraMetadata_const_entry;

/*
 * Get a metadata entry
 */
camera_status_t ACameraMetadata_getConstEntry(
        const ACameraMetadata*, uint32_t tag, ACameraMetadata_const_entry* entry);

/*
 * List all the entry tags in this metadata.
 * The memory of tags is managed by ACameraMetadata itself and must NOT be free/delete
 * by application. Do NOT access tags after calling ACameraMetadata_free
 */
camera_status_t ACameraMetadata_getAllTags(
        const ACameraMetadata*, /*out*/int32_t* numTags, /*out*/const uint32_t** tags);

/**
 * Copy a metadata. Duplicates a metadata structure.
 * The destination ACameraMetadata must be freed by the application with ACameraMetadata_free
 * after application is done using it.
 * Returns NULL when src cannot be copied
 */
ACameraMetadata* ACameraMetadata_copy(const ACameraMetadata* src);

/**
 * Frees a metadata structure.
 */
void ACameraMetadata_free(ACameraMetadata*);

#ifdef __cplusplus
} // extern "C"
#endif

#endif //_NDK_CAMERA_METADATA_H

/** @} */
