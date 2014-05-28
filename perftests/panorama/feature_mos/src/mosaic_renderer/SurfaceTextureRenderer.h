#pragma once

#include "FrameBuffer.h"
#include "Renderer.h"

#include <GLES2/gl2.h>

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

class SurfaceTextureRenderer: public Renderer {
  public:
    SurfaceTextureRenderer();
    virtual ~SurfaceTextureRenderer();

    // Initialize OpenGL resources
    // @return true if successful
    bool InitializeGLProgram();

    bool DrawTexture(GLfloat *affine);

    void SetViewportMatrix(int w, int h, int W, int H);
    void SetScalingMatrix(float xscale, float yscale);
    void SetSTMatrix(float *stmat);

 private:
    // Source code for shaders.
    const char* VertexShaderSource() const;
    const char* FragmentShaderSource() const;

    // Attribute locations
    GLint  mScalingtransLoc;
    GLint muSTMatrixHandle;
    GLint maPositionHandle;
    GLint maTextureHandle;

    GLfloat mViewportMatrix[16];
    GLfloat mScalingMatrix[16];

    GLfloat mSTMatrix[16];

};

