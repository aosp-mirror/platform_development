/*
* Copyright (C) 2011 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#ifndef __GL_UTILS_H__
#define __GL_UTILS_H__

#include <stdio.h>
#include <stdlib.h>

#ifdef GL_API
    #undef GL_API
#endif
#define GL_API

#ifdef GL_APIENTRY
    #undef GL_APIENTRY
#endif

#ifdef GL_APIENTRYP
    #undef GL_APIENTRYP
#endif
#define GL_APIENTRYP

#ifndef ANDROID
#define GL_APIENTRY
#endif

#include <GLES/gl.h>
#include <GLES/glext.h>

#ifdef __cplusplus
extern "C" {
#endif

    size_t glSizeof(GLenum type);
    size_t glUtilsParamSize(GLenum param);
    void   glUtilsPackPointerData(unsigned char *dst, unsigned char *str,
                           int size, GLenum type, unsigned int stride,
                           unsigned int datalen);
    int glUtilsPixelBitSize(GLenum format, GLenum type);
#ifdef __cplusplus
};
#endif

#endif
