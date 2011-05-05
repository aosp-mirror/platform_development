/*
* Copyright 2006 Sony Computer Entertainment Inc.
*
* Licensed under the MIT Open Source License, for details please see license.txt or the website
* http://www.opensource.org/licenses/mit-license.php
*
*/

#ifndef _COLLADA_GEOMETRY_H_
#define _COLLADA_GEOMETRY_H_

#include <dae.h>
#include <dom/domCOLLADA.h>
#include <vector>
#include <string>

#include "rsContext.h"
#include "rsMesh.h"
#include "SimpleMesh.h"

using namespace android;
using namespace android::renderscript;


class ColladaGeometry {
public:
    ColladaGeometry();
    bool init(domGeometryRef geometry);

    SimpleMesh *getMesh() {
        return &mConvertedMesh;
    }

private:

    //Store some collada stuff
    domMesh *mMesh;

    // Cache the pointers to the collada version of the data
    // This contains raw vertex data that is not necessarily the same size for all
    // Offset refers to the way collada packs each triangle's index to position / normal / etc.
    domListOfFloats *mPositionFloats;
    int mPositionOffset;
    domListOfFloats *mNormalFloats;
    int mNormalOffset;
    domListOfFloats *mTangentFloats;
    int mTangentOffset;
    domListOfFloats *mBinormalFloats;
    int mBinormalOffset;
    domListOfFloats *mTexture1Floats;
    int mTexture1Offset;

    // In the list of triangles, collada uses multiple indecies per triangle to point to the correct
    // index in all the different arrays. We need to know the total number of these guys so we can
    // just to the next triangle to process
    int mMultiIndexOffset;

    // All these vectors would contain the same number of "points"
    // index*stride would properly get to the uv, normal etc.
    // collada, like maya and many others keep point array, normal array etc
    // different size in the cases the same vertex produces divergent normals for different faces
    std::vector<float> *mPositions;
    unsigned int mPositionsStride;
    std::vector<float> *mNormals;
    unsigned int mNormalsStride;
    std::vector<float> *mTextureCoords;
    unsigned int mTextureCoordsStride;
    std::vector<float> *mTangents;
    unsigned int mTangentssStride;
    std::vector<float> *mBinormals;
    unsigned int mBinormalsStride;

    SimpleMesh mConvertedMesh;

    // This vector is used to remap a position index into a list of all divergent vertices
    std::vector<std::vector<unsigned int> > mVertexRemap;

    void addTriangles(domTriangles * colladaTriangles);
    void cacheOffsetsAndDataPointers(domTriangles * colladaTriangles);
    int remapIndexAndStoreData(const domListOfUInts &colladaIndexList, int indexToRemap);

};

#endif //COLLADA_TO_A3D_GEOMETRY
