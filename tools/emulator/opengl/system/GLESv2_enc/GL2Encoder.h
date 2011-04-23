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
#ifndef _GL2_ENCODER_H_
#define _GL2_ENCODER_H_

#include "gl2_enc.h"
#include "IOStream.h"
#include "GLClientState.h"
#include "FixedBuffer.h"


class GL2Encoder : public gl2_encoder_context_t {
public:
    GL2Encoder(IOStream *stream);
    virtual ~GL2Encoder();
    void setClientState(GLClientState *state) {
        m_state = state;
    }
    const GLClientState *state() { return m_state; }
    void flush() {
        gl2_encoder_context_t::m_stream->flush();
    }
private:
    GLClientState *m_state;

    GLint *m_compressedTextureFormats;
    GLint m_num_compressedTextureFormats;
    GLint *getCompressedTextureFormats();

    FixedBuffer m_fixedBuffer;

    void sendVertexAttributes(GLint first, GLsizei count);

    glFlush_client_proc_t m_glFlush_enc;
    static void s_glFlush(void * self);

    glPixelStorei_client_proc_t m_glPixelStorei_enc;
    static void s_glPixelStorei(void *self, GLenum param, GLint value);

    glGetString_client_proc_t m_glGetString_enc;
    static GLubyte * s_glGetString(void *self, GLenum name);

    glBindBuffer_client_proc_t m_glBindBuffer_enc;
    static void s_glBindBuffer(void *self, GLenum target, GLuint id);

    glDrawArrays_client_proc_t m_glDrawArrays_enc;
    static void s_glDrawArrays(void *self, GLenum mode, GLint first, GLsizei count);

    glDrawElements_client_proc_t m_glDrawElements_enc;
    static void s_glDrawElements(void *self, GLenum mode, GLsizei count, GLenum type, void *indices);


    glGetIntegerv_client_proc_t m_glGetIntegerv_enc;
    static void s_glGetIntegerv(void *self, GLenum pname, GLint *ptr);

    glGetFloatv_client_proc_t m_glGetFloatv_enc;
    static void s_glGetFloatv(void *self, GLenum pname, GLfloat *ptr);

    glGetBooleanv_client_proc_t m_glGetBooleanv_enc;
    static void s_glGetBooleanv(void *self, GLenum pname, GLboolean *ptr);

    glVertexAttribPointer_client_proc_t m_glVertexAttribPointer_enc;
    static void s_glVertexAtrribPointer(void *self, GLuint indx, GLint size, GLenum type,
                                        GLboolean normalized, GLsizei stride, GLvoid * ptr);

    glEnableVertexAttribArray_client_proc_t m_glEnableVertexAttribArray_enc;
    static void s_glEnableVertexAttribArray(void *self, GLuint index);

    glDisableVertexAttribArray_client_proc_t m_glDisableVertexAttribArray_enc;
    static void s_glDisableVertexAttribArray(void *self, GLuint index);

    glGetVertexAttribiv_client_proc_t m_glGetVertexAttribiv_enc;
    static void s_glGetVertexAttribiv(void *self, GLuint index, GLenum pname, GLint *params);

    glGetVertexAttribfv_client_proc_t m_glGetVertexAttribfv_enc;
    static void s_glGetVertexAttribfv(void *self, GLuint index, GLenum pname, GLfloat *params);

    glGetVertexAttribPointerv_client_proc_t m_glGetVertexAttribPointerv;
    static void s_glGetVertexAttribPointerv(void *self, GLuint index, GLenum pname, GLvoid **pointer);

    static void s_glShaderSource(void *self, GLuint shader, GLsizei count, GLstr *string, GLint *length);
};
#endif
