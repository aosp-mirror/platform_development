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

#include "GL2Encoder.h"
#include <private/ui/android_natives_priv.h>
#include <assert.h>
#include <ctype.h>

#ifndef MIN
#define MIN(a, b) ((a) < (b) ? (a) : (b))
#endif

static GLubyte *gVendorString= (GLubyte *) "Android";
static GLubyte *gRendererString= (GLubyte *) "Android HW-GLES 2.0";
static GLubyte *gVersionString= (GLubyte *) "OpenGL ES 2.0";
static GLubyte *gExtensionsString= (GLubyte *) ""; // no extensions at this point;

#define SET_ERROR_IF(condition,err) if((condition)) {                            \
        LOGE("%s:%s:%d GL error 0x%x\n", __FILE__, __FUNCTION__, __LINE__, err); \
        ctx->setError(err);                                    \
        return;                                                  \
    }


#define RET_AND_SET_ERROR_IF(condition,err,ret) if((condition)) {                \
        LOGE("%s:%s:%d GL error 0x%x\n", __FILE__, __FUNCTION__, __LINE__, err); \
        ctx->setError(err);                                    \
        return ret;                                              \
    }


GL2Encoder::GL2Encoder(IOStream *stream) : gl2_encoder_context_t(stream)
{
    m_initialized = false;
    m_state = NULL;
    m_error = GL_NO_ERROR;
    m_num_compressedTextureFormats = 0;
    m_compressedTextureFormats = NULL;
    //overrides
    m_glFlush_enc = set_glFlush(s_glFlush);
    m_glPixelStorei_enc = set_glPixelStorei(s_glPixelStorei);
    m_glGetString_enc = set_glGetString(s_glGetString);
    m_glBindBuffer_enc = set_glBindBuffer(s_glBindBuffer);
    m_glBufferData_enc = set_glBufferData(s_glBufferData);
    m_glBufferSubData_enc = set_glBufferSubData(s_glBufferSubData);
    m_glDeleteBuffers_enc = set_glDeleteBuffers(s_glDeleteBuffers);
    m_glDrawArrays_enc = set_glDrawArrays(s_glDrawArrays);
    m_glDrawElements_enc = set_glDrawElements(s_glDrawElements);
    m_glGetIntegerv_enc = set_glGetIntegerv(s_glGetIntegerv);
    m_glGetFloatv_enc = set_glGetFloatv(s_glGetFloatv);
    m_glGetBooleanv_enc = set_glGetBooleanv(s_glGetBooleanv);
    m_glVertexAttribPointer_enc = set_glVertexAttribPointer(s_glVertexAtrribPointer);
    m_glEnableVertexAttribArray_enc = set_glEnableVertexAttribArray(s_glEnableVertexAttribArray);
    m_glDisableVertexAttribArray_enc = set_glDisableVertexAttribArray(s_glDisableVertexAttribArray);
    m_glGetVertexAttribiv_enc = set_glGetVertexAttribiv(s_glGetVertexAttribiv);
    m_glGetVertexAttribfv_enc = set_glGetVertexAttribfv(s_glGetVertexAttribfv);
    m_glGetVertexAttribPointerv = set_glGetVertexAttribPointerv(s_glGetVertexAttribPointerv);
    set_glShaderSource(s_glShaderSource);
    set_glFinish(s_glFinish);
    m_glGetError_enc = set_glGetError(s_glGetError);
    m_glLinkProgram_enc = set_glLinkProgram(s_glLinkProgram);
    m_glDeleteProgram_enc = set_glDeleteProgram(s_glDeleteProgram);
    m_glGetUniformiv_enc = set_glGetUniformiv(s_glGetUniformiv);
    m_glGetUniformfv_enc = set_glGetUniformfv(s_glGetUniformfv);
    m_glCreateProgram_enc = set_glCreateProgram(s_glCreateProgram);
    m_glCreateShader_enc = set_glCreateShader(s_glCreateShader);
    m_glDeleteShader_enc = set_glDeleteShader(s_glDeleteShader);
    m_glAttachShader_enc = set_glAttachShader(s_glAttachShader);
    m_glDetachShader_enc = set_glDetachShader(s_glDetachShader);
    m_glGetUniformLocation_enc = set_glGetUniformLocation(s_glGetUniformLocation);
    m_glUseProgram_enc = set_glUseProgram(s_glUseProgram);

    m_glUniform1f_enc = set_glUniform1f(s_glUniform1f);
    m_glUniform1fv_enc = set_glUniform1fv(s_glUniform1fv);
    m_glUniform1i_enc = set_glUniform1i(s_glUniform1i);
    m_glUniform1iv_enc = set_glUniform1iv(s_glUniform1iv);
    m_glUniform2f_enc = set_glUniform2f(s_glUniform2f);
    m_glUniform2fv_enc = set_glUniform2fv(s_glUniform2fv);
    m_glUniform2i_enc = set_glUniform2i(s_glUniform2i);
    m_glUniform2iv_enc = set_glUniform2iv(s_glUniform2iv);
    m_glUniform3f_enc = set_glUniform3f(s_glUniform3f);
    m_glUniform3fv_enc = set_glUniform3fv(s_glUniform3fv);
    m_glUniform3i_enc = set_glUniform3i(s_glUniform3i);
    m_glUniform3iv_enc = set_glUniform3iv(s_glUniform3iv);
    m_glUniform4f_enc = set_glUniform4f(s_glUniform4f);
    m_glUniform4fv_enc = set_glUniform4fv(s_glUniform4fv);
    m_glUniform4i_enc = set_glUniform4i(s_glUniform4i);
    m_glUniform4iv_enc = set_glUniform4iv(s_glUniform4iv);
    m_glUniformMatrix2fv_enc = set_glUniformMatrix2fv(s_glUniformMatrix2fv);
    m_glUniformMatrix3fv_enc = set_glUniformMatrix3fv(s_glUniformMatrix3fv);
    m_glUniformMatrix4fv_enc = set_glUniformMatrix4fv(s_glUniformMatrix4fv);

    m_glActiveTexture_enc = set_glActiveTexture(s_glActiveTexture);
    m_glBindTexture_enc = set_glBindTexture(s_glBindTexture);
    m_glDeleteTextures_enc = set_glDeleteTextures(s_glDeleteTextures);
    m_glGetTexParameterfv_enc = set_glGetTexParameterfv(s_glGetTexParameterfv);
    m_glGetTexParameteriv_enc = set_glGetTexParameteriv(s_glGetTexParameteriv);
    m_glTexParameterf_enc = set_glTexParameterf(s_glTexParameterf);
    m_glTexParameterfv_enc = set_glTexParameterfv(s_glTexParameterfv);
    m_glTexParameteri_enc = set_glTexParameteri(s_glTexParameteri);
    m_glTexParameteriv_enc = set_glTexParameteriv(s_glTexParameteriv);
}

