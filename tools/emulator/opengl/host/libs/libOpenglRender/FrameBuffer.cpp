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
#include "FrameBuffer.h"
#include "FBConfig.h"
#include "EGLDispatch.h"
#include "GLDispatch.h"
#include "GL2Dispatch.h"
#include "ThreadInfo.h"
#include <stdio.h>

FrameBuffer *FrameBuffer::s_theFrameBuffer = NULL;
HandleType FrameBuffer::s_nextHandle = 0;

#ifdef WITH_GLES2
static const char *getGLES2ExtensionString(EGLDisplay p_dpy,
                                 FBNativeWindowType p_window)
{
    EGLConfig config;
    EGLSurface surface;

    GLint configAttribs[] = {
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_NONE
    };

    int n;
    if (!s_egl.eglChooseConfig(p_dpy, configAttribs,
                               &config, 1, &n)) {
        return NULL;
    }

#if defined(__linux__) || defined(_WIN32) || defined(__VC32__) && !defined(__CYGWIN__)
    surface = s_egl.eglCreateWindowSurface(p_dpy, config,
                                              (EGLNativeWindowType)p_window,
                                              NULL);
    if (surface == EGL_NO_SURFACE) {
        return NULL;
    }
#endif

    GLint gl2ContextAttribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE
    };

    EGLContext ctx = s_egl.eglCreateContext(p_dpy, config,
                                            EGL_NO_CONTEXT,
                                            gl2ContextAttribs);
    if (ctx == EGL_NO_CONTEXT) {
        s_egl.eglDestroySurface(p_dpy, surface);
        return NULL;
    }

    if (!s_egl.eglMakeCurrent(p_dpy, surface, surface, ctx)) {
        s_egl.eglDestroySurface(p_dpy, surface);
        s_egl.eglDestroyContext(p_dpy, ctx);
        return NULL;
    }

    const char *extString = (const char *)s_gl2.glGetString(GL_EXTENSIONS);
    if (!extString) {
        extString = "";
    }

    s_egl.eglMakeCurrent(p_dpy, NULL, NULL, NULL);
    s_egl.eglDestroyContext(p_dpy, ctx);
    s_egl.eglDestroySurface(p_dpy, surface);

    return extString;
}
#endif

