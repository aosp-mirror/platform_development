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
#include "GLDecoder.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <EGL/egl.h>
#include <GLES/gl.h>
#include <GLES/glext.h>

GLDecoder::GLDecoder()
{
    m_contextData = NULL;
    m_glesDso = NULL;
}

GLDecoder::~GLDecoder()
{
    if (m_glesDso != NULL) {
        delete m_glesDso;
    }
}


int GLDecoder::initGL(get_proc_func_t getProcFunc, void *getProcFuncData)
{
    if (getProcFunc == NULL) {
        const char *libname = GLES_LIBNAME;
        if (getenv(GLES_LIBNAME_VAR) != NULL) {
            libname = getenv(GLES_LIBNAME_VAR);
        }

        m_glesDso = osUtils::dynLibrary::open(libname);
        if (m_glesDso == NULL) {
            fprintf(stderr, "Couldn't find %s \n", GLES_LIBNAME);
            return -1;
        }

        this->initDispatchByName(s_getProc, this);
    } else {
        this->initDispatchByName(getProcFunc, getProcFuncData);
    }

    set_glGetCompressedTextureFormats(s_glGetCompressedTextureFormats);
    set_glVertexPointerOffset(s_glVertexPointerOffset);
    set_glColorPointerOffset(s_glColorPointerOffset);
    set_glNormalPointerOffset(s_glNormalPointerOffset);
    set_glTexCoordPointerOffset(s_glTexCoordPointerOffset);
    set_glPointSizePointerOffset(s_glPointSizePointerOffset);
    set_glWeightPointerOffset(s_glWeightPointerOffset);
    set_glMatrixIndexPointerOffset(s_glMatrixIndexPointerOffset);

    set_glVertexPointerData(s_glVertexPointerData);
    set_glColorPointerData(s_glColorPointerData);
    set_glNormalPointerData(s_glNormalPointerData);
    set_glTexCoordPointerData(s_glTexCoordPointerData);
    set_glPointSizePointerData(s_glPointSizePointerData);
    set_glWeightPointerData(s_glWeightPointerData);
    set_glMatrixIndexPointerData(s_glMatrixIndexPointerData);

    set_glDrawElementsOffset(s_glDrawElementsOffset);
    set_glDrawElementsData(s_glDrawElementsData);
    set_glFinishRoundTrip(s_glFinishRoundTrip);

    return 0;
}

int GLDecoder::s_glFinishRoundTrip(void *self)
{
    GLDecoder *ctx = (GLDecoder *)self;
    ctx->glFinish();
    return 0;
}

void GLDecoder::s_glVertexPointerOffset(void *self, GLint size, GLenum type, GLsizei stride, GLuint offset)
{
    GLDecoder *ctx = (GLDecoder *)self;
    ctx->glVertexPointer(size, type, stride, (void *)offset);
}

void GLDecoder::s_glColorPointerOffset(void *self, GLint size, GLenum type, GLsizei stride, GLuint offset)
{
    GLDecoder *ctx = (GLDecoder *)self;
    ctx->glColorPointer(size, type, stride, (void *)offset);
}

void GLDecoder::s_glTexCoordPointerOffset(void *self, GLint size, GLenum type, GLsizei stride, GLuint offset)
{
    GLDecoder *ctx = (GLDecoder *)self;
    ctx->glTexCoordPointer(size, type, stride, (void *) offset);
}

void GLDecoder::s_glNormalPointerOffset(void *self, GLenum type, GLsizei stride, GLuint offset)
{
    GLDecoder *ctx = (GLDecoder *)self;
    ctx->glNormalPointer(type, stride, (void *)offset);
}

void GLDecoder::s_glPointSizePointerOffset(void *self, GLenum type, GLsizei stride, GLuint offset)
{
    GLDecoder *ctx = (GLDecoder *)self;
    ctx->glPointSizePointerOES(type, stride, (void *)offset);
}

void GLDecoder::s_glWeightPointerOffset(void * self, GLint size, GLenum type, GLsizei stride, GLuint offset)
{
    GLDecoder *ctx = (GLDecoder *)self;
    ctx->glWeightPointerOES(size, type, stride, (void*)offset);
}

void GLDecoder::s_glMatrixIndexPointerOffset(void * self, GLint size, GLenum type, GLsizei stride, GLuint offset)
{
    GLDecoder *ctx = (GLDecoder *)self;
    ctx->glMatrixIndexPointerOES(size, type, stride, (void*)offset);
}



#define STORE_POINTER_DATA_OR_ABORT(location)    \
    if (ctx->m_contextData != NULL) {   \
        ctx->m_contextData->storePointerData((location), data, datalen); \
    } else { \
        return; \
    }

