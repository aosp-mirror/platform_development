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
#include "ColorBuffer.h"
#include "FrameBuffer.h"
#include "EGLDispatch.h"
#include "GLDispatch.h"
#include "ThreadInfo.h"
#ifdef WITH_GLES2
#include "GL2Dispatch.h"
#endif
#include <stdio.h>

ColorBuffer *ColorBuffer::create(int p_width, int p_height,
                                 GLenum p_internalFormat)
{
    FrameBuffer *fb = FrameBuffer::getFB();

    GLenum texInternalFormat = 0;

    switch(p_internalFormat) {
        case GL_RGB:
        case GL_RGB565_OES:
            texInternalFormat = GL_RGB;
            break;

        case GL_RGBA:
        case GL_RGB5_A1_OES:
        case GL_RGBA4_OES:
            texInternalFormat = GL_RGBA;
            break;

        default:
            return NULL;
            break;
    }

    if (!fb->bind_locked()) {
        return NULL;
    }

    ColorBuffer *cb = new ColorBuffer();


    s_gl.glGenTextures(1, &cb->m_tex);
    s_gl.glBindTexture(GL_TEXTURE_2D, cb->m_tex);
    int nComp = (texInternalFormat == GL_RGB ? 3 : 4);
    char *zBuff = new char[nComp*p_width*p_height];
    if (zBuff) {
        memset(zBuff, 0, nComp*p_width*p_height);
    }
    s_gl.glTexImage2D(GL_TEXTURE_2D, 0, texInternalFormat,
                      p_width, p_height, 0,
                      texInternalFormat,
                      GL_UNSIGNED_BYTE, zBuff);
    delete [] zBuff;
    s_gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    s_gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    s_gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    s_gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    s_gl.glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

    //
    // create another texture for that colorbuffer for blit
    //
    s_gl.glGenTextures(1, &cb->m_blitTex);
    s_gl.glBindTexture(GL_TEXTURE_2D, cb->m_blitTex);
    s_gl.glTexImage2D(GL_TEXTURE_2D, 0, texInternalFormat,
                      p_width, p_height, 0,
                      texInternalFormat,
                      GL_UNSIGNED_BYTE, NULL);
    s_gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    s_gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    s_gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    s_gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    s_gl.glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

    cb->m_width = p_width;
    cb->m_height = p_height;
    cb->m_internalFormat = texInternalFormat;

    if (fb->getCaps().has_eglimage_texture_2d) {
        cb->m_eglImage = s_egl.eglCreateImageKHR(fb->getDisplay(),
                                                 s_egl.eglGetCurrentContext(),
                                                 EGL_GL_TEXTURE_2D_KHR,
                                                 (EGLClientBuffer)cb->m_tex,
                                                 NULL);

        cb->m_blitEGLImage = s_egl.eglCreateImageKHR(fb->getDisplay(),
                                                 s_egl.eglGetCurrentContext(),
                                                 EGL_GL_TEXTURE_2D_KHR,
                                                 (EGLClientBuffer)cb->m_blitTex,
                                                 NULL);
    }

    fb->unbind_locked();
    return cb;
}

ColorBuffer::ColorBuffer() :
    m_tex(0),
    m_eglImage(NULL),
    m_fbo(0),
    m_internalFormat(0),
    m_warYInvertBug(false)
{
#if __APPLE__
    // On Macs running OS X 10.6 and 10.7 with Intel HD Graphics 3000, some
    // screens or parts of the screen are displayed upside down. The exact
    // conditions/sequence that triggers this aren't known yet; I haven't
    // been able to reproduce it in a standalone test. This way of enabling the
    // workaround will break if it is a driver bug (rather than a bug in this
    // code which works by accident elsewhere) and Apple/Intel release a fix for
    // it. Running a standalone test to detect the problem at runtime would be
    // more robust.
    if (strstr((const char*)s_gl.glGetString(GL_RENDERER), "Intel HD Graphics 3000"))
        m_warYInvertBug = true;
#endif
}

ColorBuffer::~ColorBuffer()
{
    FrameBuffer *fb = FrameBuffer::getFB();
    fb->bind_locked();
    s_gl.glDeleteTextures(1, &m_tex);
    if (m_eglImage) {
        s_egl.eglDestroyImageKHR(fb->getDisplay(), m_eglImage);
    }
    if (m_fbo) {
        s_gl.glDeleteFramebuffersOES(1, &m_fbo);
    }
    fb->unbind_locked();
}

void ColorBuffer::subUpdate(int x, int y, int width, int height, GLenum p_format, GLenum p_type, void *pixels)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb->bind_locked()) return;
    s_gl.glBindTexture(GL_TEXTURE_2D, m_tex);
    s_gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    s_gl.glTexSubImage2D(GL_TEXTURE_2D, 0, x, y,
                         width, height, p_format, p_type, pixels);
    fb->unbind_locked();
}

