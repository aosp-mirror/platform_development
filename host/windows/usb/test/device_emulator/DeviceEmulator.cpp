/*
 * Copyright (C) 2006 The Android Open Source Project
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

/** \file
  This file definies entry point for the DLL.
  This project has been created from DDK's SoftUSBLoopback sample project
  that is located at $(DDK_PATH)\src\Test\DSF\USB\SoftUSBLoopback
*/
    
#include "stdafx.h"
#include "resource.h"
#include <dsfif.h>
#include <USBProtocolDefs.h>
#include <softusbif.h>
#include "LoopbackDevice.h"
#include "DeviceEmulator.h"

class CDeviceEmulatorModule : public CAtlDllModuleT<CDeviceEmulatorModule> {
 public: 
  DECLARE_LIBID(LIBID_DeviceEmulatorLib)
  DECLARE_REGISTRY_APPID_RESOURCEID(IDR_DEVICEEMULATOR, "{D1C80253-8DB4-4F72-BF74-270A0EDA1FA9}")
};

CDeviceEmulatorModule _AtlModule;

/////////////////////////////////////////////////////////////////////////////
// DLL Entry Point

extern "C"
BOOL WINAPI DllMain(HINSTANCE hInstance, DWORD dwReason, LPVOID lpReserved) {
  hInstance;
  return _AtlModule.DllMain(dwReason, lpReserved);
}

/////////////////////////////////////////////////////////////////////////////
// Used to determine whether the DLL can be unloaded by OLE

STDAPI DllCanUnloadNow(void) {
  return (_AtlModule.DllCanUnloadNow());
}

/////////////////////////////////////////////////////////////////////////////
// Returns a class factory to create an object of the requested type

STDAPI DllGetClassObject(REFCLSID rclsid, REFIID riid, LPVOID* ppv) {
  return _AtlModule.DllGetClassObject(rclsid, riid, ppv);
}

/////////////////////////////////////////////////////////////////////////////
// DllRegisterServer - Adds entries to the system registry

STDAPI DllRegisterServer(void) {
  // registers object, typelib and all interfaces in typelib
  HRESULT hr =  _AtlModule.DllRegisterServer();
  return hr;
}

/////////////////////////////////////////////////////////////////////////////
// DllUnregisterServer - Removes entries from the system registry

STDAPI DllUnregisterServer(void) {
  HRESULT hr = _AtlModule.DllUnregisterServer();
  return hr;
}





