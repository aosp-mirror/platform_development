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

#include "YVURenderer.h"

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

GLushort g_iIndices3[] = { 0, 1, 2, 3 };

YVURenderer::YVURenderer() : Renderer()
                   {
}

YVURenderer::~YVURenderer() {
}

bool YVURenderer::InitializeGLProgram()
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
        mPositionLoc     = glGetAttribLocation(glProgram, "a_Position");
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

bool YVURenderer::DrawTexture()
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

        // And, finally, execute the GL draw command.
        glDrawElements(GL_TRIANGLE_STRIP, 4, GL_UNSIGNED_SHORT, g_iIndices3);

        checkGlError("glDrawElements");

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        succeeded = true;
    } while (false);
    return succeeded;
}

const char* YVURenderer::VertexShaderSource() const
{
    // All this really does is copy the coordinates into
    // variables for the fragment shader to pick up.
    static const char gVertexShader[] =
        "attribute vec4 a_Position;\n"
        "attribute vec2 a_texCoord;\n"
        "varying vec2 v_texCoord;\n"
        "void main() {\n"
        "  gl_Position = a_Position;\n"
        "  v_texCoord = a_texCoord;\n"
        "}\n";

    return gVertexShader;
}

const char* YVURenderer::FragmentShaderSource() const
{
    static const char gFragmentShader[] =
        "precision mediump float;\n"
        "uniform sampler2D s_texture;\n"
        "const vec4 coeff_y = vec4(0.257, 0.594, 0.098, 0.063);\n"
        "const vec4 coeff_v = vec4(0.439, -0.368, -0.071, 0.500);\n"
        "const vec4 coeff_u = vec4(-0.148, -0.291, 0.439, 0.500);\n"
        "varying vec2 v_texCoord;\n"
        "void main() {\n"
        "  vec4 p;\n"
        "  p = texture2D(s_texture, v_texCoord);\n"
        "  gl_FragColor[0] = dot(p, coeff_y);\n"
        "  p = texture2D(s_texture, v_texCoord);\n"
        "  gl_FragColor[1] = dot(p, coeff_v);\n"
        "  p = texture2D(s_texture, v_texCoord);\n"
        "  gl_FragColor[2] = dot(p, coeff_u);\n"
        "  p = texture2D(s_texture, v_texCoord);\n"
        "  gl_FragColor[3] = dot(p, coeff_y);\n"
        "}\n";

    return gFragmentShader;
}
