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
#include "X11Windowing.h"

#include <stdio.h>
#include <stdlib.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>

NativeDisplayType X11Windowing::getNativeDisplay()
{
    Display *dpy = XOpenDisplay(NULL);
    return (NativeDisplayType)dpy;
}

NativeWindowType X11Windowing::createNativeWindow(NativeDisplayType _dpy, int width, int height)
{
    Display *dpy = (Display *) _dpy;

    long defaultScreen = DefaultScreen( dpy );
    Window rootWindow = RootWindow(dpy, defaultScreen);
    int depth = DefaultDepth(dpy, defaultScreen);
    XVisualInfo *visualInfo = new XVisualInfo;

    XMatchVisualInfo(dpy, defaultScreen, depth, TrueColor, visualInfo);
    if (visualInfo == NULL) {
        fprintf(stderr, "couldn't find matching visual\n");
        return NULL;
    }

    Colormap x11Colormap = XCreateColormap(dpy, rootWindow, visualInfo->visual, AllocNone);
    XSetWindowAttributes sWA;
    sWA.colormap = x11Colormap;
    sWA.event_mask = StructureNotifyMask | ExposureMask;
    sWA.background_pixel = 0;
    sWA.border_pixel = 0;
    unsigned int attributes_mask = CWBackPixel | CWBorderPixel | CWEventMask | CWColormap;

    Window win = XCreateWindow( dpy,
                                rootWindow,
                               0, 0, width, height,
                                 0, CopyFromParent, InputOutput,
                               CopyFromParent, attributes_mask, &sWA);

    XMapWindow(dpy, win);
    XFlush(dpy);
    return NativeWindowType(win);
}

int X11Windowing::destroyNativeWindow(NativeDisplayType _dpy, NativeWindowType _win)
{
    Display *dpy = (Display *)_dpy;
    Window win = (Window)_win;
    XDestroyWindow(dpy, win);
    XFlush(dpy);
    return 0;
}
