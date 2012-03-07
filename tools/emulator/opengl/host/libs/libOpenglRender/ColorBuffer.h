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
#ifndef _LIBRENDER_COLORBUFFER_H
#define _LIBRENDER_COLORBUFFER_H

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES/gl.h>
#include <SmartPtr.h>

class ColorBuffer
{
public:
    static ColorBuffer *create(int p_width, int p_height,
                               GLenum p_internalFormat);
    ~ColorBuffer();

    GLuint getGLTextureName() const { return m_tex; }
    GLuint getWidth() const { return m_width; }
    GLuint getHeight() const { return m_height; }

    void subUpdate(int x, int y, int width, int height, GLenum p_format, GLenum p_type, void *pixels);
    bool post();
    bool bindToTexture();
    bool bindToRenderbuffer();
    bool blitFromCurrentReadBuffer();

private:
    ColorBuffer();
    void drawTexQuad(bool flipy);
    bool bind_fbo();  // binds a fbo which have this texture as render target

private:
    GLuint m_tex;
    GLuint m_blitTex;
    EGLImageKHR m_eglImage;
    EGLImageKHR m_blitEGLImage;
    GLuint m_width;
    GLuint m_height;
    GLuint m_fbo;
    GLenum m_internalFormat;
    bool m_warYInvertBug;
};

typedef SmartPtr<ColorBuffer> ColorBufferPtr;

#endif
