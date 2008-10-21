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

import javax.microedition.khronos.opengles.GL10;

/**
 * A class representing a set of GL_POINT objects.  GlobalTime uses this class
 * to draw city lights on the night side of the earth.
 */
public class PointCloud extends Shape {

    /**
     * Constructs a PointCloud with a point at each of the given vertex
     * (x, y, z) positions.
     * @param vertices an array of (x, y, z) positions given in fixed-point.
     */
    public PointCloud(int[] vertices) {
        this(vertices, 0, vertices.length);
    }

    /**
     * Constructs a PointCloud with a point at each of the given vertex
     * (x, y, z) positions.
     * @param vertices an array of (x, y, z) positions given in fixed-point.
     * @param off the starting offset of the vertices array
     * @param len the number of elements of the vertices array to use
     */
    public PointCloud(int[] vertices, int off, int len) {
        super(GL10.GL_POINTS, GL10.GL_UNSIGNED_SHORT,
              false, false, false);

        int numPoints = len / 3;
        short[] indices = new short[numPoints];
        for (int i = 0; i < numPoints; i++) {
            indices[i] = (short)i;
        }
        
        allocateBuffers(vertices, null, null, null, indices);
        this.mNumIndices = mIndexBuffer.capacity();
    }

    @Override public int getNumTriangles() {
        return mNumIndices * 2;
    }
}
