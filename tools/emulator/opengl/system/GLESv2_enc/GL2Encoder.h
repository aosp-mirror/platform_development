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
#include "GLSharedGroup.h"
#include "FixedBuffer.h"


class GL2Encoder : public gl2_encoder_context_t {
public:
    GL2Encoder(IOStream *stream);
    virtual ~GL2Encoder();
    void setClientState(GLClientState *state) {
        m_state = state;
    }
    void setSharedGroup(GLSharedGroupPtr shared){ m_shared = shared; }
    const GLClientState *state() { return m_state; }
    const GLSharedGroupPtr shared() { return m_shared; }
    void flush() {
        gl2_encoder_context_t::m_stream->flush();
    }

    void setInitialized(){ m_initialized = true; };
    bool isInitialized(){ return m_initialized; };

    virtual void setError(GLenum error){ m_error = error; };
    virtual GLenum getError() { return m_error; };

private:

    bool    m_initialized;
    GLClientState *m_state;
    GLSharedGroupPtr m_shared;
    GLenum  m_error;

    GLint *m_compressedTextureFormats;
    GLint m_num_compressedTextureFormats;
    GLint *getCompressedTextureFormats();

    FixedBuffer m_fixedBuffer;

    void sendVertexAttributes(GLint first, GLsizei count);

    glGetError_client_proc_t    m_glGetError_enc;
    static GLenum s_glGetError(void * self);

    glFlush_client_proc_t m_glFlush_enc;
    static void s_glFlush(void * self);

    glPixelStorei_client_proc_t m_glPixelStorei_enc;
    static void s_glPixelStorei(void *self, GLenum param, GLint value);

    glGetString_client_proc_t m_glGetString_enc;
    static const GLubyte * s_glGetString(void *self, GLenum name);

    glBindBuffer_client_proc_t m_glBindBuffer_enc;
    static void s_glBindBuffer(void *self, GLenum target, GLuint id);

    
    glBufferData_client_proc_t m_glBufferData_enc;
    static void s_glBufferData(void *self, GLenum target, GLsizeiptr size, const GLvoid * data, GLenum usage);
    glBufferSubData_client_proc_t m_glBufferSubData_enc;
    static void s_glBufferSubData(void *self, GLenum target, GLintptr offset, GLsizeiptr size, const GLvoid * data);
    glDeleteBuffers_client_proc_t m_glDeleteBuffers_enc;
    static void s_glDeleteBuffers(void *self, GLsizei n, const GLuint * buffers);

    glDrawArrays_client_proc_t m_glDrawArrays_enc;
    static void s_glDrawArrays(void *self, GLenum mode, GLint first, GLsizei count);

    glDrawElements_client_proc_t m_glDrawElements_enc;
    static void s_glDrawElements(void *self, GLenum mode, GLsizei count, GLenum type, const void *indices);


    glGetIntegerv_client_proc_t m_glGetIntegerv_enc;
    static void s_glGetIntegerv(void *self, GLenum pname, GLint *ptr);

    glGetFloatv_client_proc_t m_glGetFloatv_enc;
    static void s_glGetFloatv(void *self, GLenum pname, GLfloat *ptr);

    glGetBooleanv_client_proc_t m_glGetBooleanv_enc;
    static void s_glGetBooleanv(void *self, GLenum pname, GLboolean *ptr);

    glVertexAttribPointer_client_proc_t m_glVertexAttribPointer_enc;
    static void s_glVertexAtrribPointer(void *self, GLuint indx, GLint size, GLenum type,
                                        GLboolean normalized, GLsizei stride, const GLvoid * ptr);

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

    static void s_glShaderSource(void *self, GLuint shader, GLsizei count, const GLchar **string, const GLint *length);

    static void s_glFinish(void *self);

    glLinkProgram_client_proc_t m_glLinkProgram_enc;
    static void s_glLinkProgram(void *self, GLuint program);

    glDeleteProgram_client_proc_t m_glDeleteProgram_enc;
    static void s_glDeleteProgram(void * self, GLuint program);

    glGetUniformiv_client_proc_t m_glGetUniformiv_enc;
    static void s_glGetUniformiv(void *self, GLuint program, GLint location , GLint *params);

    glGetUniformfv_client_proc_t m_glGetUniformfv_enc;
    static void s_glGetUniformfv(void *self, GLuint program, GLint location , GLfloat *params);

    glCreateProgram_client_proc_t m_glCreateProgram_enc;
    static GLuint s_glCreateProgram(void *self);

    glCreateShader_client_proc_t m_glCreateShader_enc;
    static GLuint s_glCreateShader(void *self, GLenum shaderType);

    glDeleteShader_client_proc_t m_glDeleteShader_enc;
    static void s_glDeleteShader(void *self, GLuint shader);

};
#endif
