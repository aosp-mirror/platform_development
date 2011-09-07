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
#include "RenderingThread.h"
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <pthread.h>
#include "ReadBuffer.h"
#include "Renderer.h"
#include "TimeUtils.h"

#include <GLES/glext.h>

__thread RenderingThread * RenderingThread::m_tls;

#ifdef PVR_WAR
void RenderingThread::s_glTexParameteriv(GLenum target, GLenum param, const int *p)
{
    if (target == GL_TEXTURE_2D && param == GL_TEXTURE_CROP_RECT_OES) {
        m_tls->m_currentContext->addPendingCropRect(p);
    } else {
        m_tls->m_glTexParameteriv(target, param, p);
    }
}

void RenderingThread::s_glDrawTexfOES(GLfloat x, GLfloat y, GLfloat z, GLfloat w, GLfloat h)
{
    m_tls->applyPendingCropRects();
    m_tls->m_glDrawTexfOES(x, y, z, w, h);
    m_tls->fixTextureEnable();
}

void RenderingThread::s_glDrawTexsOES(GLshort x, GLshort y, GLshort z, GLshort w, GLshort h)
{
    m_tls->applyPendingCropRects();
    m_tls->m_glDrawTexsOES(x, y, z, w, h);
    m_tls->fixTextureEnable();
}

void RenderingThread::s_glDrawTexiOES(GLint x, GLint y, GLint z, GLint w, GLint h)
{
    m_tls->applyPendingCropRects();
    m_tls->m_glDrawTexiOES(x, y, z, w, h);
    m_tls->fixTextureEnable();
}

void RenderingThread::s_glDrawTexxOES(GLfixed x, GLfixed y, GLfixed z, GLfixed w, GLfixed h)
{
    m_tls->applyPendingCropRects();
    m_tls->m_glDrawTexxOES(x, y, z, w, h);
    m_tls->fixTextureEnable();
}

void RenderingThread::s_glDrawTexfvOES(const GLfloat *coords)
{
    m_tls->applyPendingCropRects();
    m_tls->m_glDrawTexfvOES(coords);
    m_tls->fixTextureEnable();
}

void RenderingThread::s_glDrawTexsvOES(const GLshort *coords)
{
    m_tls->applyPendingCropRects();
    m_tls->m_glDrawTexsvOES(coords);
    m_tls->fixTextureEnable();
}

void RenderingThread::s_glDrawTexivOES(const GLint *coords)
{
    m_tls->applyPendingCropRects();
    m_tls->m_glDrawTexivOES(coords);
    m_tls->fixTextureEnable();
}

void RenderingThread::s_glDrawTexxvOES(const GLfixed *coords)
{
    m_tls->applyPendingCropRects();
    m_tls->m_glDrawTexxvOES(coords);
    m_tls->fixTextureEnable();
}


void RenderingThread::s_glActiveTexture(GLenum texture)
{
    if (texture - GL_TEXTURE0 >= m_tls->m_backendCaps.maxTextureUnits) return;

    m_tls->m_currentContext->setActiveTexture(texture);
    m_tls->m_glActiveTexture(texture);
}

void RenderingThread::s_glBindTexture(GLenum target, GLuint texture)
{
    if (target == GL_TEXTURE_2D) m_tls->m_currentContext->setTex2DBind(texture);
    m_tls->m_glBindTexture(target, texture);
}

void RenderingThread::s_glEnable(GLenum cap)
{
    if (cap == GL_TEXTURE_2D) m_tls->m_currentContext->setTex2DEnable(true);
    m_tls->m_glEnable(cap);
}

void RenderingThread::s_glDisable(GLenum cap)
{
    if (cap == GL_TEXTURE_2D) m_tls->m_currentContext->setTex2DEnable(false);
    m_tls->m_glDisable(cap);
}

void RenderingThread::s_glClientActiveTexture(GLenum texture)
{
    if (texture - GL_TEXTURE0 >= m_tls->m_backendCaps.maxTextureUnits) return;
    m_tls->m_currentContext->setClientActiveTexture(texture);
    m_tls->m_glClientActiveTexture(texture);
}

void RenderingThread::s_glEnableClientState(GLenum cap)
{
    m_tls->m_currentContext->enableClientState(cap, true);
    m_tls->m_glEnableClientState(cap);
}

void RenderingThread::s_glDisableClientState(GLenum cap)
{
    m_tls->m_currentContext->enableClientState(cap, false);
    m_tls->m_glDisableClientState(cap);
}

