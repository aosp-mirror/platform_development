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

#ifndef _GEOMETRY_LOADER_H_
#define _GEOMETRY_LOADER_H_

#include "SimpleMesh.h"

class GeometryLoader {
public:
    virtual ~GeometryLoader() {
    }
    virtual bool init(const char *file) = 0;
    virtual uint32_t getNumMeshes() const = 0;
    virtual SimpleMesh *getMesh(uint32_t meshIndex) = 0;
};

#endif _GEOMETRY_LOADER_H_
