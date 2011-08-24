#include "GL2Decoder.h"
#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>


GL2Decoder::GL2Decoder()
{
    m_contextData = NULL;
    m_GL2library = NULL;
}

GL2Decoder::~GL2Decoder()
{
    delete m_GL2library;
}

void *GL2Decoder::s_getProc(const char *name, void *userData)
{
    GL2Decoder *ctx = (GL2Decoder *) userData;

    if (ctx == NULL || ctx->m_GL2library == NULL) {
        return NULL;
    }

    void *func = NULL;
#ifdef USE_EGL_GETPROCADDRESS
    func = (void *) eglGetProcAddress(name);
#endif
    if (func == NULL) {
        func = (void *) ctx->m_GL2library->findSymbol(name);
    }
    return func;
}

int GL2Decoder::initGL(get_proc_func_t getProcFunc, void *getProcFuncData)
{
    if (getProcFunc == NULL) {
        const char *libname = GLES2_LIBNAME;
        if (getenv(GLES2_LIBNAME_VAR) != NULL) {
            libname = getenv(GLES2_LIBNAME_VAR);
        }

        m_GL2library = osUtils::dynLibrary::open(libname);
        if (m_GL2library == NULL) {
            fprintf(stderr, "%s: Couldn't find %s \n", __FUNCTION__, libname);
            return -1;
        }
        this->initDispatchByName(s_getProc, this);
    } else {
        this->initDispatchByName(getProcFunc, getProcFuncData);
    }

    set_glGetCompressedTextureFormats(s_glGetCompressedTextureFormats);
    set_glVertexAttribPointerData(s_glVertexAttribPointerData);
    set_glVertexAttribPointerOffset(s_glVertexAttribPointerOffset);

    set_glDrawElementsOffset(s_glDrawElementsOffset);
    set_glDrawElementsData(s_glDrawElementsData);
    set_glShaderString(s_glShaderString);
    set_glFinishRoundTrip(s_glFinishRoundTrip);
    return 0;

}

int GL2Decoder::s_glFinishRoundTrip(void *self)
{
    GL2Decoder *ctx = (GL2Decoder *)self;
    ctx->glFinish();
    return 0;
}

void GL2Decoder::s_glGetCompressedTextureFormats(void *self, int count, GLint *formats)
{
    GL2Decoder *ctx = (GL2Decoder *) self;

    int nFormats;
    ctx->glGetIntegerv(GL_NUM_COMPRESSED_TEXTURE_FORMATS, &nFormats);
    if (nFormats > count) {
        fprintf(stderr, "%s: GetCompressedTextureFormats: The requested number of formats does not match the number that is reported by OpenGL\n", __FUNCTION__);
    } else {
        ctx->glGetIntegerv(GL_COMPRESSED_TEXTURE_FORMATS, formats);
    }
}

void GL2Decoder::s_glVertexAttribPointerData(void *self, GLuint indx, GLint size, GLenum type,
                                             GLboolean normalized, GLsizei stride,  void * data, GLuint datalen)
{
    GL2Decoder *ctx = (GL2Decoder *) self;
    if (ctx->m_contextData != NULL) {
        ctx->m_contextData->storePointerData(indx, data, datalen);
        // note - the stride of the data is always zero when it comes out of the codec.
        // See gl2.attrib for the packing function call.
        ctx->glVertexAttribPointer(indx, size, type, normalized, 0, ctx->m_contextData->pointerData(indx));
    }
}

void GL2Decoder::s_glVertexAttribPointerOffset(void *self, GLuint indx, GLint size, GLenum type,
                                               GLboolean normalized, GLsizei stride,  GLuint data)
{
    GL2Decoder *ctx = (GL2Decoder *) self;
    ctx->glVertexAttribPointer(indx, size, type, normalized, stride, (GLvoid *)data);
}


void GL2Decoder::s_glDrawElementsData(void *self, GLenum mode, GLsizei count, GLenum type, void * data, GLuint datalen)
{
    GL2Decoder *ctx = (GL2Decoder *)self;
    ctx->glDrawElements(mode, count, type, data);
}


void GL2Decoder::s_glDrawElementsOffset(void *self, GLenum mode, GLsizei count, GLenum type, GLuint offset)
{
    GL2Decoder *ctx = (GL2Decoder *)self;
    ctx->glDrawElements(mode, count, type, (void *)offset);
}

void GL2Decoder::s_glShaderString(void *self, GLuint shader, const GLchar* string, GLsizei len)
{
    GL2Decoder *ctx = (GL2Decoder *)self;
    ctx->glShaderSource(shader, 1, &string, NULL);
}
