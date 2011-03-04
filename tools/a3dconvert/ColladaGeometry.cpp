/*
* Copyright 2006 Sony Computer Entertainment Inc.
*
* Licensed under the MIT Open Source License, for details please see license.txt or the website
* http://www.opensource.org/licenses/mit-license.php
*
*/

#include "ColladaGeometry.h"
#include <iostream>
#include <sstream>

ColladaGeometry::ColladaGeometry() :
        mPositionFloats(NULL), mPositionOffset(-1),
        mNormalFloats(NULL), mNormalOffset(-1),
        mTangentFloats(NULL), mTangentOffset(-1),
        mBinormalFloats(NULL), mBinormalOffset(-1),
        mTexture1Floats(NULL), mTexture1Offset(-1),
        mMultiIndexOffset(-1),
        mPositionsStride(3), mNormalsStride(3),
        mTextureCoordsStride(2), mTangentssStride(3), mBinormalsStride(3) {

    mConvertedMesh.appendChannel("position", mPositionsStride);
    mConvertedMesh.appendChannel("normal", mNormalsStride);
    mConvertedMesh.appendChannel("texture0", mTextureCoordsStride);
    mConvertedMesh.appendChannel("binormal", mBinormalsStride);
    mConvertedMesh.appendChannel("tangent", mTangentssStride);

    mPositions = &mConvertedMesh.mChannels[0].mData;
    mNormals = &mConvertedMesh.mChannels[1].mData;
    mTextureCoords = &mConvertedMesh.mChannels[2].mData;
    mBinormals = &mConvertedMesh.mChannels[3].mData;
    mTangents = &mConvertedMesh.mChannels[4].mData;
}

bool ColladaGeometry::init(domGeometryRef geometry) {

    bool convertSuceeded = true;

    const char* geoName = geometry->getName();
    if (geoName == NULL) {
        geoName = geometry->getId();
    }
    mConvertedMesh.mName = geoName;
    mMesh = geometry->getMesh();

    // Iterate over all the index groups and build up a simple resolved tri list and vertex array
    const domTriangles_Array &allTriLists = mMesh->getTriangles_array();
    int numTriLists = allTriLists.getCount();
    mConvertedMesh.mTriangleLists.reserve(numTriLists);
    mConvertedMesh.mTriangleListNames.reserve(numTriLists);
    for (int i = 0; i < numTriLists; i ++) {
        addTriangles(allTriLists[i]);
    }

    return convertSuceeded;
}

void ColladaGeometry::addTriangles(domTriangles * colladaTriangles) {

    int numTriangles = colladaTriangles->getCount();
    int triListIndex = mConvertedMesh.mTriangleLists.size();
    mConvertedMesh.mTriangleLists.resize(triListIndex + 1);
    std::string materialName = colladaTriangles->getMaterial();
    if (materialName.size() == 0) {
        char buffer[128];
        sprintf(buffer, "index%d", triListIndex);
        materialName = buffer;
    }
    mConvertedMesh.mTriangleListNames.push_back(materialName);

    // It's a good idea to tell stl how much memory we intend to use
    // to limit the number of reallocations
    mPositions->reserve(numTriangles * 3);
    mNormals->reserve(numTriangles * 3);
    mTangents->reserve(numTriangles * 3);
    mBinormals->reserve(numTriangles * 3);
    mTextureCoords->reserve(numTriangles * 3);

    // Stores the pointers to the image data and where in the tri list that data comes from
    cacheOffsetsAndDataPointers(colladaTriangles);

    // Collapse the multiindex that collada uses
    const domListOfUInts &colladaIndexList = colladaTriangles->getP()->getValue();
    std::vector<uint32_t> &a3dIndexList = mConvertedMesh.mTriangleLists[triListIndex];
    a3dIndexList.resize(numTriangles * 3);
    for (int i = 0; i < numTriangles * 3; i ++) {

        a3dIndexList[i] = remapIndexAndStoreData(colladaIndexList, i);
    }

}

