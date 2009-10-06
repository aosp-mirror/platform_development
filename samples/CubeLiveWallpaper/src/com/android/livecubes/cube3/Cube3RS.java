/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.livecubes.cube3;

import com.android.livecubes.R;
import com.android.livecubes.RenderScriptScene;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.Primitive;
import android.renderscript.ProgramRaster;
import android.renderscript.ProgramVertex;
import android.renderscript.ScriptC;
import android.renderscript.SimpleMesh;
import android.renderscript.Type;
import android.renderscript.Element.Builder;

import java.util.TimeZone;

/*
 * This example draws a shape whose definition is read from resources (though
 * it's not user selectable like in example #2), but does the drawing using
 * RenderScript.
 */
class Cube3RS extends RenderScriptScene {

    static class ThreeDPoint {
        public float x;
        public float y;
        public float z;
    }

    static class ThreeDLine {
        int startPoint;
        int endPoint;
    }

    static class WorldState {
        public float yRotation;
        public float mCenterX;
        public float mCenterY;
    }
    ThreeDPoint [] mOriginalPoints;
    ThreeDLine [] mLines;

    WorldState mWorldState = new WorldState();
    private Type mStateType;
    private Allocation mState;

    private SimpleMesh mCubeMesh;

    private Allocation mPointAlloc;
    private float [] mPointData;

    private Allocation mLineIdxAlloc;
    private short [] mIndexData;

    private ProgramVertex mPVBackground;
    private ProgramVertex.MatrixAllocation mPVAlloc;

    private int mWidth;
    private int mHeight;

    private static final int RSID_STATE = 0;
    private static final int RSID_POINTS = 1;
    private static final int RSID_LINES = 2;
    private static final int RSID_PROGRAMVERTEX = 3;


    Cube3RS(int width, int height) {
        super(width, height);
        mWidth = width;
        mHeight = height;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        mWidth = width;
        mHeight = height;
    }

