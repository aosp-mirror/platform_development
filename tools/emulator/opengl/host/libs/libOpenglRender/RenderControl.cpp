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
#include "renderControl_dec.h"
#include "FrameBuffer.h"
#include "FBConfig.h"
#include "EGLDispatch.h"
#include "GLDispatch.h"
#include "GL2Dispatch.h"
#include "ThreadInfo.h"

static const GLint rendererVersion = 1;

static GLint rcGetRendererVersion()
{
    return rendererVersion;
}

static EGLint rcGetEGLVersion(EGLint* major, EGLint* minor)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return EGL_FALSE;
    }
    *major = (EGLint)fb->getCaps().eglMajor;
    *minor = (EGLint)fb->getCaps().eglMinor;

    return EGL_TRUE;
}

static EGLint rcQueryEGLString(EGLenum name, void* buffer, EGLint bufferSize)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return 0;
    }

    const char *str = s_egl.eglQueryString(fb->getDisplay(), name);
    if (!str) {
        return 0;
    }

    int len = strlen(str) + 1;
    if (!buffer || len > bufferSize) {
        return -len;
    }

    strcpy((char *)buffer, str);
    return len;
}

static EGLint rcGetGLString(EGLenum name, void* buffer, EGLint bufferSize)
{
    RenderThreadInfo *tInfo = getRenderThreadInfo();
    if (!tInfo || !tInfo->currContext.Ptr()) {
        return 0;
    }

    const char *str = NULL;
#ifdef WITH_GLES2
    if (tInfo->currContext->isGL2()) {
        str = (const char *)s_gl2.glGetString(name);
    }
    else {
#endif
        str = (const char *)s_gl.glGetString(name);
#ifdef WITH_GLES2
    }
#endif

    if (!str) {
        return 0;
    }

    int len = strlen(str) + 1;
    if (!buffer || len > bufferSize) {
        return -len;
    }

    strcpy((char *)buffer, str);
    return len;
}

static EGLint rcGetNumConfigs(uint32_t* numAttribs)
{
    if (numAttribs) {
        *numAttribs = FBConfig::getNumAttribs();
    }
    return FBConfig::getNumConfigs();
}

static EGLint rcGetConfigs(uint32_t bufSize, GLuint* buffer)
{
    int configSize = FBConfig::getNumAttribs();
    int nConfigs = FBConfig::getNumConfigs();
    uint32_t neededSize = (nConfigs + 1) * configSize * sizeof(GLuint);
    if (!buffer || bufSize < neededSize) {
        return -neededSize;
    }
    FBConfig::packConfigsInfo(buffer);
    return nConfigs;
}

static EGLint rcChooseConfig(EGLint *attribs, uint32_t attribs_size, uint32_t *configs, uint32_t configs_size)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return 0;
    }

    return FBConfig::chooseConfig(fb, attribs, configs, configs_size);
}

static EGLint rcGetFBParam(EGLint param)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return 0;
    }

    EGLint ret = 0;

    switch(param) {
        case FB_WIDTH:
            ret = fb->getWidth();
            break;
        case FB_HEIGHT:
            ret = fb->getHeight();
            break;
        case FB_XDPI:
            ret = 72; // XXX: should be implemented
            break;
        case FB_YDPI:
            ret = 72; // XXX: should be implemented
            break;
        case FB_FPS:
            ret = 60;
            break;
        case FB_MIN_SWAP_INTERVAL:
            ret = 1; // XXX: should be implemented
            break;
        case FB_MAX_SWAP_INTERVAL:
            ret = 1; // XXX: should be implemented
            break;
        default:
            break;
    }

    return ret;
}

static uint32_t rcCreateContext(uint32_t config,
                                uint32_t share, uint32_t glVersion)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return 0;
    }

    HandleType ret = fb->createRenderContext(config, share, glVersion == 2);
    return ret;
}

static void rcDestroyContext(uint32_t context)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return;
    }

    fb->DestroyRenderContext(context);
}

static uint32_t rcCreateWindowSurface(uint32_t config,
                                      uint32_t width, uint32_t height)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return 0;
    }

    return fb->createWindowSurface(config, width, height);
}

static void rcDestroyWindowSurface(uint32_t windowSurface)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return;
    }

    fb->DestroyWindowSurface( windowSurface );
}

