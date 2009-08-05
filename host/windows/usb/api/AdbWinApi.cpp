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
#include "adb_api.h"
#include "adb_winusb_api.h"

extern "C" {
int _forceCRTManifest;
int _forceMFCManifest;
int _forceAtlDllManifest;
};

/// References InstantiateWinUsbInterface declared in adb_api.cpp
extern PFN_INSTWINUSBINTERFACE InstantiateWinUsbInterface;

class CAdbWinApiModule : public CAtlDllModuleT< CAdbWinApiModule > {
 public:
  CAdbWinApiModule()
      : CAtlDllModuleT< CAdbWinApiModule >(),
        adbwinusbapi_handle_(NULL),
        is_initialized_(false) {
  }

  ~CAdbWinApiModule() {
    // Unload AdbWinUsbApi.dll before we exit
    if (NULL != adbwinusbapi_handle_) {
      FreeLibrary(adbwinusbapi_handle_);
    }
  }

  /** \brief Loads AdbWinUsbApi.dll and caches its InstantiateWinUsbInterface
    export.

    This method is called from DllMain on DLL_PROCESS_ATTACH event. In this
    method we will check if WINUSB.DLL required by AdbWinUsbApi.dll is
    installed, and if it is we will load AdbWinUsbApi.dll and cache address of
    InstantiateWinUsbInterface routine exported from AdbWinUsbApi.dll
  */
  void AttachToAdbWinUsbApi() {
    // We only need to run this only once.
    if (is_initialized_) {
      return;
    }

    // Just mark that we have ran initialization.
    is_initialized_ = true;

    // Before we can load AdbWinUsbApi.dll we must make sure that WINUSB.DLL
    // has been installed. Build path to the file.
    wchar_t path_to_winusb_dll[MAX_PATH+1];
    if (!GetSystemDirectory(path_to_winusb_dll, MAX_PATH)) {
      return;
    }
    wcscat(path_to_winusb_dll, L"\\WINUSB.DLL");

    if (0xFFFFFFFF == GetFileAttributes(path_to_winusb_dll)) {
      // WINUSB.DLL is not installed. We don't (in fact, can't) load
      // AdbWinUsbApi.dll
      return;
    }

    // WINUSB.DLL is installed. Lets load AdbWinUsbApi.dll and cache its
    // InstantiateWinUsbInterface export.
    // We require that AdbWinUsbApi.dll is located in the same folder
    // where AdbWinApi.dll and adb.exe are located, so by Windows
    // conventions we can pass just module name, and not the full path.
    adbwinusbapi_handle_ = LoadLibrary(L"AdbWinUsbApi.dll");
    if (NULL != adbwinusbapi_handle_) {
      InstantiateWinUsbInterface = reinterpret_cast<PFN_INSTWINUSBINTERFACE>
          (GetProcAddress(adbwinusbapi_handle_, "InstantiateWinUsbInterface"));
    }
  }

 protected:
  /// Handle to the loaded AdbWinUsbApi.dll
  HINSTANCE adbwinusbapi_handle_;

  /// Flags whether or not this module has been initialized.
  bool      is_initialized_;
};

CAdbWinApiModule _AtlModule;

// DLL Entry Point
extern "C" BOOL WINAPI DllMain(HINSTANCE instance,
                               DWORD reason,
                               LPVOID reserved) {
  // Lets see if we need to initialize InstantiateWinUsbInterface
  // variable. We do that only once, on condition that this DLL is
  // being attached to the process and InstantiateWinUsbInterface
  // address has not been calculated yet.
  if (DLL_PROCESS_ATTACH == reason) {
    _AtlModule.AttachToAdbWinUsbApi();
  }
  return _AtlModule.DllMain(reason, reserved);
}