void RenderingThread::applyPendingCropRects()
{
    PendingCropRectSet &rset = m_currentContext->getPendingCropRects();
    if (rset.size() > 0) {
        GLuint currBindedTex = m_currentContext->getTex2DBind();
        for (PendingCropRectSet::iterator i = rset.begin();
             i != rset.end();
             i++) {
            m_glBindTexture(GL_TEXTURE_2D, (*i)->texture);
            m_glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, (int *)(*i)->rect);
            delete (*i);
        }
        m_glBindTexture(GL_TEXTURE_2D, currBindedTex);
        rset.clear();
    }
}

void RenderingThread::fixTextureEnable()
{
    // restore texture units enable state
    for (unsigned int i=0; i<m_backendCaps.maxTextureUnits; i++) {
        m_glActiveTexture(GL_TEXTURE0 + i);
        if (m_currentContext->isTex2DEnable(i)) {
            m_glEnable(GL_TEXTURE_2D);
        }
        else {
            m_glDisable(GL_TEXTURE_2D);
        }
        m_glClientActiveTexture(GL_TEXTURE0 + i);
        if (m_currentContext->getClientState(GL_TEXTURE_COORD_ARRAY, i)) {
            m_glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        }
        else {
            m_glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        }
    }
    // restore current active texture
    m_glActiveTexture(m_currentContext->getActiveTexture());
    m_glClientActiveTexture(m_currentContext->getClientActiveTexture());

    // restore other client state enable bits
    if (m_currentContext->getClientState(GL_VERTEX_ARRAY, 0)) {
        m_glEnableClientState(GL_VERTEX_ARRAY);
    }
    else {
        m_glDisableClientState(GL_VERTEX_ARRAY);
    }

    if (m_currentContext->getClientState(GL_NORMAL_ARRAY, 0)) {
        m_glEnableClientState(GL_NORMAL_ARRAY);
    }
    else {
        m_glDisableClientState(GL_NORMAL_ARRAY);
    }

    if (m_currentContext->getClientState(GL_COLOR_ARRAY, 0)) {
        m_glEnableClientState(GL_COLOR_ARRAY);
    }
    else {
        m_glDisableClientState(GL_COLOR_ARRAY);
    }

    if (m_currentContext->getClientState(GL_POINT_SIZE_ARRAY_OES, 0)) {
        m_glEnableClientState(GL_POINT_SIZE_ARRAY_OES);
    }
    else {
        m_glDisableClientState(GL_POINT_SIZE_ARRAY_OES);
    }
}
#endif


int RenderingThread::s_createContext(uint32_t pid, uint32_t handle, uint32_t shareCtx, int version)
{
    return Renderer::instance()->createContext(m_tls, Renderer::ClientHandle(pid, handle),
                                               Renderer::ClientHandle(pid, shareCtx),
                                               version);

}


int RenderingThread::s_createSurface(uint32_t pid, uint32_t handle)
{
    return Renderer::instance()->createSurface(m_tls, Renderer::ClientHandle(pid, handle));
}

int RenderingThread::s_destroySurface(uint32_t pid, uint32_t handle)
{
    return Renderer::instance()->destroySurface(m_tls, Renderer::ClientHandle(pid, handle));
}

int RenderingThread::s_destroyContext(uint32_t pid, uint32_t handle)
{
    return Renderer::instance()->destroyContext(m_tls, Renderer::ClientHandle(pid, handle));
}


int RenderingThread::s_makeCurrent(uint32_t pid, uint32_t drawSurface, uint32_t readSurface, uint32_t ctx)
{
    int ret = Renderer::instance()->makeCurrent(m_tls,
                                             Renderer::ClientHandle(pid, drawSurface),
                                             Renderer::ClientHandle(pid, readSurface),
                                             Renderer::ClientHandle(pid, ctx));

    if (ret && ctx) {
        m_tls->initBackendCaps();
    }

    return ret;
}

void RenderingThread::s_swapBuffers(uint32_t pid, uint32_t surface)
{
    Renderer::instance()->swapBuffers(m_tls, Renderer::ClientHandle(pid, surface));
}


RenderingThread::RenderingThread(SocketStream *stream) :
    m_stream(stream),
    m_currentContext(NULL)
{
    m_backendCaps.initialized = false;
}

int RenderingThread::start(void)
{
    if (pthread_create(&m_thread, NULL, s_thread, this) < 0) {
        perror("pthread_create");
        return -1;
    }
    return 0;
}


