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


void ThreadInfo::updateInfo(ContextPtr eglCtx,
                            EglDisplay* dpy,
                            GLEScontext* glesCtx,
                            ShareGroupPtr share,
                            ObjectNameManager* manager) {

    eglContext  = eglCtx;
    eglDisplay  = dpy;
    glesContext = glesCtx;
    shareGroup  = share;
    objManager  = manager;
}

#ifdef __linux__

__thread ThreadInfo* thread  = NULL;

ThreadInfo* getThreadInfo(){
    if(!thread) {
        thread = new ThreadInfo();
    }
    return thread;
}

#else

#include <cutils/threads.h>
static thread_store_t s_tls = THREAD_STORE_INITIALIZER;

static void tlsDestruct(void *ptr)
{
    if (ptr) {
        ThreadInfo *ti = (ThreadInfo *)ptr;
        delete ti;
    }
}

ThreadInfo *getThreadInfo()
{
    ThreadInfo *ti = (ThreadInfo *)thread_store_get(&s_tls);
    if (!ti) {
        ti = new ThreadInfo();
        thread_store_set(&s_tls, ti, tlsDestruct);
    }
    return ti;
}

#endif
