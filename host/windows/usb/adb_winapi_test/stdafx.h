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

#pragma once

// Modify the following defines if you have to target a platform prior to the ones specified below.
// Refer to MSDN for the latest info on corresponding values for different platforms.
#ifndef WINVER                // Specifies that the minimum required platform is Windows Vista.
#define WINVER 0x0500         // Change this to the appropriate value to target other versions of Windows.
#endif

#ifndef _WIN32_WINNT          // Specifies that the minimum required platform is Windows Vista.
#define _WIN32_WINNT 0x0500   // Change this to the appropriate value to target other versions of Windows.
#endif

#ifndef _WIN32_WINDOWS        // Specifies that the minimum required platform is Windows 98.
#define _WIN32_WINDOWS 0x0410 // Change this to the appropriate value to target Windows Me or later.
#endif

#ifndef _WIN32_IE               // Specifies that the minimum required platform is Internet Explorer 7.0.
#define _WIN32_IE 0x0600        // Change this to the appropriate value to target other versions of IE.
#endif

#ifndef STRICT
#define STRICT
#endif

#include <stdio.h>
#include <tchar.h>

#define _ATL_STATIC_LIB_IMPL
#define _ATL_APARTMENT_THREADED
#define _ATL_NO_AUTOMATIC_NAMESPACE
#define _ATL_CSTRING_EXPLICIT_CONSTRUCTORS      // some CString constructors will be explicit
#define _ATL_ALL_WARNINGS

#include <windows.h>
#include <atlbase.h>

#include <initguid.h>

#pragma warning(disable: 4200)
extern "C" {
#include <usbdi.h>
#include <Setupapi.h>
}
#pragma warning(default: 4200)

extern "C" {
#include "..\api\adb_api.h"
}
