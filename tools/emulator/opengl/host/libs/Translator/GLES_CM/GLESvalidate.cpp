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
#include "GLESvalidate.h"
#include <GLcommon/GLutils.h>

bool  GLESvalidate::textureEnum(GLenum e,unsigned int maxTex) {
    return e >= GL_TEXTURE0 && e <= (GL_TEXTURE0 + maxTex);
}

bool GLESvalidate::lightEnum(GLenum e,unsigned int maxLights) {
    return  e >=GL_LIGHT0 && e <= (GL_LIGHT0+maxLights);
}

bool GLESvalidate::clipPlaneEnum(GLenum e,unsigned int maxClipPlanes) {
    return  e >=GL_CLIP_PLANE0 && e <= (GL_CLIP_PLANE0+maxClipPlanes);
}

bool GLESvalidate::textureTarget(GLenum target) {
    return target == GL_TEXTURE_2D;
}


bool GLESvalidate::alphaFunc(GLenum f) {
    switch(f) {
    case GL_NEVER:
    case GL_LESS:
    case GL_EQUAL:
    case GL_LEQUAL:
    case GL_GREATER:
    case GL_NOTEQUAL:
    case GL_GEQUAL:
    case GL_ALWAYS:
        return true;
    }
    return false;
}

bool GLESvalidate::blendSrc(GLenum s) {
   switch(s) {
    case GL_ZERO:
    case GL_ONE:
    case GL_DST_COLOR:
    case GL_ONE_MINUS_DST_COLOR:
    case GL_SRC_ALPHA:
    case GL_ONE_MINUS_SRC_ALPHA:
    case GL_DST_ALPHA:
    case GL_ONE_MINUS_DST_ALPHA:
        return true;
  }
  return false;
}

bool GLESvalidate::blendDst(GLenum d) {
   switch(d) {
    case GL_ZERO:
    case GL_ONE:
    case GL_SRC_COLOR:
    case GL_ONE_MINUS_SRC_COLOR:
    case GL_SRC_ALPHA:
    case GL_ONE_MINUS_SRC_ALPHA:
    case GL_DST_ALPHA:
    case GL_ONE_MINUS_DST_ALPHA:
        return true;
   }
   return false;
}

bool GLESvalidate::vertexPointerParams(GLint size,GLsizei stride) {
    return ((size >=2) && (size <= 4)) && (stride >=0) ;
}

bool GLESvalidate::colorPointerParams(GLint size,GLsizei stride) {
    return ((size >=3) && (size <= 4)) && (stride >=0) ;
}

bool GLESvalidate::texCoordPointerParams(GLint size,GLsizei stride) {
    return ((size >=1) && (size <= 4)) && (stride >=0) ;
}

bool GLESvalidate::supportedArrays(GLenum arr) {
    switch(arr) {
    case GL_COLOR_ARRAY:
    case GL_NORMAL_ARRAY:
    case GL_POINT_SIZE_ARRAY_OES:
    case GL_TEXTURE_COORD_ARRAY:
    case GL_VERTEX_ARRAY:
        return true;
    }
    return false;
}

bool GLESvalidate::drawMode(GLenum mode) {
    switch(mode) {
    case GL_POINTS:
    case GL_LINE_STRIP:
    case GL_LINE_LOOP:
    case GL_LINES:
    case GL_TRIANGLE_STRIP:
    case GL_TRIANGLE_FAN:
    case GL_TRIANGLES:
        return true;
    }
    return false;
}

bool GLESvalidate::drawType(GLenum mode) {
    return mode == GL_UNSIGNED_BYTE || mode == GL_UNSIGNED_SHORT;
}

bool GLESvalidate::hintTargetMode(GLenum target,GLenum mode) {
   switch(target) {
   case GL_FOG_HINT:
   case GL_GENERATE_MIPMAP_HINT:
   case GL_LINE_SMOOTH_HINT:
   case GL_PERSPECTIVE_CORRECTION_HINT:
   case GL_POINT_SMOOTH_HINT:
       break;
   default: return false;
   }
   switch(mode) {
   case GL_FASTEST:
   case GL_NICEST:
   case GL_DONT_CARE:
       break;
   default: return false;
   }
   return true;
}

bool GLESvalidate::texParams(GLenum target,GLenum pname) {
    switch(pname) {
    case GL_TEXTURE_MIN_FILTER:
    case GL_TEXTURE_MAG_FILTER:
    case GL_TEXTURE_WRAP_S:
    case GL_TEXTURE_WRAP_T:
        break;
    default:
        return false;
    }
    return target == GL_TEXTURE_2D;
}

