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
    m_useEGLImage(false),
    m_useBindToTexture(false)
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

    FrameBuffer *fb = FrameBuffer::getFB();
    const FrameBufferCaps &caps = fb->getCaps();

    //
    // We can use eglimage and prevent copies if:
    //     GL_KHR_gl_texture_2D_image is present.
    //     and either there is no need for depth or stencil buffer
    //     or GL_KHR_gl_renderbuffer_image present.
    //
#if 0
    //XXX: This path should be implemented
    win->m_useEGLImage =
         (caps.has_eglimage_texture_2d &&
          (caps.has_eglimage_renderbuffer ||
           (fbconf->getDepthSize() + fbconf->getStencilSize() == 0)) );
#else
    win->m_useEGLImage = false;
#endif

    if (win->m_useEGLImage) {
    }
    else if (0 != (fbconf->getSurfaceType() & EGL_PBUFFER_BIT)) {

        //
        // Use Pbuffer for the rendering surface, if possible
        // set it such that it will be able to be bound to a texture
        // later to prevent readback.
        //
        EGLint pbufAttribs[12];
        pbufAttribs[0] = EGL_WIDTH;
        pbufAttribs[1] = p_width;
        pbufAttribs[2] = EGL_HEIGHT;
        pbufAttribs[3] = p_height;

        if (caps.has_BindToTexture) {
            pbufAttribs[4] = EGL_TEXTURE_FORMAT;
            pbufAttribs[5] = EGL_TEXTURE_RGBA;
            pbufAttribs[6] = EGL_TEXTURE_TARGET;
            pbufAttribs[7] = EGL_TEXTURE_2D;
            pbufAttribs[8] = EGL_NONE;
            win->m_useBindToTexture = true;
        }
        else {
            pbufAttribs[4] = EGL_NONE;
        }

        win->m_eglSurface = s_egl.eglCreatePbufferSurface(fb->getDisplay(),
                                                    fbconf->getEGLConfig(),
                                                    pbufAttribs);
        if (win->m_eglSurface == EGL_NO_SURFACE) {
            delete win;
            return NULL;
        }
    }
    else {
        // no EGLImage support and not Pbuffer support - fail
        delete win;
        return NULL;
    }

    win->m_width = p_width;
    win->m_height = p_height;

    return win;
}

//
// setColorBuffer - this function is called when a new color buffer needs to
//    be attached to the surface. The function should make sure that the
//    previous attached color buffer is updated, if copy or blit should be done
//    in order to update it - it is being done here.
//
void WindowSurface::setColorBuffer(ColorBufferPtr p_colorBuffer)
{
    if (m_attachedColorBuffer.Ptr() != NULL) {

        if (!m_useEGLImage) {
            bool copied = false;
            if (m_useBindToTexture) {
                copied = m_attachedColorBuffer->blitFromPbuffer(m_eglSurface);
            }

            if (!copied) {
                copyToColorBuffer();
            }
        }
        else {
        }
    }

    m_attachedColorBuffer = p_colorBuffer;
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

    if (m_useEGLImage) {
        // XXX: should be implemented
    }
}

void WindowSurface::copyToColorBuffer()
{
    if (!m_width && !m_height) return;

    if (m_attachedColorBuffer->getWidth() != m_width ||
        m_attachedColorBuffer->getHeight() != m_height) {
        // XXX: should never happen - how this needs to be handled?
        return;
    }

    void *data = m_xferBuffer.alloc(m_width * m_height * 4);
    if (!data) {
        fprintf(stderr,"WARNING: Failed to copy buffer data - OutOfMemory\n");
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

    if (m_drawContext->isGL2()) {
#ifdef WITH_GLES2
        s_gl2.glPixelStorei(GL_PACK_ALIGNMENT, 1);
        s_gl2.glReadPixels(0, 0, m_width, m_height,
                          GL_RGBA, GL_UNSIGNED_BYTE, data);
#else
        return; // should never happen, context cannot be GL2 in this case.
#endif
    }
    else {
        s_gl.glPixelStorei(GL_PACK_ALIGNMENT, 1);
        s_gl.glReadPixels(0, 0, m_width, m_height,
                          GL_RGBA, GL_UNSIGNED_BYTE, data);
    }

    // update the attached color buffer with the readback pixels
    m_attachedColorBuffer->update(GL_RGBA, GL_UNSIGNED_BYTE, data);

    // restore current context/surface
    s_egl.eglMakeCurrent(fb->getDisplay(), prevDrawSurf,
                         prevReadSurf, prevContext);

    free(data);
}