GL2Encoder::~GL2Encoder()
{
    delete m_compressedTextureFormats;
}

GLenum GL2Encoder::s_glGetError(void * self)
{
    GL2Encoder *ctx = (GL2Encoder *)self;
    GLenum err = ctx->getError();
    if(err != GL_NO_ERROR) {
        ctx->setError(GL_NO_ERROR);
        return err;
    }

    return ctx->m_glGetError_enc(self);

}

void GL2Encoder::s_glFlush(void *self)
{
    GL2Encoder *ctx = (GL2Encoder *) self;
    ctx->m_glFlush_enc(self);
    ctx->m_stream->flush();
}

const GLubyte *GL2Encoder::s_glGetString(void *self, GLenum name)
{
    GLubyte *retval =  (GLubyte *) "";
    switch(name) {
    case GL_VENDOR:
        retval = gVendorString;
        break;
    case GL_RENDERER:
        retval = gRendererString;
        break;
    case GL_VERSION:
        retval = gVersionString;
        break;
    case GL_EXTENSIONS:
        retval = gExtensionsString;
        break;
    }
    return retval;
}

void GL2Encoder::s_glPixelStorei(void *self, GLenum param, GLint value)
{
    GL2Encoder *ctx = (GL2Encoder *)self;
    ctx->m_glPixelStorei_enc(ctx, param, value);
    assert(ctx->m_state != NULL);
    ctx->m_state->setPixelStore(param, value);
}


void GL2Encoder::s_glBindBuffer(void *self, GLenum target, GLuint id)
{
    GL2Encoder *ctx = (GL2Encoder *) self;
    assert(ctx->m_state != NULL);
    ctx->m_state->bindBuffer(target, id);
    // TODO set error state if needed;
    ctx->m_glBindBuffer_enc(self, target, id);
}

void GL2Encoder::s_glBufferData(void * self, GLenum target, GLsizeiptr size, const GLvoid * data, GLenum usage)
{
    GL2Encoder *ctx = (GL2Encoder *) self;
    GLuint bufferId = ctx->m_state->getBuffer(target);
    SET_ERROR_IF(bufferId==0, GL_INVALID_OPERATION);
    SET_ERROR_IF(size<0, GL_INVALID_VALUE);

    ctx->m_shared->updateBufferData(bufferId, size, (void*)data);
    ctx->m_glBufferData_enc(self, target, size, data, usage);
}

void GL2Encoder::s_glBufferSubData(void * self, GLenum target, GLintptr offset, GLsizeiptr size, const GLvoid * data)
{
    GL2Encoder *ctx = (GL2Encoder *) self;
    GLuint bufferId = ctx->m_state->getBuffer(target);
    SET_ERROR_IF(bufferId==0, GL_INVALID_OPERATION);

    GLenum res = ctx->m_shared->subUpdateBufferData(bufferId, offset, size, (void*)data);
    SET_ERROR_IF(res, res);

    ctx->m_glBufferSubData_enc(self, target, offset, size, data);
}

void GL2Encoder::s_glDeleteBuffers(void * self, GLsizei n, const GLuint * buffers)
{
    GL2Encoder *ctx = (GL2Encoder *) self;
    SET_ERROR_IF(n<0, GL_INVALID_VALUE);
    for (int i=0; i<n; i++) {
        ctx->m_shared->deleteBufferData(buffers[i]);
        ctx->m_glDeleteBuffers_enc(self,1,&buffers[i]);
    }
}

void GL2Encoder::s_glVertexAtrribPointer(void *self, GLuint indx, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const GLvoid * ptr)
{
    GL2Encoder *ctx = (GL2Encoder *)self;
    assert(ctx->m_state != NULL);
    ctx->m_state->setState(indx, size, type, normalized, stride, ptr);
}

void GL2Encoder::s_glGetIntegerv(void *self, GLenum param, GLint *ptr)
{
    GL2Encoder *ctx = (GL2Encoder *) self;
    assert(ctx->m_state != NULL);
    GLClientState* state = ctx->m_state;

    switch (param) {
    case GL_NUM_SHADER_BINARY_FORMATS:
        *ptr = 0;
        break;
    case GL_SHADER_BINARY_FORMATS:
        // do nothing
        break;

    case GL_COMPRESSED_TEXTURE_FORMATS: {
        GLint *compressedTextureFormats = ctx->getCompressedTextureFormats();
        if (ctx->m_num_compressedTextureFormats > 0 &&
                compressedTextureFormats != NULL) {
            memcpy(ptr, compressedTextureFormats,
                    ctx->m_num_compressedTextureFormats * sizeof(GLint));
        }
        break;
    }

    case GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS:
    case GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS:
    case GL_MAX_TEXTURE_IMAGE_UNITS:
        ctx->m_glGetIntegerv_enc(self, param, ptr);
        *ptr = MIN(*ptr, GLClientState::MAX_TEXTURE_UNITS);
        break;

    case GL_TEXTURE_BINDING_2D:
        *ptr = state->getBoundTexture(GL_TEXTURE_2D);
        break;
    case GL_TEXTURE_BINDING_EXTERNAL_OES:
        *ptr = state->getBoundTexture(GL_TEXTURE_EXTERNAL_OES);
        break;

    default:
        if (!ctx->m_state->getClientStateParameter<GLint>(param, ptr)) {
            ctx->m_glGetIntegerv_enc(self, param, ptr);
        }
        break;
    }
}


