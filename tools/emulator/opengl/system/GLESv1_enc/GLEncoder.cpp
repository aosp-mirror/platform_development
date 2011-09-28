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
#include "GLEncoder.h"
#include "glUtils.h"
#include "FixedBuffer.h"
#include <cutils/log.h>
#include <assert.h>

static GLubyte *gVendorString= (GLubyte *) "Android";
static GLubyte *gRendererString= (GLubyte *) "Android HW-GLES 1.0";
static GLubyte *gVersionString= (GLubyte *) "OpenGL ES-CM 1.0";
static GLubyte *gExtensionsString= (GLubyte *) ""; // no extensions at this point;

#define DEFINE_AND_VALIDATE_HOST_CONNECTION(ret) \
    HostConnection *hostCon = HostConnection::get(); \
    if (!hostCon) { \
        LOGE("egl: Failed to get host connection\n"); \
        return ret; \
    } \
    renderControl_encoder_context_t *rcEnc = hostCon->rcEncoder(); \
    if (!rcEnc) { \
        LOGE("egl: Failed to get renderControl encoder context\n"); \
        return ret; \
    }

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

GLenum GLEncoder::s_glGetError(void * self)
{
    GLEncoder *ctx = (GLEncoder *)self;
    GLenum err = ctx->getError();
    if(err != GL_NO_ERROR) {
        ctx->setError(GL_NO_ERROR);
        return err;
    }

    return ctx->m_glGetError_enc(self);

}

GLint * GLEncoder::getCompressedTextureFormats()
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

void GLEncoder::s_glGetIntegerv(void *self, GLenum param, GLint *ptr)
{
    GLEncoder *ctx = (GLEncoder *)self;
    assert(ctx->m_state != NULL);
    if (param == GL_COMPRESSED_TEXTURE_FORMATS) {
        GLint * compressedTextureFormats = ctx->getCompressedTextureFormats();
        if (ctx->m_num_compressedTextureFormats > 0 && compressedTextureFormats != NULL) {
            memcpy(ptr, compressedTextureFormats, ctx->m_num_compressedTextureFormats * sizeof(GLint));
        }
    }
    else if (!ctx->m_state->getClientStateParameter<GLint>(param,ptr)) {
        ctx->m_glGetIntegerv_enc(self, param, ptr);
    }
}

void GLEncoder::s_glGetFloatv(void *self, GLenum param, GLfloat *ptr)
{
    GLEncoder *ctx = (GLEncoder *)self;
    assert(ctx->m_state != NULL);
    if (param == GL_COMPRESSED_TEXTURE_FORMATS) {
        GLint * compressedTextureFormats = ctx->getCompressedTextureFormats();
        if (ctx->m_num_compressedTextureFormats > 0 && compressedTextureFormats != NULL) {
            for (int i = 0; i < ctx->m_num_compressedTextureFormats; i++) {
                ptr[i] = (GLfloat) compressedTextureFormats[i];
            }
        }
    }
    else if (!ctx->m_state->getClientStateParameter<GLfloat>(param,ptr)) {
        ctx->m_glGetFloatv_enc(self, param, ptr);
    }
}

void GLEncoder::s_glGetFixedv(void *self, GLenum param, GLfixed *ptr)
{
    GLEncoder *ctx = (GLEncoder *)self;
    assert(ctx->m_state != NULL);
    if (param == GL_COMPRESSED_TEXTURE_FORMATS) {
        GLint * compressedTextureFormats = ctx->getCompressedTextureFormats();
        if (ctx->m_num_compressedTextureFormats > 0 && compressedTextureFormats != NULL) {
            for (int i = 0; i < ctx->m_num_compressedTextureFormats; i++) {
                ptr[i] =  compressedTextureFormats[i] << 16;
            }
        }
    }
    else if (!ctx->m_state->getClientStateParameter<GLfixed>(param,ptr)) {
        ctx->m_glGetFixedv_enc(self, param, ptr);
    }
}

