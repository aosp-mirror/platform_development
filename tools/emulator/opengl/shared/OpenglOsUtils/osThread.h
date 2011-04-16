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
#ifndef _OSUTILS_THREAD_H
#define _OSUTILS_THREAD_H

#ifdef _WIN32
#include <windows.h>
#else // !WIN32
#include <pthread.h>
#endif

namespace osUtils {

class Thread
{
public:
    Thread();
    virtual ~Thread();

    virtual int Main() = 0;

    bool start();
    bool  wait(int *exitStatus);
    bool trywait(int *exitStatus);

private:
#ifdef _WIN32
    static DWORD WINAPI thread_main(void *p_arg);
#else // !WIN32
    static void* thread_main(void *p_arg);
#endif

private:
#ifdef _WIN32
    HANDLE m_thread;
    DWORD m_threadId;
#else // !WIN32
    pthread_t m_thread;
    int       m_exitStatus;
    pthread_mutex_t m_lock;
#endif
    bool m_isRunning;
};

} // of namespace osUtils

#endif