void GL2Encoder::s_glGetFloatv(void *self, GLenum param, GLfloat *ptr)
{
    GL2Encoder *ctx = (GL2Encoder *)self;
    assert(ctx->m_state != NULL);
    GLClientState* state = ctx->m_state;

    switch (param) {
    case GL_NUM_SHADER_BINARY_FORMATS:
        *ptr = 0;
        break;
    case GL_SHADER_BINARY_FORMATS:
        // do nothing
        break;

    case GL_COMPRESSED_TEXTURE_FORMATS: {
        GLint *compressedTextureFormats = ctx->getCompressedTextureFormats();
        if (ctx->m_num_compressedTextureFormats > 0 &&
                compressedTextureFormats != NULL) {
            for (int i = 0; i < ctx->m_num_compressedTextureFormats; i++) {
                ptr[i] = (GLfloat) compressedTextureFormats[i];
            }
        }
        break;
    }

    case GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS:
    case GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS:
    case GL_MAX_TEXTURE_IMAGE_UNITS:
        ctx->m_glGetFloatv_enc(self, param, ptr);
        *ptr = MIN(*ptr, (GLfloat)GLClientState::MAX_TEXTURE_UNITS);
        break;

    case GL_TEXTURE_BINDING_2D:
        *ptr = (GLfloat)state->getBoundTexture(GL_TEXTURE_2D);
        break;
    case GL_TEXTURE_BINDING_EXTERNAL_OES:
        *ptr = (GLfloat)state->getBoundTexture(GL_TEXTURE_EXTERNAL_OES);
        break;

    default:
        if (!ctx->m_state->getClientStateParameter<GLfloat>(param, ptr)) {
            ctx->m_glGetFloatv_enc(self, param, ptr);
        }
        break;
    }
}


void GL2Encoder::s_glGetBooleanv(void *self, GLenum param, GLboolean *ptr)
{
    GL2Encoder *ctx = (GL2Encoder *)self;
    assert(ctx->m_state != NULL);
    GLClientState* state = ctx->m_state;

    switch (param) {
    case GL_NUM_SHADER_BINARY_FORMATS:
        *ptr = GL_FALSE;
        break;
    case GL_SHADER_BINARY_FORMATS:
        // do nothing
        break;

    case GL_COMPRESSED_TEXTURE_FORMATS: {
        GLint *compressedTextureFormats = ctx->getCompressedTextureFormats();
        if (ctx->m_num_compressedTextureFormats > 0 &&
                compressedTextureFormats != NULL) {
            for (int i = 0; i < ctx->m_num_compressedTextureFormats; i++) {
                ptr[i] = compressedTextureFormats[i] != 0 ? GL_TRUE : GL_FALSE;
            }
        }
        break;
    }

    case GL_TEXTURE_BINDING_2D:
        *ptr = state->getBoundTexture(GL_TEXTURE_2D) != 0 ? GL_TRUE : GL_FALSE;
        break;
    case GL_TEXTURE_BINDING_EXTERNAL_OES:
        *ptr = state->getBoundTexture(GL_TEXTURE_EXTERNAL_OES) != 0
                ? GL_TRUE : GL_FALSE;
        break;

    default:
        if (!ctx->m_state->getClientStateParameter<GLboolean>(param, ptr)) {
            ctx->m_glGetBooleanv_enc(self, param, ptr);
        }
        break;
    }
}


void GL2Encoder::s_glEnableVertexAttribArray(void *self, GLuint index)
{
    GL2Encoder *ctx = (GL2Encoder *)self;
    assert(ctx->m_state);
    ctx->m_state->enable(index, 1);
}

void GL2Encoder::s_glDisableVertexAttribArray(void *self, GLuint index)
{
    GL2Encoder *ctx = (GL2Encoder *)self;
    assert(ctx->m_state);
    ctx->m_state->enable(index, 0);
}


void GL2Encoder::s_glGetVertexAttribiv(void *self, GLuint index, GLenum pname, GLint *params)
{
    GL2Encoder *ctx = (GL2Encoder *)self;
    assert(ctx->m_state);

    if (!ctx->m_state->getVertexAttribParameter<GLint>(index, pname, params)) {
        ctx->m_glGetVertexAttribiv_enc(self, index, pname, params);
    }
}

void GL2Encoder::s_glGetVertexAttribfv(void *self, GLuint index, GLenum pname, GLfloat *params)
{
    GL2Encoder *ctx = (GL2Encoder *)self;
    assert(ctx->m_state);

    if (!ctx->m_state->getVertexAttribParameter<GLfloat>(index, pname, params)) {
        ctx->m_glGetVertexAttribfv_enc(self, index, pname, params);
    }
}

void GL2Encoder::s_glGetVertexAttribPointerv(void *self, GLuint index, GLenum pname, GLvoid **pointer)
{
    GL2Encoder *ctx = (GL2Encoder *)self;
    if (ctx->m_state == NULL) return;

    const GLClientState::VertexAttribState *va_state = ctx->m_state->getState(index);
    if (va_state != NULL) {
        *pointer = va_state->data;
    }
}