void GLEncoder::s_glGetBooleanv(void *self, GLenum param, GLboolean *ptr)
{
    GLEncoder *ctx = (GLEncoder *)self;
    assert(ctx->m_state != NULL);
    if (param == GL_COMPRESSED_TEXTURE_FORMATS) {
        // ignore the command, although we should have generated a GLerror;
    }
    else if (!ctx->m_state->getClientStateParameter<GLboolean>(param,ptr)) {
        ctx->m_glGetBooleanv_enc(self, param, ptr);
    }
}

void GLEncoder::s_glGetPointerv(void * self, GLenum param, GLvoid **params)
{
    GLEncoder * ctx = (GLEncoder *) self;
    assert(ctx->m_state != NULL);
    ctx->m_state->getClientStatePointer(param,params);
}

void GLEncoder::s_glFlush(void *self)
{
    GLEncoder *ctx = (GLEncoder *)self;
    ctx->m_glFlush_enc(self);
    ctx->m_stream->flush();
}

const GLubyte *GLEncoder::s_glGetString(void *self, GLenum name)
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

void GLEncoder::s_glPixelStorei(void *self, GLenum param, GLint value)
{
    GLEncoder *ctx = (GLEncoder *)self;
    ctx->m_glPixelStorei_enc(ctx, param, value);
    LOG_ASSERT(ctx->m_state, "GLEncoder::s_glPixelStorei");
    ctx->m_state->setPixelStore(param, value);
}

void GLEncoder::s_glVertexPointer(void *self, int size, GLenum type, GLsizei stride, const void *data)
{
    GLEncoder *ctx = (GLEncoder *)self;
    assert(ctx->m_state != NULL);
    ctx->m_state->setState(GLClientState::VERTEX_LOCATION, size, type, false, stride, data);
}

void GLEncoder::s_glNormalPointer(void *self, GLenum type, GLsizei stride, const void *data)
{
    GLEncoder *ctx = (GLEncoder *)self;
    assert(ctx->m_state != NULL);
    ctx->m_state->setState(GLClientState::NORMAL_LOCATION, 3, type, false, stride, data);
}

void GLEncoder::s_glColorPointer(void *self, int size, GLenum type, GLsizei stride, const void *data)
{
    GLEncoder *ctx = (GLEncoder *)self;
    assert(ctx->m_state != NULL);
    ctx->m_state->setState(GLClientState::COLOR_LOCATION, size, type, false, stride, data);
}

void GLEncoder::s_glPointsizePointer(void *self, GLenum type, GLsizei stride, const void *data)
{
    GLEncoder *ctx = (GLEncoder *)self;
    assert(ctx->m_state != NULL);
    ctx->m_state->setState(GLClientState::POINTSIZE_LOCATION, 1, type, false, stride, data);
}

void GLEncoder::s_glClientActiveTexture(void *self, GLenum texture)
{
    GLEncoder *ctx = (GLEncoder *)self;
    assert(ctx->m_state != NULL);
    ctx->m_state->setActiveTexture(texture - GL_TEXTURE0);
}

void GLEncoder::s_glTexcoordPointer(void *self, int size, GLenum type, GLsizei stride, const void *data)
{
    GLEncoder *ctx = (GLEncoder *)self;
    assert(ctx->m_state != NULL);
    int loc = ctx->m_state->getLocation(GL_TEXTURE_COORD_ARRAY);
    ctx->m_state->setState(loc, size, type, false, stride, data);
}

void GLEncoder::s_glMatrixIndexPointerOES(void *self, int size, GLenum type, GLsizei stride, const void * data)
{
    GLEncoder *ctx = (GLEncoder *)self;
    assert(ctx->m_state != NULL);
    int loc = ctx->m_state->getLocation(GL_MATRIX_INDEX_ARRAY_OES);
    ctx->m_state->setState(loc, size, type, false, stride, data);
}

void GLEncoder::s_glWeightPointerOES(void * self, int size, GLenum type, GLsizei stride, const void * data)
{
    GLEncoder *ctx = (GLEncoder *)self;
    assert(ctx->m_state != NULL);
    int loc = ctx->m_state->getLocation(GL_WEIGHT_ARRAY_OES);
    ctx->m_state->setState(loc, size, type, false, stride, data);
}

