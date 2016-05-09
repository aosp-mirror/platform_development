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
 * @file NdkImageReader.h
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

#ifndef _NDK_IMAGE_READER_H
#define _NDK_IMAGE_READER_H

#include <android/native_window.h>
#include "NdkMediaError.h"
#include "NdkImage.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct AImageReader AImageReader;

media_status_t AImageReader_new(
        int32_t width, int32_t height, int32_t format, int32_t maxImages,
        /*out*/AImageReader** reader);

// Return all images acquired from this AImageReader back to system and delete
// the AImageReader instance from memory
// Do NOT use `reader` after this call
void AImageReader_delete(AImageReader* reader);

// Do NOT call ANativeWindow_release on the output. Just use AImageReader_delete.
media_status_t AImageReader_getWindow(AImageReader*, /*out*/ANativeWindow** window);

media_status_t AImageReader_getWidth(const AImageReader* reader, /*out*/int32_t* width);
media_status_t AImageReader_getHeight(const AImageReader* reader, /*out*/int32_t* height);
media_status_t AImageReader_getFormat(const AImageReader* reader, /*out*/int32_t* format);
media_status_t AImageReader_getMaxImages(const AImageReader* reader, /*out*/int32_t* maxImages);

media_status_t AImageReader_acquireNextImage(AImageReader* reader, /*out*/AImage** image);

media_status_t AImageReader_acquireLatestImage(AImageReader* reader, /*out*/AImage** image);

// The callback happens on one dedicated thread per AImageReader instance
// It's okay to use AImageReader_*/AImage_* APIs within the callback
typedef void (*AImageReader_ImageCallback)(void* context, AImageReader* reader);

typedef struct AImageReader_ImageListener {
    void*                      context; // optional application context.
    AImageReader_ImageCallback onImageAvailable;
} AImageReader_ImageListener;

media_status_t AImageReader_setImageListener(
        AImageReader* reader, AImageReader_ImageListener* listener);

#ifdef __cplusplus
} // extern "C"
#endif

#endif //_NDK_IMAGE_READER_H

/** @} */
