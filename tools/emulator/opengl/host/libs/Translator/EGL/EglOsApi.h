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
#include <EGL/eglinternalplatform.h>
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

    void queryConfigs(EGLNativeInternalDisplayType dpy,int renderable_type,ConfigsList& listOut);
    bool releasePbuffer(EGLNativeInternalDisplayType dis,EGLNativeSurfaceType pb);
    bool destroyContext(EGLNativeInternalDisplayType dpy,EGLNativeContextType ctx);
    bool releaseDisplay(EGLNativeInternalDisplayType dpy);
    bool validNativeWin(EGLNativeInternalDisplayType dpy,EGLNativeSurfaceType win);
    bool validNativeWin(EGLNativeInternalDisplayType dpy,EGLNativeWindowType win);
    bool validNativePixmap(EGLNativeInternalDisplayType dpy,EGLNativeSurfaceType pix);
    bool checkWindowPixelFormatMatch(EGLNativeInternalDisplayType dpy,EGLNativeWindowType win,EglConfig* cfg,unsigned int* width,unsigned int* height);
    bool checkPixmapPixelFormatMatch(EGLNativeInternalDisplayType dpy,EGLNativePixmapType pix,EglConfig* cfg,unsigned int* width,unsigned int* height);
    bool makeCurrent(EGLNativeInternalDisplayType dpy,EglSurface* read,EglSurface* draw,EGLNativeContextType);
    void swapBuffers(EGLNativeInternalDisplayType dpy,EGLNativeSurfaceType srfc);
    void swapInterval(EGLNativeInternalDisplayType dpy,EGLNativeSurfaceType win,int interval);
    void waitNative();

    EGLNativeInternalDisplayType getDefaultDisplay();
    EGLNativeInternalDisplayType getInternalDisplay(EGLNativeDisplayType dpy);
    void deleteDisplay(EGLNativeInternalDisplayType idpy);
    EGLNativeSurfaceType createPbufferSurface(EGLNativeInternalDisplayType dpy,EglConfig* cfg,EglPbufferSurface* pb);
    EGLNativeContextType createContext(EGLNativeInternalDisplayType dpy,EglConfig* cfg,EGLNativeContextType sharedContext);
    EGLNativeSurfaceType createWindowSurface(EGLNativeWindowType wnd);
    EGLNativeSurfaceType createPixmapSurface(EGLNativePixmapType pix);
    void destroySurface(EGLNativeSurfaceType srfc);
#ifdef _WIN32
    void initPtrToWglFunctions();
#endif
};

#endif
