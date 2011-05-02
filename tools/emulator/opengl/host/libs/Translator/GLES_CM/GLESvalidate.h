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

static bool lightEnum(GLenum e,unsigned int maxLIghts);
static bool clipPlaneEnum(GLenum e,unsigned int maxClipPlanes);
static bool alphaFunc(GLenum f);
static bool blendSrc(GLenum s);
static bool blendDst(GLenum d);
static bool vertexPointerParams(GLint size,GLsizei stride);
static bool colorPointerParams(GLint size,GLsizei stride);
static bool supportedArrays(GLenum arr);
static bool drawMode(GLenum mode);
static bool drawType(GLenum mode);
static bool hintTargetMode(GLenum target,GLenum mode);
static bool capability(GLenum cap,int maxLights,int maxClipPlanes);
static bool texParams(GLenum target,GLenum pname);
static bool texCoordPointerParams(GLint size,GLsizei stride);
static bool textureTarget(GLenum target);
static bool textureEnum(GLenum e,unsigned int maxTex);
static bool texEnv(GLenum target,GLenum pname);
static bool pixelFrmt(GLenum format);
static bool pixelType(GLenum type);
static bool pixelOp(GLenum format,GLenum type);
static bool texCompImgFrmt(GLenum format);
static bool texImgDim(GLsizei width,GLsizei height,int maxTexSize);
static bool bufferTarget(GLenum target);
static bool bufferParam(GLenum param);
};

#endif
