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

#ifndef _OBJ_LOADER_H_
#define _OBJ_LOADER_H_

#include <vector>
#include <string>
#include <iostream>
#include <fstream>

#include "GeometryLoader.h"

using namespace android;
using namespace android::renderscript;

#define MAX_INDEX 0xffffffff

class ObjLoader : public GeometryLoader {
public:
    ObjLoader();
    virtual ~ObjLoader() {
    }
    virtual bool init(const char *objFile);

    virtual SimpleMesh *getMesh(uint32_t meshIndex) {
        return &mMeshes[meshIndex];
    }
    virtual uint32_t getNumMeshes() const {
        return mMeshes.size();
    }

private:
    // .obj has a global list of vertex data
    std::vector<float> mObjPositions;
    std::vector<float> mObjNormals;
    std::vector<float> mObjTextureCoords;

    struct PrimitiveVtx {
        uint32_t vertIdx;
        uint32_t normIdx;
        uint32_t texIdx;

        PrimitiveVtx() : vertIdx(MAX_INDEX),
                         normIdx(MAX_INDEX),
                         texIdx(MAX_INDEX){
        }
    };

    // Scratch buffer for faces
    std::vector<std::string> mRawFaces;
    std::vector<PrimitiveVtx> mParsedFaces;
    std::string mLastMtl;

    // Groups are used to separate multiple meshes within the same .obj file
    class ObjMesh : public SimpleMesh {
    public:

        std::vector<std::vector<PrimitiveVtx> > mUnfilteredFaces;

        void appendUnfilteredFaces(std::string name) {
            appendFaceList(name);
            mUnfilteredFaces.push_back(std::vector<PrimitiveVtx>());
            // Reserve some space for index data
            static const uint32_t numReserveIndecies = 128;
            mUnfilteredFaces.back().reserve(numReserveIndecies);
        }

        ObjMesh() {
            appendChannel("position", 3);
            appendChannel("normal", 3);
            appendChannel("texture0", 2);
        }
    };

    std::vector<ObjMesh> mMeshes;
    void checkNewMeshCreation(std::string &newGroup);

    void parseRawFaces();
    void handleObjLine(char *line);

    void reIndexGeometry();
    uint32_t reIndexGeometryPrim(ObjMesh &mesh, PrimitiveVtx &prim);

    unsigned int mPositionsStride;
    unsigned int mNormalsStride;
    unsigned int mTextureCoordsStride;

    // This vector is used to remap a position index into a list
    //  of all divergent vertices
    std::vector<std::vector<unsigned int> > mVertexRemap;
};

#endif //_OBJ_LOADER_H_