void GLDecoder::s_glVertexPointerData(void *self, GLint size, GLenum type, GLsizei stride, void *data, GLuint datalen)
{
    GLDecoder *ctx = (GLDecoder *)self;

    STORE_POINTER_DATA_OR_ABORT(GLDecoderContextData::VERTEX_LOCATION);

    ctx->glVertexPointer(size, type, 0, ctx->m_contextData->pointerData(GLDecoderContextData::VERTEX_LOCATION));
}

void GLDecoder::s_glColorPointerData(void *self, GLint size, GLenum type, GLsizei stride, void *data, GLuint datalen)
{
    GLDecoder *ctx = (GLDecoder *)self;

    STORE_POINTER_DATA_OR_ABORT(GLDecoderContextData::COLOR_LOCATION);

    ctx->glColorPointer(size, type, 0, ctx->m_contextData->pointerData(GLDecoderContextData::COLOR_LOCATION));
}

void GLDecoder::s_glTexCoordPointerData(void *self, GLint unit, GLint size, GLenum type, GLsizei stride, void *data, GLuint datalen)
{
    GLDecoder *ctx = (GLDecoder *)self;
    STORE_POINTER_DATA_OR_ABORT((GLDecoderContextData::PointerDataLocation)
                                (GLDecoderContextData::TEXCOORD0_LOCATION + unit));

    ctx->glTexCoordPointer(size, type, 0,
                           ctx->m_contextData->pointerData((GLDecoderContextData::PointerDataLocation)
                                                           (GLDecoderContextData::TEXCOORD0_LOCATION + unit)));
}

void GLDecoder::s_glNormalPointerData(void *self, GLenum type, GLsizei stride, void *data, GLuint datalen)
{
    GLDecoder *ctx = (GLDecoder *)self;

    STORE_POINTER_DATA_OR_ABORT(GLDecoderContextData::NORMAL_LOCATION);

    ctx->glNormalPointer(type, 0, ctx->m_contextData->pointerData(GLDecoderContextData::NORMAL_LOCATION));
}

void GLDecoder::s_glPointSizePointerData(void *self, GLenum type, GLsizei stride, void *data, GLuint datalen)
{
    GLDecoder *ctx = (GLDecoder *)self;

    STORE_POINTER_DATA_OR_ABORT(GLDecoderContextData::POINTSIZE_LOCATION);

    ctx->glPointSizePointerOES(type, 0, ctx->m_contextData->pointerData(GLDecoderContextData::POINTSIZE_LOCATION));
}

void GLDecoder::s_glWeightPointerData(void * self, GLint size, GLenum type, GLsizei stride, void * data, GLuint datalen)
{
    GLDecoder *ctx = (GLDecoder *)self;

    STORE_POINTER_DATA_OR_ABORT(GLDecoderContextData::WEIGHT_LOCATION);

    ctx->glWeightPointerOES(size, type, 0, ctx->m_contextData->pointerData(GLDecoderContextData::WEIGHT_LOCATION));
}

void GLDecoder::s_glMatrixIndexPointerData(void * self, GLint size, GLenum type, GLsizei stride, void * data, GLuint datalen)
{
    GLDecoder *ctx = (GLDecoder *)self;

    STORE_POINTER_DATA_OR_ABORT(GLDecoderContextData::MATRIXINDEX_LOCATION);

    ctx->glMatrixIndexPointerOES(size, type, 0, ctx->m_contextData->pointerData(GLDecoderContextData::MATRIXINDEX_LOCATION));
}

void GLDecoder::s_glDrawElementsOffset(void *self, GLenum mode, GLsizei count, GLenum type, GLuint offset)
{
    GLDecoder *ctx = (GLDecoder *)self;
    ctx->glDrawElements(mode, count, type, (void *)offset);
}

void GLDecoder::s_glDrawElementsData(void *self, GLenum mode, GLsizei count, GLenum type, void * data, GLuint datalen)
{
    GLDecoder *ctx = (GLDecoder *)self;
    ctx->glDrawElements(mode, count, type, data);
}

void GLDecoder::s_glGetCompressedTextureFormats(void *self, GLint count, GLint *data)
{
    GLDecoder *ctx = (GLDecoder *) self;
    ctx->glGetIntegerv(GL_COMPRESSED_TEXTURE_FORMATS, data);
}

void *GLDecoder::s_getProc(const char *name, void *userData)
{
    GLDecoder *ctx = (GLDecoder *)userData;

    if (ctx == NULL || ctx->m_glesDso == NULL) {
        return NULL;
    }

    void *func = NULL;
#ifdef USE_EGL_GETPROCADDRESS
    func = (void *) eglGetProcAddress(name);
#endif
    if (func == NULL) {
        func = (void *)(ctx->m_glesDso->findSymbol(name));
    }
    return func;
}
