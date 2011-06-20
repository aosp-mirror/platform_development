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
#ifndef _GL_ENCODER_H_
#define _GL_ENCODER_H_

#include "gl_enc.h"
#include "GLClientState.h"
#include "FixedBuffer.h"

class GLEncoder : public gl_encoder_context_t {

public:
    GLEncoder(IOStream *stream);
    virtual ~GLEncoder();
    void setClientState(GLClientState *state) {
        m_state = state;
    }
    void flush() { m_stream->flush(); }
    size_t pixelDataSize(GLsizei width, GLsizei height, GLenum format, GLenum type, int pack);
private:

    GLClientState *m_state;
    FixedBuffer m_fixedBuffer;
    GLint *m_compressedTextureFormats;
    GLint m_num_compressedTextureFormats;

    GLint *getCompressedTextureFormats();
    // original functions;
    glGetIntegerv_client_proc_t m_glGetIntegerv_enc;
    glGetFloatv_client_proc_t m_glGetFloatv_enc;
    glGetFixedv_client_proc_t m_glGetFixedv_enc;
    glGetBooleanv_client_proc_t m_glGetBooleanv_enc;
    glGetPointerv_client_proc_t m_glGetPointerv_enc;

    glPixelStorei_client_proc_t m_glPixelStorei_enc;
    glVertexPointer_client_proc_t m_glVertexPointer_enc;
    glNormalPointer_client_proc_t m_glNormalPointer_enc;
    glColorPointer_client_proc_t m_glColorPointer_enc;
    glPointSizePointerOES_client_proc_t m_glPointSizePointerOES_enc;
    glTexCoordPointer_client_proc_t m_glTexCoordPointer_enc;
    glClientActiveTexture_client_proc_t m_glClientActiveTexture_enc;
    glMatrixIndexPointerOES_client_proc_t m_glMatrixIndexPointerOES_enc;
    glWeightPointerOES_client_proc_t m_glWeightPointerOES_enc;

    glBindBuffer_client_proc_t m_glBindBuffer_enc;
    glEnableClientState_client_proc_t m_glEnableClientState_enc;
    glDisableClientState_client_proc_t m_glDisableClientState_enc;
    glIsEnabled_client_proc_t m_glIsEnabled_enc;
    glDrawArrays_client_proc_t m_glDrawArrays_enc;
    glDrawElements_client_proc_t m_glDrawElements_enc;
    glFlush_client_proc_t m_glFlush_enc;

    // statics
    static void s_glGetIntegerv(void *self, GLenum pname, GLint *ptr);
    static void s_glGetBooleanv(void *self, GLenum pname, GLboolean *ptr);
    static void s_glGetFloatv(void *self, GLenum pname, GLfloat *ptr);
    static void s_glGetFixedv(void *self, GLenum pname, GLfixed *ptr);
    static void s_glGetPointerv(void *self, GLenum pname, GLvoid **params);

    static void s_glFlush(void * self);
    static const GLubyte * s_glGetString(void *self, GLenum name);
    static void s_glVertexPointer(void *self, int size, GLenum type, GLsizei stride, const void *data);
    static void s_glNormalPointer(void *self, GLenum type, GLsizei stride, const void *data);
    static void s_glColorPointer(void *self, int size, GLenum type, GLsizei stride, const void *data);
    static void s_glPointsizePointer(void *self, GLenum type, GLsizei stride, const void *data);
    static void s_glClientActiveTexture(void *self, GLenum texture);
    static void s_glTexcoordPointer(void *self, int size, GLenum type, GLsizei stride, const void *data);
    static void s_glMatrixIndexPointerOES(void *self, int size, GLenum type, GLsizei stride, const void * data);
    static void s_glWeightPointerOES(void *self, int size, GLenum type, GLsizei stride, const void * data);
    static void s_glDisableClientState(void *self, GLenum state);
    static void s_glEnableClientState(void *self, GLenum state);
    static GLboolean s_glIsEnabled(void *self, GLenum cap);
    static void s_glBindBuffer(void *self, GLenum target, GLuint id);
    static void s_glDrawArrays(void *self, GLenum mode, GLint first, GLsizei count);
    static void s_glDrawElements(void *self, GLenum mode, GLsizei count, GLenum type, const void *indices);
    static void s_glPixelStorei(void *self, GLenum param, GLint value);
    void sendVertexData(unsigned first, unsigned count);
};
#endif
