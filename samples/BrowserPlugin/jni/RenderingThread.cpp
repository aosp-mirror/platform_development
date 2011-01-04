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

#include "ANPOpenGL_npapi.h"

extern ANPLogInterfaceV0       gLogI;
extern ANPOpenGLInterfaceV0    gOpenGLI;

RenderingThread::RenderingThread(NPP npp) : android::Thread() {
    m_npp = npp;
    m_width = -1;
    m_height = -1;
    gLogI.log(kError_ANPLogType, "Created Rendering Thread");
}

android::status_t RenderingThread::readyToRun() {

    gLogI.log(kError_ANPLogType, "in ready to run");

    EGLContext context = gOpenGLI.acquireContext(m_npp);

    gLogI.log(kError_ANPLogType, "context: %p", context);

    if (context == EGL_NO_CONTEXT) {
        gLogI.log(kError_ANPLogType, "Unable to create EGLContext for a TextureProducer thread");
        return android::UNKNOWN_ERROR;
    }
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

void RenderingThread::createTextureWithBitmap(GLuint texture, SkBitmap& bitmap) {
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glBindTexture(GL_TEXTURE_2D, texture);
    checkGlError("glBindTexture");
    SkBitmap::Config config = bitmap.getConfig();
    int internalformat = getInternalFormat(config);
    int type = getType(config);
    bitmap.lockPixels();
    glTexImage2D(GL_TEXTURE_2D, 0, internalformat, bitmap.width(), bitmap.height(),
                 0, internalformat, type, bitmap.getPixels());
    bitmap.unlockPixels();
    checkGlError("glTexImage2D");
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
}

void RenderingThread::updateTextureWithBitmap(GLuint texture, SkBitmap& bitmap) {
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glBindTexture(GL_TEXTURE_2D, texture);
    checkGlError("glBindTexture");
    SkBitmap::Config config = bitmap.getConfig();
    int internalformat = getInternalFormat(config);
    int type = getType(config);
    bitmap.lockPixels();
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, bitmap.width(), bitmap.height(),
                    internalformat, type, bitmap.getPixels());
    bitmap.unlockPixels();
    checkGlError("glTexSubImage2D");
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
}