bool FrameBuffer::initialize(FBNativeWindowType p_window,
                             int p_x, int p_y,
                             int p_width, int p_height)
{
    if (s_theFrameBuffer != NULL) {
        return true;
    }

    //
    // Load EGL Plugin
    //
    if (!init_egl_dispatch()) {
        // Failed to load EGL
        printf("Failed to init_egl_dispatch\n");
        return false;
    }

    //
    // Load GLES Plugin
    //
    if (!init_gl_dispatch()) {
        // Failed to load GLES
        ERR("Failed to init_gl_dispatch\n");
        return false;
    }

    //
    // allocate space for the FrameBuffer object
    //
    FrameBuffer *fb = new FrameBuffer(p_x, p_y, p_width, p_height);
    if (!fb) {
        ERR("Failed to create fb\n");
        return false;
    }

#ifdef WITH_GLES2
    //
    // Try to load GLES2 Plugin, not mandatory
    //
    if (getenv("ANDROID_NO_GLES2")) {
        fb->m_caps.hasGL2 = false;
    }
    else {
        fb->m_caps.hasGL2 = init_gl2_dispatch();
    }
#else
    fb->m_caps.hasGL2 = false;
#endif

    //
    // Initialize backend EGL display
    //
    fb->m_eglDisplay = s_egl.eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (fb->m_eglDisplay == EGL_NO_DISPLAY) {
        ERR("Failed to Initialize backend EGL display\n");
        delete fb;
        return false;
    }

    if (!s_egl.eglInitialize(fb->m_eglDisplay, &fb->m_caps.eglMajor, &fb->m_caps.eglMinor)) {
        ERR("Failed to eglInitialize\n");
        delete fb;
        return false;
    }

    DBG("egl: %d %d\n", fb->m_caps.eglMajor, fb->m_caps.eglMinor);
    s_egl.eglBindAPI(EGL_OPENGL_ES_API);

    fb->m_nativeWindow = p_window;

    //
    // if GLES2 plugin has loaded - try to make GLES2 context and
    // get GLES2 extension string
    //
    const char *gl2Extensions = NULL;
#ifdef WITH_GLES2
    if (fb->m_caps.hasGL2) {
        gl2Extensions = getGLES2ExtensionString(fb->m_eglDisplay, p_window);
        if (!gl2Extensions) {
            // Could not create GLES2 context - drop GL2 capability
            fb->m_caps.hasGL2 = false;
        }
    }
#endif

    //
    // Create EGL context and Surface attached to the native window, for
    // framebuffer post rendering.
    //
#if 0
    GLint configAttribs[] = {
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES_BIT,
        EGL_NONE
    };
#else
    GLint configAttribs[] = {
        EGL_RED_SIZE, 1,
        EGL_GREEN_SIZE, 1,
        EGL_BLUE_SIZE, 1,
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_NONE
    };
#endif
    EGLConfig eglConfig;
    int n;
    if (!s_egl.eglChooseConfig(fb->m_eglDisplay, configAttribs,
                               &eglConfig, 1, &n)) {
        ERR("Failed on eglChooseConfig\n");
        delete fb;
        return false;
    }

#if defined(__linux__) || defined(_WIN32) || defined(__VC32__) && !defined(__CYGWIN__)
    fb->m_eglSurface = s_egl.eglCreateWindowSurface(fb->m_eglDisplay, eglConfig,
                                                  (EGLNativeWindowType)p_window,
                                                  NULL);
    if (fb->m_eglSurface == EGL_NO_SURFACE) {
        ERR("Failed to create surface\n");
        delete fb;
        return false;
    }
#endif

    GLint glContextAttribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 1,
        EGL_NONE
    };

    fb->m_eglContext = s_egl.eglCreateContext(fb->m_eglDisplay, eglConfig,
                                              EGL_NO_CONTEXT,
                                              glContextAttribs);
    if (fb->m_eglContext == EGL_NO_CONTEXT) {
        printf("Failed to create Context 0x%x\n", s_egl.eglGetError());
        delete fb;
        return false;
    }

    // Make the context current
    if (!fb->bind_locked()) {
        ERR("Failed to make current\n");
        delete fb;
        return false;
    }

    //
    // Initilize framebuffer capabilities
    //
    const char *glExtensions = (const char *)s_gl.glGetString(GL_EXTENSIONS);
    bool has_gl_oes_image = false;
    if (glExtensions) {
        has_gl_oes_image = strstr(glExtensions, "GL_OES_EGL_image") != NULL;
    }

    if (fb->m_caps.hasGL2 && has_gl_oes_image) {
        has_gl_oes_image &= (strstr(gl2Extensions, "GL_OES_EGL_image") != NULL);
    }

    const char *eglExtensions = s_egl.eglQueryString(fb->m_eglDisplay,
                                                     EGL_EXTENSIONS);

    if (eglExtensions && has_gl_oes_image) {
        fb->m_caps.has_eglimage_texture_2d =
             strstr(eglExtensions, "EGL_KHR_gl_texture_2D_image") != NULL;
        fb->m_caps.has_eglimage_renderbuffer =
             strstr(eglExtensions, "EGL_KHR_gl_renderbuffer_image") != NULL;
    }
    else {
        fb->m_caps.has_eglimage_texture_2d = false;
        fb->m_caps.has_eglimage_renderbuffer = false;
    }

    //
    // Initialize set of configs
    //
    InitConfigStatus configStatus = FBConfig::initConfigList(fb);
    if (configStatus == INIT_CONFIG_FAILED) {
        ERR("Failed: Initialize set of configs\n");
        delete fb;
        return false;
    }

    //
    // Check that we have config for each GLES and GLES2
    //
    int nConfigs = FBConfig::getNumConfigs();
    int nGLConfigs = 0;
    int nGL2Configs = 0;
    for (int i=0; i<nConfigs; i++) {
        GLint rtype = FBConfig::get(i)->getRenderableType();
        if (0 != (rtype & EGL_OPENGL_ES_BIT)) {
            nGLConfigs++;
        }
        if (0 != (rtype & EGL_OPENGL_ES2_BIT)) {
            nGL2Configs++;
        }
    }

    //
    // Fail initialization if no GLES configs exist
    //
    if (nGLConfigs == 0) {
        delete fb;
        return false;
    }

    //
    // If no GLES2 configs exist - not GLES2 capability
    //
    if (nGL2Configs == 0) {
        fb->m_caps.hasGL2 = false;
    }

    //
    // update Pbuffer bind to texture capability based on configs
    //
    fb->m_caps.has_BindToTexture =
        (configStatus == INIT_CONFIG_HAS_BIND_TO_TEXTURE);


    //
    // Initialize some GL state
    //
    s_gl.glMatrixMode(GL_PROJECTION);
    s_gl.glLoadIdentity();
    s_gl.glOrthof(-1.0, 1.0, -1.0, 1.0, -1.0, 1.0);
    s_gl.glMatrixMode(GL_MODELVIEW);
    s_gl.glLoadIdentity();

    // release the FB context
    fb->unbind_locked();

    //
    // Keep the singleton framebuffer pointer
    //
    s_theFrameBuffer = fb;
    return true;
}

