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
#ifndef _RENDERING_THREAD_H_
#define _RENDERING_THREAD_H_

#include "SocketStream.h"
#include "GLDecoder.h"
#include "GL2Decoder.h"
#include "ut_rendercontrol_dec.h"
#include <pthread.h>

#define GL_API
#define GL_APIENTRY

#include <GLES/egl.h>
#include <GLES/gl.h>


#define WINDOW_WIDTH    320
#define WINDOW_HEIGHT   480

#define DECODER_BUF_SIZE (4 * 1024 * 1024)

class RendererContext;

class RenderingThread {
public:
    RenderingThread(SocketStream *stream);
    int start();
    void *thread();
    RendererContext *currentContext() { return m_currentContext; }
    void setCurrentContext(RendererContext *ctx) { m_currentContext = ctx; }
    GLDecoder & glDecoder() { return m_glDec; }
    GL2Decoder & gl2Decoder() { return m_gl2Dec; }

private:
    void initBackendCaps();

private:
    GLDecoder   m_glDec;
    ut_rendercontrol_decoder_context_t m_utDec;
    GL2Decoder m_gl2Dec;

    SocketStream   *m_stream;
    pthread_t m_thread;
    RendererContext * m_currentContext;

    struct BackendCaps {
        bool initialized;
        GLuint maxTextureUnits;
    } m_backendCaps;

    static void * s_thread(void *data);
    static __thread RenderingThread *m_tls;

    static int s_createContext(uint32_t pid, uint32_t handle, uint32_t shareCtx, int version);
    static int s_createSurface(uint32_t pid, uint32_t handle);
    static int s_destroySurface(uint32_t pid, uint32_t handle);
    static int s_destroyContext(uint32_t pid, uint32_t handle);
    static int s_makeCurrent(uint32_t pid, uint32_t drawSurface, uint32_t readSurface, uint32_t ctx);
    static void s_swapBuffers(uint32_t pid, uint32_t surface);
#ifdef PVR_WAR
    static void s_glTexParameteriv(GLenum target, GLenum param, const int *p);
    static void s_glDrawTexfOES(GLfloat x, GLfloat y, GLfloat z, GLfloat w, GLfloat h);
    static void s_glDrawTexsOES(GLshort x, GLshort y, GLshort z, GLshort w, GLshort h);
    static void s_glDrawTexiOES(GLint x, GLint y, GLint z, GLint w, GLint h);
    static void s_glDrawTexxOES(GLfixed x, GLfixed y, GLfixed z, GLfixed w, GLfixed h);
    static void s_glDrawTexfvOES(const GLfloat *coords);
    static void s_glDrawTexsvOES(const GLshort *coords);
    static void s_glDrawTexivOES(const GLint *coords);
    static void s_glDrawTexxvOES(const GLfixed *coords);

    static void s_glActiveTexture(GLenum texture);
    static void s_glBindTexture(GLenum target, GLuint texture);
    static void s_glEnable(GLenum cap);
    static void s_glDisable(GLenum cap);
    static void s_glClientActiveTexture(GLenum texture);
    static void s_glEnableClientState(GLenum cap);
    static void s_glDisableClientState(GLenum cap);

    void applyPendingCropRects();
    void fixTextureEnable();

    glTexParameteriv_server_proc_t m_glTexParameteriv;
    glDrawTexfOES_server_proc_t m_glDrawTexfOES;
    glDrawTexiOES_server_proc_t m_glDrawTexiOES;
    glDrawTexsOES_server_proc_t m_glDrawTexsOES;
    glDrawTexxOES_server_proc_t m_glDrawTexxOES;
    glDrawTexfvOES_server_proc_t m_glDrawTexfvOES;
    glDrawTexivOES_server_proc_t m_glDrawTexivOES;
    glDrawTexsvOES_server_proc_t m_glDrawTexsvOES;
    glDrawTexxvOES_server_proc_t m_glDrawTexxvOES;
    glActiveTexture_server_proc_t m_glActiveTexture;
    glBindTexture_server_proc_t m_glBindTexture;
    glEnable_server_proc_t m_glEnable;
    glDisable_server_proc_t m_glDisable;
    glClientActiveTexture_server_proc_t m_glClientActiveTexture;
    glEnableClientState_server_proc_t m_glEnableClientState;
    glDisableClientState_server_proc_t m_glDisableClientState;
#endif

};

#endif
