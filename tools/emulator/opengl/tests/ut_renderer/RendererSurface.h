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
#ifndef _RENDERER_SURFACE_H_
#define _RENDERER_SURFACE_H_

#include <EGL/egl.h>
#include "NativeWindowing.h"
#include "RendererObject.h"

#define DEFAULT_HEIGHT 480
#define DEFAULT_WIDTH 320

class RendererSurface : public RendererObject {
public:
    typedef enum { CONFIG_DEPTH = 1 << 0 } SurfaceConfig;

    EGLSurface eglSurface() { return m_eglSurface; }
    EGLConfig eglConfig() { return m_config; }
    EGLDisplay eglDisplay() { return m_eglDisplay; }

    static RendererSurface * create(EGLDisplay eglDisplay, SurfaceConfig config, NativeWindowing *nw);
    static EGLConfig getEglConfig(EGLDisplay eglDisplay, SurfaceConfig config);

    int destroy(NativeWindowing *nw);

private:
    RendererSurface(EGLDisplay display, NativeWindowType window, EGLSurface surface, EGLConfig config) :
        m_eglDisplay(display),
        m_config(config),
        m_window(window),
        m_eglSurface(surface)
    {}

    EGLDisplay m_eglDisplay;
    EGLConfig m_config;
    NativeWindowType m_window;
    EGLSurface m_eglSurface;
};
#endif
