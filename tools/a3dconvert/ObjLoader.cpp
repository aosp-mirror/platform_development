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

#include "ObjLoader.h"
#include <rsFileA3D.h>
#include <sstream>

ObjLoader::ObjLoader() :
    mPositionsStride(3), mNormalsStride(3), mTextureCoordsStride(2) {

}

bool isWhitespace(char c) {
    const char whiteSpace[] = { ' ', '\n', '\t', '\f', '\r' };
    const uint32_t numWhiteSpaceChars = 5;
    for (uint32_t i = 0; i < numWhiteSpaceChars; i ++) {
        if (whiteSpace[i] == c) {
            return true;
        }
    }
    return false;
}

void eatWhitespace(std::istream &is) {
    while(is.good() && isWhitespace(is.peek())) {
        is.get();
    }
}

bool getToken(std::istream &is, std::string &token) {
    eatWhitespace(is);
    token.clear();
    char c;
    while(is.good() && !isWhitespace(is.peek())) {
        c = is.get();
        if (is.good()){
            token += c;
        }
    }
    return token.size() > 0;
}

void appendDataFromStream(std::vector<float> &dataVec, uint32_t numFloats, std::istream &is) {
    std::string token;
    for (uint32_t i = 0; i < numFloats; i ++){
        bool valid = getToken(is, token);
        if (valid) {
            dataVec.push_back((float)atof(token.c_str()));
        } else {
            fprintf(stderr, "Encountered error reading geometry data");
            dataVec.push_back(0.0f);
        }
    }
}

bool checkNegativeIndex(int idx) {
    if(idx < 0) {
        fprintf(stderr, "Negative indices are not supported. Skipping face\n");
        return false;
    }
    return true;
}

void ObjLoader::parseRawFaces(){
    // We need at least a triangle
    if (mRawFaces.size() < 3) {
        return;
    }

    const char slash = '/';
    mParsedFaces.resize(mRawFaces.size());
    for (uint32_t i = 0; i < mRawFaces.size(); i ++) {
        size_t firstSeparator = mRawFaces[i].find_first_of(slash);
        size_t nextSeparator = mRawFaces[i].find_last_of(slash);

        // Use the string as a temp buffer to parse the index
        // Insert 0 instead of the slash to avoid substrings
        if (firstSeparator != std::string::npos) {
            mRawFaces[i][firstSeparator] = 0;
        }
        // Simple case, only one index
        int32_t vIdx = atoi(mRawFaces[i].c_str());
        // We do not support negative indices
        if (!checkNegativeIndex(vIdx)) {
            return;
        }
        // obj indices things beginning 1
        mParsedFaces[i].vertIdx = (uint32_t)vIdx - 1;

        if (nextSeparator != std::string::npos && nextSeparator != firstSeparator) {
            mRawFaces[i][nextSeparator] = 0;
            uint32_t nIdx = atoi(mRawFaces[i].c_str() + nextSeparator + 1);
            if (!checkNegativeIndex(nIdx)) {
                return;
            }
            // obj indexes things beginning 1
            mParsedFaces[i].normIdx = (uint32_t)nIdx - 1;
        }

        // second case is where we have vertex and texture indices
        if (nextSeparator != std::string::npos &&
           (nextSeparator > firstSeparator + 1 || nextSeparator == firstSeparator)) {
            uint32_t tIdx = atoi(mRawFaces[i].c_str() + firstSeparator + 1);
            if (!checkNegativeIndex(tIdx)) {
                return;
            }
            // obj indexes things beginning 1
            mParsedFaces[i].texIdx = (uint32_t)tIdx - 1;
        }
    }

    // Make sure a face list exists before we go adding to it
    if (mMeshes.back().mUnfilteredFaces.size() == 0) {
        mMeshes.back().appendUnfilteredFaces(mLastMtl);
    }

    // Now we have our parsed face, that we need to triangulate as necessary
    // Treat more complex polygons as fans.
    // This approach will only work only for convex polygons
    // but concave polygons need to be addressed elsewhere anyway
    for (uint32_t next = 1; next < mParsedFaces.size() - 1; next ++) {
        // push it to our current mesh
        mMeshes.back().mUnfilteredFaces.back().push_back(mParsedFaces[0]);
        mMeshes.back().mUnfilteredFaces.back().push_back(mParsedFaces[next]);
        mMeshes.back().mUnfilteredFaces.back().push_back(mParsedFaces[next + 1]);
    }
}

void ObjLoader::checkNewMeshCreation(std::string &newGroup) {
    // start a new mesh if we have some faces
    // accumulated on the current mesh.
    // It's possible to have multiple group statements
    // but we only care to actually start a new mesh
    // once we can have something we can draw on the previous one
    if (mMeshes.back().mUnfilteredFaces.size()) {
        mMeshes.push_back(ObjMesh());
    }

    mMeshes.back().mName = newGroup;
    printf("Converting vertex group: %s\n", newGroup.c_str());
}

void ObjLoader::handleObjLine(char *line) {
    const char* vtxToken    = "v";
    const char* normToken   = "vn";
    const char* texToken    = "vt";
    const char* groupToken  = "g";
    const char* mtlToken    = "usemtl";
    const char* faceToken   = "f";

    std::istringstream lineStream(line, std::istringstream::in);

    std::string token;
    bool valid = getToken(lineStream, token);
    if (!valid) {
        return;
    }

    if (token == vtxToken) {
        appendDataFromStream(mObjPositions, 3, lineStream);
    } else if (token == normToken) {
        appendDataFromStream(mObjNormals, 3, lineStream);
    } else if (token == texToken) {
        appendDataFromStream(mObjTextureCoords, 2, lineStream);
    } else if (token == groupToken) {
        valid = getToken(lineStream, token);
        checkNewMeshCreation(token);
    } else if (token == faceToken) {
        mRawFaces.clear();
        while(getToken(lineStream, token)) {
            mRawFaces.push_back(token);
        }
        parseRawFaces();
    }
    // Ignore materials for now
    else if (token == mtlToken) {
        valid = getToken(lineStream, token);
        mLastMtl = token;

        mMeshes.back().appendUnfilteredFaces(token);
    }
}

