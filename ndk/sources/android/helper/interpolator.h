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

#ifndef INTERPOLATOR_H_
#define INTERPOLATOR_H_

#include <jni.h>
#include <errno.h>
#include <time.h>
#include "JNIHelper.h"
#include "perfMonitor.h"
#include <list>

enum INTERPOLATOR_TYPE
{
    INTERPOLATOR_TYPE_LINEAR,
    INTERPOLATOR_TYPE_EASEINQUAD,
    INTERPOLATOR_TYPE_EASEOUTQUAD,
    INTERPOLATOR_TYPE_EASEINOUTQUAD,
    INTERPOLATOR_TYPE_EASEINCUBIC,
    INTERPOLATOR_TYPE_EASEOUTCUBIC,
    INTERPOLATOR_TYPE_EASEINOUTCUBIC,
    INTERPOLATOR_TYPE_EASEINQUART,
    INTERPOLATOR_TYPE_EASEINEXPO,
    INTERPOLATOR_TYPE_EASEOUTEXPO,
};

struct interpolatorParam {
    float fDestValue;
    INTERPOLATOR_TYPE type;
    double dDuration;
};

/******************************************************************
 * Interpolates values with several interpolation methods
 */
class interpolator {
private:
    double _dStartTime;
    double _dDestTime;
    INTERPOLATOR_TYPE _type;

    float    _fStartValue;
    float    _fDestValue;
    std::list< interpolatorParam > m_listParams;

    float   getFormula(INTERPOLATOR_TYPE type, float t, float b, float d, float c);
public:
    interpolator();
    ~interpolator();

    interpolator& set(const float start,
            const float dest,
            INTERPOLATOR_TYPE type, double duration);

    interpolator& add(const float dest,
            INTERPOLATOR_TYPE type, double duration);

    bool update( const double currentTime, float& p );

    void clear();
};


#endif /* INTERPOLATOR_H_ */
