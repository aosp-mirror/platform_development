#ifndef _GL2_DECODER_H_
#define _GL2_DECODER_H_

#define GLES2_LIBNAME_VAR "ANDROID_GLESv2_LIB"
#define GLES2_LIBNAME     "libGLESv2.so"

#include "gl2_dec.h"
#include "OpenglOsUtils/osDynLibrary.h"
#include "GLDecoderContextData.h"


class GL2Decoder : public gl2_decoder_context_t
{
public:
    typedef void *(*get_proc_func_t)(const char *name, void *userData);
    GL2Decoder();
    ~GL2Decoder();
    int initGL(get_proc_func_t getProcFunc = NULL, void *getProcFuncData = NULL);
    void setContextData(GLDecoderContextData *contextData) { m_contextData = contextData; }
private:
    GLDecoderContextData *m_contextData;
    osUtils::dynLibrary * m_GL2library;

    static void *s_getProc(const char *name, void *userData);
    static void s_glGetCompressedTextureFormats(void *self, int count, GLint *formats);
    static void s_glVertexAttribPointerData(void *self, GLuint indx, GLint size, GLenum type,
                                      GLboolean normalized, GLsizei stride,  void * data, GLuint datalen);
    static void s_glVertexAttribPointerOffset(void *self, GLuint indx, GLint size, GLenum type,
                                        GLboolean normalized, GLsizei stride,  GLuint offset);

    static void s_glDrawElementsOffset(void *self, GLenum mode, GLsizei count, GLenum type, GLuint offset);
    static void s_glDrawElementsData(void *self, GLenum mode, GLsizei count, GLenum type, void * data, GLuint datalen);
    static void s_glShaderString(void *self, GLuint shader, GLstr string, GLsizei len);
};
#endif
