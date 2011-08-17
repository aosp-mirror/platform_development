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

#include <iostream>
#include <vector>

#include "ColladaLoader.h"
#include "ObjLoader.h"
#include <rsContext.h>
#include <rsFileA3D.h>

bool rsdAllocationInit(const Context *rsc, Allocation *alloc, bool forceZero) {
    void * ptr = malloc(alloc->mHal.state.type->getSizeBytes());
    if (!ptr) {
        return false;
    }

    alloc->mHal.drvState.mallocPtr = ptr;
    if (forceZero) {
        memset(ptr, 0, alloc->mHal.state.type->getSizeBytes());
    }
    return true;
}

void rsdAllocationDestroy(const Context *rsc, Allocation *alloc) {
    if (alloc->mHal.drvState.mallocPtr) {
        free(alloc->mHal.drvState.mallocPtr);
        alloc->mHal.drvState.mallocPtr = NULL;
    }
}

// We only care to implement allocation memory initialization and destruction
// because we need no other renderscript hal features for serialization
static RsdHalFunctions FunctionTable = {
    NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    { NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,NULL },
    {
        rsdAllocationInit,
        rsdAllocationDestroy,
        NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
    },
    { NULL, NULL, NULL }, { NULL, NULL, NULL }, { NULL, NULL, NULL },
    { NULL, NULL, NULL }, { NULL, NULL, NULL }, { NULL, NULL },
    { NULL, NULL, NULL},
};

// No-op initizlizer for rs context hal since we only
bool rsdHalInit(Context *rsc, uint32_t version_major, uint32_t version_minor) {
    rsc->mHal.funcs = FunctionTable;
    return true;
}

bool convertToA3D(GeometryLoader *loader, const char *a3dFile) {
    if (!loader->getNumMeshes()) {
        return false;
    }
    // Now write all this stuff out
    Context *rsc = Context::createContextLite();
    rsdHalInit(rsc, 0, 0);
    FileA3D file(rsc);

    for (uint32_t i = 0; i < loader->getNumMeshes(); i ++) {
        Mesh *exportedMesh = loader->getMesh(i)->getRsMesh(rsc);
        file.appendToFile(exportedMesh);
        delete exportedMesh;
    }

    file.writeFile(a3dFile);
    delete rsc;
    return true;
}

int main (int argc, char * const argv[]) {
    const char *objExt = ".obj";
    const char *daeExt = ".dae";

    if(argc != 3 && argc != 4) {
        printf("-----------------------------------------------------------------\n");
        printf("Usage:\n");
        printf("a3dconvert input_file a3d_output_file\n");
        printf("Currently .obj and .dae (collada) input files are accepted\n");
        printf("-----------------------------------------------------------------\n");
        return 1;
    }

    bool isSuccessful = false;

    std::string filename = argv[1];
    size_t dotPos = filename.find_last_of('.');
    if (dotPos == std::string::npos) {
        printf("Invalid input. Currently .obj and .dae (collada) input files are accepted\n");
        return 1;
    }

    bool stripColladaGeo = false;
    GeometryLoader *loader = NULL;
    std::string ext = filename.substr(dotPos);
    if (ext == daeExt) {
        loader = new ColladaLoader();
        if (argc == 4) {
            std::string option = argv[3];
            if (option == "-d") {
                stripColladaGeo = true;
            }
        }
    } else if (ext == objExt) {
        loader = new ObjLoader();
    } else {
        printf("Invalid input. Currently .obj and .dae (collada) input files are accepted\n");
        return 1;
    }

    isSuccessful = loader->init(argv[1]);
    if (isSuccessful) {
        isSuccessful = convertToA3D(loader, argv[2]);
    }

    if (isSuccessful && stripColladaGeo) {
        ColladaLoader *colladaLoader = (ColladaLoader*)loader;
        colladaLoader->stripGeometryAndSave();
    }

    delete loader;

    if(isSuccessful) {
        printf("---All done---\n");
    } else {
        printf("---Encountered errors, conversion failed---\n");
    }

    return isSuccessful ? 0 : 1;
}
