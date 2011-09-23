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
    m_thread((pthread_t)NULL),
    m_exitStatus(0),
    m_isRunning(false)
{
    pthread_mutex_init(&m_lock, NULL);
}

Thread::~Thread()
{
    pthread_mutex_destroy(&m_lock);
}

bool
Thread::start()
{
    pthread_mutex_lock(&m_lock);
    m_isRunning = true;
    int ret = pthread_create(&m_thread, NULL, Thread::thread_main, this);
    if(ret) {
        m_isRunning = false;
    }
    pthread_mutex_unlock(&m_lock);
    return m_isRunning;
}

bool
Thread::wait(int *exitStatus)
{
    if (!m_isRunning) {
        return false;
    }

    void *retval;
    if (pthread_join(m_thread,&retval)) {
        return false;
    }

    long long int ret=(long long int)retval;
    if (exitStatus) {
        *exitStatus = (int)ret;
    }
    return true;
}

bool
Thread::trywait(int *exitStatus)
{
    bool ret = false;

    pthread_mutex_lock(&m_lock);
    if (!m_isRunning) {
        *exitStatus = m_exitStatus;
        ret = true;
    }
    pthread_mutex_unlock(&m_lock);
    return ret;
}

void *
Thread::thread_main(void *p_arg)
{
    Thread *self = (Thread *)p_arg;
    void *ret = (void *)self->Main();

    pthread_mutex_lock(&self->m_lock);
    self->m_isRunning = false;
    self->m_exitStatus = (int)ret;
    pthread_mutex_unlock(&self->m_lock);

    return ret;
}

} // of namespace osUtils

