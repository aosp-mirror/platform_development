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

#include "ColladaLoader.h"
#include "ColladaConditioner.h"
#include "ColladaGeometry.h"

#include <dae.h>
#include <dom/domCOLLADA.h>

ColladaLoader::ColladaLoader() {

}

ColladaLoader::~ColladaLoader() {
    if (mDae) {
        delete mDae;
    }
    clearGeometry();
}

void ColladaLoader::clearGeometry() {
    for (uint32_t i = 0; i < mGeometries.size(); i++) {
        delete mGeometries[i];
    }
    mGeometries.clear();
}

bool ColladaLoader::init(const char *colladaFile) {
    if (mDae) {
        delete mDae;
    }
    clearGeometry();

    mDae = new DAE();

    bool convertSuceeded = true;

    domCOLLADA* root = mDae->open(colladaFile);
    if (!root) {
        fprintf(stderr, "Failed to read file %s.\n", colladaFile);
        return false;
    }

    // We only want to deal with triangulated meshes since rendering complex polygons is not feasible
    ColladaConditioner conditioner;
    conditioner.triangulate(mDae);

    domLibrary_geometries *allGeometry = daeSafeCast<domLibrary_geometries>(root->getDescendant("library_geometries"));

    if (allGeometry) {
        convertSuceeded = convertAllGeometry(allGeometry) && convertSuceeded;
    }

    return convertSuceeded;
}

SimpleMesh *ColladaLoader::getMesh(uint32_t meshIndex) {
    return mGeometries[meshIndex]->getMesh();
}

bool ColladaLoader::convertAllGeometry(domLibrary_geometries *allGeometry) {

    bool convertSuceeded = true;
    domGeometry_Array &geo_array = allGeometry->getGeometry_array();
    for (size_t i = 0; i < geo_array.getCount(); i++) {
        domGeometry *geometry = geo_array[i];
        const char *geometryName = geometry->getName();
        if (geometryName == NULL) {
            geometryName = geometry->getId();
        }

        domMeshRef mesh = geometry->getMesh();
        if (mesh != NULL) {
            printf("Converting geometry: %s\n", geometryName);
            convertSuceeded = convertGeometry(geometry) && convertSuceeded;
        } else {
            printf("Skipping geometry: %s, unsupported type\n", geometryName);
        }

    }

    return convertSuceeded;
}

bool ColladaLoader::convertGeometry(domGeometry *geometry) {
    bool convertSuceeded = true;

    domMeshRef mesh = geometry->getMesh();

    ColladaGeometry *convertedGeo = new ColladaGeometry();
    convertedGeo->init(geometry);

    mGeometries.push_back(convertedGeo);

    return convertSuceeded;
}

bool ColladaLoader::stripGeometryAndSave() {

    ColladaConditioner conditioner;
    bool convertSuceeded = conditioner.stripGeometry(mDae);

    mDae->writeAll();
    if(!convertSuceeded) {
        printf("Encountered errors\n");
    } else {
        printf("Stripped geometry data from collada file\n");
    }

    return convertSuceeded;
}
