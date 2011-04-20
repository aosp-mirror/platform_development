#include "GL2Encoder.h"
#include <assert.h>


static GLubyte *gVendorString= (GLubyte *) "Android";
static GLubyte *gRendererString= (GLubyte *) "Android HW-GLES 2.0";
static GLubyte *gVersionString= (GLubyte *) "OpenGL ES 2.0";
static GLubyte *gExtensionsString= (GLubyte *) ""; // no extensions at this point;

GL2Encoder::GL2Encoder(IOStream *stream) : gl2_encoder_context_t(stream)
{
    m_state = NULL;
    m_glFlush_enc = set_glFlush(s_glFlush);
    m_glPixelStorei_enc = set_glPixelStorei(s_glPixelStorei);
    m_glGetString_enc = set_glGetString(s_glGetString);
    m_glBindBuffer_enc = set_glBindBuffer(s_glBindBuffer);
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
}

GL2Encoder::~GL2Encoder()
{
    delete m_compressedTextureFormats;
}

void GL2Encoder::s_glFlush(void *self)
{
    GL2Encoder *ctx = (GL2Encoder *) self;
    ctx->m_glFlush_enc(self);
    ctx->m_stream->flush();
}

GLubyte *GL2Encoder::s_glGetString(void *self, GLenum name)
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

void GL2Encoder::s_glVertexAtrribPointer(void *self, GLuint indx, GLint size, GLenum type, GLboolean normalized, GLsizei stride, GLvoid * ptr)
{
    GL2Encoder *ctx = (GL2Encoder *)self;
    assert(ctx->m_state != NULL);
    ctx->m_state->setState(indx, size, type, normalized, stride, ptr);
}

void GL2Encoder::s_glGetIntegerv(void *self, GLenum param, GLint *params)
{
    GL2Encoder *ctx = (GL2Encoder *) self;
    assert(ctx->m_state != NULL);
    if (param == GL_NUM_SHADER_BINARY_FORMATS) {
        *params = 0;
    } else if (param == GL_SHADER_BINARY_FORMATS) {
        // do nothing
    } else  if (param == GL_COMPRESSED_TEXTURE_FORMATS) {
        GLint *compressedTextureFormats = ctx->getCompressedTextureFormats();
        if (ctx->m_num_compressedTextureFormats > 0 && compressedTextureFormats != NULL) {
            memcpy(params, compressedTextureFormats, ctx->m_num_compressedTextureFormats * sizeof(GLint));
        }
    } else if (!ctx->m_state->getClientStateParameter<GLint>(param, params)) {
        ctx->m_glGetIntegerv_enc(self, param, params);
    }
}


void GL2Encoder::s_glGetFloatv(void *self, GLenum param, GLfloat *ptr)
{
    GL2Encoder *ctx = (GL2Encoder *)self;
    assert(ctx->m_state != NULL);
    if (param == GL_NUM_SHADER_BINARY_FORMATS) {
        *ptr = 0;
    } else if (param == GL_SHADER_BINARY_FORMATS) {
        // do nothing;
    } else  if (param == GL_COMPRESSED_TEXTURE_FORMATS) {
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


void GL2Encoder::s_glGetBooleanv(void *self, GLenum param, GLboolean *ptr)
{
    GL2Encoder *ctx = (GL2Encoder *)self;
    assert(ctx->m_state != NULL);
    if (param == GL_COMPRESSED_TEXTURE_FORMATS) {
        // ignore the command, although we should have generated a GLerror;
    }
    else if (!ctx->m_state->getClientStateParameter<GLboolean>(param,ptr)) {
        ctx->m_glGetBooleanv_enc(self, param, ptr);
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
                this->glBindBuffer(this, GL_ARRAY_BUFFER, state->bufferObject);
                this->glVertexAttribPointerOffset(this, i, state->size, state->type, state->normalized, state->stride,
                                                  (GLuint) state->data + firstIndex);
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


void GL2Encoder::s_glDrawElements(void *self, GLenum mode, GLsizei count, GLenum type, void *indices)
{

    GL2Encoder *ctx = (GL2Encoder *)self;
    assert(ctx->m_state != NULL);

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

    if (ctx->m_state->currentIndexVbo() != 0) {
        if (!has_immediate_arrays) {
            ctx->sendVertexAttributes(0, count);
            ctx->glDrawElementsOffset(ctx, mode, count, type, (GLuint)indices);
        } else {
            LOGE("glDrawElements: indirect index arrays, with immidate-mode data array is not supported\n");
        }
    } else {
        void *adjustedIndices = indices;
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
                LOGD("unoptimized drawelements !!!\n");
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
