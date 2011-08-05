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
#include "android_npapi.h"
#include "SkCanvas.h"
#include "SkBitmap.h"

#include <EGL/egl.h>
#include <GLES2/gl2.h>

#ifndef RenderingThread__DEFINED
#define RenderingThread__DEFINED

#define USE_SOFTWARE_RENDERING false
#define MS_PER_FRAME 17 // approx 60 fps

class RenderingThread : public android::Thread {
public:
    RenderingThread(NPP npp);
    virtual ~RenderingThread() {};
    virtual android::status_t readyToRun();

    void setDimensions(int width, int height);
    void getDimensions(int& width, int& height);

protected:
    NPP m_npp;
    ANativeWindow* m_ANW;

    static void printGLString(const char *name, GLenum s);
    static void checkGlError(const char* op);
    static GLenum getInternalFormat(SkBitmap::Config config);
    static GLenum getType(SkBitmap::Config config);
    void setupNativeWindow(ANativeWindow* ANW, const SkBitmap& bitmap);
    void updateNativeWindow(ANativeWindow* ANW, const SkBitmap& bitmap);

private:
    virtual bool threadLoop() = 0;

    android::Mutex m_sync;
    int m_width;
    int m_height;

#if (!USE_SOFTWARE_RENDERING)
    EGLDisplay m_eglDisplay;
    EGLSurface m_eglSurface;
    EGLContext m_eglContext;
    EGLConfig  m_eglConfig;
#endif
};




#endif // RenderingThread__DEFINED
