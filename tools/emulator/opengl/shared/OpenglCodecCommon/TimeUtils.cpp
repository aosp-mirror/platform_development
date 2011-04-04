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
#include "TimeUtils.h"

#ifdef _WIN32
#include <Windows.h>
#include <time.h>
#include <stdio.h>
#elif defined(__linux__)
#include <stdlib.h>
#include <sys/time.h>
#include <time.h>
#else
#error "Unsupported platform"
#endif

long long GetCurrentTimeMS()
{
#ifdef _WIN32
    static LARGE_INTEGER freq;
    static bool bNotInit = true;
    if ( bNotInit ) {
        bNotInit = (QueryPerformanceFrequency( &freq ) == FALSE);
    }
    LARGE_INTEGER currVal;
    QueryPerformanceCounter( &currVal );

    return currVal.QuadPart / (freq.QuadPart / 1000);

#elif defined(__linux__)

    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    long long iDiff = (now.tv_sec * 1000LL) + now.tv_nsec/1000000LL;
    return iDiff;

#else

#error "Unsupported platform"

#endif
}
