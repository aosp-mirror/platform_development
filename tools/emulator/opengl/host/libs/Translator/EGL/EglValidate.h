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
#ifndef EGL_VALIDATE_H
#define EGL_VALIDATE_H

#include <EGL/egl.h>

class EglValidate {
public:
    static bool confAttrib(EGLint attrib);
    static bool noAttribs(const EGLint* attrib);
    static bool pbufferAttribs(EGLint width,EGLint height,bool texFormatIsNoTex,bool texTargetIsNoTex);
    static bool releaseContext(EGLContext ctx,EGLSurface s1,EGLSurface s2);
    static bool badContextMatch(EGLContext ctx,EGLSurface s1,EGLSurface s2);
    static bool surfaceTarget(EGLint target);
    static bool engine(EGLint engine);
    static bool stringName(EGLint name);
    static bool supportedApi(EGLenum api);
};
#endif