void * RenderingThread::s_thread(void *data)
{
    RenderingThread *self = (RenderingThread *)data;
    m_tls = self;
    return self->thread();
}

void RenderingThread::initBackendCaps()
{
    if (m_backendCaps.initialized) return;

    m_glDec.glGetIntegerv(GL_MAX_TEXTURE_UNITS, (GLint *)&m_backendCaps.maxTextureUnits);
    m_backendCaps.initialized = true;
}

void *RenderingThread::thread()
{

    // initialize our decoders;
    m_glDec.initGL();

#ifdef PVR_WAR
    m_glTexParameteriv = m_glDec.set_glTexParameteriv(s_glTexParameteriv);
    m_glDrawTexfOES = m_glDec.set_glDrawTexfOES(s_glDrawTexfOES);
    m_glDrawTexsOES = m_glDec.set_glDrawTexsOES(s_glDrawTexsOES);
    m_glDrawTexiOES = m_glDec.set_glDrawTexiOES(s_glDrawTexiOES);
    m_glDrawTexxOES = m_glDec.set_glDrawTexxOES(s_glDrawTexxOES);
    m_glDrawTexfvOES = m_glDec.set_glDrawTexfvOES(s_glDrawTexfvOES);
    m_glDrawTexsvOES = m_glDec.set_glDrawTexsvOES(s_glDrawTexsvOES);
    m_glDrawTexivOES = m_glDec.set_glDrawTexivOES(s_glDrawTexivOES);
    m_glDrawTexxvOES = m_glDec.set_glDrawTexxvOES(s_glDrawTexxvOES);
    m_glActiveTexture = m_glDec.set_glActiveTexture(s_glActiveTexture);
    m_glBindTexture = m_glDec.set_glBindTexture(s_glBindTexture);
    m_glEnable = m_glDec.set_glEnable(s_glEnable);
    m_glDisable = m_glDec.set_glDisable(s_glDisable);
    m_glClientActiveTexture = m_glDec.set_glClientActiveTexture(s_glClientActiveTexture);
    m_glEnableClientState = m_glDec.set_glEnableClientState(s_glEnableClientState);
    m_glDisableClientState = m_glDec.set_glDisableClientState(s_glDisableClientState);
#endif

    m_gl2Dec.initGL();

    m_utDec.set_swapBuffers(s_swapBuffers);
    m_utDec.set_createContext(s_createContext);
    m_utDec.set_destroyContext(s_destroyContext);
    m_utDec.set_createSurface(s_createSurface);
    m_utDec.set_destroySurface(s_destroySurface);
    m_utDec.set_makeCurrentContext(s_makeCurrent);

    ReadBuffer readBuf(m_stream, DECODER_BUF_SIZE);

    int stats_totalBytes = 0;
    long long stats_t0 = GetCurrentTimeMS();

    while (1) {

        int stat = readBuf.getData();
        if (stat == 0) {
            fprintf(stderr, "client shutdown\n");
            break;
        } else if (stat < 0) {
            perror("getData");
            break;
        }

        //
        // log received bandwidth statistics
        //
        stats_totalBytes += readBuf.validData();
        long long dt = GetCurrentTimeMS() - stats_t0;
        if (dt > 1000) {
            float dts = (float)dt / 1000.0f;
            printf("Used Bandwidth %5.3f MB/s\n", ((float)stats_totalBytes / dts) / (1024.0f*1024.0f));
            stats_totalBytes = 0;
            stats_t0 = GetCurrentTimeMS();
        }

        bool progress = true;
        while (progress) {
            progress = false;
            // we need at least one header (8 bytes) in our buffer
            if (readBuf.validData() >= 8) {
                size_t last = m_glDec.decode(readBuf.buf(), readBuf.validData(), m_stream);
                if (last > 0) {
                    progress = true;
                    readBuf.consume(last);
                }
            }

            if (readBuf.validData() >= 8) {
                size_t last = m_gl2Dec.decode(readBuf.buf(), readBuf.validData(), m_stream);
                if (last > 0) {
                    readBuf.consume(last);
                    progress = true;
                }
            }

            if (readBuf.validData() >= 8) {
                size_t last = m_utDec.decode(readBuf.buf(), readBuf.validData(), m_stream);
                if (last > 0) {
                    readBuf.consume(last);
                    progress = true;
                }
            }
        }
    }
    // shutdown
    if (m_currentContext != NULL) {
        m_currentContext->unref();
    }

    return NULL;
}
