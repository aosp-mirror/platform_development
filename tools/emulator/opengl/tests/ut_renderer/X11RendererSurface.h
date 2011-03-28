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
#ifndef _X11_RENDERER_SURFACE_H_
#define _X11_RENDERER_SURFACE_H_

#include <X11/Xutil.h>
#include <X11/Xlib.h>
#include <EGL/egl.h>

include "RendererSurface.h"

class X11RendererSurface : public RendererSurface
{
public:
    X11RendererSurface() : RendererSurface() {
        m_display = NULL;
    }
    NativeDisplayType getNativeDisplay();
    NativeWindowType createNativeWindow();
    int destroyNativeWindow(NativeWindowType win);
private:
    Display m_display;
};
#endif