void GLEncoder::s_glEnableClientState(void *self, GLenum state)
{
    GLEncoder *ctx = (GLEncoder *) self;
    assert(ctx->m_state != NULL);
    int loc = ctx->m_state->getLocation(state);
    ctx->m_state->enable(loc, 1);
}

void GLEncoder::s_glDisableClientState(void *self, GLenum state)
{
    GLEncoder *ctx = (GLEncoder *) self;
    assert(ctx->m_state != NULL);
    int loc = ctx->m_state->getLocation(state);
    ctx->m_state->enable(loc, 0);
}

GLboolean GLEncoder::s_glIsEnabled(void *self, GLenum cap)
{
    GLEncoder *ctx = (GLEncoder *) self;
    assert(ctx->m_state != NULL);
    int loc = ctx->m_state->getLocation(cap);
    const GLClientState::VertexAttribState *state = ctx->m_state->getState(loc);

    if (state!=NULL)
      return state->enabled;

    return ctx->m_glIsEnabled_enc(self,cap);
}

void GLEncoder::s_glBindBuffer(void *self, GLenum target, GLuint id)
{
    GLEncoder *ctx = (GLEncoder *) self;
    assert(ctx->m_state != NULL);
    ctx->m_state->bindBuffer(target, id);
    // TODO set error state if needed;
    ctx->m_glBindBuffer_enc(self, target, id);
}

void GLEncoder::s_glBufferData(void * self, GLenum target, GLsizeiptr size, const GLvoid * data, GLenum usage)
{
    GLEncoder *ctx = (GLEncoder *) self;
    GLuint bufferId = ctx->m_state->getBuffer(target);
    SET_ERROR_IF(bufferId==0, GL_INVALID_OPERATION);
    SET_ERROR_IF(size<0, GL_INVALID_VALUE);

    ctx->m_shared->updateBufferData(bufferId, size, (void*)data);
    ctx->m_glBufferData_enc(self, target, size, data, usage);
}

void GLEncoder::s_glBufferSubData(void * self, GLenum target, GLintptr offset, GLsizeiptr size, const GLvoid * data)
{
    GLEncoder *ctx = (GLEncoder *) self;
    GLuint bufferId = ctx->m_state->getBuffer(target);
    SET_ERROR_IF(bufferId==0, GL_INVALID_OPERATION);

    GLenum res = ctx->m_shared->subUpdateBufferData(bufferId, offset, size, (void*)data);
    SET_ERROR_IF(res, res);

    ctx->m_glBufferSubData_enc(self, target, offset, size, data);
}

void GLEncoder::s_glDeleteBuffers(void * self, GLsizei n, const GLuint * buffers)
{
    GLEncoder *ctx = (GLEncoder *) self;
    SET_ERROR_IF(n<0, GL_INVALID_VALUE);
    for (int i=0; i<n; i++) {
        ctx->m_shared->deleteBufferData(buffers[i]);
        ctx->m_glDeleteBuffers_enc(self,1,&buffers[i]);
    }
}

