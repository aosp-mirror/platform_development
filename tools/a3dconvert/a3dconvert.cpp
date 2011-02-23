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

int main (int argc, char * const argv[]) {
    const char *objExt = ".obj";
    const char *daeExt = ".dae";

    if(argc != 3) {
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

    std::string ext = filename.substr(dotPos);
    if (ext == daeExt) {
        ColladaLoader converter;
        isSuccessful = converter.init(argv[1]);
        if (isSuccessful) {
            isSuccessful = converter.convertToA3D(argv[2]);
        }
    } else if (ext == objExt) {
        ObjLoader objConv;
        isSuccessful = objConv.init(argv[1]);
        if (isSuccessful) {
            isSuccessful = objConv.convertToA3D(argv[2]);
        }
    } else {
        printf("Invalid input. Currently .obj and .dae (collada) input files are accepted\n");
        return 1;
    }

    if(isSuccessful) {
        printf("---All done---\n");
    } else {
        printf("---Encountered errors, conversion failed---\n");
    }

    return isSuccessful ? 0 : 1;
}
