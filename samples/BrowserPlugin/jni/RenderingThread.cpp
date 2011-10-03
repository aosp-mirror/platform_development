/*
 * Copyright 2010, The Android Open Source Project
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE COMPUTER, INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
#include "RenderingThread.h"

#include "ANPNativeWindow_npapi.h"

extern ANPLogInterfaceV0           gLogI;
extern ANPNativeWindowInterfaceV0  gNativeWindowI;

RenderingThread::RenderingThread(NPP npp) : android::Thread() {
    m_npp = npp;
    m_width = -1;
    m_height = -1;

    m_ANW = NULL;
#if (!USE_SOFTWARE_RENDERING)
    m_eglSurface = EGL_NO_SURFACE;
    m_eglContext = EGL_NO_CONTEXT;
    m_eglDisplay = EGL_NO_DISPLAY;
#endif
}

android::status_t RenderingThread::readyToRun() {
    gLogI.log(kError_ANPLogType, "thread %p acquiring native window...", this);
    while (m_ANW == NULL) {
        m_ANW = gNativeWindowI.acquireNativeWindow(m_npp);
        if (!m_ANW)
            gLogI.log(kError_ANPLogType, "thread %p acquire native window FAILED!", this);

    }
    gLogI.log(kError_ANPLogType, "thread %p acquired native window successfully!", this);

#if (!USE_SOFTWARE_RENDERING)
    m_eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);

    //initialize context
    EGLint numConfigs;
    static const EGLint configAttribs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_RED_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_BLUE_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_NONE
    };

    eglChooseConfig(m_eglDisplay, configAttribs, &m_eglConfig, 1, &numConfigs);
    checkGlError("eglChooseConfig");

    static const EGLint contextAttribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE
    };

    m_eglContext = eglCreateContext(m_eglDisplay, m_eglConfig, NULL, contextAttribs);
    checkGlError("eglCreateContext");
#endif

    return android::NO_ERROR;
}

void RenderingThread::setDimensions(int width, int height) {
    android::Mutex::Autolock lock(m_sync);
    m_width = width;
    m_height = height;
}

void RenderingThread::getDimensions(int& width, int& height) {
    android::Mutex::Autolock lock(m_sync);
    width = m_width;
    height = m_height;
}

void RenderingThread::printGLString(const char *name, GLenum s) {
    const char *v = (const char *) glGetString(s);
    gLogI.log(kError_ANPLogType, "GL %s = %s\n", name, v);
}

void RenderingThread::checkGlError(const char* op) {
    for (GLint error = glGetError(); error; error
            = glGetError()) {
        gLogI.log(kError_ANPLogType, "after %s() glError (0x%x)\n", op, error);
    }
}

GLenum RenderingThread::getInternalFormat(SkBitmap::Config config)
{
    switch(config) {
        case SkBitmap::kA8_Config:
            return GL_ALPHA;
        case SkBitmap::kARGB_4444_Config:
            return GL_RGBA;
        case SkBitmap::kARGB_8888_Config:
            return GL_RGBA;
        case SkBitmap::kRGB_565_Config:
            return GL_RGB;
        default:
            return -1;
    }
}

GLenum RenderingThread::getType(SkBitmap::Config config)
{
    switch(config) {
        case SkBitmap::kA8_Config:
            return GL_UNSIGNED_BYTE;
        case SkBitmap::kARGB_4444_Config:
            return GL_UNSIGNED_SHORT_4_4_4_4;
        case SkBitmap::kARGB_8888_Config:
            return GL_UNSIGNED_BYTE;
        case SkBitmap::kIndex8_Config:
            return -1; // No type for compressed data.
        case SkBitmap::kRGB_565_Config:
            return GL_UNSIGNED_SHORT_5_6_5;
        default:
            return -1;
    }
}

void RenderingThread::setupNativeWindow(ANativeWindow* ANW, const SkBitmap& bitmap)
{
    int result = ANativeWindow_setBuffersGeometry(ANW, bitmap.width(),
            bitmap.height(), WINDOW_FORMAT_RGBA_8888);

    if (android::NO_ERROR != result) {
        gLogI.log(kError_ANPLogType, "ERROR setBuffersGeometry() status is (%d)", result);
    }

#if (!USE_SOFTWARE_RENDERING)
    if (m_eglSurface != EGL_NO_SURFACE) {
        gLogI.log(kDebug_ANPLogType, "destroying old surface");
        eglDestroySurface(m_eglDisplay, m_eglSurface);
    }

    m_eglSurface = eglCreateWindowSurface(m_eglDisplay, m_eglConfig, ANW, NULL);
    checkGlError("eglCreateWindowSurface");

    eglMakeCurrent(m_eglDisplay, m_eglSurface, m_eglSurface, m_eglContext);

    //optional: enable async mode
    //eglSwapInterval(m_eglDisplay, 0);
#endif

    updateNativeWindow(ANW, bitmap);
}

void RenderingThread::updateNativeWindow(ANativeWindow* ANW,
                                         const SkBitmap& bitmap)
{
#if USE_SOFTWARE_RENDERING
    if (bitmap.height() == 0 || bitmap.width() == 0)
        return;

    //STEP 1: lock the ANW, getting a buffer
    ANativeWindow_Buffer buffer;
    if (ANativeWindow_lock(ANW, &buffer, NULL) < 0 ) // todo: use rect parameter for efficiency
        return;

    //STEP 2: draw into the buffer
    uint8_t* img = (uint8_t*)buffer.bits;
    int row, col;
    int bpp = 4; // Here we only deal with RGBA8888 format.
    bitmap.lockPixels();
    uint8_t* bitmapOrigin = static_cast<uint8_t*>(bitmap.getPixels());
    // Copy line by line to handle offsets and stride
    for (row = 0 ; row < bitmap.height(); row ++) {
        uint8_t* dst = &(img[(buffer.stride * (row + 0) + 0) * bpp]);
        uint8_t* src = &(bitmapOrigin[bitmap.width() * row * bpp]);
        memcpy(dst, src, bpp * bitmap.width());
    }
    bitmap.unlockPixels();

    //STEP 3: push the buffer to the queue
    ANativeWindow_unlockAndPost(ANW);

#else

    //rotate the intensity of the green channel, other channels fixed
    static int i = 0;
    i = (i >= 245) ? 0 : i+10;

    glClearColor(0.6, (i*1.0/256), 0.6, 0.6);
    glClear(GL_COLOR_BUFFER_BIT);

    eglSwapBuffers(m_eglDisplay, m_eglSurface);
#endif
}

