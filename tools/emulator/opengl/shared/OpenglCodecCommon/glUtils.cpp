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
#include "glUtils.h"
#include <string.h>
#include "ErrorLog.h"

size_t glSizeof(GLenum type)
{
    size_t retval = 0;
    switch(type) {
    case GL_BYTE:
    case GL_UNSIGNED_BYTE:
        retval = 1;
        break;
    case GL_SHORT:
    case GL_UNSIGNED_SHORT:
        retval = 2;
        break;
    case GL_FLOAT:
    case GL_FIXED:
        retval =  4;
        break;
#ifdef GL_DOUBLE
    case GL_DOUBLE:
        retval = 8;
        break;
#endif
    }
    return retval;

}

size_t glUtilsParamSize(GLenum param)
{
    size_t s = 0;

    switch(param)
    {
    case GL_MAX_TEXTURE_SIZE:
    case GL_TEXTURE_GEN_MODE_OES:
    case GL_TEXTURE_ENV_MODE:
    case GL_FOG_MODE:
    case GL_FOG_DENSITY:
    case GL_FOG_START:
    case GL_FOG_END:
    case GL_SPOT_EXPONENT:
    case GL_CONSTANT_ATTENUATION:
    case GL_LINEAR_ATTENUATION:
    case GL_QUADRATIC_ATTENUATION:
    case GL_SHININESS:
    case GL_LIGHT_MODEL_TWO_SIDE:
    case GL_POINT_SIZE:
    case GL_POINT_SIZE_MIN:
    case GL_POINT_SIZE_MAX:
    case GL_POINT_FADE_THRESHOLD_SIZE:
    case GL_CULL_FACE_MODE:
    case GL_FRONT_FACE:
    case GL_SHADE_MODEL:
    case GL_DEPTH_WRITEMASK:
    case GL_DEPTH_CLEAR_VALUE:
    case GL_STENCIL_FAIL:
    case GL_STENCIL_PASS_DEPTH_FAIL:
    case GL_STENCIL_PASS_DEPTH_PASS:
    case GL_STENCIL_REF:
    case GL_STENCIL_WRITEMASK:
    case GL_MATRIX_MODE:
    case GL_MODELVIEW_STACK_DEPTH:
    case GL_PROJECTION_STACK_DEPTH:
    case GL_TEXTURE_STACK_DEPTH:
    case GL_ALPHA_TEST_FUNC:
    case GL_ALPHA_TEST_REF:
    case GL_BLEND_DST:
    case GL_BLEND_SRC:
    case GL_LOGIC_OP_MODE:
    case GL_SCISSOR_TEST:
    case GL_MAX_TEXTURE_UNITS:
        s = 1;
        break;
    case GL_ALIASED_LINE_WIDTH_RANGE:
    case GL_ALIASED_POINT_SIZE_RANGE:
    case GL_DEPTH_RANGE:
    case GL_MAX_VIEWPORT_DIMS:
    case GL_SMOOTH_POINT_SIZE_RANGE:
    case GL_SMOOTH_LINE_WIDTH_RANGE:
        s= 2;
        break;
    case GL_SPOT_DIRECTION:
    case GL_POINT_DISTANCE_ATTENUATION:
    case GL_CURRENT_NORMAL:
        s =  3;
        break;
    case GL_CURRENT_TEXTURE_COORDS:
    case GL_CURRENT_COLOR:
    case GL_FOG_COLOR:
    case GL_AMBIENT:
    case GL_DIFFUSE:
    case GL_SPECULAR:
    case GL_EMISSION:
    case GL_POSITION:
    case GL_LIGHT_MODEL_AMBIENT:
    case GL_TEXTURE_ENV_COLOR:
    case GL_SCISSOR_BOX:
    case GL_VIEWPORT:
    case GL_TEXTURE_CROP_RECT_OES:
        s =  4;
        break;
    case GL_MODELVIEW_MATRIX:
    case GL_PROJECTION_MATRIX:
    case GL_TEXTURE_MATRIX:
        s = 16;
    default:
        ERR("glUtilsParamSize: unknow param 0x%08x\n", param);
        s = 1; // assume 1
    }
    return s;
}

void glUtilsPackPointerData(unsigned char *dst, unsigned char *src,
                     int size, GLenum type, unsigned int stride,
                     unsigned int datalen)
{
    unsigned int  vsize = size * glSizeof(type);
    if (stride == 0) stride = vsize;

    if (stride == vsize) {
        memcpy(dst, src, datalen);
    } else {
        for (unsigned int i = 0; i < datalen; i += vsize) {
            memcpy(dst, src, vsize);
            dst += vsize;
            src += stride;
        }
    }
}

int glUtilsPixelBitSize(GLenum format, GLenum type)
{
    int components = 0;
    int componentsize = 0;
    int pixelsize = 0;
    switch(type) {
    case GL_UNSIGNED_BYTE:
        componentsize = 8;
        break;
    case GL_UNSIGNED_SHORT_5_6_5:
    case GL_UNSIGNED_SHORT_4_4_4_4:
    case GL_UNSIGNED_SHORT_5_5_5_1:
    case GL_RGB565_OES:
    case GL_RGB5_A1_OES:
    case GL_RGBA4_OES:
        pixelsize = 16;
        break;
    default:
        ERR("glUtilsPixelBitSize: unknown pixel type - assuming pixel data 0\n");
        componentsize = 0;
    }

    if (pixelsize == 0) {
        switch(format) {
#if 0
        case GL_RED:
        case GL_GREEN:
        case GL_BLUE:
#endif
        case GL_ALPHA:
        case GL_LUMINANCE:
            components = 1;
            break;
        case GL_LUMINANCE_ALPHA:
            components = 2;
            break;
        case GL_RGB:
#if 0
        case GL_BGR:
#endif
            components = 3;
            break;
        case GL_RGBA:
        case GL_BGRA_EXT:
            components = 4;
            break;
        default:
            ERR("glUtilsPixelBitSize: unknown pixel format...\n");
            components = 0;
        }
        pixelsize = components * componentsize;
    }

    return pixelsize;
}