bool ObjLoader::init(const char *fileName) {

    std::ifstream ifs(fileName , std::ifstream::in);
    if (!ifs.good()) {
        fprintf(stderr, "Failed to read file %s.\n", fileName);
        return false;
    }

    mMeshes.clear();

    const uint32_t maxBufferSize = 2048;
    char *buffer = new char[maxBufferSize];

    mMeshes.push_back(ObjMesh());

    std::string token;
    bool isDone = false;
    while(!isDone) {
        ifs.getline(buffer, maxBufferSize);
        if (ifs.good() && ifs.gcount() > 0) {
            handleObjLine(buffer);
        } else {
            isDone = true;
        }
    }

    ifs.close();
    delete buffer;

    reIndexGeometry();

    return true;
}

void ObjLoader::reIndexGeometry() {
    // We want to know where each vertex lands
    mVertexRemap.resize(mObjPositions.size() / mPositionsStride);

    for (uint32_t m = 0; m < mMeshes.size(); m ++) {
        // clear the remap vector of old data
        for (uint32_t r = 0; r < mVertexRemap.size(); r ++) {
            mVertexRemap[r].clear();
        }

        for (uint32_t i = 0; i < mMeshes[m].mUnfilteredFaces.size(); i ++) {
            mMeshes[m].mTriangleLists[i].reserve(mMeshes[m].mUnfilteredFaces[i].size() * 2);
            for (uint32_t fI = 0; fI < mMeshes[m].mUnfilteredFaces[i].size(); fI ++) {
                uint32_t newIndex = reIndexGeometryPrim(mMeshes[m], mMeshes[m].mUnfilteredFaces[i][fI]);
                mMeshes[m].mTriangleLists[i].push_back(newIndex);
            }
        }
    }
}

uint32_t ObjLoader::reIndexGeometryPrim(ObjMesh &mesh, PrimitiveVtx &prim) {

    std::vector<float> &mPositions = mesh.mChannels[0].mData;
    std::vector<float> &mNormals = mesh.mChannels[1].mData;
    std::vector<float> &mTextureCoords = mesh.mChannels[2].mData;

    float posX = mObjPositions[prim.vertIdx * mPositionsStride + 0];
    float posY = mObjPositions[prim.vertIdx * mPositionsStride + 1];
    float posZ = mObjPositions[prim.vertIdx * mPositionsStride + 2];

    float normX = 0.0f;
    float normY = 0.0f;
    float normZ = 0.0f;
    if (prim.normIdx != MAX_INDEX) {
        normX = mObjNormals[prim.normIdx * mNormalsStride + 0];
        normY = mObjNormals[prim.normIdx * mNormalsStride + 1];
        normZ = mObjNormals[prim.normIdx * mNormalsStride + 2];
    }

    float texCoordX = 0.0f;
    float texCoordY = 0.0f;
    if (prim.texIdx != MAX_INDEX) {
        texCoordX = mObjTextureCoords[prim.texIdx * mTextureCoordsStride + 0];
        texCoordY = mObjTextureCoords[prim.texIdx * mTextureCoordsStride + 1];
    }

    std::vector<unsigned int> &ithRemapList = mVertexRemap[prim.vertIdx];
    // We may have some potential vertices we can reuse
    // loop over all the potential candidates and see if any match our guy
    for (unsigned int i = 0; i < ithRemapList.size(); i ++) {

        int ithRemap = ithRemapList[i];
        // compare existing vertex with the new one
        if (mPositions[ithRemap * mPositionsStride + 0] != posX ||
            mPositions[ithRemap * mPositionsStride + 1] != posY ||
            mPositions[ithRemap * mPositionsStride + 2] != posZ) {
            continue;
        }

        // Now go over normals
        if (prim.normIdx != MAX_INDEX) {
            if (mNormals[ithRemap * mNormalsStride + 0] != normX ||
                mNormals[ithRemap * mNormalsStride + 1] != normY ||
                mNormals[ithRemap * mNormalsStride + 2] != normZ) {
                continue;
            }
        }

        // And texcoords
        if (prim.texIdx != MAX_INDEX) {
            if (mTextureCoords[ithRemap * mTextureCoordsStride + 0] != texCoordX ||
                mTextureCoords[ithRemap * mTextureCoordsStride + 1] != texCoordY) {
                continue;
            }
        }

        // If we got here the new vertex is identical to the one that we already stored
        return ithRemap;
    }

    // We did not encounter this vertex yet, store it and return its index
    mPositions.push_back(posX);
    mPositions.push_back(posY);
    mPositions.push_back(posZ);

    if (prim.normIdx != MAX_INDEX) {
        mNormals.push_back(normX);
        mNormals.push_back(normY);
        mNormals.push_back(normZ);
    }

    if (prim.texIdx != MAX_INDEX) {
        mTextureCoords.push_back(texCoordX);
        mTextureCoords.push_back(texCoordY);
    }

    // We need to remember this mapping. Since we are storing floats, not vec3's, need to
    // divide by position size to get the right index
    int currentVertexIndex = (mPositions.size()/mPositionsStride) - 1;
    ithRemapList.push_back(currentVertexIndex);

    return currentVertexIndex;
}
