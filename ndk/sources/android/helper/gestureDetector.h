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

//--------------------------------------------------------------------------------
// gestureDetector.h
//--------------------------------------------------------------------------------

#ifndef GESTUREDETECTOR_H_
#define GESTUREDETECTOR_H_

#include <vector>

#include <android/sensor.h>
#include <android/log.h>
#include <android_native_app_glue.h>
#include <android/native_window_jni.h>
#include "JNIHelper.h"
#include "vecmath.h"

//--------------------------------------------------------------------------------
// Constants
//--------------------------------------------------------------------------------
const int32_t DOUBLE_TAP_TIMEOUT = 300 * 1000000;
const int32_t TAP_TIMEOUT = 180 * 1000000;
const int32_t DOUBLE_TAP_SLOP = 100;
const int32_t TOUCH_SLOP = 8;

#define GESTURE_STATE_NONE (0)
#define GESTURE_STATE_START (1)
#define GESTURE_STATE_MOVE (2)
#define GESTURE_STATE_END (4)
#define GESTURE_STATE_ACTION (GESTURE_STATE_START | GESTURE_STATE_END)
typedef int32_t GESTURE_STATE;

/******************************************************************
 * Base class of Gesture Detectors
 * GestureDetectors handles input events and detect gestures
 * Note that different detectors may detect gestures with an event at
 * same time. The caller needs to manage gesture priority accordingly
 *
 */
class GestureDetector
{
protected:
    float _fDpFactor;
public:
    GestureDetector();
    virtual ~GestureDetector() {}
    virtual void setConfiguration(AConfiguration* config);

    virtual GESTURE_STATE detect(const AInputEvent* motion_event) = 0;
};

/******************************************************************
 * Tap gesture detector
 * Returns GESTURE_STATE_ACTION when a tap gesture is detected
 *
 */
class TapDetector : public GestureDetector
{
private:
    int32_t _iDownPointerID;
    float _fDownX;
    float _fDownY;
public:
    TapDetector() {}
    virtual ~TapDetector() {}
    virtual GESTURE_STATE detect(const AInputEvent* motion_event);
};

/******************************************************************
 * Pinch gesture detector
 * Returns GESTURE_STATE_ACTION when a double-tap gesture is detected
 *
 */
class DoubletapDetector : public GestureDetector
{
private:
    TapDetector _tapDetector;
    int64_t _lastTapTime;
    float _fLastTapX;
    float _fLastTapY;

public:
    DoubletapDetector() {}
    virtual ~DoubletapDetector() {}
    virtual GESTURE_STATE detect(const AInputEvent* motion_event);
    virtual void setConfiguration(AConfiguration* config);
};

/******************************************************************
 * Double gesture detector
 * Returns pinch gesture state when a pinch gesture is detected
 * The class handles multiple touches more than 2
 * When the finger 1,2,3 are tapped and then finger 1 is released,
 * the detector start new pinch gesture with finger 2 & 3.
 */
class PinchDetector : public GestureDetector
{
private:
    int32_t findIndex( const AInputEvent* event, int32_t iID );
    const AInputEvent* _event;
    std::vector<int32_t> _vecPointers;

public:
    PinchDetector() {}
    virtual ~PinchDetector() {}
    virtual GESTURE_STATE detect(const AInputEvent* event);
    bool getPointers( vec2& v1, vec2& v2 );
};

/******************************************************************
 * Drag gesture detector
 * Returns drag gesture state when a drag-tap gesture is detected
 *
 */
class DragDetector : public GestureDetector
{
private:
    int32_t findIndex( const AInputEvent* event, int32_t iID );
    const AInputEvent* _event;
    std::vector<int32_t> _vecPointers;
public:
    DragDetector() {}
    virtual ~DragDetector() {}
    virtual GESTURE_STATE detect(const AInputEvent* event);
    bool getPointer( vec2& v );
};

#endif /* GESTUREDETECTOR_H_ */
