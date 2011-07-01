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
#ifndef _GL_DECODER_H_
#define _GL_DECODER_H_

#include "gl_dec.h"
#include "FixedBuffer.h"
#include "GLDecoderContextData.h"
#include <osDynLibrary.h>

#define GLES_LIBNAME_VAR "ANDROID_GLESv1_LIB"
#define GLES_LIBNAME "libGLES_CM.so"

class GLDecoder : public gl_decoder_context_t
{
public:
    typedef void *(*get_proc_func_t)(const char *name, void *userData);

    GLDecoder();
    ~GLDecoder();
    int initGL(get_proc_func_t getProcFunc = NULL, void *getProcFuncData = NULL);
    void setContextData(GLDecoderContextData *contextData) { m_contextData = contextData; }

private:
    static void gl_APIENTRY s_glGetCompressedTextureFormats(void * self, GLint cont, GLint *data);
    static void gl_APIENTRY s_glVertexPointerData(void *self, GLint size, GLenum type, GLsizei stride, void *data, GLuint datalen);
    static void gl_APIENTRY s_glVertexPointerOffset(void *self, GLint size, GLenum type, GLsizei stride, GLuint offset);

    static void gl_APIENTRY s_glColorPointerData(void *self, GLint size, GLenum type, GLsizei stride, void *data, GLuint datalen);
    static void gl_APIENTRY s_glColorPointerOffset(void *self, GLint size, GLenum type, GLsizei stride, GLuint offset);

    static void gl_APIENTRY s_glTexCoordPointerData(void *self, GLint unit, GLint size, GLenum type, GLsizei stride, void *data, GLuint datalen);
    static void gl_APIENTRY s_glTexCoordPointerOffset(void *self, GLint size, GLenum type, GLsizei stride, GLuint offset);

    static void gl_APIENTRY s_glNormalPointerData(void *self, GLenum type, GLsizei stride, void *data, GLuint datalen);
    static void gl_APIENTRY s_glNormalPointerOffset(void *self, GLenum type, GLsizei stride, GLuint offset);

    static void gl_APIENTRY s_glPointSizePointerData(void *self, GLenum type, GLsizei stride, void *data, GLuint datalen);
    static void gl_APIENTRY s_glPointSizePointerOffset(void *self, GLenum type, GLsizei stride, GLuint offset);

    static void gl_APIENTRY s_glDrawElementsOffset(void *self, GLenum mode, GLsizei count, GLenum type, GLuint offset);
    static void gl_APIENTRY s_glDrawElementsData(void *self, GLenum mode, GLsizei count, GLenum type, void * data, GLuint datalen);

    static void gl_APIENTRY s_glWeightPointerData(void * self, GLint size, GLenum type, GLsizei stride, void * data, GLuint datalen);
    static void gl_APIENTRY s_glWeightPointerOffset(void * self, GLint size, GLenum type, GLsizei stride, GLuint offset);

    static void gl_APIENTRY s_glMatrixIndexPointerData(void * self, GLint size, GLenum type, GLsizei stride, void * data, GLuint datalen);
    static void gl_APIENTRY s_glMatrixIndexPointerOffset(void * self, GLint size, GLenum type, GLsizei stride, GLuint offset);

    static int gl_APIENTRY s_glFinishRoundTrip(void *self);

    static void * s_getProc(const char *name, void *userData);

    GLDecoderContextData *m_contextData;
    osUtils::dynLibrary* m_glesDso;
};

#endif
