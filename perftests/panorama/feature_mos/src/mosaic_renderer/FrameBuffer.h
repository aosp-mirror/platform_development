#pragma once

#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#define checkGlError(op)  checkGLErrorDetail(__FILE__, __LINE__, (op))

extern bool checkGLErrorDetail(const char* file, int line, const char* op);
extern void checkFramebufferStatus(const char* name);

class FrameBuffer {
  public:
    FrameBuffer();
    virtual ~FrameBuffer();

    bool InitializeGLContext();
    bool Init(int width, int height, GLenum format);
    GLuint GetTextureName() const;
    GLuint GetFrameBufferName() const;
    GLenum GetFormat() const;

    int GetWidth() const;
    int GetHeight() const;

 private:
    void Reset();
    bool CreateBuffers();
    GLuint mFrameBufferName;
    GLuint mTextureName;
    int mWidth;
    int mHeight;
    GLenum mFormat;
};
