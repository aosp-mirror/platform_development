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
#ifndef _OPENGL_RENDERER_RENDER_API_H
#define _OPENGL_RENDERER_RENDER_API_H

#include "IOStream.h"

#if defined(_WIN32) || defined(__VC32__) && !defined(__CYGWIN__) && !defined(__SCITECH_SNAP__) /* Win32 and WinCE */
#include <windows.h>

typedef HDC     FBNativeDisplayType;
typedef HWND    FBNativeWindowType;

#elif defined(__linux__)

#include <X11/Xlib.h>
#include <X11/Xutil.h>

typedef Window   FBNativeWindowType;

#else
#error "Unsupported platform"
#endif


//
// initOpenGLRenderer - initialize the OpenGL renderer process.
//     window is the native window to be used as the framebuffer.
//     x,y,width,height are the dimensions of the rendering subwindow.
//     portNum is the tcp port number the renderer is listening to.
//
// returns true if renderer has been starter successfully;
//
// This function is *NOT* thread safe and should be called first
// to initialize the renderer.
//
bool initOpenGLRenderer(FBNativeWindowType window,
                        int x, int y, int width, int height,
                        int portNum);

//
// stopOpenGLRenderer - stops the OpenGL renderer process.
//     This functions is *NOT* thread safe and should be called
//     only if previous initOpenGLRenderer has returned true.
//
bool stopOpenGLRenderer();

//
// createRenderThread - opens a new communication channel to the renderer
//   process and creates new rendering thread.
//   returns a pointer to IOStream through which command tokens are being sent
//   to the render thread for execution. 'p_stream_buffer_size' is the internal
//   stream buffer size.
//   The thread is destroyed when deleting the IOStream object.
//
IOStream *createRenderThread(int p_stream_buffer_size);

#endif
