#include "FrameBuffer.h"

FrameBuffer::FrameBuffer()
{
    Reset();
}

FrameBuffer::~FrameBuffer() {
}

void FrameBuffer::Reset() {
    mFrameBufferName = -1;
    mTextureName = -1;
    mWidth = 0;
    mHeight = 0;
    mFormat = -1;
}

bool FrameBuffer::InitializeGLContext() {
    Reset();
    return CreateBuffers();
}

bool FrameBuffer::Init(int width, int height, GLenum format) {
    if (mFrameBufferName == (GLuint)-1) {
        if (!CreateBuffers()) {
            return false;
        }
    }
    glBindFramebuffer(GL_FRAMEBUFFER, mFrameBufferName);
    glBindTexture(GL_TEXTURE_2D, mTextureName);

    glTexImage2D(GL_TEXTURE_2D,
                 0,
                 format,
                 width,
                 height,
                 0,
                 format,
                 GL_UNSIGNED_BYTE,
                 NULL);
    if (!checkGlError("bind/teximage")) {
        return false;
    }
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    // This is necessary to work with user-generated frame buffers with
    // dimensions that are NOT powers of 2.
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    // Attach texture to frame buffer.
    glFramebufferTexture2D(GL_FRAMEBUFFER,
                           GL_COLOR_ATTACHMENT0,
                           GL_TEXTURE_2D,
                           mTextureName,
                           0);
    checkFramebufferStatus("FrameBuffer.cpp");
    checkGlError("framebuffertexture2d");

    if (!checkGlError("texture setup")) {
        return false;
    }
    mWidth = width;
    mHeight = height;
    mFormat = format;
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    return true;
}

bool FrameBuffer::CreateBuffers() {
    glGenFramebuffers(1, &mFrameBufferName);
    glGenTextures(1, &mTextureName);
    if (!checkGlError("texture generation")) {
        return false;
    }
    return true;
}

GLuint FrameBuffer::GetTextureName() const {
    return mTextureName;
}

GLuint FrameBuffer::GetFrameBufferName() const {
    return mFrameBufferName;
}

GLenum FrameBuffer::GetFormat() const {
    return mFormat;
}

int FrameBuffer::GetWidth() const {
    return mWidth;
}

int FrameBuffer::GetHeight() const {
    return mHeight;
}
