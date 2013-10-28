/*
 * Copyright 2013 The Android Open Source Project
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

#ifndef PERFMONITOR_H_
#define PERFMONITOR_H_

#include <jni.h>
#include <errno.h>
#include <time.h>
#include "JNIHelper.h"

const int32_t NUM_SAMPLES = 100;

/******************************************************************
 * Helper class for a performance monitoring and get current tick time
 */
class perfMonitor {
private:
    float _fCurrentFPS;
    time_t _tvLastSec;

    double _dLastTick;
    int32_t  _tickindex;
    double _ticksum;
    double _ticklist[ NUM_SAMPLES ];

    double updateTick(double currentTick);
public:
    perfMonitor();
    virtual ~perfMonitor();

    bool update(float &fFPS);

    static double getCurrentTime()
    {
        struct timeval Time;
        gettimeofday( &Time, NULL );
        double dTime = Time.tv_sec + Time.tv_usec * 1.0/1000000.0;
        return dTime;
    }
};

#endif /* PERFMONITOR_H_ */
