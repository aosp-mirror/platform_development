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
 * A class that draws a ring with a given center and inner and outer radii.
 * The inner and outer rings each have a color and the remaining pixels are
 * colored by interpolation.  GlobalTime uses this class to simulate an
 * "atmosphere" around the earth.
 */
public class Annulus extends Shape {

    /**
     * Constructs an annulus.
     * 
     * @param centerX the X coordinate of the center point
     * @param centerY the Y coordinate of the center point
     * @param Z the fixed Z for the entire ring
     * @param innerRadius the inner radius
     * @param outerRadius the outer radius
     * @param rInner the red channel of the color of the inner ring
     * @param gInner the green channel of the color of the inner ring
     * @param bInner the blue channel of the color of the inner ring
     * @param aInner the alpha channel of the color of the inner ring
     * @param rOuter the red channel of the color of the outer ring
     * @param gOuter the green channel of the color of the outer ring
     * @param bOuter the blue channel of the color of the outer ring
     * @param aOuter the alpha channel of the color of the outer ring
     * @param sectors the number of sectors used to approximate curvature
     */
    public Annulus(float centerX, float centerY, float Z,
        float innerRadius, float outerRadius,
        float rInner, float gInner, float bInner, float aInner,
        float rOuter, float gOuter, float bOuter, float aOuter,
        int sectors) {
        super(GL10.GL_TRIANGLES, GL10.GL_UNSIGNED_SHORT,
              false, false, true);

        int radii = sectors + 1;

        int[] vertices = new int[2 * 3 * radii];
        int[] colors = new int[2 * 4 * radii];
        short[] indices = new short[2 * 3 * radii];

        int vidx = 0;
        int cidx = 0;
        int iidx = 0;

        for (int i = 0; i < radii; i++) {
            float theta = (i * TWO_PI) / (radii - 1);
            float cosTheta = (float) Math.cos(theta);
            float sinTheta = (float) Math.sin(theta);

            vertices[vidx++] = toFixed(centerX + innerRadius * cosTheta);
            vertices[vidx++] = toFixed(centerY + innerRadius * sinTheta);
            vertices[vidx++] = toFixed(Z);

            vertices[vidx++] = toFixed(centerX + outerRadius * cosTheta);
            vertices[vidx++] = toFixed(centerY + outerRadius * sinTheta);
            vertices[vidx++] = toFixed(Z);
        
            colors[cidx++] = toFixed(rInner);
            colors[cidx++] = toFixed(gInner);
            colors[cidx++] = toFixed(bInner);
            colors[cidx++] = toFixed(aInner);

            colors[cidx++] = toFixed(rOuter);
            colors[cidx++] = toFixed(gOuter);
            colors[cidx++] = toFixed(bOuter);
            colors[cidx++] = toFixed(aOuter);
        }

        for (int i = 0; i < sectors; i++) {
            indices[iidx++] = (short) (2 * i);
            indices[iidx++] = (short) (2 * i + 1);
            indices[iidx++] = (short) (2 * i + 2);

            indices[iidx++] = (short) (2 * i + 1);
            indices[iidx++] = (short) (2 * i + 3);
            indices[iidx++] = (short) (2 * i + 2);
        }
        
        allocateBuffers(vertices, null, null, colors, indices);
    }
}