bool ColorBuffer::blitFromCurrentReadBuffer()
{
    RenderThreadInfo *tInfo = getRenderThreadInfo();
    if (!tInfo->currContext.Ptr()) {
        // no Current context
        return false;
    }

    //
    // Create a temporary texture inside the current context
    // from the blit_texture EGLImage and copy the pixels
    // from the current read buffer to that texture
    //
    GLuint tmpTex;
    GLint currTexBind;
    if (tInfo->currContext->isGL2()) {
        s_gl2.glGetIntegerv(GL_TEXTURE_BINDING_2D, &currTexBind);
        s_gl2.glGenTextures(1,&tmpTex);
        s_gl2.glBindTexture(GL_TEXTURE_2D, tmpTex);
        s_gl2.glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, m_blitEGLImage);
        s_gl2.glCopyTexImage2D(GL_TEXTURE_2D, 0, m_internalFormat,
                               0, 0, m_width, m_height, 0);
    }
    else {
        s_gl.glGetIntegerv(GL_TEXTURE_BINDING_2D, &currTexBind);
        s_gl.glGenTextures(1,&tmpTex);
        s_gl.glBindTexture(GL_TEXTURE_2D, tmpTex);
        s_gl.glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, m_blitEGLImage);
        s_gl.glCopyTexImage2D(GL_TEXTURE_2D, 0, m_internalFormat,
                              0, 0, m_width, m_height, 0);
    }


    //
    // Now bind the frame buffer context and blit from
    // m_blitTex into m_tex
    //
    FrameBuffer *fb = FrameBuffer::getFB();
    if (fb->bind_locked()) {

        //
        // bind FBO object which has this colorbuffer as render target
        //
        if (bind_fbo()) {

            //
            // save current viewport and match it to the current
            // colorbuffer size
            //
            GLint vport[4];
            s_gl.glGetIntegerv(GL_VIEWPORT, vport);
            s_gl.glViewport(0, 0, m_width, m_height);

            // render m_blitTex
            s_gl.glBindTexture(GL_TEXTURE_2D, m_blitTex);
            s_gl.glEnable(GL_TEXTURE_2D);
            s_gl.glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
            drawTexQuad(!m_warYInvertBug);

            // unbind the fbo
            s_gl.glBindFramebufferOES(GL_FRAMEBUFFER_OES, 0);

            // restrore previous viewport
            s_gl.glViewport(vport[0], vport[1], vport[2], vport[3]);
        }

        // unbind from the FrameBuffer context
        fb->unbind_locked();
    }

    //
    // delete the temporary texture and restore the texture binding
    // inside the current context
    //
    if (tInfo->currContext->isGL2()) {
        s_gl2.glDeleteTextures(1, &tmpTex);
        s_gl2.glBindTexture(GL_TEXTURE_2D, currTexBind);
    }
    else {
        s_gl.glDeleteTextures(1, &tmpTex);
        s_gl.glBindTexture(GL_TEXTURE_2D, currTexBind);
    }

    return true;
}

bool ColorBuffer::bindToTexture()
{
    if (m_eglImage) {
        RenderThreadInfo *tInfo = getRenderThreadInfo();
        if (tInfo->currContext.Ptr()) {
#ifdef WITH_GLES2
            if (tInfo->currContext->isGL2()) {
                s_gl2.glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, m_eglImage);
            }
            else {
                s_gl.glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, m_eglImage);
            }
#else
            s_gl.glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, m_eglImage);
#endif
            return true;
        }
    }
    return false;
}

bool ColorBuffer::bindToRenderbuffer()
{
    if (m_eglImage) {
        RenderThreadInfo *tInfo = getRenderThreadInfo();
        if (tInfo->currContext.Ptr()) {
#ifdef WITH_GLES2
            if (tInfo->currContext->isGL2()) {
                s_gl2.glEGLImageTargetRenderbufferStorageOES(GL_RENDERBUFFER_OES, m_eglImage);
            }
            else {
                s_gl.glEGLImageTargetRenderbufferStorageOES(GL_RENDERBUFFER_OES, m_eglImage);
            }
#else
            s_gl.glEGLImageTargetRenderbufferStorageOES(GL_RENDERBUFFER_OES, m_eglImage);
#endif
            return true;
        }
    }
    return false;
}

bool ColorBuffer::bind_fbo()
{
    if (m_fbo) {
        // fbo already exist - just bind
        s_gl.glBindFramebufferOES(GL_FRAMEBUFFER_OES, m_fbo);
        return true;
    }

    s_gl.glGenFramebuffersOES(1, &m_fbo);
    s_gl.glBindFramebufferOES(GL_FRAMEBUFFER_OES, m_fbo);
    s_gl.glFramebufferTexture2DOES(GL_FRAMEBUFFER_OES,
                                   GL_COLOR_ATTACHMENT0_OES,
                                   GL_TEXTURE_2D, m_tex, 0);
    GLenum status = s_gl.glCheckFramebufferStatusOES(GL_FRAMEBUFFER_OES);
    if (status != GL_FRAMEBUFFER_COMPLETE_OES) {
        s_gl.glBindFramebufferOES(GL_FRAMEBUFFER_OES, 0);
        s_gl.glDeleteFramebuffersOES(1, &m_fbo);
        m_fbo = 0;
        return false;
    }

    return true;
}

bool ColorBuffer::post()
{
    s_gl.glBindTexture(GL_TEXTURE_2D, m_tex);
    s_gl.glEnable(GL_TEXTURE_2D);
    s_gl.glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
    drawTexQuad(true);

    return true;
}

void ColorBuffer::drawTexQuad(bool flipy)
{
    GLfloat verts[] = { -1.0f, -1.0f, 0.0f,
                         -1.0f, +1.0f, 0.0f,
                         +1.0f, -1.0f, 0.0f,
                         +1.0f, +1.0f, 0.0f };

    GLfloat tcoords[] = { 0.0f, 1.0f,
                           0.0f, 0.0f,
                           1.0f, 1.0f,
                           1.0f, 0.0f };

    if (!flipy) {
        for (int i = 0; i < 4; i++) {
            // swap 0.0/1.0 in second element of each tcoord vector
            tcoords[2*i + 1] = tcoords[2*i + 1] == 0.0f ? 1.0f : 0.0f;
        }
    }

    s_gl.glClientActiveTexture(GL_TEXTURE0);
    s_gl.glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    s_gl.glTexCoordPointer(2, GL_FLOAT, 0, tcoords);

    s_gl.glEnableClientState(GL_VERTEX_ARRAY);
    s_gl.glVertexPointer(3, GL_FLOAT, 0, verts);
    s_gl.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
}
