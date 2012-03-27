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

/* If a function with this signature is passed to initOpenGLRenderer(),
 * it will be called by the renderer just before each new frame is displayed,
 * providing a copy of the framebuffer contents.
 *
 * The callback will be called from one of the renderer's threads, so will
 * probably need synchronization on any data structures it modifies. The
 * pixels buffer may be overwritten as soon as the callback returns; if it needs
 * the pixels afterwards it must copy them.
 *
 * The pixels buffer is intentionally not const: the callback may modify the
 * data without copying to another buffer if it wants, e.g. in-place RGBA to RGB
 * conversion, or in-place y-inversion.
 *
 * Parameters are:
 *   context        The pointer optionally provided when the callback was
 *                  registered. The client can use this to pass whatever
 *                  information it wants to the callback.
 *   width, height  Dimensions of the image, in pixels. Rows are tightly packed;
 *                  there is no inter-row padding.
 *   ydir           Indicates row order: 1 means top-to-bottom order, -1 means
 *                  bottom-to-top order.
 *   format, type   Format and type GL enums, as used in glTexImage2D() or
 *                  glReadPixels(), describing the pixel format.
 *   pixels         The framebuffer image.
 *
 * In the first implementation, ydir is always -1 (bottom to top), format and
 * type are always GL_RGBA and GL_UNSIGNED_BYTE, and the width and height will
 * always be the same as the ones passed to initOpenGLRenderer().
 */
typedef void (*OnPostFn)(void* context, int width, int height, int ydir,
                         int format, int type, unsigned char* pixels);

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
bool initOpenGLRenderer(int width, int height, int portNum,
                        OnPostFn onPost, void* onPostContext);

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
