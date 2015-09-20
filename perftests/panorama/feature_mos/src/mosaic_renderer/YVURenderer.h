#pragma once

#include "FrameBuffer.h"
#include "Renderer.h"

#include <GLES2/gl2.h>

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

class YVURenderer: public Renderer {
  public:
    YVURenderer();
    virtual ~YVURenderer();

    // Initialize OpenGL resources
    // @return true if successful
    bool InitializeGLProgram();

    bool DrawTexture();

 private:
    // Source code for shaders.
    const char* VertexShaderSource() const;
    const char* FragmentShaderSource() const;

    // Attribute locations
    GLint  mPositionLoc;
    GLint  mTexCoordLoc;

    // Sampler location
    GLint mSamplerLoc;
};

