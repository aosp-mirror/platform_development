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
#ifndef _OSUTILS_DYN_LIBRARY_H
#define _OSUTILS_DYN_LIBRARY_H

#ifdef _WIN32
#include <windows.h>
#endif

namespace osUtils {

typedef void (*dynFuncPtr)(void);

class dynLibrary
{
public:
    static dynLibrary *open(const char *p_libName);
    ~dynLibrary();

    dynFuncPtr findSymbol(const char *p_symName);

private:
    dynLibrary();

private:
#ifdef _WIN32
    HMODULE m_lib;
#else
    void *m_lib;
#endif
};

} // of namespace osUtils

#endif
