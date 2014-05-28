#pragma once

#include "FrameBuffer.h"

#include <GLES2/gl2.h>

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

class Renderer {
  public:
    Renderer();
    virtual ~Renderer();

    // Initialize OpenGL resources
    // @return true if successful
    virtual bool InitializeGLProgram() = 0;

    bool SetupGraphics(FrameBuffer* buffer);
    bool SetupGraphics(int width, int height);

    bool Clear(float r, float g, float b, float a);

    int GetTextureName();
    void SetInputTextureName(GLuint textureName);
    void SetInputTextureDimensions(int width, int height);
    void SetInputTextureType(GLenum textureType);

    void InitializeGLContext();

  protected:

    GLuint loadShader(GLenum shaderType, const char* pSource);
    GLuint createProgram(const char*, const char* );

    int SurfaceWidth() const { return mSurfaceWidth; }
    int SurfaceHeight() const { return mSurfaceHeight; }

    // Source code for shaders.
    virtual const char* VertexShaderSource() const = 0;
    virtual const char* FragmentShaderSource() const = 0;

    // Redefine this to use special texture types such as
    // GL_TEXTURE_EXTERNAL_OES.
    GLenum InputTextureType() const { return mInputTextureType; }

    GLuint mGlProgram;
    GLuint mInputTextureName;
    GLenum mInputTextureType;
    int mInputTextureWidth;
    int mInputTextureHeight;

    // Attribute locations
    GLint  mScalingtransLoc;
    GLint maPositionHandle;
    GLint maTextureHandle;


    int mSurfaceWidth;      // Width of target surface.
    int mSurfaceHeight;     // Height of target surface.

    FrameBuffer *mFrameBuffer;
};