void GL2Encoder::sendVertexAttributes(GLint first, GLsizei count)
{
    assert(m_state);

    for (int i = 0; i < m_state->nLocations(); i++) {
        bool enableDirty;
        const GLClientState::VertexAttribState *state = m_state->getStateAndEnableDirty(i, &enableDirty);

        if (!state) {
            continue;
        }

        if (!enableDirty && !state->enabled) {
            continue;
        }


        if (state->enabled) {
            m_glEnableVertexAttribArray_enc(this, i);

            unsigned int datalen = state->elementSize * count;
            int stride = state->stride == 0 ? state->elementSize : state->stride;
            int firstIndex = stride * first;

            if (state->bufferObject == 0) {
                this->glVertexAttribPointerData(this, i, state->size, state->type, state->normalized, state->stride,
                                                (unsigned char *)state->data + firstIndex, datalen);
            } else {
                this->m_glBindBuffer_enc(this, GL_ARRAY_BUFFER, state->bufferObject);
                this->glVertexAttribPointerOffset(this, i, state->size, state->type, state->normalized, state->stride,
                                                  (GLuint) state->data + firstIndex);
                this->m_glBindBuffer_enc(this, GL_ARRAY_BUFFER, m_state->currentArrayVbo());
            }
        } else {
            this->m_glDisableVertexAttribArray_enc(this, i);
        }
    }
}

void GL2Encoder::s_glDrawArrays(void *self, GLenum mode, GLint first, GLsizei count)
{
    GL2Encoder *ctx = (GL2Encoder *)self;
    ctx->sendVertexAttributes(first, count);
    ctx->m_glDrawArrays_enc(ctx, mode, 0, count);
}


void GL2Encoder::s_glDrawElements(void *self, GLenum mode, GLsizei count, GLenum type, const void *indices)
{

    GL2Encoder *ctx = (GL2Encoder *)self;
    assert(ctx->m_state != NULL);
    SET_ERROR_IF(count<0, GL_INVALID_VALUE);

    bool has_immediate_arrays = false;
    bool has_indirect_arrays = false;
    int nLocations = ctx->m_state->nLocations();

    for (int i = 0; i < nLocations; i++) {
        const GLClientState::VertexAttribState *state = ctx->m_state->getState(i);
        if (state->enabled) {
            if (state->bufferObject != 0) {
                has_indirect_arrays = true;
            } else {
                has_immediate_arrays = true;
            }
        }
    }

    if (!has_immediate_arrays && !has_indirect_arrays) {
        LOGE("glDrawElements: no data bound to the command - ignoring\n");
        return;
    }

    bool adjustIndices = true;
    if (ctx->m_state->currentIndexVbo() != 0) {
        if (!has_immediate_arrays) {
            ctx->sendVertexAttributes(0, count);
            ctx->m_glBindBuffer_enc(self, GL_ELEMENT_ARRAY_BUFFER, ctx->m_state->currentIndexVbo());
            ctx->glDrawElementsOffset(ctx, mode, count, type, (GLuint)indices);
            adjustIndices = false;
        } else {
            BufferData * buf = ctx->m_shared->getBufferData(ctx->m_state->currentIndexVbo());
            ctx->m_glBindBuffer_enc(self, GL_ELEMENT_ARRAY_BUFFER, 0);
            indices = (void*)((GLintptr)buf->m_fixedBuffer.ptr() + (GLintptr)indices);
        }
    } 
    if (adjustIndices) {
        void *adjustedIndices = (void*)indices;
        int minIndex = 0, maxIndex = 0;

        switch(type) {
        case GL_BYTE:
        case GL_UNSIGNED_BYTE:
            GLUtils::minmax<unsigned char>((unsigned char *)indices, count, &minIndex, &maxIndex);
            if (minIndex != 0) {
                adjustedIndices =  ctx->m_fixedBuffer.alloc(glSizeof(type) * count);
                GLUtils::shiftIndices<unsigned char>((unsigned char *)indices,
                                                 (unsigned char *)adjustedIndices,
                                                 count, -minIndex);
            }
            break;
        case GL_SHORT:
        case GL_UNSIGNED_SHORT:
            GLUtils::minmax<unsigned short>((unsigned short *)indices, count, &minIndex, &maxIndex);
            if (minIndex != 0) {
                adjustedIndices = ctx->m_fixedBuffer.alloc(glSizeof(type) * count);
                GLUtils::shiftIndices<unsigned short>((unsigned short *)indices,
                                                  (unsigned short *)adjustedIndices,
                                                  count, -minIndex);
            }
            break;
        default:
            LOGE("unsupported index buffer type %d\n", type);
        }
        if (has_indirect_arrays || 1) {
            ctx->sendVertexAttributes(minIndex, maxIndex - minIndex + 1);
            ctx->glDrawElementsData(ctx, mode, count, type, adjustedIndices,
                                    count * glSizeof(type));
            // XXX - OPTIMIZATION (see the other else branch) should be implemented
            if(!has_indirect_arrays) {
                //LOGD("unoptimized drawelements !!!\n");
            }
        } else {
            // we are all direct arrays and immidate mode index array -
            // rebuild the arrays and the index array;
            LOGE("glDrawElements: direct index & direct buffer data - will be implemented in later versions;\n");
        }
    }
}


GLint * GL2Encoder::getCompressedTextureFormats()
{
    if (m_compressedTextureFormats == NULL) {
        this->glGetIntegerv(this, GL_NUM_COMPRESSED_TEXTURE_FORMATS,
                            &m_num_compressedTextureFormats);
        if (m_num_compressedTextureFormats > 0) {
            // get number of texture formats;
            m_compressedTextureFormats = new GLint[m_num_compressedTextureFormats];
            this->glGetCompressedTextureFormats(this, m_num_compressedTextureFormats, m_compressedTextureFormats);
        }
    }
    return m_compressedTextureFormats;
}

