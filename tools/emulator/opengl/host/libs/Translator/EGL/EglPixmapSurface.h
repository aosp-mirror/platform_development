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
#ifndef EGL_PIXMAP_SURFACE_H
#define EGL_PIXMAP_SURFACE_H

#include <set>
#include <EGL/egl.h>
#include "EglSurface.h"

class EglDisplay;

class EglPixmapSurface: public EglSurface {
public:
    EglPixmapSurface(EglDisplay *dpy, EGLNativePixmapType pix,EglConfig* config);
    ~EglPixmapSurface();

    bool getAttrib(EGLint attrib,EGLint* val);

    static bool alreadyAssociatedWithConfig(EGLNativePixmapType pix);
private:
   EGLNativePixmapType m_pixmap;
   static std::set<EGLNativePixmapType> s_associatedPixmaps;
};
#endif
