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
  Visual Studio generated include file for standard system include files, or
  project specific include files that are used frequently, but are changed
  infrequently.
*/

#pragma once

#ifndef STRICT
#define STRICT
#endif

// Modify the following defines if you have to target a platform prior to the ones specified below.
// Refer to MSDN for the latest info on corresponding values for different platforms.
#ifndef WINVER				// Allow use of features specific to Windows 95 and Windows NT 4 or later.
#define WINVER 0x0500		// Change this to the appropriate value to target Windows 98 and Windows 2000 or later.
#endif

#ifndef _WIN32_WINNT		// Allow use of features specific to Windows NT 4 or later.
#define _WIN32_WINNT 0x0500	// Change this to the appropriate value to target Windows 2000 or later.
#endif						

#ifndef _WIN32_WINDOWS		// Allow use of features specific to Windows 98 or later.
#define _WIN32_WINDOWS 0x0500 // Change this to the appropriate value to target Windows Me or later.
#endif

#ifndef _WIN32_IE			// Allow use of features specific to IE 4.0 or later.
#define _WIN32_IE 0x0501	// Change this to the appropriate value to target IE 5.0 or later.
#endif

// These defines prevent the MS header files from ejecting #pragma comment
// statements with the manifest information of the used ATL, STL, and CRT
#define _ATL_NOFORCE_MANIFEST
#define _STL_NOFORCE_MANIFEST
#define _CRT_NOFORCE_MANIFEST

#define _ATL_APARTMENT_THREADED
#define _ATL_NO_AUTOMATIC_NAMESPACE

#define _ATL_CSTRING_EXPLICIT_CONSTRUCTORS	// some CString constructors will be explicit

// turns off ATL's hiding of some common and often safely ignored warning messages
#define _ATL_ALL_WARNINGS

// #define WIN32_LEAN_AND_MEAN             // Exclude rarely-used stuff from Windows headers

#include <windows.h>
#pragma warning(disable: 4702)
#pragma warning(disable: 4201)
#include <atlbase.h>
#include <winioctl.h>
#include <setupapi.h>
#include <vector>
#include <map>
#include <string>
#pragma warning(default: 4201)
#pragma warning(disable: 4200)
#include <usbdi.h>
#include <usb100.h>

#include "resource.h"

using namespace ATL;
