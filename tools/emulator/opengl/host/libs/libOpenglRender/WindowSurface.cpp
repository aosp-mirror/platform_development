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
#include "WindowSurface.h"
#include "FBConfig.h"
#include "FrameBuffer.h"
#include <GLES/glext.h>
#include "EGLDispatch.h"
#include "GLDispatch.h"
#include "GL2Dispatch.h"
#include <stdio.h>
#include <string.h>
#include "GLErrorLog.h"

WindowSurface::WindowSurface() :
    m_fbObj(0),
    m_depthRB(0),
    m_stencilRB(0),
    m_eglSurface(NULL),
    m_attachedColorBuffer(NULL),
    m_readContext(NULL),
    m_drawContext(NULL),
    m_width(0),
    m_height(0),
    m_pbufWidth(0),
    m_pbufHeight(0)
{
}

WindowSurface::~WindowSurface()
{
    s_egl.eglDestroySurface(FrameBuffer::getFB()->getDisplay(), m_eglSurface);
}

WindowSurface *WindowSurface::create(int p_config, int p_width, int p_height)
{
    const FBConfig *fbconf = FBConfig::get(p_config);
    if (!fbconf) {
        return NULL;
    }

    // allocate space for the WindowSurface object
    WindowSurface *win = new WindowSurface();
    if (!win) {
        return NULL;
    }
    win->m_fbconf = fbconf;

    FrameBuffer *fb = FrameBuffer::getFB();
    const FrameBufferCaps &caps = fb->getCaps();

    //
    // Create a pbuffer to be used as the egl surface
    // for that window.
    //
    if (!win->resizePbuffer(p_width, p_height)) {
        delete win;
        return NULL;
    }

    win->m_width = p_width;
    win->m_height = p_height;

    return win;
}

//
// flushColorBuffer - The function makes sure that the
//    previous attached color buffer is updated, if copy or blit should be done
//    in order to update it - it is being done here.
//
void WindowSurface::flushColorBuffer()
{
    if (m_attachedColorBuffer.Ptr() != NULL) {
        blitToColorBuffer();
    }
}

//
// setColorBuffer - this function is called when a new color buffer needs to
//    be attached to the surface. The function doesn't make sure that the
//    previous attached color buffer is updated, this is done by flushColorBuffer
//
void WindowSurface::setColorBuffer(ColorBufferPtr p_colorBuffer)
{
    m_attachedColorBuffer = p_colorBuffer;

    //
    // resize the window if the attached color buffer is of different
    // size
    //
    unsigned int cbWidth = m_attachedColorBuffer->getWidth();
    unsigned int cbHeight = m_attachedColorBuffer->getHeight();

    if (cbWidth != m_width || cbHeight != m_height) {

        if (m_pbufWidth && m_pbufHeight) {
            // if we use pbuffer, need to resize it
            resizePbuffer(cbWidth, cbHeight);
        }

        m_width = cbWidth;
        m_height = cbHeight;
    }
}

//
// This function is called after the context and eglSurface is already
// bound in the current thread (eglMakeCurrent has been called).
// This function should take actions required on the other surface objects
// when being bind/unbound
//
void WindowSurface::bind(RenderContextPtr p_ctx, SurfaceBindType p_bindType)
{
    if (p_bindType == SURFACE_BIND_READ) {
        m_readContext = p_ctx;
    }
    else if (p_bindType == SURFACE_BIND_DRAW) {
        m_drawContext = p_ctx;
    }
    else if (p_bindType == SURFACE_BIND_READDRAW) {
        m_readContext = p_ctx;
        m_drawContext = p_ctx;
    }
    else {
        return;  // bad param
    }

}

void WindowSurface::blitToColorBuffer()
{
    if (!m_width && !m_height) return;

    if (m_attachedColorBuffer->getWidth() != m_width ||
        m_attachedColorBuffer->getHeight() != m_height) {
        // XXX: should never happen - how this needs to be handled?
        return;
    }

    //
    // Make the surface current
    //
    EGLContext prevContext = s_egl.eglGetCurrentContext();
    EGLSurface prevReadSurf = s_egl.eglGetCurrentSurface(EGL_READ);
    EGLSurface prevDrawSurf = s_egl.eglGetCurrentSurface(EGL_DRAW);
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!s_egl.eglMakeCurrent(fb->getDisplay(), m_eglSurface,
                              m_eglSurface, m_drawContext->getEGLContext())) {
        return;
    }

    m_attachedColorBuffer->blitFromCurrentReadBuffer();

    // restore current context/surface
    s_egl.eglMakeCurrent(fb->getDisplay(), prevDrawSurf,
                         prevReadSurf, prevContext);

}

bool WindowSurface::resizePbuffer(unsigned int p_width, unsigned int p_height)
{
    if (m_eglSurface && 
        m_pbufWidth == p_width &&
        m_pbufHeight == p_height) {
        // no need to resize
        return true;
    }

    FrameBuffer *fb = FrameBuffer::getFB();

    EGLContext prevContext = s_egl.eglGetCurrentContext();
    EGLSurface prevReadSurf = s_egl.eglGetCurrentSurface(EGL_READ);
    EGLSurface prevDrawSurf = s_egl.eglGetCurrentSurface(EGL_DRAW);
    EGLSurface prevPbuf = m_eglSurface;
    bool needRebindContext = m_eglSurface &&
                             (prevReadSurf == m_eglSurface ||
                              prevDrawSurf == m_eglSurface);

    if (needRebindContext) {
        s_egl.eglMakeCurrent(fb->getDisplay(), EGL_NO_SURFACE,
                              EGL_NO_SURFACE, EGL_NO_CONTEXT);
    }

    //
    // Destroy previous surface
    //
    if (m_eglSurface) {
        s_egl.eglDestroySurface(fb->getDisplay(), m_eglSurface);
        m_eglSurface = NULL;
    }

    const FrameBufferCaps &caps = fb->getCaps();

    //
    // Create pbuffer surface.
    //
    EGLint pbufAttribs[5];
    pbufAttribs[0] = EGL_WIDTH;
    pbufAttribs[1] = p_width;
    pbufAttribs[2] = EGL_HEIGHT;
    pbufAttribs[3] = p_height;
    pbufAttribs[4] = EGL_NONE;

    m_eglSurface = s_egl.eglCreatePbufferSurface(fb->getDisplay(),
                                                 m_fbconf->getEGLConfig(),
                                                 pbufAttribs);
    if (m_eglSurface == EGL_NO_SURFACE) {
        fprintf(stderr, "Renderer error: failed to create/resize pbuffer!!\n");
        return false;
    }

    m_pbufWidth = p_width;
    m_pbufHeight = p_height;

    if (needRebindContext) {
        s_egl.eglMakeCurrent(fb->getDisplay(), 
                     (prevDrawSurf==prevPbuf) ? m_eglSurface : prevDrawSurf,
                     (prevReadSurf==prevPbuf) ? m_eglSurface : prevReadSurf,
                     prevContext);
    }

    return true;
}
