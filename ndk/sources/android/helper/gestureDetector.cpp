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
// gestureDetector.cpp
//--------------------------------------------------------------------------------

//--------------------------------------------------------------------------------
// includes
//--------------------------------------------------------------------------------
#include "gestureDetector.h"

//--------------------------------------------------------------------------------
// GestureDetector
//--------------------------------------------------------------------------------
GestureDetector::GestureDetector()
{
    _fDpFactor = 1.f;
}

void GestureDetector::setConfiguration(AConfiguration* config)
{
    _fDpFactor = 160.f / AConfiguration_getDensity(config);
}

//--------------------------------------------------------------------------------
// TapDetector
//--------------------------------------------------------------------------------
GESTURE_STATE TapDetector::detect(const AInputEvent* motion_event)
{
    if( AMotionEvent_getPointerCount(motion_event) > 1 )
    {
        //Only support single touch
        return false;
    }

    int32_t iAction = AMotionEvent_getAction(motion_event);
    unsigned int flags = iAction & AMOTION_EVENT_ACTION_MASK;
    switch( flags )
    {
    case AMOTION_EVENT_ACTION_DOWN:
        _iDownPointerID = AMotionEvent_getPointerId(motion_event, 0);
        _fDownX = AMotionEvent_getX(motion_event, 0);
        _fDownY = AMotionEvent_getY(motion_event, 0);
        break;
    case AMOTION_EVENT_ACTION_UP:
    {
        int64_t eventTime = AMotionEvent_getEventTime(motion_event);
        int64_t downTime = AMotionEvent_getDownTime(motion_event);
        if( eventTime - downTime <= TAP_TIMEOUT )
        {
            if( _iDownPointerID == AMotionEvent_getPointerId(motion_event, 0) )
            {
                float fX = AMotionEvent_getX(motion_event, 0) - _fDownX;
                float fY = AMotionEvent_getY(motion_event, 0) - _fDownY;
                if( fX * fX + fY * fY < TOUCH_SLOP * TOUCH_SLOP * _fDpFactor )
                {
                    LOGI("TapDetector: Tap detected");
                    return GESTURE_STATE_ACTION;
                }
            }
        }
        break;
    }
    }
    return GESTURE_STATE_NONE;
}

//--------------------------------------------------------------------------------
// DoubletapDetector
//--------------------------------------------------------------------------------
GESTURE_STATE DoubletapDetector::detect(const AInputEvent* motion_event)
{
    if( AMotionEvent_getPointerCount(motion_event) > 1 )
    {
        //Only support single double tap
        return false;
    }

    bool bDetectedTap = _tapDetector.detect(motion_event);

    int32_t iAction = AMotionEvent_getAction(motion_event);
    unsigned int flags = iAction & AMOTION_EVENT_ACTION_MASK;
    switch( flags )
    {
    case AMOTION_EVENT_ACTION_DOWN:
    {
        int64_t eventTime = AMotionEvent_getEventTime(motion_event);
        if( eventTime - _lastTapTime <= DOUBLE_TAP_TIMEOUT )
        {
            float fX = AMotionEvent_getX(motion_event, 0) - _fLastTapX;
            float fY = AMotionEvent_getY(motion_event, 0) - _fLastTapY;
            if( fX * fX + fY * fY < DOUBLE_TAP_SLOP * DOUBLE_TAP_SLOP * _fDpFactor )
            {
                LOGI("DoubletapDetector: Doubletap detected");
                return GESTURE_STATE_ACTION;
            }
        }
        break;
    }
    case AMOTION_EVENT_ACTION_UP:
        if( bDetectedTap )
        {
            _lastTapTime = AMotionEvent_getEventTime(motion_event);
            _fLastTapX = AMotionEvent_getX(motion_event, 0);
            _fLastTapY = AMotionEvent_getY(motion_event, 0);
        }
        break;
    }
    return GESTURE_STATE_NONE;
}

void DoubletapDetector::setConfiguration(AConfiguration* config)
{
    _fDpFactor = 160.f / AConfiguration_getDensity(config);
    _tapDetector.setConfiguration(config);
}

//--------------------------------------------------------------------------------
// PinchDetector
//--------------------------------------------------------------------------------

int32_t PinchDetector::findIndex( const AInputEvent* event, int32_t iID )
{
    int32_t iCount = AMotionEvent_getPointerCount(event);
    for( uint32_t i = 0; i < iCount; ++i )
    {
        if( iID == AMotionEvent_getPointerId(event, i) )
            return i;
    }
    return -1;
}