void ColladaGeometry::cacheOffsetsAndDataPointers(domTriangles * colladaTriangles) {
    // Define the names of known vertex channels
    const char *positionSemantic = "POSITION";
    const char *vertexSemantic = "VERTEX";
    const char *normalSemantic = "NORMAL";
    const char *tangentSemantic = "TANGENT";
    const char *binormalSemantic = "BINORMAL";
    const char *texture1Semantic = "TEXCOORD";

    const domInputLocalOffset_Array &inputs = colladaTriangles->getInput_array();
    mMultiIndexOffset = inputs.getCount();

    // inputs with offsets
    // There are two places collada can put links to our data
    // 1 - in the VERTEX, which is its way of saying follow a link to the vertex structure
    //     then every geometry array you find there is the same size as the position array
    // 2 - a direct link to the channel from the primitive list. This tells us that there are
    //     potentially more or less floats in those channels because there is some vertex re-use
    //     or divergence in that data channel. For example, highly segmented uv set would produce a
    //     larger array because for every physical vertex position thre might be 2 or more uv coords
    for (uint32_t i = 0; i < inputs.getCount(); i ++) {

        int currentOffset = inputs[i]->getOffset();
        const char *currentSemantic = inputs[i]->getSemantic();

        domSource * source = (domSource*) (domElement*) inputs[i]->getSource().getElement();
        if (strcmp(vertexSemantic, currentSemantic) == 0) {
            mPositionOffset = currentOffset;
        }
        else if (strcmp(normalSemantic, currentSemantic) == 0) {
            mNormalOffset = currentOffset;
            mNormalFloats = &source->getFloat_array()->getValue();
        }
        else if (strcmp(tangentSemantic, currentSemantic) == 0) {
            mTangentOffset = currentOffset;
            mTangentFloats = &source->getFloat_array()->getValue();
        }
        else if (strcmp(binormalSemantic, currentSemantic) == 0) {
            mBinormalOffset = currentOffset;
            mBinormalFloats = &source->getFloat_array()->getValue();
        }
        else if (strcmp(texture1Semantic, currentSemantic) == 0) {
            mTexture1Offset = currentOffset;
            mTexture1Floats = & source->getFloat_array()->getValue();
        }
    }

    // There are multiple ways of getting to data, so follow them all
    domVertices * vertices = mMesh->getVertices();
    const domInputLocal_Array &verticesInputs = vertices->getInput_array();
    for (uint32_t i = 0; i < verticesInputs.getCount(); i ++) {

        const char *currentSemantic = verticesInputs[i]->getSemantic();

        domSource * source = (domSource*) (domElement*) verticesInputs[i]->getSource().getElement();
        if (strcmp(positionSemantic, currentSemantic) == 0) {
            mPositionFloats = & source->getFloat_array()->getValue();
            // TODO: Querry this from the accessor in the future because
            // I supopose it's possible to have 4 floats if we hide something in w
            int numberOfFloatsPerPoint = 3;
            // We want to cllapse duplicate vertices, otherwise we could just unroll the tri list
            mVertexRemap.resize(source->getFloat_array()->getCount()/numberOfFloatsPerPoint);
        }
        else if (strcmp(normalSemantic, currentSemantic) == 0) {
            mNormalFloats = & source->getFloat_array()->getValue();
            mNormalOffset = mPositionOffset;
        }
        else if (strcmp(tangentSemantic, currentSemantic) == 0) {
            mTangentFloats = & source->getFloat_array()->getValue();
            mTangentOffset = mPositionOffset;
        }
        else if (strcmp(binormalSemantic, currentSemantic) == 0) {
            mBinormalFloats = & source->getFloat_array()->getValue();
            mBinormalOffset = mPositionOffset;
        }
        else if (strcmp(texture1Semantic, currentSemantic) == 0) {
            mTexture1Floats = & source->getFloat_array()->getValue();
            mTexture1Offset = mPositionOffset;
        }
    }
}

