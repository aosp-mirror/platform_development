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
#ifndef _LIBRENDER_FRAMEBUFFER_H
#define _LIBRENDER_FRAMEBUFFER_H

#include "libOpenglRender/render_api.h"
#include "ColorBuffer.h"
#include "RenderContext.h"
#include "WindowSurface.h"
#include <utils/threads.h>
#include <map>
#include <EGL/egl.h>
#include <stdint.h>

typedef uint32_t HandleType;
struct ColorBufferRef {
    ColorBufferPtr cb;
    uint32_t refcount;  // number of client-side references
};
typedef std::map<HandleType, RenderContextPtr> RenderContextMap;
typedef std::map<HandleType, WindowSurfacePtr> WindowSurfaceMap;
typedef std::map<HandleType, ColorBufferRef> ColorBufferMap;

struct FrameBufferCaps
{
    bool hasGL2;
    bool has_eglimage_texture_2d;
    bool has_eglimage_renderbuffer;
    EGLint eglMajor;
    EGLint eglMinor;
};

class FrameBuffer
{
public:
    static bool initialize(int width, int height);
    static bool setupSubWindow(FBNativeWindowType p_window,
                                int x, int y,
                                int width, int height, float zRot);
    static bool removeSubWindow();
    static void finalize();
    static FrameBuffer *getFB() { return s_theFrameBuffer; }

    const FrameBufferCaps &getCaps() const { return m_caps; }

    int getWidth() const { return m_width; }
    int getHeight() const { return m_height; }

    HandleType createRenderContext(int p_config, HandleType p_share, bool p_isGL2 = false);
    HandleType createWindowSurface(int p_config, int p_width, int p_height);
    HandleType createColorBuffer(int p_width, int p_height, GLenum p_internalFormat);
    void DestroyRenderContext(HandleType p_context);
    void DestroyWindowSurface(HandleType p_surface);
    void openColorBuffer(HandleType p_colorbuffer);
    void closeColorBuffer(HandleType p_colorbuffer);

    bool  bindContext(HandleType p_context, HandleType p_drawSurface, HandleType p_readSurface);
    bool  setWindowSurfaceColorBuffer(HandleType p_surface, HandleType p_colorbuffer);
    bool  flushWindowSurfaceColorBuffer(HandleType p_surface);
    bool  bindColorBufferToTexture(HandleType p_colorbuffer);
    bool  bindColorBufferToRenderbuffer(HandleType p_colorbuffer);
    bool updateColorBuffer(HandleType p_colorbuffer,
                           int x, int y, int width, int height,
                           GLenum format, GLenum type, void *pixels);

    bool post(HandleType p_colorbuffer, bool needLock = true);
    bool repost();

    EGLDisplay getDisplay() const { return m_eglDisplay; }
    EGLNativeWindowType getSubWindow() const { return m_subWin; }
    bool bind_locked();
    bool unbind_locked();

    void setDisplayRotation(float zRot) {
        m_zRot = zRot;
        repost();
    }

private:
    FrameBuffer(int p_width, int p_height);
    ~FrameBuffer();
    HandleType genHandle();
    bool bindSubwin_locked();
    void initGLState();

private:
    static FrameBuffer *s_theFrameBuffer;
    static HandleType s_nextHandle;
    int m_x;
    int m_y;
    int m_width;
    int m_height;
    android::Mutex m_lock;
    FBNativeWindowType m_nativeWindow;
    FrameBufferCaps m_caps;
    EGLDisplay m_eglDisplay;
    RenderContextMap m_contexts;
    WindowSurfaceMap m_windows;
    ColorBufferMap m_colorbuffers;

    EGLSurface m_eglSurface;
    EGLContext m_eglContext;
    EGLSurface m_pbufSurface;
    EGLContext m_pbufContext;

    EGLContext m_prevContext;
    EGLSurface m_prevReadSurf;
    EGLSurface m_prevDrawSurf;
    EGLNativeWindowType m_subWin;
    EGLNativeDisplayType m_subWinDisplay;
    EGLConfig  m_eglConfig;
    HandleType m_lastPostedColorBuffer;
    float      m_zRot;
    bool       m_eglContextInitialized;

    int m_statsNumFrames;
    long long m_statsStartTime;
    bool m_fpsStats;
};
#endif
