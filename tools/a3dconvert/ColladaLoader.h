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

#ifndef _COLLADA_LOADER_H_
#define _COLLADA_LOADER_H_

#include <vector>

class DAE;
class domLibrary_geometries;
class domGeometry;
class ColladaGeometry;
class SimpleMesh;

#include "GeometryLoader.h"

class ColladaLoader : public GeometryLoader {
public:
    ColladaLoader();
    virtual ~ColladaLoader();

    virtual bool init(const char *colladaFile);
    virtual SimpleMesh *getMesh(uint32_t meshIndex);
    virtual uint32_t getNumMeshes() const {
        return mGeometries.size();
    }
    bool stripGeometryAndSave();

private:
    DAE *mDae;
    void clearGeometry();
    std::vector<ColladaGeometry*> mGeometries;

    bool convertAllGeometry(domLibrary_geometries *allGeometry);
    bool convertGeometry(domGeometry *geometry);

};

#endif