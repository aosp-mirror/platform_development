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
#ifdef WITH_GLES2
#include "GL2Dispatch.h"
#include <stdio.h>
#include <stdlib.h>
#include "osDynLibrary.h"

gl2_decoder_context_t s_gl2;
int                   s_gl2_enabled;

static osUtils::dynLibrary *s_gles2_lib = NULL;

#ifdef _WIN32
#define DEFAULT_GLES_V2_LIB "libGLES_V2_translator"
#elif defined(__APPLE__)
#define DEFAULT_GLES_V2_LIB "libGLES_V2_translator.dylib"
#else
#define DEFAULT_GLES_V2_LIB "libGLES_V2_translator.so"
#endif

//
// This function is called only once during initialiation before
// any thread has been created - hence it should NOT be thread safe.
//
bool init_gl2_dispatch()
{
    const char *libName = getenv("ANDROID_GLESv2_LIB");
    if (!libName) libName = DEFAULT_GLES_V2_LIB;

    //
    // Load the GLES library
    //
    s_gles2_lib = osUtils::dynLibrary::open(libName);
    if (!s_gles2_lib) return false;

    //
    // init the GLES dispatch table
    //
    s_gl2.initDispatchByName( gl2_dispatch_get_proc_func, NULL );
    s_gl2_enabled = true;
    return true;
}

//
// This function is called only during initialiation before
// any thread has been created - hence it should NOT be thread safe.
//
void *gl2_dispatch_get_proc_func(const char *name, void *userData)
{
    if (!s_gles2_lib) {
        return NULL;
    }
    return (void *)s_gles2_lib->findSymbol(name);
}

#endif