static uint32_t rcCreateColorBuffer(uint32_t width,
                                    uint32_t height, GLenum internalFormat)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return 0;
    }

    return fb->createColorBuffer(width, height, internalFormat);
}

static void rcOpenColorBuffer(uint32_t colorbuffer)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return;
    }
    fb->openColorBuffer( colorbuffer );
}

static void rcCloseColorBuffer(uint32_t colorbuffer)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return;
    }
    fb->closeColorBuffer( colorbuffer );
}

static int rcFlushWindowColorBuffer(uint32_t windowSurface)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return -1;
    }
    fb->flushWindowSurfaceColorBuffer(windowSurface);
    return 0;
}

static void rcSetWindowColorBuffer(uint32_t windowSurface,
                                   uint32_t colorBuffer)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return;
    }
    fb->setWindowSurfaceColorBuffer(windowSurface, colorBuffer);
}

static EGLint rcMakeCurrent(uint32_t context,
                            uint32_t drawSurf, uint32_t readSurf)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return EGL_FALSE;
    }

    bool ret = fb->bindContext(context, drawSurf, readSurf);

    return (ret ? EGL_TRUE : EGL_FALSE);
}

static void rcFBPost(uint32_t colorBuffer)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return;
    }

    fb->post(colorBuffer);
}

static void rcFBSetSwapInterval(EGLint interval)
{
   // XXX: TBD - should be implemented
}

static void rcBindTexture(uint32_t colorBuffer)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return;
    }

    fb->bindColorBufferToTexture(colorBuffer);
}

static void rcBindRenderbuffer(uint32_t colorBuffer)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return;
    }

    fb->bindColorBufferToRenderbuffer(colorBuffer);
}

static EGLint rcColorBufferCacheFlush(uint32_t colorBuffer,
                                      EGLint postCount, int forRead)
{
   // XXX: TBD - should be implemented
   return 0;
}

static void rcReadColorBuffer(uint32_t colorBuffer,
                              GLint x, GLint y,
                              GLint width, GLint height,
                              GLenum format, GLenum type, void* pixels)
{
   // XXX: TBD - should be implemented
}

static int rcUpdateColorBuffer(uint32_t colorBuffer,
                                GLint x, GLint y,
                                GLint width, GLint height,
                                GLenum format, GLenum type, void* pixels)
{
    FrameBuffer *fb = FrameBuffer::getFB();
    if (!fb) {
        return -1;
    }

    fb->updateColorBuffer(colorBuffer, x, y, width, height, format, type, pixels);
    return 0;
}

void initRenderControlContext(renderControl_decoder_context_t *dec)
{
    dec->set_rcGetRendererVersion(rcGetRendererVersion);
    dec->set_rcGetEGLVersion(rcGetEGLVersion);
    dec->set_rcQueryEGLString(rcQueryEGLString);
    dec->set_rcGetGLString(rcGetGLString);
    dec->set_rcGetNumConfigs(rcGetNumConfigs);
    dec->set_rcGetConfigs(rcGetConfigs);
    dec->set_rcChooseConfig(rcChooseConfig);
    dec->set_rcGetFBParam(rcGetFBParam);
    dec->set_rcCreateContext(rcCreateContext);
    dec->set_rcDestroyContext(rcDestroyContext);
    dec->set_rcCreateWindowSurface(rcCreateWindowSurface);
    dec->set_rcDestroyWindowSurface(rcDestroyWindowSurface);
    dec->set_rcCreateColorBuffer(rcCreateColorBuffer);
    dec->set_rcOpenColorBuffer(rcOpenColorBuffer);
    dec->set_rcCloseColorBuffer(rcCloseColorBuffer);
    dec->set_rcSetWindowColorBuffer(rcSetWindowColorBuffer);
    dec->set_rcFlushWindowColorBuffer(rcFlushWindowColorBuffer);
    dec->set_rcMakeCurrent(rcMakeCurrent);
    dec->set_rcFBPost(rcFBPost);
    dec->set_rcFBSetSwapInterval(rcFBSetSwapInterval);
    dec->set_rcBindTexture(rcBindTexture);
    dec->set_rcBindRenderbuffer(rcBindRenderbuffer);
    dec->set_rcColorBufferCacheFlush(rcColorBufferCacheFlush);
    dec->set_rcReadColorBuffer(rcReadColorBuffer);
    dec->set_rcUpdateColorBuffer(rcUpdateColorBuffer);
}
