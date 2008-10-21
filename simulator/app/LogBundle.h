/*
 * Copyright (C) 2008 The Android Open Source Project
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
#ifndef _SIM_LOG_BUNDLE_H
#define _SIM_LOG_BUNDLE_H

#ifdef HAVE_PTHREADS
#include <pthread.h>
#endif
#include <cutils/logd.h> // for android_LogPriority.

#ifdef __cplusplus
extern "C" {
#endif

typedef struct android_LogBundle {
    time_t              when;
    android_LogPriority priority;
    pid_t               pid;
#ifndef HAVE_PTHREADS
    unsigned            tid;
#else    
    pthread_t           tid;
#endif    
    const char*         tag;
    const struct iovec* msgVec;
    size_t              msgCount;
    int                 fd;
} android_LogBundle;

#ifdef __cplusplus
}
#endif

#endif // _SIM_LOG_BUNDLE_H
