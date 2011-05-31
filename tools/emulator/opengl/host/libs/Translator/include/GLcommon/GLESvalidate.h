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
#ifndef GLES_VALIDATE_H
#define GLES_VALIDATE_H

#include <GLES/gl.h>

struct GLESvalidate
{
static bool textureEnum(GLenum e,unsigned int maxTex);
static bool pixelType(GLenum type);
static bool pixelOp(GLenum format,GLenum type); 
static bool pixelFrmt(GLenum format);
static bool bufferTarget(GLenum target);
static bool bufferParam(GLenum param);
static bool drawMode(GLenum mode);
static bool drawType(GLenum mode);
};

#endif
