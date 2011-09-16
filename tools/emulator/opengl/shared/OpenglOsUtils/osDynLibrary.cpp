/*
* Copyright (C) 2011 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#include "osDynLibrary.h"

#ifndef _WIN32
#include <dlfcn.h>
#endif
#include <stdio.h>

namespace osUtils {

dynLibrary *dynLibrary::open(const char *p_libName)
{
    dynLibrary *lib = new dynLibrary();
    if (!lib) {
        return NULL;
    }

#ifdef _WIN32
    lib->m_lib = LoadLibrary(p_libName);
#else // !WIN32
    lib->m_lib = dlopen(p_libName, RTLD_NOW);
#endif

    if (lib->m_lib == NULL) {
        printf("Failed to load %s\n", p_libName);
#ifndef _WIN32
        printf("error %s\n", dlerror()); //only on linux
#endif
        delete lib;
        return NULL;
    }

    return lib;
}

dynLibrary::dynLibrary() :
    m_lib(NULL)
{
}

dynLibrary::~dynLibrary()
{
    if (NULL != m_lib) {
#ifdef _WIN32
        FreeLibrary(m_lib);
#else // !WIN32
        dlclose(m_lib);
#endif
    }
}

dynFuncPtr dynLibrary::findSymbol(const char *p_symName)
{
    if (NULL == m_lib) {
        return NULL;
    }

#ifdef _WIN32
    return (dynFuncPtr) GetProcAddress(m_lib, p_symName);
#else // !WIN32
    return (dynFuncPtr) dlsym(m_lib, p_symName);
#endif
}

} // of namespace osUtils