int ColladaGeometry::remapIndexAndStoreData(const domListOfUInts &colladaIndexList, int indexToRemap) {

    domUint positionIndex = colladaIndexList[indexToRemap*mMultiIndexOffset + mPositionOffset];

    float posX = (*mPositionFloats)[positionIndex * mPositionsStride + 0];
    float posY = (*mPositionFloats)[positionIndex * mPositionsStride + 1];
    float posZ = (*mPositionFloats)[positionIndex * mPositionsStride + 2];

    float normX = 0;
    float normY = 0;
    float normZ = 0;

    if (mNormalOffset != -1) {
        domUint normalIndex = colladaIndexList[indexToRemap*mMultiIndexOffset + mNormalOffset];
        normX = (*mNormalFloats)[normalIndex * mNormalsStride + 0];
        normY = (*mNormalFloats)[normalIndex * mNormalsStride + 1];
        normZ = (*mNormalFloats)[normalIndex * mNormalsStride + 2];
    }

    float tanX = 0;
    float tanY = 0;
    float tanZ = 0;

    if (mTangentOffset != -1) {
        domUint tangentIndex = colladaIndexList[indexToRemap*mMultiIndexOffset + mTangentOffset];
        tanX = (*mTangentFloats)[tangentIndex * mTangentssStride + 0];
        tanY = (*mTangentFloats)[tangentIndex * mTangentssStride + 1];
        tanZ = (*mTangentFloats)[tangentIndex * mTangentssStride + 2];
    }

    float binormX = 0;
    float binormY = 0;
    float binormZ = 0;

    if (mBinormalOffset != -1) {
        domUint binormalIndex = colladaIndexList[indexToRemap*mMultiIndexOffset + mNormalOffset];
        binormX = (*mBinormalFloats)[binormalIndex * mBinormalsStride + 0];
        binormY = (*mBinormalFloats)[binormalIndex * mBinormalsStride + 1];
        binormZ = (*mBinormalFloats)[binormalIndex * mBinormalsStride + 2];
    }

    float texCoordX = 0;
    float texCoordY = 0;

    if (mTexture1Offset != -1) {
        domUint texCoordIndex = colladaIndexList[indexToRemap*mMultiIndexOffset + mTexture1Offset];
        texCoordX = (*mTexture1Floats)[texCoordIndex * mTextureCoordsStride + 0];
        texCoordY = (*mTexture1Floats)[texCoordIndex * mTextureCoordsStride + 1];
    }

    std::vector<uint32_t> &ithRemapList = mVertexRemap[positionIndex];
    // We may have some potential vertices we can reuse
    // loop over all the potential candidates and see if any match our guy
    for (uint32_t i = 0; i < ithRemapList.size(); i ++) {

        int ithRemap = ithRemapList[i];
        // compare existing vertex with the new one
        if ((*mPositions)[ithRemap * mPositionsStride + 0] != posX ||
            (*mPositions)[ithRemap * mPositionsStride + 1] != posY ||
            (*mPositions)[ithRemap * mPositionsStride + 2] != posZ) {
            continue;
        }

        // Now go over normals
        if (mNormalOffset != -1) {
            if ((*mNormals)[ithRemap * mNormalsStride + 0] != normX ||
                (*mNormals)[ithRemap * mNormalsStride + 1] != normY ||
                (*mNormals)[ithRemap * mNormalsStride + 2] != normZ) {
                continue;
            }
        }

        // Now go over tangents
        if (mTangentOffset != -1) {
            if ((*mTangents)[ithRemap * mTangentssStride + 0] != tanX ||
                (*mTangents)[ithRemap * mTangentssStride + 1] != tanY ||
                (*mTangents)[ithRemap * mTangentssStride + 2] != tanZ) {
                continue;
            }
        }

        // Now go over binormals
        if (mBinormalOffset != -1) {
            if ((*mBinormals)[ithRemap * mBinormalsStride + 0] != binormX ||
                (*mBinormals)[ithRemap * mBinormalsStride + 1] != binormY ||
                (*mBinormals)[ithRemap * mBinormalsStride + 2] != binormZ) {
                continue;
            }
        }

        // And texcoords
        if (mTexture1Offset != -1) {
            if ((*mTextureCoords)[ithRemap * mTextureCoordsStride + 0] != texCoordX ||
                (*mTextureCoords)[ithRemap * mTextureCoordsStride + 1] != texCoordY) {
               continue;
            }
        }

        // If we got here the new vertex is identical to the one that we already stored
        return ithRemap;
    }

    // We did not encounter this vertex yet, store it and return its index
    mPositions->push_back(posX);
    mPositions->push_back(posY);
    mPositions->push_back(posZ);

    if (mNormalOffset != -1) {
        mNormals->push_back(normX);
        mNormals->push_back(normY);
        mNormals->push_back(normZ);
    }

    if (mTangentOffset != -1) {
        mTangents->push_back(tanX);
        mTangents->push_back(tanY);
        mTangents->push_back(tanZ);
    }

    if (mBinormalOffset != -1) {
        mBinormals->push_back(binormX);
        mBinormals->push_back(binormY);
        mBinormals->push_back(binormZ);
    }

    if (mTexture1Offset != -1) {
        mTextureCoords->push_back(texCoordX);
        mTextureCoords->push_back(texCoordY);
    }

    // We need to remember this mapping. Since we are storing floats, not vec3's, need to
    // divide by position size to get the right index
    int currentVertexIndex = (mPositions->size()/mPositionsStride) - 1;
    ithRemapList.push_back(currentVertexIndex);

    return currentVertexIndex;
}