FrameBuffer::FrameBuffer(int p_x, int p_y, int p_width, int p_height) :
    m_x(p_x),
    m_y(p_y),
    m_width(p_width),
    m_height(p_height),
    m_eglDisplay(EGL_NO_DISPLAY),
    m_eglSurface(EGL_NO_SURFACE),
    m_eglContext(EGL_NO_CONTEXT),
    m_prevContext(EGL_NO_CONTEXT),
    m_prevReadSurf(EGL_NO_SURFACE),
    m_prevDrawSurf(EGL_NO_SURFACE)
{
}

FrameBuffer::~FrameBuffer()
{
}

HandleType FrameBuffer::genHandle()
{
    HandleType id;
    do {
        id = ++s_nextHandle;
    } while( id == 0 ||
             m_contexts.find(id) != m_contexts.end() ||
             m_windows.find(id) != m_windows.end() );

    return id;
}

HandleType FrameBuffer::createColorBuffer(int p_width, int p_height,
                                          GLenum p_internalFormat)
{
    android::Mutex::Autolock mutex(m_lock);
    HandleType ret = 0;

    ColorBufferPtr cb( ColorBuffer::create(p_width, p_height, p_internalFormat) );
    if (cb.Ptr() != NULL) {
        ret = genHandle();
        m_colorbuffers[ret] = cb;
    }
    return ret;
}

HandleType FrameBuffer::createRenderContext(int p_config, HandleType p_share,
                                            bool p_isGL2)
{
    android::Mutex::Autolock mutex(m_lock);
    HandleType ret = 0;

    RenderContextPtr share(NULL);
    if (p_share != 0) {
        RenderContextMap::iterator s( m_contexts.find(p_share) );
        if (s == m_contexts.end()) {
            return 0;
        }
        share = (*s).second;
    }

    RenderContextPtr rctx( RenderContext::create(p_config, share, p_isGL2) );
    if (rctx.Ptr() != NULL) {
        ret = genHandle();
        m_contexts[ret] = rctx;
    }
    return ret;
}

HandleType FrameBuffer::createWindowSurface(int p_config, int p_width, int p_height)
{
    android::Mutex::Autolock mutex(m_lock);

    HandleType ret = 0;
    WindowSurfacePtr win( WindowSurface::create(p_config, p_width, p_height) );
    if (win.Ptr() != NULL) {
        ret = genHandle();
        m_windows[ret] = win;
    }

    return ret;
}

void FrameBuffer::DestroyRenderContext(HandleType p_context)
{
    android::Mutex::Autolock mutex(m_lock);
    m_contexts.erase(p_context);
}

void FrameBuffer::DestroyWindowSurface(HandleType p_surface)
{
    android::Mutex::Autolock mutex(m_lock);
    m_windows.erase(p_surface);
}

void FrameBuffer::DestroyColorBuffer(HandleType p_colorbuffer)
{
    android::Mutex::Autolock mutex(m_lock);
    m_colorbuffers.erase(p_colorbuffer);
}

bool FrameBuffer::flushWindowSurfaceColorBuffer(HandleType p_surface)
{
    android::Mutex::Autolock mutex(m_lock);

    WindowSurfaceMap::iterator w( m_windows.find(p_surface) );
    if (w == m_windows.end()) {
        // bad surface handle
        return false;
    }

    (*w).second->flushColorBuffer();

    return true;
}

bool FrameBuffer::setWindowSurfaceColorBuffer(HandleType p_surface,
                                              HandleType p_colorbuffer)
{
    android::Mutex::Autolock mutex(m_lock);

    WindowSurfaceMap::iterator w( m_windows.find(p_surface) );
    if (w == m_windows.end()) {
        // bad surface handle
        return false;
    }

    ColorBufferMap::iterator c( m_colorbuffers.find(p_colorbuffer) );
    if (c == m_colorbuffers.end()) {
        // bad colorbuffer handle
        return false;
    }

    (*w).second->setColorBuffer( (*c).second );

    return true;
}

bool FrameBuffer::updateColorBuffer(HandleType p_colorbuffer,
                                    int x, int y, int width, int height,
                                    GLenum format, GLenum type, void *pixels)
{
    android::Mutex::Autolock mutex(m_lock);

    ColorBufferMap::iterator c( m_colorbuffers.find(p_colorbuffer) );
    if (c == m_colorbuffers.end()) {
        // bad colorbuffer handle
        return false;
    }

    (*c).second->subUpdate(x, y, width, height, format, type, pixels);

    return true;
}