void GLEncoder::sendVertexData(unsigned int first, unsigned int count)
{
    assert(m_state != NULL);
    for (int i = 0; i < GLClientState::LAST_LOCATION; i++) {
        bool enableDirty;
        const GLClientState::VertexAttribState *state = m_state->getStateAndEnableDirty(i, &enableDirty);

        // do not process if state not valid
        if (!state) continue;

        // do not send disable state if state was already disabled
        if (!enableDirty && !state->enabled) continue;

        if ( i >= GLClientState::TEXCOORD0_LOCATION &&
            i <= GLClientState::TEXCOORD7_LOCATION ) {
            m_glClientActiveTexture_enc(this, GL_TEXTURE0 + i - GLClientState::TEXCOORD0_LOCATION);
        }

        if (state->enabled) {

            if (enableDirty)
                m_glEnableClientState_enc(this, state->glConst);

            unsigned int datalen = state->elementSize * count;
            int stride = state->stride;
            if (stride == 0) stride = state->elementSize;
            int firstIndex = stride * first;

            if (state->bufferObject == 0) {

                switch(i) {
                case GLClientState::VERTEX_LOCATION:
                    this->glVertexPointerData(this, state->size, state->type, state->stride,
                                              (unsigned char *)state->data + firstIndex, datalen);
                    break;
                case GLClientState::NORMAL_LOCATION:
                    this->glNormalPointerData(this, state->type, state->stride,
                                              (unsigned char *)state->data + firstIndex, datalen);
                    break;
                case GLClientState::COLOR_LOCATION:
                    this->glColorPointerData(this, state->size, state->type, state->stride,
                                             (unsigned char *)state->data + firstIndex, datalen);
                    break;
                case GLClientState::TEXCOORD0_LOCATION:
                case GLClientState::TEXCOORD1_LOCATION:
                case GLClientState::TEXCOORD2_LOCATION:
                case GLClientState::TEXCOORD3_LOCATION:
                case GLClientState::TEXCOORD4_LOCATION:
                case GLClientState::TEXCOORD5_LOCATION:
                case GLClientState::TEXCOORD6_LOCATION:
                case GLClientState::TEXCOORD7_LOCATION:
                    this->glTexCoordPointerData(this, i - GLClientState::TEXCOORD0_LOCATION, state->size, state->type, state->stride,
                                                (unsigned char *)state->data + firstIndex, datalen);
                    break;
                case GLClientState::POINTSIZE_LOCATION:
                    this->glPointSizePointerData(this, state->type, state->stride,
                                                 (unsigned char *) state->data + firstIndex, datalen);
                    break;
                case GLClientState::WEIGHT_LOCATION:
                    this->glWeightPointerData(this, state->size, state->type, state->stride,
                                              (unsigned char * ) state->data + firstIndex, datalen);
                    break;
                case GLClientState::MATRIXINDEX_LOCATION:
                    this->glMatrixIndexPointerData(this, state->size, state->type, state->stride,
                                                  (unsigned char *)state->data + firstIndex, datalen);
                    break;
                }
            } else {
                this->m_glBindBuffer_enc(this, GL_ARRAY_BUFFER, state->bufferObject);

                switch(i) {
                case GLClientState::VERTEX_LOCATION:
                    this->glVertexPointerOffset(this, state->size, state->type, state->stride,
                                                (GLuint)state->data + firstIndex);
                    break;
                case GLClientState::NORMAL_LOCATION:
                    this->glNormalPointerOffset(this, state->type, state->stride,
                                                (GLuint) state->data + firstIndex);
                    break;
                case GLClientState::POINTSIZE_LOCATION:
                    this->glPointSizePointerOffset(this, state->type, state->stride,
                                                   (GLuint) state->data + firstIndex);
                    break;
                case GLClientState::COLOR_LOCATION:
                    this->glColorPointerOffset(this, state->size, state->type, state->stride,
                                               (GLuint) state->data + firstIndex);
                    break;
                case GLClientState::TEXCOORD0_LOCATION:
                case GLClientState::TEXCOORD1_LOCATION:
                case GLClientState::TEXCOORD2_LOCATION:
                case GLClientState::TEXCOORD3_LOCATION:
                case GLClientState::TEXCOORD4_LOCATION:
                case GLClientState::TEXCOORD5_LOCATION:
                case GLClientState::TEXCOORD6_LOCATION:
                case GLClientState::TEXCOORD7_LOCATION:
                    this->glTexCoordPointerOffset(this, state->size, state->type, state->stride,
                                                  (GLuint) state->data + firstIndex);
                    break;
                case GLClientState::WEIGHT_LOCATION:
                    this->glWeightPointerOffset(this,state->size,state->type,state->stride,
                                                (GLuint)state->data+firstIndex);
                    break;
                case GLClientState::MATRIXINDEX_LOCATION:
                    this->glMatrixIndexPointerOffset(this,state->size,state->type,state->stride,
                                              (GLuint)state->data+firstIndex);
                    break;
                }                
                this->m_glBindBuffer_enc(this, GL_ARRAY_BUFFER, m_state->currentArrayVbo());
            }
        } else {
            this->m_glDisableClientState_enc(this, state->glConst);
        }
    }
}