bool GLESvalidate::texEnv(GLenum target,GLenum pname) {
    switch(pname) {
    case GL_TEXTURE_ENV_MODE:
    case GL_COMBINE_RGB:
    case GL_COMBINE_ALPHA:
    case GL_SRC0_RGB:
    case GL_SRC1_RGB:
    case GL_SRC2_RGB:
    case GL_SRC0_ALPHA:
    case GL_SRC1_ALPHA:
    case GL_SRC2_ALPHA:
    case GL_OPERAND0_RGB:
    case GL_OPERAND1_RGB:
    case GL_OPERAND2_RGB:
    case GL_OPERAND0_ALPHA:
    case GL_OPERAND1_ALPHA:
    case GL_OPERAND2_ALPHA:
    case GL_RGB_SCALE:
    case GL_ALPHA_SCALE:
    case GL_COORD_REPLACE_OES:
        break;
    default:
        return false;
    }
    return (target == GL_TEXTURE_ENV || target == GL_POINT_SPRITE_OES);
}

bool GLESvalidate::capability(GLenum cap,int maxLights,int maxClipPlanes) {
    switch(cap) {
    case GL_ALPHA_TEST:
    case GL_BLEND:
    case GL_COLOR_ARRAY:
    case GL_COLOR_LOGIC_OP:
    case GL_COLOR_MATERIAL:
    case GL_CULL_FACE:
    case GL_DEPTH_TEST:
    case GL_DITHER:
    case GL_FOG:
    case GL_LIGHTING:
    case GL_LINE_SMOOTH:
    case GL_MULTISAMPLE:
    case GL_NORMAL_ARRAY:
    case GL_NORMALIZE:
    case GL_POINT_SIZE_ARRAY_OES:
    case GL_POINT_SMOOTH:
    case GL_POINT_SPRITE_OES:
    case GL_POLYGON_OFFSET_FILL:
    case GL_RESCALE_NORMAL:
    case GL_SAMPLE_ALPHA_TO_COVERAGE:
    case GL_SAMPLE_ALPHA_TO_ONE:
    case GL_SAMPLE_COVERAGE:
    case GL_SCISSOR_TEST:
    case GL_STENCIL_TEST:
    case GL_TEXTURE_2D:
    case GL_TEXTURE_COORD_ARRAY:
    case GL_VERTEX_ARRAY:
        return true;
    }
    return GLESvalidate::lightEnum(cap,maxLights) || GLESvalidate::clipPlaneEnum(cap,maxClipPlanes);
}

bool GLESvalidate::pixelType(GLenum type) {
    switch(type) {
    case GL_UNSIGNED_BYTE:
    case GL_UNSIGNED_SHORT_5_6_5:
    case GL_UNSIGNED_SHORT_4_4_4_4:
    case GL_UNSIGNED_SHORT_5_5_5_1:
        return true;
    }
    return false;
}

bool GLESvalidate::pixelFrmt(GLenum format) {
    switch(format) {
    case GL_ALPHA:
    case GL_RGB:
    case GL_RGBA:
    case GL_LUMINANCE:
    case GL_LUMINANCE_ALPHA:
        return true;
    }
    return false;
}

bool GLESvalidate::texCompImgFrmt(GLenum format) {
    switch(format) {
    case GL_PALETTE4_RGB8_OES:
    case GL_PALETTE4_RGBA8_OES:
    case GL_PALETTE4_R5_G6_B5_OES:
    case GL_PALETTE4_RGBA4_OES:
    case GL_PALETTE4_RGB5_A1_OES:
    case GL_PALETTE8_RGB8_OES:
    case GL_PALETTE8_RGBA8_OES:
    case GL_PALETTE8_R5_G6_B5_OES:
    case GL_PALETTE8_RGBA4_OES:
    case GL_PALETTE8_RGB5_A1_OES:
        return true;
    }
    return false;
}

bool GLESvalidate::pixelOp(GLenum format,GLenum type) {
     switch(type) {
     case GL_UNSIGNED_SHORT_4_4_4_4:
     case GL_UNSIGNED_SHORT_5_5_5_1:
         return format == GL_RGBA;
     case GL_UNSIGNED_SHORT_5_6_5:
         return format == GL_RGB;
     }
     return true;
}

bool GLESvalidate::texImgDim(GLsizei width,GLsizei height,int maxTexSize) {

 if( width < 0 || height < 0 || width > maxTexSize || height > maxTexSize)
    return false;
 return isPowerOf2(width) && isPowerOf2(height);
}

bool GLESvalidate::bufferTarget(GLenum target) {
    return target == GL_ARRAY_BUFFER || target == GL_ELEMENT_ARRAY_BUFFER;
}

bool GLESvalidate::bufferParam(GLenum param) {
 return  (param == GL_BUFFER_SIZE) || (param == GL_BUFFER_USAGE);
}
