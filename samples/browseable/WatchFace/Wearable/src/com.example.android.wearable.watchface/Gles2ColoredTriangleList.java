/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.wearable.watchface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.Log;

/**
 * A list of triangles drawn in a single solid color using OpenGL ES 2.0.
 */
public class Gles2ColoredTriangleList {
    private static final String TAG = "GlColoredTriangleList";

    /** Whether to check for GL errors. This is slow, so not appropriate for production builds. */
    private static final boolean CHECK_GL_ERRORS = false;

    /** Number of coordinates per vertex in this array: one for each of x, y, and z. */
    private static final int COORDS_PER_VERTEX = 3;

    /** Number of bytes to store a float in GL. */
    public static final int BYTES_PER_FLOAT = 4;

    /** Number of bytes per vertex. */
    private static final int VERTEX_STRIDE = COORDS_PER_VERTEX * BYTES_PER_FLOAT;

    /** Triangles have three vertices. */
    private static final int VERTICE_PER_TRIANGLE = 3;

    /**
     * Number of components in an OpenGL color. The components are:<ol>
     * <li>red
     * <li>green
     * <li>blue
     * <li>alpha
     * </ol>
     */
    private static final int NUM_COLOR_COMPONENTS = 4;

    /** Shaders to render this triangle list. */
    private final Program mProgram;

    /** The VBO containing the vertex coordinates. */
    private final FloatBuffer mVertexBuffer;

    /**
     * Color of this triangle list represented as an array of floats in the range [0, 1] in RGBA
     * order.
     */
    private final float mColor[];

    /** Number of coordinates in this triangle list. */
    private final int mNumCoords;

    /**
     * Creates a Gles2ColoredTriangleList to draw a triangle list with the given vertices and color.
     *
     * @param program program for drawing triangles
     * @param triangleCoords flat array of 3D coordinates of triangle vertices in counterclockwise
     *                       order
     * @param color color in RGBA order, each in the range [0, 1]
     */
    public Gles2ColoredTriangleList(Program program, float[] triangleCoords, float[] color) {
        if (triangleCoords.length % (VERTICE_PER_TRIANGLE * COORDS_PER_VERTEX) != 0) {
            throw new IllegalArgumentException("must be multiple"
                    + " of VERTICE_PER_TRIANGLE * COORDS_PER_VERTEX coordinates");
        }
        if (color.length != NUM_COLOR_COMPONENTS) {
            throw new IllegalArgumentException("wrong number of color components");
        }
        mProgram = program;
        mColor = color;

        ByteBuffer bb = ByteBuffer.allocateDirect(triangleCoords.length * BYTES_PER_FLOAT);

        // Use the device hardware's native byte order.
        bb.order(ByteOrder.nativeOrder());

        // Create a FloatBuffer that wraps the ByteBuffer.
        mVertexBuffer = bb.asFloatBuffer();

        // Add the coordinates to the FloatBuffer.
        mVertexBuffer.put(triangleCoords);

        // Go back to the start for reading.
        mVertexBuffer.position(0);

        mNumCoords = triangleCoords.length / COORDS_PER_VERTEX;
    }

    /**
     * Draws this triangle list using OpenGL commands.
     *
     * @param mvpMatrix the Model View Project matrix to draw this triangle list
     */
    public void draw(float[] mvpMatrix) {
        // Pass the MVP matrix, vertex data, and color to OpenGL.
        mProgram.bind(mvpMatrix, mVertexBuffer, mColor);

        // Draw the triangle list.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mNumCoords);
        if (CHECK_GL_ERRORS) checkGlError("glDrawArrays");
    }

