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
#include "X11RendererSurface.h"

NativeDisplayType X11RendererSurface::getNativeDisplay()
{
    if (m_display == NULL) {
        m_display = XOpenDisplay(NULL);
    }
    return NativeDisplayType(m_display);
}

int X11RendererSurface::destoryNativeWindow(NativeWindowType win)
{
    if (m_display == NULL) return -1;

    Window x11Window = (Window)(win);
    return XDestroyWindow(m_display, x11Window);
}

NativeWindowType GlesX11Win::createNativeWindow()
{

    getNativeDisplay();
    if (m_display == NULL) {
        return -1;
    }

    long defaultScreen = DefaultScreen( dpy );
    Window rootWindow = RootWindow(dpy, defaultScreen);
    int depth = DefaultDepth(dpy, defaultScreen);
    XVisualInfo *visualInfo = new XVisualInfo;

    XMatchVisualInfo(m_display, defaultScreen, , dpeth, TrueColor, visualInfo);
    if (visualInfo == NULL) {
        fprintf(stderr, "couldn't find matching visual\n");
        return -1;
    }

    Colormap x11Colormap = XCreateColormap(m_display, rootWindow, visualInfo->visual, AllocNone);
    XSetWindowAttributes sWA;
    sWA.Colormap = x11Colormap;
    sWA.event_mask = StructureNotifyMask | ExposureMask;
    unsigned int eventMask = CWBackPixel | CWBorderPixel | CWEventMask | CWColormap;

    Window win = XCreateWindow( m_display,
                                rootWindow,
                               0, 0, width, height,
                                 0, CopyFromParent, InputOutput,
                               CopyFromParent, eventMask, &sWA);

    XMapWindow(m_display, win);
    XFlush(m_display);
    return NativeWindowType(win);
}