bool FrameBuffer::bindColorBufferToTexture(HandleType p_colorbuffer)
{
    android::Mutex::Autolock mutex(m_lock);

    ColorBufferMap::iterator c( m_colorbuffers.find(p_colorbuffer) );
    if (c == m_colorbuffers.end()) {
        // bad colorbuffer handle
        return false;
    }

    return (*c).second->bindToTexture();
}

bool FrameBuffer::bindContext(HandleType p_context,
                              HandleType p_drawSurface,
                              HandleType p_readSurface)
{
    android::Mutex::Autolock mutex(m_lock);

    WindowSurfacePtr draw(NULL), read(NULL);
    RenderContextPtr ctx(NULL);

    //
    // if this is not an unbind operation - make sure all handles are good
    //
    if (p_context || p_drawSurface || p_readSurface) {
        RenderContextMap::iterator r( m_contexts.find(p_context) );
        if (r == m_contexts.end()) {
            // bad context handle
            return false;
        }

        ctx = (*r).second;
        WindowSurfaceMap::iterator w( m_windows.find(p_drawSurface) );
        if (w == m_windows.end()) {
            // bad surface handle
            return false;
        }
        draw = (*w).second;

        if (p_readSurface != p_drawSurface) {
            WindowSurfaceMap::iterator w( m_windows.find(p_readSurface) );
            if (w == m_windows.end()) {
                // bad surface handle
                return false;
            }
            read = (*w).second;
        }
        else {
            read = draw;
        }
    }

    if (!s_egl.eglMakeCurrent(m_eglDisplay,
                              draw ? draw->getEGLSurface() : EGL_NO_SURFACE,
                              read ? read->getEGLSurface() : EGL_NO_SURFACE,
                              ctx ? ctx->getEGLContext() : EGL_NO_CONTEXT)) {
        // MakeCurrent failed
        return false;
    }

    //
    // Bind the surface(s) to the context
    //
    RenderThreadInfo *tinfo = getRenderThreadInfo();
    if (draw.Ptr() == NULL && read.Ptr() == NULL) {
        // if this is an unbind operation - make sure the current bound
        // surfaces get unbound from the context.
        draw = tinfo->currDrawSurf;
        read = tinfo->currReadSurf;
    }

    if (draw.Ptr() != NULL && read.Ptr() != NULL) {
        if (p_readSurface != p_drawSurface) {
            draw->bind( ctx, SURFACE_BIND_DRAW );
            read->bind( ctx, SURFACE_BIND_READ );
        }
        else {
            draw->bind( ctx, SURFACE_BIND_READDRAW );
        }
    }

    //
    // update thread info with current bound context
    //
    tinfo->currContext = ctx;
    tinfo->currDrawSurf = draw;
    tinfo->currReadSurf = read;
    if (ctx) {
        if (ctx->isGL2()) tinfo->m_gl2Dec.setContextData(&ctx->decoderContextData());
        else tinfo->m_glDec.setContextData(&ctx->decoderContextData());
    }
    else {
        tinfo->m_glDec.setContextData(NULL);
        tinfo->m_gl2Dec.setContextData(NULL);
    }
    return true;
}

//
// The framebuffer lock should be held when calling this function !
//
bool FrameBuffer::bind_locked()
{
    EGLContext prevContext = s_egl.eglGetCurrentContext();
    EGLSurface prevReadSurf = s_egl.eglGetCurrentSurface(EGL_READ);
    EGLSurface prevDrawSurf = s_egl.eglGetCurrentSurface(EGL_DRAW);

    if (!s_egl.eglMakeCurrent(m_eglDisplay, m_eglSurface,
                              m_eglSurface, m_eglContext)) {
        ERR("eglMakeCurrent failed\n");
        return false;
    }

    m_prevContext = prevContext;
    m_prevReadSurf = prevReadSurf;
    m_prevDrawSurf = prevDrawSurf;
    return true;
}

bool FrameBuffer::unbind_locked()
{
    if (!s_egl.eglMakeCurrent(m_eglDisplay, m_prevDrawSurf,
                              m_prevReadSurf, m_prevContext)) {
        return false;
    }

    m_prevContext = EGL_NO_CONTEXT;
    m_prevReadSurf = EGL_NO_SURFACE;
    m_prevDrawSurf = EGL_NO_SURFACE;
    return true;
}

bool FrameBuffer::post(HandleType p_colorbuffer)
{
    android::Mutex::Autolock mutex(m_lock);
    bool ret = false;

    ColorBufferMap::iterator c( m_colorbuffers.find(p_colorbuffer) );
    if (c != m_colorbuffers.end()) {
        if (!bind_locked()) {
            return false;
        }
        ret = (*c).second->post();
        if (ret) {
            s_egl.eglSwapBuffers(m_eglDisplay, m_eglSurface);
        }
        unbind_locked();
    }

    return ret;
}