void GLEncoder::s_glDrawArrays(void *self, GLenum mode, GLint first, GLsizei count)
{
    GLEncoder *ctx = (GLEncoder *)self;

    ctx->sendVertexData(first, count);
    ctx->m_glDrawArrays_enc(ctx, mode, /*first*/ 0, count);
}

void GLEncoder::s_glDrawElements(void *self, GLenum mode, GLsizei count, GLenum type, const void *indices)
{

    GLEncoder *ctx = (GLEncoder *)self;
    assert(ctx->m_state != NULL);
    SET_ERROR_IF(count<0, GL_INVALID_VALUE);

    bool has_immediate_arrays = false;
    bool has_indirect_arrays = false;

    for (int i = 0; i < GLClientState::LAST_LOCATION; i++) {
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
            ctx->sendVertexData(0, count);
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
            ctx->sendVertexData(minIndex, maxIndex - minIndex + 1);
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

GLEncoder::GLEncoder(IOStream *stream) : gl_encoder_context_t(stream)
{
    m_initialized = false;
    m_state = NULL;
    m_error = GL_NO_ERROR;
    m_num_compressedTextureFormats = 0;
    m_compressedTextureFormats = NULL;
    // overrides;
    m_glFlush_enc = set_glFlush(s_glFlush);
    m_glPixelStorei_enc = set_glPixelStorei(s_glPixelStorei);
    m_glVertexPointer_enc = set_glVertexPointer(s_glVertexPointer);
    m_glNormalPointer_enc = set_glNormalPointer(s_glNormalPointer);
    m_glColorPointer_enc = set_glColorPointer(s_glColorPointer);
    m_glPointSizePointerOES_enc = set_glPointSizePointerOES(s_glPointsizePointer);
    m_glClientActiveTexture_enc = set_glClientActiveTexture(s_glClientActiveTexture);
    m_glTexCoordPointer_enc = set_glTexCoordPointer(s_glTexcoordPointer);
    m_glMatrixIndexPointerOES_enc = set_glMatrixIndexPointerOES(s_glMatrixIndexPointerOES);
    m_glWeightPointerOES_enc = set_glWeightPointerOES(s_glWeightPointerOES);

    m_glGetIntegerv_enc = set_glGetIntegerv(s_glGetIntegerv);
    m_glGetFloatv_enc = set_glGetFloatv(s_glGetFloatv);
    m_glGetBooleanv_enc = set_glGetBooleanv(s_glGetBooleanv);
    m_glGetFixedv_enc = set_glGetFixedv(s_glGetFixedv);
    m_glGetPointerv_enc = set_glGetPointerv(s_glGetPointerv);

    m_glBindBuffer_enc = set_glBindBuffer(s_glBindBuffer);
    m_glBufferData_enc = set_glBufferData(s_glBufferData);
    m_glBufferSubData_enc = set_glBufferSubData(s_glBufferSubData);
    m_glDeleteBuffers_enc = set_glDeleteBuffers(s_glDeleteBuffers);

    m_glEnableClientState_enc = set_glEnableClientState(s_glEnableClientState);
    m_glDisableClientState_enc = set_glDisableClientState(s_glDisableClientState);
    m_glIsEnabled_enc = set_glIsEnabled(s_glIsEnabled);
    m_glDrawArrays_enc = set_glDrawArrays(s_glDrawArrays);
    m_glDrawElements_enc = set_glDrawElements(s_glDrawElements);
    set_glGetString(s_glGetString);
    set_glFinish(s_glFinish);
    m_glGetError_enc = set_glGetError(s_glGetError);

}

GLEncoder::~GLEncoder()
{
    delete [] m_compressedTextureFormats;
}

size_t GLEncoder::pixelDataSize(GLsizei width, GLsizei height, GLenum format, GLenum type, int pack)
{
    assert(m_state != NULL);
    return m_state->pixelDataSize(width, height, format, type, pack);
}

void GLEncoder::s_glFinish(void *self)
{
    GLEncoder *ctx = (GLEncoder *)self;
    ctx->glFinishRoundTrip(self);
}