GESTURE_STATE PinchDetector::detect(const AInputEvent* event)
{
    GESTURE_STATE ret = GESTURE_STATE_NONE;
    int32_t iAction = AMotionEvent_getAction(event);
    uint32_t flags = iAction & AMOTION_EVENT_ACTION_MASK;
    _event = event;

    int32_t iCount = AMotionEvent_getPointerCount(event);
    switch( flags )
    {
    case AMOTION_EVENT_ACTION_DOWN:
        _vecPointers.push_back( AMotionEvent_getPointerId( event, 0 ) );
        break;
    case AMOTION_EVENT_ACTION_POINTER_DOWN:
    {
        int32_t iIndex = (iAction & AMOTION_EVENT_ACTION_POINTER_INDEX_MASK) >> AMOTION_EVENT_ACTION_POINTER_INDEX_SHIFT;
        _vecPointers.push_back(AMotionEvent_getPointerId(event, iIndex));
        if( iCount == 2 )
        {
            //Start new pinch
            ret = GESTURE_STATE_START;
        }
    }
        break;
    case AMOTION_EVENT_ACTION_UP:
        _vecPointers.pop_back();
        break;
    case AMOTION_EVENT_ACTION_POINTER_UP:
    {
        int32_t iIndex = (iAction & AMOTION_EVENT_ACTION_POINTER_INDEX_MASK) >> AMOTION_EVENT_ACTION_POINTER_INDEX_SHIFT;
        int32_t iReleasedPointerID = AMotionEvent_getPointerId(event, iIndex);

        std::vector<int32_t>::iterator it = _vecPointers.begin();
        std::vector<int32_t>::iterator itEnd = _vecPointers.end();
        int32_t i = 0;
        for(;it!=itEnd;++it, ++i)
        {
            if( *it == iReleasedPointerID )
            {
                _vecPointers.erase(it);
                break;
            }
        }

        if( i <= 1 )
        {
            //Reset pinch or drag
            if( iCount != 2 )
            {
                //Start new pinch
                ret = GESTURE_STATE_START | GESTURE_STATE_END;
            }
        }
    }
        break;
    case AMOTION_EVENT_ACTION_MOVE:
        switch(iCount)
        {
        case 1:
            break;
        default:
            //Multi touch
            ret = GESTURE_STATE_MOVE;
            break;
        }
        break;
    case AMOTION_EVENT_ACTION_CANCEL:
        break;
    }


    return ret;
}

bool PinchDetector::getPointers( vec2& v1, vec2& v2 )
{
    if( _vecPointers.size() < 2 )
        return false;

    int32_t iIndex = findIndex( _event, _vecPointers[0] );
    if( iIndex == -1 )
        return false;

    float fX = AMotionEvent_getX( _event, iIndex);
    float fY = AMotionEvent_getY( _event, iIndex);

    iIndex = findIndex( _event, _vecPointers[1] );
    if( iIndex == -1 )
        return false;

    float fX2 = AMotionEvent_getX( _event, iIndex);
    float fY2 = AMotionEvent_getY( _event, iIndex);

    v1 = vec2( fX, fY );
    v2 = vec2( fX2, fY2 );

    return true;
}

//--------------------------------------------------------------------------------
// DragDetector
//--------------------------------------------------------------------------------

int32_t DragDetector::findIndex( const AInputEvent* event, int32_t iID )
{
    int32_t iCount = AMotionEvent_getPointerCount(event);
    for( uint32_t i = 0; i < iCount; ++i )
    {
        if( iID == AMotionEvent_getPointerId(event, i) )
            return i;
    }
    return -1;
}


GESTURE_STATE DragDetector::detect(const AInputEvent* event)
{
    GESTURE_STATE ret = GESTURE_STATE_NONE;
    int32_t iAction = AMotionEvent_getAction(event);
    int32_t iIndex = (iAction & AMOTION_EVENT_ACTION_POINTER_INDEX_MASK) >> AMOTION_EVENT_ACTION_POINTER_INDEX_SHIFT;
    uint32_t flags = iAction & AMOTION_EVENT_ACTION_MASK;
    _event = event;

    int32_t iCount = AMotionEvent_getPointerCount(event);
    switch( flags )
    {
    case AMOTION_EVENT_ACTION_DOWN:
        _vecPointers.push_back( AMotionEvent_getPointerId( event, 0 ) );
        ret = GESTURE_STATE_START;
        break;
    case AMOTION_EVENT_ACTION_POINTER_DOWN:
        _vecPointers.push_back(AMotionEvent_getPointerId(event, iIndex));
        break;
    case AMOTION_EVENT_ACTION_UP:
        _vecPointers.pop_back();
        ret = GESTURE_STATE_END;
        break;
    case AMOTION_EVENT_ACTION_POINTER_UP:
    {
        int32_t iReleasedPointerID = AMotionEvent_getPointerId(event, iIndex);

        std::vector<int32_t>::iterator it = _vecPointers.begin();
        std::vector<int32_t>::iterator itEnd = _vecPointers.end();
        int32_t i = 0;
        for(;it!=itEnd;++it, ++i)
        {
            if( *it == iReleasedPointerID )
            {
                _vecPointers.erase(it);
                break;
            }
        }

        if( i <= 1 )
        {
            //Reset pinch or drag
            if( iCount == 2 )
            {
                ret = GESTURE_STATE_START;
            }
        }
        break;
    }
    case AMOTION_EVENT_ACTION_MOVE:
        switch(iCount)
        {
        case 1:
            //Drag
            ret = GESTURE_STATE_MOVE;
            break;
        default:
            break;
        }
        break;
    case AMOTION_EVENT_ACTION_CANCEL:
        break;
    }

    return ret;
}

bool DragDetector::getPointer( vec2& v )
{
    if( _vecPointers.size() < 1 )
        return false;

    int32_t iIndex = findIndex( _event, _vecPointers[0] );
    if( iIndex == -1 )
        return false;

    float fX = AMotionEvent_getX( _event, iIndex);
    float fY = AMotionEvent_getY( _event, iIndex);

    v = vec2( fX, fY );

    return true;
}