    @Override
    protected ScriptC createScript() {

        // Read the model in to our point/line objects
        readModel();

        // Create a renderscript type from a java class. The specified name doesn't
        // really matter; the name by which we refer to the object in RenderScript
        // will be specified later.
        mStateType = Type.createFromClass(mRS, WorldState.class, 1, "WorldState");
        // Create an allocation from the type we just created.
        mState = Allocation.createTyped(mRS, mStateType);
        // set our java object as the data for the renderscript allocation
        mWorldState.yRotation = (-0.5f) * 2 * 180 / (float) Math.PI;
        mState.data(mWorldState);

        /*
         *  Now put our model in to a form that renderscript can work with:
         *  - create a buffer of floats that are the coordinates for the points that define the cube
         *  - create a buffer of integers that are the indices of the points that form lines
         *  - combine the two in to a mesh
         */

        // First set up the coordinate system and such
        ProgramVertex.Builder pvb = new ProgramVertex.Builder(mRS, null, null);
        mPVBackground = pvb.create();
        mPVBackground.setName("PVBackground");
        mPVAlloc = new ProgramVertex.MatrixAllocation(mRS);
        mPVBackground.bindAllocation(mPVAlloc);
        mPVAlloc.setupProjectionNormalized(mWidth, mHeight);

        // Start creating the mesh
        final SimpleMesh.Builder meshBuilder = new SimpleMesh.Builder(mRS);

        // Create the Element for the points
        Builder elementBuilder = new Builder(mRS);
        // By specifying a prefix, even an empty one, the members will be accessible
        // in the renderscript. If we just called addFloatXYZ(), the members would be
        // unnamed in the renderscript struct definition.
        elementBuilder.addFloatXYZ("");
        final Element vertexElement = elementBuilder.create();
        final int vertexSlot = meshBuilder.addVertexType(vertexElement, mOriginalPoints.length);
        // Specify the type and number of indices we need. We'll allocate them later.
        meshBuilder.setIndexType(Element.INDEX_16(mRS), mLines.length * 2);
        // This will be a line mesh
        meshBuilder.setPrimitive(Primitive.LINE);

        // Create the Allocation for the vertices
        mCubeMesh = meshBuilder.create();
        mCubeMesh.setName("CubeMesh");
        mPointAlloc = mCubeMesh.createVertexAllocation(vertexSlot);
        mPointAlloc.setName("PointBuffer");

        // Create the Allocation for the indices
        mLineIdxAlloc = mCubeMesh.createIndexAllocation();

        // Bind the allocations to the mesh
        mCubeMesh.bindVertexAllocation(mPointAlloc, 0);
        mCubeMesh.bindIndexAllocation(mLineIdxAlloc);

        /*
         *  put the vertex and index data in their respective buffers
         */
        // one float each for x, y and z, and the 4th float will hold rgba
        mPointData = new float[mOriginalPoints.length * 3];
        for(int i = 0; i < mOriginalPoints.length; i ++) {
            mPointData[i*3]   = mOriginalPoints[i].x;
            mPointData[i*3+1] = mOriginalPoints[i].y;
            mPointData[i*3+2] = mOriginalPoints[i].z;
        }
        mIndexData = new short[mLines.length * 2];
        for(int i = 0; i < mLines.length; i++) {
            mIndexData[i * 2] = (short)(mLines[i].startPoint);
            mIndexData[i * 2 + 1] = (short)(mLines[i].endPoint);
        }

        /*
         *  upload the vertex and index data
         */
        mPointAlloc.data(mPointData);
        mPointAlloc.uploadToBufferObject();
        mLineIdxAlloc.data(mIndexData);
        mLineIdxAlloc.uploadToBufferObject();

        // Time to create the script
        ScriptC.Builder sb = new ScriptC.Builder(mRS);
        // Specify the name by which to refer to the WorldState object in the
        // renderscript.
        sb.setType(mStateType, "State", RSID_STATE);
        sb.setType(mCubeMesh.getVertexType(0), "Points", RSID_POINTS);
        // this crashes when uncommented
        //sb.setType(mCubeMesh.getIndexType(), "Lines", RSID_LINES);

        // Set the render script that will make use of the objects we defined above
        sb.setScript(mResources, R.raw.cube);
        sb.setRoot(true);

        ScriptC script = sb.create();
        script.setClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        script.setTimeZone(TimeZone.getDefault().getID());

        script.bindAllocation(mState, RSID_STATE);
        script.bindAllocation(mPointAlloc, RSID_POINTS);
        script.bindAllocation(mLineIdxAlloc, RSID_LINES);
        script.bindAllocation(mPVAlloc.mAlloc, RSID_PROGRAMVERTEX);

        return script;
    }

    @Override
    public void setOffset(float xOffset, float yOffset, int xPixels, int yPixels) {
        // update our state, then push it to the renderscript
        mWorldState.yRotation = (xOffset - 0.5f) * 2 * 180 / (float) Math.PI;
        mState.data(mWorldState);
    }

    /*
     *  Read the model definition from the resource.
     */
    private void readModel() {

        String [] p = mResources.getStringArray(R.array.cubepoints);
        int numpoints = p.length;
        mOriginalPoints = new ThreeDPoint[numpoints];

        for (int i = 0; i < numpoints; i++) {
            mOriginalPoints[i] = new ThreeDPoint();
            String [] coord = p[i].split(" ");
            mOriginalPoints[i].x = Float.valueOf(coord[0]);
            mOriginalPoints[i].y = Float.valueOf(coord[1]);
            mOriginalPoints[i].z = Float.valueOf(coord[2]);
        }

        String [] l = mResources.getStringArray(R.array.cubelines);
        int numlines = l.length;
        mLines = new ThreeDLine[numlines];

        for (int i = 0; i < numlines; i++) {
            mLines[i] = new ThreeDLine();
            String [] idx = l[i].split(" ");
            mLines[i].startPoint = Integer.valueOf(idx[0]);
            mLines[i].endPoint = Integer.valueOf(idx[1]);
        }
    }

}
