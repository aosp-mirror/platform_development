/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.globaltime;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * An abstract superclass for various three-dimensional objects to be drawn
 * using OpenGL ES.  Each subclass is responsible for setting up NIO buffers
 * containing vertices, texture coordinates, colors, normals, and indices.
 * The {@link #draw(GL10)} method draws the object to the given OpenGL context.
 */
public abstract class Shape {

    public static final int INT_BYTES = 4;
    public static final int SHORT_BYTES = 2;

    public static final float DEGREES_TO_RADIANS = (float) Math.PI / 180.0f;
    public static final float PI = (float) Math.PI;
    public static final float TWO_PI = (float) (2.0 * Math.PI);
    public static final float PI_OVER_TWO = (float) (Math.PI / 2.0);

    protected int mPrimitive;
    protected int mIndexDatatype;

    protected boolean mEmitTextureCoordinates;
    protected boolean mEmitNormals;
    protected boolean mEmitColors;

    protected IntBuffer mVertexBuffer;
    protected IntBuffer mTexcoordBuffer;
    protected IntBuffer mColorBuffer;
    protected IntBuffer mNormalBuffer;
    protected Buffer mIndexBuffer;
    protected int mNumIndices = -1;

    /**
     * Constructs a Shape.
     * 
     * @param primitive a GL primitive type understood by glDrawElements,
     * such as GL10.GL_TRIANGLES
     * @param indexDatatype the GL datatype for the  index buffer, such as
     * GL10.GL_UNSIGNED_SHORT
     * @param emitTextureCoordinates true to enable use of the texture
     * coordinate buffer
     * @param emitNormals true to enable use of the normal buffer
     * @param emitColors true to enable use of the color buffer
     */
    protected Shape(int primitive,
        int indexDatatype,
        boolean emitTextureCoordinates,
        boolean emitNormals,
        boolean emitColors) {
        mPrimitive = primitive;
        mIndexDatatype = indexDatatype;
        mEmitTextureCoordinates = emitTextureCoordinates;
        mEmitNormals = emitNormals;
        mEmitColors = emitColors;
    }

    /**
     * Converts the given floating-point value to fixed-point.
     */
    public static int toFixed(float x) {
        return (int) (x * 65536.0);
    }

    /**
     * Converts the given fixed-point value to floating-point.
     */
    public static float toFloat(int x) {
        return (float) (x / 65536.0);
    }

    /**
     * Computes the cross-product of two vectors p and q and places
     * the result in out. 
     */
    public static void cross(float[] p, float[] q, float[] out) {
        out[0] = p[1] * q[2] - p[2] * q[1];
        out[1] = p[2] * q[0] - p[0] * q[2];
        out[2] = p[0] * q[1] - p[1] * q[0];
    }

    /**
     * Returns the length of a vector, given as three floats.
     */
    public static float length(float vx, float vy, float vz) {
        return (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
    }

    /**
     * Returns the length of a vector, given as an array of three floats.
     */
    public static float length(float[] v) { 
        return length(v[0], v[1], v[2]);
    }

    /**
     * Normalizes the given vector of three floats to have length == 1.0.
     * Vectors with length zero are unaffected.
     */
    public static void normalize(float[] v) {
        float length = length(v);
        if (length != 0.0f) {
            float norm = 1.0f / length;
            v[0] *= norm;
            v[1] *= norm;
            v[2] *= norm;
        }
    }

    /**
     * Returns the number of triangles associated with this shape.
     */
    public int getNumTriangles() {
        if (mPrimitive == GL10.GL_TRIANGLES) {
            return mIndexBuffer.capacity() / 3;
        } else if (mPrimitive == GL10.GL_TRIANGLE_STRIP) {
            return mIndexBuffer.capacity() - 2;
        }
        return 0;
    }
    
    /**
     * Copies the given data into the instance
     * variables mVertexBuffer, mTexcoordBuffer, mNormalBuffer, mColorBuffer,
     * and mIndexBuffer.
     * 
     * @param vertices an array of fixed-point vertex coordinates
     * @param texcoords an array of fixed-point texture coordinates
     * @param normals an array of fixed-point normal vector coordinates
     * @param colors an array of fixed-point color channel values
     * @param indices an array of short indices
     */
    public void allocateBuffers(int[] vertices, int[] texcoords, int[] normals,
        int[] colors, short[] indices) {
        allocate(vertices, texcoords, normals, colors);
        
        ByteBuffer ibb =
            ByteBuffer.allocateDirect(indices.length * SHORT_BYTES);
        ibb.order(ByteOrder.nativeOrder());
        ShortBuffer shortIndexBuffer = ibb.asShortBuffer();
        shortIndexBuffer.put(indices);
        shortIndexBuffer.position(0);
        this.mIndexBuffer = shortIndexBuffer;
    }
    
    /**
     * Copies the given data into the instance
     * variables mVertexBuffer, mTexcoordBuffer, mNormalBuffer, mColorBuffer,
     * and mIndexBuffer.
     * 
     * @param vertices an array of fixed-point vertex coordinates
     * @param texcoords an array of fixed-point texture coordinates
     * @param normals an array of fixed-point normal vector coordinates
     * @param colors an array of fixed-point color channel values
     * @param indices an array of int indices
     */
    public void allocateBuffers(int[] vertices, int[] texcoords, int[] normals,
        int[] colors, int[] indices) {
        allocate(vertices, texcoords, normals, colors);
        
        ByteBuffer ibb =
            ByteBuffer.allocateDirect(indices.length * INT_BYTES);
        ibb.order(ByteOrder.nativeOrder());
        IntBuffer intIndexBuffer = ibb.asIntBuffer();
        intIndexBuffer.put(indices);
        intIndexBuffer.position(0);
        this.mIndexBuffer = intIndexBuffer;
    }
    
    /**
     * Allocate the vertex, texture coordinate, normal, and color buffer.
     */
    private void allocate(int[] vertices, int[] texcoords, int[] normals,
        int[] colors) {
        ByteBuffer vbb =
            ByteBuffer.allocateDirect(vertices.length * INT_BYTES);
        vbb.order(ByteOrder.nativeOrder());
        mVertexBuffer = vbb.asIntBuffer();
        mVertexBuffer.put(vertices);
        mVertexBuffer.position(0);

        if ((texcoords != null) && mEmitTextureCoordinates) {
            ByteBuffer tbb =
                ByteBuffer.allocateDirect(texcoords.length * INT_BYTES);
            tbb.order(ByteOrder.nativeOrder());
            mTexcoordBuffer = tbb.asIntBuffer();
            mTexcoordBuffer.put(texcoords);
            mTexcoordBuffer.position(0);
        }

        if ((normals != null) && mEmitNormals) {
            ByteBuffer nbb =
                ByteBuffer.allocateDirect(normals.length * INT_BYTES);
            nbb.order(ByteOrder.nativeOrder());
            mNormalBuffer = nbb.asIntBuffer();
            mNormalBuffer.put(normals);
            mNormalBuffer.position(0);
        }

        if ((colors != null) && mEmitColors) {
            ByteBuffer cbb =
                ByteBuffer.allocateDirect(colors.length * INT_BYTES);
            cbb.order(ByteOrder.nativeOrder());
            mColorBuffer = cbb.asIntBuffer();
            mColorBuffer.put(colors);
            mColorBuffer.position(0);
        }
    }

    /**
     * Draws the shape to the given OpenGL ES 1.0 context.  Texture coordinates,
     * normals, and colors are emitted according the the preferences set for
     * this shape.
     */
    public void draw(GL10 gl) {
        gl.glVertexPointer(3, GL10.GL_FIXED, 0, mVertexBuffer);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

        if (mEmitTextureCoordinates) {
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
            gl.glTexCoordPointer(2, GL10.GL_FIXED, 0, mTexcoordBuffer);
            gl.glEnable(GL10.GL_TEXTURE_2D);
        } else {
            gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
            gl.glDisable(GL10.GL_TEXTURE_2D);
        }

        if (mEmitNormals) {
            gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
            gl.glNormalPointer(GL10.GL_FIXED, 0, mNormalBuffer);
        } else {
            gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
        }

        if (mEmitColors) {
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
            gl.glColorPointer(4, GL10.GL_FIXED, 0, mColorBuffer);
        } else {
            gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        }

        gl.glDrawElements(mPrimitive,
                          mNumIndices > 0 ? mNumIndices : mIndexBuffer.capacity(),
                          mIndexDatatype,
                          mIndexBuffer);
    }
}
