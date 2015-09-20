#pragma once

#include "FrameBuffer.h"
#include "Renderer.h"

#include <GLES2/gl2.h>

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

class WarpRenderer: public Renderer {
  public:
    WarpRenderer();
    virtual ~WarpRenderer();

    // Initialize OpenGL resources
    // @return true if successful
    bool InitializeGLProgram();

    void SetViewportMatrix(int w, int h, int W, int H);
    void SetScalingMatrix(float xscale, float yscale);

    bool DrawTexture(GLfloat *affine);

 private:
    // Source code for shaders.
    const char* VertexShaderSource() const;
    const char* FragmentShaderSource() const;

    GLuint mTexHandle;                  // Handle to s_texture.
    GLuint mTexCoordHandle;             // Handle to a_texCoord.
    GLuint mTriangleVerticesHandle;     // Handle to vPosition.

    // Attribute locations
    GLint  mPositionLoc;
    GLint  mAffinetransLoc;
    GLint  mViewporttransLoc;
    GLint  mScalingtransLoc;
    GLint  mTexCoordLoc;

    GLfloat mViewportMatrix[16];
    GLfloat mScalingMatrix[16];

    // Sampler location
    GLint mSamplerLoc;
};