// Replace uses of samplerExternalOES with sampler2D, recording the names of
// modified shaders in data. Also remove
//   #extension GL_OES_EGL_image_external : require
// statements.
//
// This implementation assumes the input has already been pre-processed. If not,
// a few cases will be mishandled:
//
// 1. "mySampler" will be incorrectly recorded as being a samplerExternalOES in
//    the following code:
//      #if 1
//      uniform sampler2D mySampler;
//      #else
//      uniform samplerExternalOES mySampler;
//      #endif
//
// 2. Comments that look like sampler declarations will be incorrectly modified
//    and recorded:
//      // samplerExternalOES hahaFooledYou
//
// 3. However, GLSL ES does not have a concatentation operator, so things like
//    this (valid in C) are invalid and not a problem:
//      #define SAMPLER(TYPE, NAME) uniform sampler#TYPE NAME
//      SAMPLER(ExternalOES, mySampler);
//
static bool replaceSamplerExternalWith2D(char* const str, ShaderData* const data)
{
    static const char STR_HASH_EXTENSION[] = "#extension";
    static const char STR_GL_OES_EGL_IMAGE_EXTERNAL[] = "GL_OES_EGL_image_external";
    static const char STR_SAMPLER_EXTERNAL_OES[] = "samplerExternalOES";
    static const char STR_SAMPLER2D_SPACE[]      = "sampler2D         ";

    // -- overwrite all "#extension GL_OES_EGL_image_external : xxx" statements
    char* c = str;
    while ((c = strstr(c, STR_HASH_EXTENSION))) {
        char* start = c;
        c += sizeof(STR_HASH_EXTENSION)-1;
        while (isspace(*c) && *c != '\0') {
            c++;
        }
        if (strncmp(c, STR_GL_OES_EGL_IMAGE_EXTERNAL,
                sizeof(STR_GL_OES_EGL_IMAGE_EXTERNAL)-1) == 0)
        {
            // #extension statements are terminated by end of line
            c = start;
            while (*c != '\0' && *c != '\r' && *c != '\n') {
                *c++ = ' ';
            }
        }
    }

    // -- replace "samplerExternalOES" with "sampler2D" and record name
    c = str;
    while ((c = strstr(c, STR_SAMPLER_EXTERNAL_OES))) {
        // Make sure "samplerExternalOES" isn't a substring of a larger token
        if (c == str || !isspace(*(c-1))) {
            c++;
            continue;
        }
        char* sampler_start = c;
        c += sizeof(STR_SAMPLER_EXTERNAL_OES)-1;
        if (!isspace(*c) && *c != '\0') {
            continue;
        }

        // capture sampler name
        while (isspace(*c) && *c != '\0') {
            c++;
        }
        if (!isalpha(*c) && *c != '_') {
            // not an identifier
            return false;
        }
        char* name_start = c;
        do {
            c++;
        } while (isalnum(*c) || *c == '_');
        data->samplerExternalNames.push_back(
                android::String8(name_start, c - name_start));

        // memcpy instead of strcpy since we don't want the NUL terminator
        memcpy(sampler_start, STR_SAMPLER2D_SPACE, sizeof(STR_SAMPLER2D_SPACE)-1);
    }

    return true;
}

void GL2Encoder::s_glShaderSource(void *self, GLuint shader, GLsizei count, const GLchar **string, const GLint *length)
{
    GL2Encoder* ctx = (GL2Encoder*)self;
    ShaderData* shaderData = ctx->m_shared->getShaderData(shader);
    SET_ERROR_IF(!shaderData, GL_INVALID_VALUE);

    int len = glUtilsCalcShaderSourceLen((char**)string, (GLint*)length, count);
    char *str = new char[len + 1];
    glUtilsPackStrings(str, (char**)string, (GLint*)length, count);

    // TODO: pre-process str before calling replaceSamplerExternalWith2D().
    // Perhaps we can borrow Mesa's pre-processor?

    if (!replaceSamplerExternalWith2D(str, shaderData)) {
        delete str;
        ctx->setError(GL_OUT_OF_MEMORY);
        return;
    }

    ctx->glShaderString(ctx, shader, str, len + 1);
    delete str;
}

void GL2Encoder::s_glFinish(void *self)
{
    GL2Encoder *ctx = (GL2Encoder *)self;
    ctx->glFinishRoundTrip(self);
}

void GL2Encoder::s_glLinkProgram(void * self, GLuint program)
{
    GL2Encoder *ctx = (GL2Encoder *)self;
    ctx->m_glLinkProgram_enc(self, program);

    GLint linkStatus = 0;
    ctx->glGetProgramiv(self,program,GL_LINK_STATUS,&linkStatus);
    if (!linkStatus)
        return;

    //get number of active uniforms in the program
    GLint numUniforms=0;
    ctx->glGetProgramiv(self, program, GL_ACTIVE_UNIFORMS, &numUniforms);
    ctx->m_shared->initProgramData(program,numUniforms);

    //get the length of the longest uniform name
    GLint maxLength=0;
    ctx->glGetProgramiv(self, program, GL_ACTIVE_UNIFORM_MAX_LENGTH, &maxLength);

    GLint size;
    GLenum type;
    GLchar *name = new GLchar[maxLength+1];
    GLint location;
    //for each active uniform, get its size and starting location.
    for (GLint i=0 ; i<numUniforms ; ++i) 
    {
        ctx->glGetActiveUniform(self, program, i, maxLength, NULL, &size, &type, name);
        location = ctx->m_glGetUniformLocation_enc(self, program, name);
        ctx->m_shared->setProgramIndexInfo(program, i, location, size, type, name);
    }
    ctx->m_shared->setupLocationShiftWAR(program);

    delete[] name;
}

void GL2Encoder::s_glDeleteProgram(void *self, GLuint program)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    ctx->m_glDeleteProgram_enc(self, program);

    ctx->m_shared->deleteProgramData(program);
}

