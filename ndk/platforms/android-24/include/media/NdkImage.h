/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * @addtogroup Media Camera
 * @{
 */

/**
 * @file NdkImage.h
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

#ifndef _NDK_IMAGE_H
#define _NDK_IMAGE_H

#include "NdkMediaError.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct AImage AImage;

// Formats not listed here will not be supported by AImageReader
enum {
    AIMAGE_FORMAT_YUV_420_888       = 0x23,
    AIMAGE_FORMAT_JPEG              = 0x100,
    AIMAGE_FORMAT_RAW16             = 0x20,
    AIMAGE_FORMAT_RAW_PRIVATE       = 0x24,
    AIMAGE_FORMAT_RAW10             = 0x25,
    AIMAGE_FORMAT_RAW12             = 0x26,
    AIMAGE_FORMAT_DEPTH16           = 0x44363159,
    AIMAGE_FORMAT_DEPTH_POINT_CLOUD = 0x101,
    AIMAGE_FORMAT_PRIVATE           = 0x22 ///> Not supported by AImageReader yet
};

typedef struct AImageCropRect {
    int32_t left;
    int32_t top;
    int32_t right;
    int32_t bottom;
} AImageCropRect;

// Return the image back to system and delete the AImage from memory
// Do NOT use `image` after this call
void AImage_delete(AImage* image);

// AMEDIA_ERROR_INVALID_OBJECT will be returned if the parent AImageReader is deleted
media_status_t AImage_getWidth(const AImage* image, /*out*/int32_t* width);

// AMEDIA_ERROR_INVALID_OBJECT will be returned if the parent AImageReader is deleted
media_status_t AImage_getHeight(const AImage* image, /*out*/int32_t* height);

// AMEDIA_ERROR_INVALID_OBJECT will be returned if the parent AImageReader is deleted
media_status_t AImage_getFormat(const AImage* image, /*out*/int32_t* format);

// AMEDIA_ERROR_INVALID_OBJECT will be returned if the parent AImageReader is deleted
media_status_t AImage_getCropRect(const AImage* image, /*out*/AImageCropRect* rect);

// AMEDIA_ERROR_INVALID_OBJECT will be returned if the parent AImageReader is deleted
media_status_t AImage_getTimestamp(const AImage* image, /*out*/int64_t* timestampNs);

// AMEDIA_ERROR_INVALID_OBJECT will be returned if the parent AImageReader is deleted
media_status_t AImage_getNumberOfPlanes(const AImage* image, /*out*/int32_t* numPlanes);

// AMEDIA_ERROR_INVALID_OBJECT will be returned if the parent AImageReader is deleted
media_status_t AImage_getPlanePixelStride(
        const AImage* image, int planeIdx, /*out*/int32_t* pixelStride);

// AMEDIA_ERROR_INVALID_OBJECT will be returned if the parent AImageReader is deleted
media_status_t AImage_getPlaneRowStride(
        const AImage* image, int planeIdx, /*out*/int32_t* rowStride);

// AMEDIA_ERROR_INVALID_OBJECT will be returned if the parent AImageReader is deleted
// Note that once the AImage or the parent AImageReader is deleted, the `*data` returned from
// previous AImage_getPlaneData call becomes dangling pointer. Do NOT use it after
// AImage or AImageReader is deleted
media_status_t AImage_getPlaneData(
        const AImage* image, int planeIdx,
        /*out*/uint8_t** data, /*out*/int* dataLength);

#ifdef __cplusplus
} // extern "C"
#endif

#endif //_NDK_IMAGE_H

/** @} */
