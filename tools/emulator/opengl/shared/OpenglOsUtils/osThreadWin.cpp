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
#include "osThread.h"

namespace osUtils {

Thread::Thread() :
    m_thread(NULL),
    m_threadId(0),
    m_isRunning(false)
{
}

Thread::~Thread()
{
    if(m_thread) {
        CloseHandle(m_thread);
    }
}

bool
Thread::start()
{
    m_isRunning = true;
    m_thread = CreateThread(NULL, 0, &Thread::thread_main, this, 0, &m_threadId);
    if(!m_thread) {
        m_isRunning = false;
    }
    return m_isRunning;
}

bool
Thread::wait(int *exitStatus)
{
    if (!m_isRunning) {
        return false;
    }

    if(WaitForSingleObject(m_thread, INFINITE) == WAIT_FAILED) {
        return false;
    }

    DWORD retval;
    if (!GetExitCodeThread(m_thread,&retval)) {
        return false;
    }

    m_isRunning = 0;

    if (exitStatus) {
        *exitStatus = retval;
    }
    return true;
}

bool
Thread::trywait(int *exitStatus)
{
    if (!m_isRunning) {
        return false;
    }

    if(WaitForSingleObject(m_thread, 0) == WAIT_OBJECT_0) {

        DWORD retval;
        if (!GetExitCodeThread(m_thread,&retval)) {
            return true;
        }

        if (exitStatus) {
            *exitStatus = retval;
        }
        return true;
    }

    return false;
}

DWORD WINAPI
Thread::thread_main(void *p_arg)
{
    Thread *self = (Thread *)p_arg;
    int ret = self->Main();
    self->m_isRunning = false;
    return ret;
}

} // of namespace osUtils