void GL2Encoder::s_glGetUniformiv(void *self, GLuint program, GLint location, GLint* params)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    SET_ERROR_IF(!ctx->m_shared->isProgram(program), GL_INVALID_VALUE);
    SET_ERROR_IF(!ctx->m_shared->isProgramInitialized(program), GL_INVALID_OPERATION);
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(program, location);
    SET_ERROR_IF(ctx->m_shared->getProgramUniformType(program,hostLoc)==0, GL_INVALID_OPERATION);
    ctx->m_glGetUniformiv_enc(self, program, hostLoc, params);
}
void GL2Encoder::s_glGetUniformfv(void *self, GLuint program, GLint location, GLfloat* params)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    SET_ERROR_IF(!ctx->m_shared->isProgram(program), GL_INVALID_VALUE);
    SET_ERROR_IF(!ctx->m_shared->isProgramInitialized(program), GL_INVALID_OPERATION);
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(program,location);
    SET_ERROR_IF(ctx->m_shared->getProgramUniformType(program,hostLoc)==0, GL_INVALID_OPERATION);
    ctx->m_glGetUniformfv_enc(self, program, hostLoc, params);
}

GLuint GL2Encoder::s_glCreateProgram(void * self)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLuint program = ctx->m_glCreateProgram_enc(self);
    if (program!=0)
        ctx->m_shared->addProgramData(program);
    return program;
}

GLuint GL2Encoder::s_glCreateShader(void *self, GLenum shaderType)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLuint shader = ctx->m_glCreateShader_enc(self, shaderType);
    if (shader != 0) {
        if (!ctx->m_shared->addShaderData(shader)) {
            ctx->m_glDeleteShader_enc(self, shader);
            return 0;
        }
    }
    return shader;
}

void GL2Encoder::s_glDeleteShader(void *self, GLenum shader)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    ctx->m_glDeleteShader_enc(self,shader);
    ctx->m_shared->unrefShaderData(shader);
}

void GL2Encoder::s_glAttachShader(void *self, GLuint program, GLuint shader)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    ctx->m_glAttachShader_enc(self, program, shader);
    ctx->m_shared->attachShader(program, shader);
}

void GL2Encoder::s_glDetachShader(void *self, GLuint program, GLuint shader)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    ctx->m_glDetachShader_enc(self, program, shader);
    ctx->m_shared->detachShader(program, shader);
}

int GL2Encoder::s_glGetUniformLocation(void *self, GLuint program, const GLchar *name)
{
    if (!name) return -1;

    GL2Encoder *ctx = (GL2Encoder*)self;

    // if we need the uniform location WAR
    // parse array index from the end of the name string
    int arrIndex = 0;
    bool needLocationWAR = ctx->m_shared->needUniformLocationWAR(program);
    if (needLocationWAR) {
        int namelen = strlen(name);
        if (name[namelen-1] == ']') {
            char *brace = strrchr(name,'[');
            if (!brace || sscanf(brace+1,"%d",&arrIndex) != 1) {
                return -1;
            }
        
        }
    }

    int hostLoc = ctx->m_glGetUniformLocation_enc(self, program, name);
    if (hostLoc >= 0 && needLocationWAR) {
        return ctx->m_shared->locationWARHostToApp(program, hostLoc, arrIndex);
    }
    return hostLoc;
}

bool GL2Encoder::updateHostTexture2DBinding(GLenum texUnit, GLenum newTarget)
{
    if (newTarget != GL_TEXTURE_2D && newTarget != GL_TEXTURE_EXTERNAL_OES)
        return false;

    m_state->setActiveTextureUnit(texUnit);

    GLenum oldTarget = m_state->getPriorityEnabledTarget(GL_TEXTURE_2D);
    if (newTarget != oldTarget) {
        if (newTarget == GL_TEXTURE_EXTERNAL_OES) {
            m_state->disableTextureTarget(GL_TEXTURE_2D);
            m_state->enableTextureTarget(GL_TEXTURE_EXTERNAL_OES);
        } else {
            m_state->disableTextureTarget(GL_TEXTURE_EXTERNAL_OES);
            m_state->enableTextureTarget(GL_TEXTURE_2D);
        }
        m_glActiveTexture_enc(this, texUnit);
        m_glBindTexture_enc(this, GL_TEXTURE_2D,
                m_state->getBoundTexture(newTarget));
        return true;
    }

    return false;
}

void GL2Encoder::s_glUseProgram(void *self, GLuint program)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLClientState* state = ctx->m_state;
    GLSharedGroupPtr shared = ctx->m_shared;

    ctx->m_glUseProgram_enc(self, program);
    ctx->m_state->setCurrentProgram(program);

    GLenum origActiveTexture = state->getActiveTextureUnit();
    GLenum hostActiveTexture = origActiveTexture;
    GLint samplerIdx = -1;
    GLint samplerVal;
    GLenum samplerTarget;
    while ((samplerIdx = shared->getNextSamplerUniform(program, samplerIdx, &samplerVal, &samplerTarget)) != -1) {
        if (samplerVal < 0 || samplerVal >= GLClientState::MAX_TEXTURE_UNITS)
            continue;
        if (ctx->updateHostTexture2DBinding(GL_TEXTURE0 + samplerVal,
                samplerTarget))
        {
            hostActiveTexture = GL_TEXTURE0 + samplerVal;
        }
    }
    state->setActiveTextureUnit(origActiveTexture);
    if (hostActiveTexture != origActiveTexture) {
        ctx->m_glActiveTexture_enc(self, origActiveTexture);
    }
}

void GL2Encoder::s_glUniform1f(void *self , GLint location, GLfloat x)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniform1f_enc(self, hostLoc, x);
}

void GL2Encoder::s_glUniform1fv(void *self , GLint location, GLsizei count, const GLfloat* v)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniform1fv_enc(self, hostLoc, count, v);
}

