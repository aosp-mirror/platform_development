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
#include "EglWindowSurface.h"
#include "EglOsApi.h"

std::set<EGLNativeWindowType> EglWindowSurface::s_associatedWins;

bool EglWindowSurface::alreadyAssociatedWithConfig(EGLNativeWindowType win) {
    return s_associatedWins.find(win) != s_associatedWins.end();

}

EglWindowSurface::EglWindowSurface(EglDisplay *dpy, 
                                   EGLNativeWindowType win,
                                   EglConfig* config,
                                   unsigned int width,unsigned int height) :
                  EglSurface(dpy, WINDOW,config,width,height),
                  m_win(win)
{
    s_associatedWins.insert(win);
    m_native = EglOS::createWindowSurface(win);
}

EglWindowSurface:: ~EglWindowSurface() {
    s_associatedWins.erase(m_win);
}

bool  EglWindowSurface::getAttrib(EGLint attrib,EGLint* val) {
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
