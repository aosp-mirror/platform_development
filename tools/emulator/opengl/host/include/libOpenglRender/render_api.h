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

// initLibrary - initialize the library and tries to load the corresponding
//     GLES translator libraries. This function must be called before anything
//     else to ensure that everything works. If it returns an error, then
//     you cannot use the library at all (this can happen under certain
//     environments where the desktop GL libraries are not available)
//
// returns true if the library could be initialized successfully;
//
bool initLibrary(void);

// list of constants to be passed to setStreamMode, which determines
// which
#define STREAM_MODE_DEFAULT   0
#define STREAM_MODE_TCP       1
#define STREAM_MODE_UNIX      2
#define STREAM_MODE_PIPE      3

// Change the stream mode. This must be called before initOpenGLRenderer
int setStreamMode(int mode);

//
// initOpenGLRenderer - initialize the OpenGL renderer process.
//     portNum is the tcp port number the renderer is listening to.
//     width and height are the framebuffer dimensions that will be
//     reported to the guest display driver.
//
// returns true if renderer has been started successfully;
//
// This function is *NOT* thread safe and should be called first
// to initialize the renderer after initLibrary().
//
bool initOpenGLRenderer(int width, int height, int portNum);


//
// createOpenGLSubwindow -
//     Create a native subwindow which is a child of 'window'
//     to be used for framebuffer display.
//     Framebuffer will not get displayed if a subwindow is not
//     created.
//     x,y,width,height are the dimensions of the rendering subwindow.
//     zRot is the rotation to apply on the framebuffer display image.
//
bool createOpenGLSubwindow(FBNativeWindowType window,
                           int x, int y, int width, int height, float zRot);

//
// destroyOpenGLSubwindow -
//   destroys the created native subwindow. Once destroyed,
//   Framebuffer content will not be visible until a new
//   subwindow will be created.
//
bool destroyOpenGLSubwindow();

//
// setOpenGLDisplayRotation -
//    set the framebuffer display image rotation in units
//    of degrees around the z axis
//
void setOpenGLDisplayRotation(float zRot);

//
// repaintOpenGLDisplay -
//    causes the OpenGL subwindow to get repainted with the
//    latest framebuffer content.
//
void repaintOpenGLDisplay();

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
