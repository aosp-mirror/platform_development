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

#ifdef __cplusplus
extern "C" {
#endif

#include "render_api_platform_types.h"

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

#ifdef __cplusplus
}
#endif

#endif