void GL2Encoder::s_glUniform1i(void *self , GLint location, GLint x)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLClientState* state = ctx->m_state;
    GLSharedGroupPtr shared = ctx->m_shared;

    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniform1i_enc(self, hostLoc, x);

    GLenum target;
    if (shared->setSamplerUniform(state->currentProgram(), location, x, &target)) {
        GLenum origActiveTexture = state->getActiveTextureUnit();
        if (ctx->updateHostTexture2DBinding(GL_TEXTURE0 + x, target)) {
            ctx->m_glActiveTexture_enc(self, origActiveTexture);
        }
        state->setActiveTextureUnit(origActiveTexture);
    }
}

void GL2Encoder::s_glUniform1iv(void *self , GLint location, GLsizei count, const GLint* v)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniform1iv_enc(self, hostLoc, count, v);
}

void GL2Encoder::s_glUniform2f(void *self , GLint location, GLfloat x, GLfloat y)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniform2f_enc(self, hostLoc, x, y);
}

void GL2Encoder::s_glUniform2fv(void *self , GLint location, GLsizei count, const GLfloat* v)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniform2fv_enc(self, hostLoc, count, v);
}

void GL2Encoder::s_glUniform2i(void *self , GLint location, GLint x, GLint y)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniform2i_enc(self, hostLoc, x, y);
}

void GL2Encoder::s_glUniform2iv(void *self , GLint location, GLsizei count, const GLint* v)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniform2iv_enc(self, hostLoc, count, v);
}

void GL2Encoder::s_glUniform3f(void *self , GLint location, GLfloat x, GLfloat y, GLfloat z)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniform3f_enc(self, hostLoc, x, y, z);
}

void GL2Encoder::s_glUniform3fv(void *self , GLint location, GLsizei count, const GLfloat* v)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniform3fv_enc(self, hostLoc, count, v);
}

void GL2Encoder::s_glUniform3i(void *self , GLint location, GLint x, GLint y, GLint z)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniform3i_enc(self, hostLoc, x, y, z);
}

void GL2Encoder::s_glUniform3iv(void *self , GLint location, GLsizei count, const GLint* v)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniform3iv_enc(self, hostLoc, count, v);
}

void GL2Encoder::s_glUniform4f(void *self , GLint location, GLfloat x, GLfloat y, GLfloat z, GLfloat w)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniform4f_enc(self, hostLoc, x, y, z, w);
}

void GL2Encoder::s_glUniform4fv(void *self , GLint location, GLsizei count, const GLfloat* v)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniform4fv_enc(self, hostLoc, count, v);
}

void GL2Encoder::s_glUniform4i(void *self , GLint location, GLint x, GLint y, GLint z, GLint w)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniform4i_enc(self, hostLoc, x, y, z, w);
}

void GL2Encoder::s_glUniform4iv(void *self , GLint location, GLsizei count, const GLint* v)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniform4iv_enc(self, hostLoc, count, v);
}

void GL2Encoder::s_glUniformMatrix2fv(void *self , GLint location, GLsizei count, GLboolean transpose, const GLfloat* value)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniformMatrix2fv_enc(self, hostLoc, count, transpose, value);
}

void GL2Encoder::s_glUniformMatrix3fv(void *self , GLint location, GLsizei count, GLboolean transpose, const GLfloat* value)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniformMatrix3fv_enc(self, hostLoc, count, transpose, value);
}

void GL2Encoder::s_glUniformMatrix4fv(void *self , GLint location, GLsizei count, GLboolean transpose, const GLfloat* value)
{
    GL2Encoder *ctx = (GL2Encoder*)self;
    GLint hostLoc = ctx->m_shared->locationWARAppToHost(ctx->m_state->currentProgram(),location);
    ctx->m_glUniformMatrix4fv_enc(self, hostLoc, count, transpose, value);
}

void GL2Encoder::s_glActiveTexture(void* self, GLenum texture)
{
    GL2Encoder* ctx = (GL2Encoder*)self;
    GLClientState* state = ctx->m_state;
    GLenum err;

    SET_ERROR_IF((err = state->setActiveTextureUnit(texture)) != GL_NO_ERROR, err);

    ctx->m_glActiveTexture_enc(ctx, texture);
}

