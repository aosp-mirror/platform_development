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

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include "RenderingThread.h"
#include "Renderer.h"

// include operating-system dependent windowing system impelemntation
#ifdef _WIN32
#    error "WINDOWS IS NOT SUPPORTED AT THE MOMENT"
#elif defined __APPLE__
#    error "Apple OS-X IS NOT SUPPORTED"
#elif defined (__unix__)
# include "X11Windowing.h"
#endif





Renderer * Renderer::m_instance = NULL;

Renderer * Renderer::instance()
{
    if (m_instance == NULL) m_instance = new Renderer;
    return m_instance;
}

Renderer::Renderer()
{
    // Unix specific, use your platform specific windowing implementation
#ifdef __unix__
    m_nw = new X11Windowing;
#endif

    m_dpy = eglGetDisplay(m_nw->getNativeDisplay());
    EGLint major, minor;
    eglInitialize(m_dpy, &major, &minor);
    fprintf(stderr, "egl initialized : %d.%d\n", major, minor);
}

int Renderer::createSurface(RenderingThread *thread, const ClientHandle & handle)
{
    android::Mutex::Autolock(this->m_mutex);

    assert(m_surfaces.find(handle) == m_surfaces.end());
    if (handle.handle == 0) {
        fprintf(stderr, "trying to create surface for EGL_NO_SURFACE !!!\n");
        return -1;
    } else {
        RendererSurface  *surface = RendererSurface::create(m_dpy, RendererSurface::CONFIG_DEPTH, m_nw);
        if (surface == NULL) {
            printf("failed to create surface !!\n");
            return -1;
        }
        m_surfaces.insert(SurfaceMap::value_type(handle, surface));
    }
    return 0;
}

int Renderer::destroySurface(RenderingThread *thread, const ClientHandle &handle)
{
    android::Mutex::Autolock(this->m_mutex);

    SurfaceMap::iterator i = m_surfaces.find(handle);
    if (i == m_surfaces.end()) {
        printf("removing surface that doesn't exists\n");
        return -1;
    }
    if (i->second->destroy(m_nw)) {
        m_surfaces.erase(handle);
    }
    return 0;
}

int Renderer::createContext(RenderingThread *thread, const ClientHandle &handle, ClientHandle shareCtx, int version)
{
    android::Mutex::Autolock(this->m_mutex);

    assert(m_ctxs.find(handle) == m_ctxs.end());
    RendererContext *shared = NULL;
    if (shareCtx.handle != 0) {
        ContextMap::iterator sctx = m_ctxs.find(shareCtx);
        if (sctx != m_ctxs.end()) {
            shared = sctx->second;
        }
    }

    RendererContext *ctx =
        RendererContext::create(m_dpy,
                                RendererSurface::getEglConfig(m_dpy, RendererSurface::CONFIG_DEPTH),
                                shared, version);
    if (ctx == NULL) {
        fprintf(stderr, "failed to create context\n");
        return -1;
    }
    m_ctxs.insert(ContextMap::value_type(handle, ctx));
    return 0;
}

int Renderer::destroyContext(RenderingThread *thread, const ClientHandle &handle)
{
    android::Mutex::Autolock(this->m_mutex);

    ContextMap::iterator i = m_ctxs.find(handle);
    if (i == m_ctxs.end()) {
        printf("removing context that doesn't exists\n");
        return -1;
    }
    if (i->second->destroy()) {
        m_ctxs.erase(handle);
    }
    return 0;
}

int Renderer::makeCurrent(RenderingThread *thread,
                          const ClientHandle &drawSurface,
                          const ClientHandle &readSurface,
                          const ClientHandle & ctx)
{
    android::Mutex::Autolock(this->m_mutex);

    RendererContext *currentContext = thread->currentContext();

    ContextMap::iterator c = m_ctxs.find(ctx);
    EGLContext eglContext;
    if (ctx.handle != 0 && c != m_ctxs.end()) {
        if (c->second != currentContext) {
            // new context is set
            if (currentContext != NULL) currentContext->unref();
            c->second->ref();
            eglContext = c->second->eglContext();
            thread->setCurrentContext(c->second);
            thread->glDecoder().setContextData(&c->second->decoderContextData());
            thread->gl2Decoder().setContextData(&c->second->decoderContextData());
        } else {
            // same context is already set
            eglContext = c->second->eglContext();
        }
    } else {
        eglContext = EGL_NO_CONTEXT;
        if (currentContext != NULL) currentContext->unref();
        thread->setCurrentContext(NULL);
        thread->glDecoder().setContextData(NULL);
        thread->gl2Decoder().setContextData(NULL);
    }

    EGLSurface draw = EGL_NO_SURFACE;
    EGLSurface read = EGL_NO_SURFACE;
    SurfaceMap::iterator i;
    i = m_surfaces.find(drawSurface);   if (i != m_surfaces.end()) draw = i->second->eglSurface();
    i = m_surfaces.find(readSurface);   if (i != m_surfaces.end()) read = i->second->eglSurface();

    return eglMakeCurrent(m_dpy, draw, read, eglContext);
}

int Renderer::swapBuffers(RenderingThread *thread,
                          const ClientHandle &surface)
{
    android::Mutex::Autolock(this->m_mutex);

    SurfaceMap::iterator s = m_surfaces.find(surface);
    if (s == m_surfaces.end()) {
        fprintf(stderr, "swapping buffers for non existing surface\n");
        return -1;
    }
    return eglSwapBuffers(m_dpy, s->second->eglSurface());
}
