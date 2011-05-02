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
#ifndef EGL_OS_API_H
#define EGL_OS_API_H

#include <EGL/egl.h>
#ifdef __APPLE__
#include <OpenGL/gl.h>
#else
#include <GL/gl.h>
#endif
#include "EglConfig.h"
#include "EglDisplay.h"
#include "EglPbufferSurface.h"

#define PBUFFER_MAX_WIDTH  32767
#define PBUFFER_MAX_HEIGHT 32767
#define PBUFFER_MAX_PIXELS 32767*32767

namespace EglOS{

    void queryConfigs(EGLNativeDisplayType dpy,ConfigsList& listOut);
    bool releasePbuffer(EGLNativeDisplayType dis,EGLNativePbufferType pb);
    bool destroyContext(EGLNativeDisplayType dpy,EGLNativeContextType ctx);
    bool releaseDisplay(EGLNativeDisplayType dpy);
    bool validNativeWin(EGLNativeWindowType win);
    bool validNativePixmap(EGLNativePixmapType pix);
    bool checkWindowPixelFormatMatch(EGLNativeDisplayType dpy,EGLNativeWindowType win,EglConfig* cfg,unsigned int* width,unsigned int* height);
    bool checkPixmapPixelFormatMatch(EGLNativeDisplayType dpy,EGLNativePixmapType pix,EglConfig* cfg,unsigned int* width,unsigned int* height);
    bool makeCurrent(EGLNativeDisplayType dpy,EglSurface* read,EglSurface* draw,EGLNativeContextType);
    void swapBuffers(EGLNativeDisplayType dpy,EGLNativeWindowType win);
    void swapInterval(EGLNativeDisplayType dpy,EGLNativeWindowType win,int interval);
    void waitNative();

    EGLNativeDisplayType getDefaultDisplay();
    EGLNativePbufferType createPbuffer(EGLNativeDisplayType dpy,EglConfig* cfg,EglPbufferSurface* pb);
    EGLNativeContextType createContext(EGLNativeDisplayType dpy,EglConfig* cfg,EGLNativeContextType sharedContext);
};

#endif
