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
#include "GLESv2Validate.h"

bool GLESv2Validate::blendEquationMode(GLenum mode){
    return mode == GL_FUNC_ADD             ||
           mode == GL_FUNC_SUBTRACT        ||
           mode == GL_FUNC_REVERSE_SUBTRACT;
}

bool GLESv2Validate::blendSrc(GLenum s) {
   switch(s) {
   case GL_ZERO:
   case GL_ONE:
   case GL_SRC_COLOR:
   case GL_ONE_MINUS_SRC_COLOR:
   case GL_DST_COLOR:
   case GL_ONE_MINUS_DST_COLOR:
   case GL_SRC_ALPHA:
   case GL_ONE_MINUS_SRC_ALPHA:
   case GL_DST_ALPHA:
   case GL_ONE_MINUS_DST_ALPHA:
   case GL_CONSTANT_COLOR:
   case GL_ONE_MINUS_CONSTANT_COLOR:
   case GL_CONSTANT_ALPHA:
   case GL_ONE_MINUS_CONSTANT_ALPHA:
   case GL_SRC_ALPHA_SATURATE:
        return true;
  }
  return false;
}


bool GLESv2Validate::blendDst(GLenum d) {
   switch(d) {
   case GL_ZERO:
   case GL_ONE:
   case GL_SRC_COLOR:
   case GL_ONE_MINUS_SRC_COLOR:
   case GL_DST_COLOR:
   case GL_ONE_MINUS_DST_COLOR:
   case GL_SRC_ALPHA:
   case GL_ONE_MINUS_SRC_ALPHA:
   case GL_DST_ALPHA:
   case GL_ONE_MINUS_DST_ALPHA:
   case GL_CONSTANT_COLOR:
   case GL_ONE_MINUS_CONSTANT_COLOR:
   case GL_CONSTANT_ALPHA:
   case GL_ONE_MINUS_CONSTANT_ALPHA:
        return true;
   }
   return false;
}

bool GLESv2Validate::textureTarget(GLenum target){
    return target == GL_TEXTURE_2D || target == GL_TEXTURE_CUBE_MAP;
}

bool GLESv2Validate::textureParams(GLenum param){
    switch(param) {
    case GL_TEXTURE_MIN_FILTER:
    case GL_TEXTURE_MAG_FILTER:
    case GL_TEXTURE_WRAP_S:
    case GL_TEXTURE_WRAP_T:
        return true;
    default:
        return false;
    }
}

bool GLESv2Validate::hintTargetMode(GLenum target,GLenum mode){

   switch(mode) {
   case GL_FASTEST:
   case GL_NICEST:
   case GL_DONT_CARE:
       break;
   default: return false;
   }
   return target == GL_GENERATE_MIPMAP_HINT;
}

bool GLESv2Validate::capability(GLenum cap){
    switch(cap){
    case GL_BLEND:
    case GL_CULL_FACE:
    case GL_DEPTH_TEST:
    case GL_DITHER:
    case GL_POLYGON_OFFSET_FILL:
    case GL_SAMPLE_ALPHA_TO_COVERAGE:
    case GL_SAMPLE_COVERAGE:
    case GL_SCISSOR_TEST:
    case GL_STENCIL_TEST:
        return true;
    }
    return false;
}

bool GLESv2Validate::pixelStoreParam(GLenum param){
    return param == GL_PACK_ALIGNMENT || param == GL_UNPACK_ALIGNMENT;
}

bool GLESv2Validate::readPixelFrmt(GLenum format){
    switch(format) {
    case GL_ALPHA:
    case GL_RGB:
    case GL_RGBA:
        return true;
    }
    return false;
}

bool GLESv2Validate::textureTargetEx(GLenum target){
    switch(target){
    case GL_TEXTURE_2D:
    case GL_TEXTURE_CUBE_MAP_POSITIVE_X:
    case GL_TEXTURE_CUBE_MAP_NEGATIVE_X:
    case GL_TEXTURE_CUBE_MAP_POSITIVE_Y:
    case GL_TEXTURE_CUBE_MAP_NEGATIVE_Y:
    case GL_TEXTURE_CUBE_MAP_POSITIVE_Z:
    case GL_TEXTURE_CUBE_MAP_NEGATIVE_Z:
        return true;
    }
    return false;
}

bool GLESv2Validate::framebufferTarget(GLenum target){
    return target == GL_FRAMEBUFFER;
}

bool GLESv2Validate::framebufferAttachment(GLenum attachment){
    switch(attachment){
    case GL_COLOR_ATTACHMENT0:
    case GL_DEPTH_ATTACHMENT:
    case GL_STENCIL_ATTACHMENT:
        return true;
    }
    return false;
}

bool GLESv2Validate::framebufferAttachmentParams(GLenum pname){
    switch(pname){
    case GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE:
    case GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME:
    case GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL:
    case GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE:
        return true;
    }
    return false;
}

bool GLESv2Validate::renderbufferTarget(GLenum target){
    return target == GL_RENDERBUFFER;
}

bool GLESv2Validate::renderbufferParams(GLenum pname){
    switch(pname){
    case GL_RENDERBUFFER_WIDTH:
    case GL_RENDERBUFFER_HEIGHT:
    case GL_RENDERBUFFER_INTERNAL_FORMAT:
    case GL_RENDERBUFFER_RED_SIZE:
    case GL_RENDERBUFFER_GREEN_SIZE:
    case GL_RENDERBUFFER_BLUE_SIZE:
    case GL_RENDERBUFFER_ALPHA_SIZE:
    case GL_RENDERBUFFER_DEPTH_SIZE:
    case GL_RENDERBUFFER_STENCIL_SIZE:
        return true;
    }
    return false;
}