    /**
     * Checks if any of the GL calls since the last time this method was called set an error
     * condition. Call this method immediately after calling a GL method. Pass the name of the GL
     * operation. For example:
     *
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "uColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an exception.
     *
     * <p><em>Note</em> This is quite slow so it's best to use it sparingly in production builds.
     *
     * @param glOperation name of the OpenGL call to check
     */
    private static void checkGlError(String glOperation) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String errorString = GLU.gluErrorString(error);
            if (errorString == null) {
                errorString = GLUtils.getEGLErrorString(error);
            }
            String message = glOperation + " caused GL error 0x" + Integer.toHexString(error) +
                    ": " + errorString;
            Log.e(TAG, message);
            throw new RuntimeException(message);
        }
    }

    /**
     * Compiles an OpenGL shader.
     *
     * @param type {@link GLES20#GL_VERTEX_SHADER} or {@link GLES20#GL_FRAGMENT_SHADER}
     * @param shaderCode string containing the shader source code
     * @return ID for the shader
     */
    private static int loadShader(int type, String shaderCode){
        // Create a vertex or fragment shader.
        int shader = GLES20.glCreateShader(type);
        if (CHECK_GL_ERRORS) checkGlError("glCreateShader");
        if (shader == 0) {
            throw new IllegalStateException("glCreateShader failed");
        }

        // Add the source code to the shader and compile it.
        GLES20.glShaderSource(shader, shaderCode);
        if (CHECK_GL_ERRORS) checkGlError("glShaderSource");
        GLES20.glCompileShader(shader);
        if (CHECK_GL_ERRORS) checkGlError("glCompileShader");

        return shader;
    }

    /** OpenGL shaders for drawing solid colored triangle lists. */
    public static class Program {
        /** Trivial vertex shader that transforms the input vertex by the MVP matrix. */
        private static final String VERTEX_SHADER_CODE = "" +
                "uniform mat4 uMvpMatrix;\n" +
                "attribute vec4 aPosition;\n" +
                "void main() {\n" +
                "    gl_Position = uMvpMatrix * aPosition;\n" +
                "}\n";

        /** Trivial fragment shader that draws with a fixed color. */
        private static final String FRAGMENT_SHADER_CODE = "" +
                "precision mediump float;\n" +
                "uniform vec4 uColor;\n" +
                "void main() {\n" +
                "    gl_FragColor = uColor;\n" +
                "}\n";

        /** ID OpenGL uses to identify this program. */
        private final int mProgramId;

        /** Handle for uMvpMatrix uniform in vertex shader. */
        private final int mMvpMatrixHandle;

        /** Handle for aPosition attribute in vertex shader. */
        private final int mPositionHandle;

        /** Handle for uColor uniform in fragment shader. */
        private final int mColorHandle;

        /**
         * Creates a program to draw triangle lists. For optimal drawing efficiency, one program
         * should be used for all triangle lists being drawn.
         */
        public Program() {
            // Prepare shaders.
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE);

            // Create empty OpenGL Program.
            mProgramId = GLES20.glCreateProgram();
            if (CHECK_GL_ERRORS) checkGlError("glCreateProgram");
            if (mProgramId == 0) {
                throw new IllegalStateException("glCreateProgram failed");
            }

            // Add the shaders to the program.
            GLES20.glAttachShader(mProgramId, vertexShader);
            if (CHECK_GL_ERRORS) checkGlError("glAttachShader");
            GLES20.glAttachShader(mProgramId, fragmentShader);
            if (CHECK_GL_ERRORS) checkGlError("glAttachShader");

            // Link the program so it can be executed.
            GLES20.glLinkProgram(mProgramId);
            if (CHECK_GL_ERRORS) checkGlError("glLinkProgram");

            // Get a handle to the uMvpMatrix uniform in the vertex shader.
            mMvpMatrixHandle = GLES20.glGetUniformLocation(mProgramId, "uMvpMatrix");
            if (CHECK_GL_ERRORS) checkGlError("glGetUniformLocation");

            // Get a handle to the vertex shader's aPosition attribute.
            mPositionHandle = GLES20.glGetAttribLocation(mProgramId, "aPosition");
            if (CHECK_GL_ERRORS) checkGlError("glGetAttribLocation");

            // Enable vertex array (VBO).
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            if (CHECK_GL_ERRORS) checkGlError("glEnableVertexAttribArray");

            // Get a handle to fragment shader's uColor uniform.
            mColorHandle = GLES20.glGetUniformLocation(mProgramId, "uColor");
            if (CHECK_GL_ERRORS) checkGlError("glGetUniformLocation");
        }

        /**
         * Tells OpenGL to use this program. Call this method before drawing a sequence of
         * triangle lists.
         */
        public void use() {
            GLES20.glUseProgram(mProgramId);
            if (CHECK_GL_ERRORS) checkGlError("glUseProgram");
        }

        /** Sends the given MVP matrix, vertex data, and color to OpenGL. */
        public void bind(float[] mvpMatrix, FloatBuffer vertexBuffer, float[] color) {
            // Pass the MVP matrix to OpenGL.
            GLES20.glUniformMatrix4fv(mMvpMatrixHandle, 1 /* count */, false /* transpose */,
                    mvpMatrix, 0 /* offset */);
            if (CHECK_GL_ERRORS) checkGlError("glUniformMatrix4fv");

            // Pass the VBO with the triangle list's vertices to OpenGL.
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            if (CHECK_GL_ERRORS) checkGlError("glEnableVertexAttribArray");
            GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                    false /* normalized */, VERTEX_STRIDE, vertexBuffer);
            if (CHECK_GL_ERRORS) checkGlError("glVertexAttribPointer");

            // Pass the triangle list's color to OpenGL.
            GLES20.glUniform4fv(mColorHandle, 1 /* count */, color, 0 /* offset */);
            if (CHECK_GL_ERRORS) checkGlError("glUniform4fv");
        }
    }
}
