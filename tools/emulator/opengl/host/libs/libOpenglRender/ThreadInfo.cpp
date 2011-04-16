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
#include "ThreadInfo.h"

#ifdef __linux__

static __thread RenderThreadInfo *tinfo = NULL;

RenderThreadInfo *getRenderThreadInfo()
{
    if (!tinfo) {
        tinfo = new RenderThreadInfo();
    }
    return tinfo;
}

#else

#include <cutils/threads.h>
static thread_store_t s_tls = THREAD_STORE_INITIALIZER;

static void tlsDestruct(void *ptr)
{
    if (ptr) {
        RenderThreadInfo *ti = (RenderThreadInfo *)ptr;
        delete ti;
    }
}

RenderThreadInfo *getRenderThreadInfo()
{
    RenderThreadInfo *ti = (RenderThreadInfo *)thread_store_get(&s_tls);
    if (!ti) {
        ti = new RenderThreadInfo();
        thread_store_set(&s_tls, ti, tlsDestruct);
    }
    return ti;
}
#endif

