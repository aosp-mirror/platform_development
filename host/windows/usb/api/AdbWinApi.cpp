/*
 * Copyright (C) 2008 The Android Open Source Project
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

// AdbWinApi.cpp : Implementation of DLL Exports.

#include "stdafx.h"

extern "C" {
int _forceCRTManifest;
int _forceMFCManifest;
int _forceAtlDllManifest;
};

class CAdbWinApiModule : public CAtlDllModuleT< CAdbWinApiModule > {
public:
};

CAdbWinApiModule _AtlModule;

// DLL Entry Point
extern "C" BOOL WINAPI DllMain(HINSTANCE instance,
                               DWORD reason,
                               LPVOID reserved) {
    return _AtlModule.DllMain(reason, reserved); 
}
