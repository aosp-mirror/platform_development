/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "WarpRenderer.h"

#include <GLES2/gl2ext.h>

const GLfloat g_vVertices[] = {
    -1.f, 1.f, 0.0f, 1.0f,  // Position 0
    0.0f,  1.0f,         // TexCoord 0
     1.f, 1.f, 0.0f, 1.0f, // Position 1
    1.0f,  1.0f,         // TexCoord 1
    -1.f, -1.f, 0.0f, 1.0f, // Position 2
    0.0f,  0.0f,         // TexCoord 2
    1.f,  -1.f, 0.0f, 1.0f, // Position 3
    1.0f,  0.0f          // TexCoord 3
};

const int VERTEX_STRIDE = 6 * sizeof(GLfloat);

GLushort g_iIndices[] = { 0, 1, 2, 3 };

WarpRenderer::WarpRenderer() : Renderer()
{
}

WarpRenderer::~WarpRenderer() {
}

void WarpRenderer::SetViewportMatrix(int w, int h, int W, int H)
{
    for(int i=0; i<16; i++)
    {
        mViewportMatrix[i] = 0.0f;
    }

    mViewportMatrix[0] = float(w)/float(W);
    mViewportMatrix[5] = float(h)/float(H);
    mViewportMatrix[10] = 1.0f;
    mViewportMatrix[12] = -1.0f + float(w)/float(W);
    mViewportMatrix[13] = -1.0f + float(h)/float(H);
    mViewportMatrix[15] = 1.0f;
}

void WarpRenderer::SetScalingMatrix(float xscale, float yscale)
{
    for(int i=0; i<16; i++)
    {
        mScalingMatrix[i] = 0.0f;
    }

    mScalingMatrix[0] = xscale;
    mScalingMatrix[5] = yscale;
    mScalingMatrix[10] = 1.0f;
    mScalingMatrix[15] = 1.0f;
}

bool WarpRenderer::InitializeGLProgram()
{
    bool succeeded = false;
    do {
        GLuint glProgram;
        glProgram = createProgram(VertexShaderSource(),
                FragmentShaderSource());
        if (!glProgram) {
            break;
        }

        glUseProgram(glProgram);
        if (!checkGlError("glUseProgram")) break;

        // Get attribute locations
        mPositionLoc     = glGetAttribLocation(glProgram, "a_position");
        mAffinetransLoc  = glGetUniformLocation(glProgram, "u_affinetrans");
        mViewporttransLoc = glGetUniformLocation(glProgram, "u_viewporttrans");
        mScalingtransLoc = glGetUniformLocation(glProgram, "u_scalingtrans");
        mTexCoordLoc     = glGetAttribLocation(glProgram, "a_texCoord");

        // Get sampler location
        mSamplerLoc      = glGetUniformLocation(glProgram, "s_texture");

        mGlProgram = glProgram;
        succeeded = true;
    } while (false);

    if (!succeeded && (mGlProgram != 0))
    {
        glDeleteProgram(mGlProgram);
        checkGlError("glDeleteProgram");
        mGlProgram = 0;
    }
    return succeeded;
}

bool WarpRenderer::DrawTexture(GLfloat *affine)
{
    bool succeeded = false;
    do {
        bool rt = (mFrameBuffer == NULL)?
                SetupGraphics(mSurfaceWidth, mSurfaceHeight) :
                SetupGraphics(mFrameBuffer);

        if(!rt)
            break;

        glDisable(GL_BLEND);

        glActiveTexture(GL_TEXTURE0);
        if (!checkGlError("glActiveTexture")) break;

        const GLenum texture_type = InputTextureType();
        glBindTexture(texture_type, mInputTextureName);
        if (!checkGlError("glBindTexture")) break;

        // Set the sampler texture unit to 0
        glUniform1i(mSamplerLoc, 0);

        // Load the vertex position
        glVertexAttribPointer(mPositionLoc, 4, GL_FLOAT,
                GL_FALSE, VERTEX_STRIDE, g_vVertices);

        // Load the texture coordinate
        glVertexAttribPointer(mTexCoordLoc, 2, GL_FLOAT,
                GL_FALSE, VERTEX_STRIDE, &g_vVertices[4]);

        glEnableVertexAttribArray(mPositionLoc);
        glEnableVertexAttribArray(mTexCoordLoc);

        // pass matrix information to the vertex shader
        glUniformMatrix4fv(mAffinetransLoc, 1, GL_FALSE, affine);
        glUniformMatrix4fv(mViewporttransLoc, 1, GL_FALSE, mViewportMatrix);
        glUniformMatrix4fv(mScalingtransLoc, 1, GL_FALSE, mScalingMatrix);

        // And, finally, execute the GL draw command.
        glDrawElements(GL_TRIANGLE_STRIP, 4, GL_UNSIGNED_SHORT, g_iIndices);

        checkGlError("glDrawElements");

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        succeeded = true;
    } while (false);
    return succeeded;
}

const char* WarpRenderer::VertexShaderSource() const
{
    static const char gVertexShader[] =
        "uniform mat4 u_affinetrans;  \n"
        "uniform mat4 u_viewporttrans;  \n"
        "uniform mat4 u_scalingtrans;  \n"
        "attribute vec4 a_position;   \n"
        "attribute vec2 a_texCoord;   \n"
        "varying vec2 v_texCoord;     \n"
        "void main()                  \n"
        "{                            \n"
        "   gl_Position = u_scalingtrans * u_viewporttrans * u_affinetrans * a_position; \n"
        "   v_texCoord = a_texCoord;  \n"
        "}                            \n";

    return gVertexShader;
}

const char* WarpRenderer::FragmentShaderSource() const
{
    static const char gFragmentShader[] =
        "precision mediump float;                            \n"
        "varying vec2 v_texCoord;                            \n"
        "uniform sampler2D s_texture;                        \n"
        "void main()                                         \n"
        "{                                                   \n"
        "  vec4 color;                                       \n"
        "  color = texture2D(s_texture, v_texCoord);       \n"
        "  gl_FragColor = color;                             \n"
        "}                                                   \n";

    return gFragmentShader;
}
