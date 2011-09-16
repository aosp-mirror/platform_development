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
#include "EglPixmapSurface.h"
#include "EglOsApi.h"

std::set<EGLNativePixmapType> EglPixmapSurface::s_associatedPixmaps;

bool EglPixmapSurface::alreadyAssociatedWithConfig(EGLNativePixmapType pix) {
    return s_associatedPixmaps.find(pix) != s_associatedPixmaps.end();

}

EglPixmapSurface::EglPixmapSurface(EglDisplay *dpy,
                                   EGLNativePixmapType pix,
                                   EglConfig* config) :
           EglSurface(dpy, PIXMAP,config,0,0),
           m_pixmap(pix)
{
    s_associatedPixmaps.insert(pix);
    m_native = EglOS::createPixmapSurface(pix);
}

EglPixmapSurface::~EglPixmapSurface() {
    s_associatedPixmaps.erase(m_pixmap);
}

bool EglPixmapSurface::getAttrib(EGLint attrib,EGLint* val) {
    switch(attrib) {
    case EGL_CONFIG_ID:
        *val = m_config->id();
        break;
    case EGL_WIDTH:
        *val = m_width;
        break;
    case EGL_HEIGHT:
        *val = m_height;
        break;
    case EGL_LARGEST_PBUFFER:
    case EGL_TEXTURE_FORMAT:
    case EGL_TEXTURE_TARGET:
    case EGL_MIPMAP_TEXTURE:
        break;
    default:
        return false;
    }
    return true;
}