void GL2Encoder::s_glBindTexture(void* self, GLenum target, GLuint texture)
{
    GL2Encoder* ctx = (GL2Encoder*)self;
    GLClientState* state = ctx->m_state;
    GLenum err;
    GLboolean firstUse;

    SET_ERROR_IF((err = state->bindTexture(target, texture, &firstUse)) != GL_NO_ERROR, err);

    if (target != GL_TEXTURE_2D && target != GL_TEXTURE_EXTERNAL_OES) {
        ctx->m_glBindTexture_enc(ctx, target, texture);
        return;
    }

    GLenum priorityTarget = state->getPriorityEnabledTarget(GL_TEXTURE_2D);

    if (target == GL_TEXTURE_EXTERNAL_OES && firstUse) {
        ctx->m_glBindTexture_enc(ctx, GL_TEXTURE_2D, texture);
        ctx->m_glTexParameteri_enc(ctx, GL_TEXTURE_2D,
                GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        ctx->m_glTexParameteri_enc(ctx, GL_TEXTURE_2D,
                GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        ctx->m_glTexParameteri_enc(ctx, GL_TEXTURE_2D,
                GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        if (target != priorityTarget) {
            ctx->m_glBindTexture_enc(ctx, GL_TEXTURE_2D,
                    state->getBoundTexture(GL_TEXTURE_2D));
        }
    }

    if (target == priorityTarget) {
        ctx->m_glBindTexture_enc(ctx, GL_TEXTURE_2D, texture);
    }
}

void GL2Encoder::s_glDeleteTextures(void* self, GLsizei n, const GLuint* textures)
{
    GL2Encoder* ctx = (GL2Encoder*)self;
    GLClientState* state = ctx->m_state;

    state->deleteTextures(n, textures);
    ctx->m_glDeleteTextures_enc(ctx, n, textures);
}

void GL2Encoder::s_glGetTexParameterfv(void* self,
        GLenum target, GLenum pname, GLfloat* params)
{
    GL2Encoder* ctx = (GL2Encoder*)self;
    const GLClientState* state = ctx->m_state;

    if (target == GL_TEXTURE_2D || target == GL_TEXTURE_EXTERNAL_OES) {
        ctx->override2DTextureTarget(target);
        ctx->m_glGetTexParameterfv_enc(ctx, GL_TEXTURE_2D, pname, params);
        ctx->restore2DTextureTarget();
    } else {
        ctx->m_glGetTexParameterfv_enc(ctx, target, pname, params);
    }
}

void GL2Encoder::s_glGetTexParameteriv(void* self,
        GLenum target, GLenum pname, GLint* params)
{
    GL2Encoder* ctx = (GL2Encoder*)self;
    const GLClientState* state = ctx->m_state;

    switch (pname) {
    case GL_REQUIRED_TEXTURE_IMAGE_UNITS_OES:
        *params = 1;
        break;

    default:
        if (target == GL_TEXTURE_2D || target == GL_TEXTURE_EXTERNAL_OES) {
            ctx->override2DTextureTarget(target);
            ctx->m_glGetTexParameteriv_enc(ctx, GL_TEXTURE_2D, pname, params);
            ctx->restore2DTextureTarget();
        } else {
            ctx->m_glGetTexParameteriv_enc(ctx, target, pname, params);
        }
        break;
    }
}

static bool isValidTextureExternalParam(GLenum pname, GLenum param)
{
    switch (pname) {
    case GL_TEXTURE_MIN_FILTER:
    case GL_TEXTURE_MAG_FILTER:
        return param == GL_NEAREST || param == GL_LINEAR;

    case GL_TEXTURE_WRAP_S:
    case GL_TEXTURE_WRAP_T:
        return param == GL_CLAMP_TO_EDGE;

    default:
        return true;
    }
}

void GL2Encoder::s_glTexParameterf(void* self,
        GLenum target, GLenum pname, GLfloat param)
{
    GL2Encoder* ctx = (GL2Encoder*)self;
    const GLClientState* state = ctx->m_state;

    SET_ERROR_IF((target == GL_TEXTURE_EXTERNAL_OES &&
            !isValidTextureExternalParam(pname, (GLenum)param)),
            GL_INVALID_ENUM);

    if (target == GL_TEXTURE_2D || target == GL_TEXTURE_EXTERNAL_OES) {
        ctx->override2DTextureTarget(target);
        ctx->m_glTexParameterf_enc(ctx, GL_TEXTURE_2D, pname, param);
        ctx->restore2DTextureTarget();
    } else {
        ctx->m_glTexParameterf_enc(ctx, target, pname, param);
    }
}

void GL2Encoder::s_glTexParameterfv(void* self,
        GLenum target, GLenum pname, const GLfloat* params)
{
    GL2Encoder* ctx = (GL2Encoder*)self;
    const GLClientState* state = ctx->m_state;

    SET_ERROR_IF((target == GL_TEXTURE_EXTERNAL_OES &&
            !isValidTextureExternalParam(pname, (GLenum)params[0])),
            GL_INVALID_ENUM);

    if (target == GL_TEXTURE_2D || target == GL_TEXTURE_EXTERNAL_OES) {
        ctx->override2DTextureTarget(target);
        ctx->m_glTexParameterfv_enc(ctx, GL_TEXTURE_2D, pname, params);
        ctx->restore2DTextureTarget();
    } else {
        ctx->m_glTexParameterfv_enc(ctx, target, pname, params);
    }
}

void GL2Encoder::s_glTexParameteri(void* self,
        GLenum target, GLenum pname, GLint param)
{
    GL2Encoder* ctx = (GL2Encoder*)self;
    const GLClientState* state = ctx->m_state;

    SET_ERROR_IF((target == GL_TEXTURE_EXTERNAL_OES &&
            !isValidTextureExternalParam(pname, (GLenum)param)),
            GL_INVALID_ENUM);

    if (target == GL_TEXTURE_2D || target == GL_TEXTURE_EXTERNAL_OES) {
        ctx->override2DTextureTarget(target);
        ctx->m_glTexParameteri_enc(ctx, GL_TEXTURE_2D, pname, param);
        ctx->restore2DTextureTarget();
    } else {
        ctx->m_glTexParameteri_enc(ctx, target, pname, param);
    }
}

void GL2Encoder::s_glTexParameteriv(void* self,
        GLenum target, GLenum pname, const GLint* params)
{
    GL2Encoder* ctx = (GL2Encoder*)self;
    const GLClientState* state = ctx->m_state;

    SET_ERROR_IF((target == GL_TEXTURE_EXTERNAL_OES &&
            !isValidTextureExternalParam(pname, (GLenum)params[0])),
            GL_INVALID_ENUM);

    if (target == GL_TEXTURE_2D || target == GL_TEXTURE_EXTERNAL_OES) {
        ctx->override2DTextureTarget(target);
        ctx->m_glTexParameteriv_enc(ctx, GL_TEXTURE_2D, pname, params);
        ctx->restore2DTextureTarget();
    } else {
        ctx->m_glTexParameteriv_enc(ctx, target, pname, params);
    }
}

void GL2Encoder::override2DTextureTarget(GLenum target)
{
    if ((target == GL_TEXTURE_2D || target == GL_TEXTURE_EXTERNAL_OES) &&
        target != m_state->getPriorityEnabledTarget(GL_TEXTURE_2D)) {
            m_glBindTexture_enc(this, GL_TEXTURE_2D,
                    m_state->getBoundTexture(target));
    }
}

void GL2Encoder::restore2DTextureTarget()
{
    GLenum priorityTarget = m_state->getPriorityEnabledTarget(GL_TEXTURE_2D);
    m_glBindTexture_enc(this, GL_TEXTURE_2D,
            m_state->getBoundTexture(priorityTarget));
}
