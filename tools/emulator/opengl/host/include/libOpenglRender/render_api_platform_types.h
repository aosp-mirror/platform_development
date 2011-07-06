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
