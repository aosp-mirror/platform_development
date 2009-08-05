/*
 * Copyright (C) 2009 The Android Open Source Project
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

// AdbWinUsbApi.cpp : Implementation of DLL Exports.

#include "stdafx.h"
#include "adb_winusb_interface.h"

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

/** \brief Instantiates interface instance that uses WinUsb API to communicate
  with USB driver.

  This is the only exported routine from this DLL. This routine instantiates an
  object of AdbWinUsbInterfaceObject on request from AdbWinApi.dll when it is
  detected that underlying USB driver is WinUsb.sys.
  @param[in] interface_name Name of the interface.
  @return AdbInterfaceObject - casted instance of AdbWinUsbInterfaceObject
          object on success, or NULL on failure with GetLastError providing
          information on an error that occurred.
*/
extern "C" __declspec(dllexport)
AdbInterfaceObject* __cdecl InstantiateWinUsbInterface(
    const wchar_t* interface_name) {
    // Validate parameter.
    if (NULL == interface_name) {
        return NULL;
    }

    // Instantiate requested object.
    try {
        return new AdbWinUsbInterfaceObject(interface_name);
    } catch (...) {
        // We expect only OOM exceptions here.
        SetLastError(ERROR_OUTOFMEMORY);
        return NULL;
    }
}
