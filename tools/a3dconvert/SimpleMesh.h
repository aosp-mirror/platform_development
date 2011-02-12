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

#ifndef _SIMPLE_MESH_H_
#define _SIMPLE_MESH_H_

#include <rsContext.h>
#include <rsMesh.h>
using namespace android;
using namespace android::renderscript;

class SimpleMesh {
public:
    struct Channel {
        std::vector<float> mData;
        std::string mName;
        uint32_t mStride;
    };

    // Vertex channels (position, normal)
    // This assumes all the data array are the same size
    std::vector<Channel> mChannels;

    // Triangle list index data
    std::vector<std::vector<uint32_t> > mTriangleLists;
    // Names of all the triangle lists
    std::vector<std::string> mTriangleListNames;
    // Name of the entire object
    std::string mName;

    // Adds another index set to the mesh
    void appendFaceList(std::string name) {
        mTriangleListNames.push_back(name);
        mTriangleLists.push_back(std::vector<uint32_t>());
    }

    // Adds another data channel (position, normal, etc.)
    void appendChannel(std::string name, uint32_t stride) {
        mChannels.push_back(Channel());
        static const uint32_t reserveVtx = 128;
        mChannels.back().mData.reserve(reserveVtx*stride);
        mChannels.back().mName = name;
        mChannels.back().mStride = stride;
    }

    SimpleMesh() {
        // reserve some data in the vectors
        // simply letting it grow by itself tends to waste a lot of time on
        // rallocations / copies when dealing with geometry data
        static const uint32_t reserveFaces = 8;
        static const uint32_t reserveChannels = 8;
        mTriangleLists.reserve(reserveFaces);
        mTriangleListNames.reserve(reserveFaces);
        mChannels.reserve(reserveChannels);
    }

    // Generates a renderscript mesh that could be used for a3d serialization
    Mesh *getMesh(Context *rsc) {
        if (mChannels.size() == 0) {
            return NULL;
        }

        // Generate the element that describes our channel layout
        rsc->mStateElement.elementBuilderBegin();
        for (uint32_t c = 0; c < mChannels.size(); c ++) {
            // Skip empty channels
            if (mChannels[c].mData.size() == 0) {
                continue;
            }
            const Element *subElem = Element::create(rsc, RS_TYPE_FLOAT_32, RS_KIND_USER, false, mChannels[c].mStride);
            rsc->mStateElement.elementBuilderAdd(subElem, mChannels[c].mName.c_str(), 1);
        }
        const Element *vertexDataElem = rsc->mStateElement.elementBuilderCreate(rsc);

        uint32_t numVerts = mChannels[0].mData.size()/mChannels[0].mStride;
        Type *vertexDataType = Type::getType(rsc, vertexDataElem, numVerts, 0, 0, false, false);
        vertexDataType->compute();

        Allocation *vertexAlloc = new Allocation(rsc, vertexDataType, RS_ALLOCATION_USAGE_SCRIPT);

        uint32_t vertexSize = vertexDataElem->getSizeBytes()/sizeof(float);
        // Fill this allocation with some data
        float *dataPtr = (float*)vertexAlloc->getPtr();
        for (uint32_t i = 0; i < numVerts; i ++) {
            // Find the pointer to the current vertex's data
            uint32_t vertexPos = i*vertexSize;
            float *vertexPtr = dataPtr + vertexPos;

            for (uint32_t c = 0; c < mChannels.size(); c ++) {
                // Skip empty channels
                if (mChannels[c].mData.size() == 0) {
                    continue;
                }
                for (uint32_t cStride = 0; cStride < mChannels[c].mStride; cStride ++) {
                    *(vertexPtr++) = mChannels[c].mData[i * mChannels[c].mStride + cStride];
                }
            }
        }

        // Now lets write index data
        const Element *indexElem = Element::create(rsc, RS_TYPE_UNSIGNED_16, RS_KIND_USER, false, 1);

        Mesh *mesh = new Mesh(rsc);
        mesh->setName(mName.c_str());
        mesh->mVertexBufferCount = 1;
        mesh->mVertexBuffers = new ObjectBaseRef<Allocation>[1];
        mesh->mVertexBuffers[0].set(vertexAlloc);

        mesh->mPrimitivesCount = mTriangleLists.size();
        mesh->mPrimitives = new Mesh::Primitive_t *[mesh->mPrimitivesCount];

        // load all primitives
        for (uint32_t pCount = 0; pCount < mesh->mPrimitivesCount; pCount ++) {
            Mesh::Primitive_t *prim = new Mesh::Primitive_t;
            mesh->mPrimitives[pCount] = prim;

            uint32_t numIndicies = mTriangleLists[pCount].size();
            Type *indexType = Type::getType(rsc, indexElem, numIndicies, 0, 0, false, false );

            indexType->compute();

            Allocation *indexAlloc = new Allocation(rsc, indexType, RS_ALLOCATION_USAGE_SCRIPT);
            uint16_t *indexPtr = (uint16_t*)indexAlloc->getPtr();
            const std::vector<uint32_t> &indexList = mTriangleLists[pCount];
            uint32_t numTries = numIndicies / 3;

            for (uint32_t i = 0; i < numTries; i ++) {
                indexPtr[i * 3 + 0] = (uint16_t)indexList[i * 3 + 0];
                indexPtr[i * 3 + 1] = (uint16_t)indexList[i * 3 + 1];
                indexPtr[i * 3 + 2] = (uint16_t)indexList[i * 3 + 2];
            }
            indexAlloc->setName(mTriangleListNames[pCount].c_str());
            prim->mIndexBuffer.set(indexAlloc);
            prim->mPrimitive = RS_PRIMITIVE_TRIANGLE;
        }

        return mesh;
    }
};

#endif
