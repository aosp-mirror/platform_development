/*
* Copyright 2011 The Android Open Source Project
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

#ifndef _RENDER_API_PLATFORM_TYPES_H
#define _RENDER_API_PLATFORM_TYPES_H

#if defined(_WIN32) || defined(__VC32__) && !defined(__CYGWIN__) && !defined(__SCITECH_SNAP__) /* Win32 and WinCE */
#include <windows.h>

typedef HDC     FBNativeDisplayType;
typedef HWND    FBNativeWindowType;

#elif defined(__linux__)

#include <X11/Xlib.h>
#include <X11/Xutil.h>

typedef Window   FBNativeWindowType;

#elif defined(__APPLE__)

typedef void*   FBNativeWindowType;

#else
#warning "Unsupported platform"
#endif

#endif // of  _RENDER_API_PLATFORM_TYPES_H
