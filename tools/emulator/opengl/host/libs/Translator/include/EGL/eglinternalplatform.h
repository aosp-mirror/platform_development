#ifndef EGL_INTERNAL_PLATFORM_H
#define EGL_INTERNAL_PLATFORM_H

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

class SrfcInfo; //defined in Egl{$platform}Api.cpp
typedef SrfcInfo* SURFACE;
typedef SURFACE EGLNativeSurfaceType;

#if defined(_WIN32) || defined(__VC32__) && !defined(__CYGWIN__) && !defined(__SCITECH_SNAP__) /* Win32 and WinCE */
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN 1
#endif

#include <GL/gl.h>
#define WGL_WGLEXT_PROTOTYPES
#include <GL/wglext.h>

class WinDisplay; //defined in EglWindows.cpp
typedef WinDisplay* DISPLAY;

typedef PIXELFORMATDESCRIPTOR  EGLNativePixelFormatType;
#define PIXEL_FORMAT_INITIALIZER {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
typedef HGLRC                  EGLNativeContextType;
typedef HPBUFFERARB            EGLNativePbufferType;
typedef DISPLAY                EGLNativeInternalDisplayType;

#elif defined(__APPLE__)

typedef void*                  EGLNativePixelFormatType;
#define PIXEL_FORMAT_INITIALIZER NULL
typedef void*                  EGLNativeContextType;
typedef void*                  EGLNativePbufferType;
typedef EGLNativeDisplayType   EGLNativeInternalDisplayType;


#elif defined(__unix__)

/* X11 (tentative)  */
#include <GL/glx.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>

typedef GLXFBConfig           EGLNativePixelFormatType;
#define PIXEL_FORMAT_INITIALIZER 0;
typedef GLXContext            EGLNativeContextType;
typedef GLXPbuffer            EGLNativePbufferType;
typedef EGLNativeDisplayType  EGLNativeInternalDisplayType;

#else
#error "Platform not recognized"
#endif


#endif
