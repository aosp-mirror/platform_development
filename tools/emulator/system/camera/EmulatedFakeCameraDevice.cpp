/*
 * Copyright (C) 2011 The Android Open Source Project
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

/*
 * Contains implementation of a class EmulatedFakeCameraDevice that encapsulates
 * fake camera device.
 */

#define LOG_NDEBUG 0
#define LOG_TAG "EmulatedCamera_FakeDevice"
#include <cutils/log.h>
#include "EmulatedFakeCamera.h"
#include "EmulatedFakeCameraDevice.h"

namespace android {

EmulatedFakeCameraDevice::EmulatedFakeCameraDevice(EmulatedFakeCamera* camera_hal)
    : EmulatedCameraDevice(camera_hal),
      mBlackYUV(kBlack32),
      mWhiteYUV(kWhite32),
      mRedYUV(kRed8),
      mGreenYUV(kGreen8),
      mBlueYUV(kBlue8),
      mCheckX(0),
      mCheckY(0),
      mCcounter(0)
{
}

EmulatedFakeCameraDevice::~EmulatedFakeCameraDevice()
{
}

/****************************************************************************
 * Emulated camera device abstract interface implementation.
 ***************************************************************************/

status_t EmulatedFakeCameraDevice::connectDevice()
{
    LOGV("%s", __FUNCTION__);

    Mutex::Autolock locker(&mObjectLock);
    if (!isInitialized()) {
        LOGE("%s: Fake camera device is not initialized.", __FUNCTION__);
        return EINVAL;
    }
    if (isConnected()) {
        LOGW("%s: Fake camera device is already connected.", __FUNCTION__);
        return NO_ERROR;
    }

    mState = ECDS_CONNECTED;

    return NO_ERROR;
}

status_t EmulatedFakeCameraDevice::disconnectDevice()
{
    LOGV("%s", __FUNCTION__);

    Mutex::Autolock locker(&mObjectLock);
    if (!isConnected()) {
        LOGW("%s: Fake camera device is already disconnected.", __FUNCTION__);
        return NO_ERROR;
    }
    if (isCapturing()) {
        LOGE("%s: Cannot disconnect while in the capturing state.", __FUNCTION__);
        return EINVAL;
    }

    mState = ECDS_INITIALIZED;

    return NO_ERROR;
}

status_t EmulatedFakeCameraDevice::startDevice()
{
    LOGV("%s", __FUNCTION__);

    Mutex::Autolock locker(&mObjectLock);
    if (!isConnected()) {
        LOGE("%s: Fake camera device is not connected.", __FUNCTION__);
        return EINVAL;
    }
    if (isCapturing()) {
        LOGW("%s: Fake camera device is already capturing.", __FUNCTION__);
        return NO_ERROR;
    }

    /* Used in calculating U/V position when drawing the square. */
    mHalfWidth = mFrameWidth / 2;

    /* Just start the worker thread: there is no real device to deal with. */
    const status_t ret = startWorkerThread();
    if (ret == NO_ERROR) {
        mState = ECDS_CAPTURING;
    }

    return ret;
}

status_t EmulatedFakeCameraDevice::stopDevice()
{
    LOGV("%s", __FUNCTION__);

    if (!isCapturing()) {
        LOGW("%s: Fake camera device is not capturing.", __FUNCTION__);
        return NO_ERROR;
    }

    /* Just stop the worker thread: there is no real device to deal with. */
    const status_t ret = stopWorkerThread();
    if (ret == NO_ERROR) {
        mState = ECDS_CONNECTED;
    }

    return ret;
}

/****************************************************************************
 * Worker thread management overrides.
 ***************************************************************************/

bool EmulatedFakeCameraDevice::inWorkerThread()
{
    /* Wait till FPS timeout expires, or thread exit message is received. */
    WorkerThread::SelectRes res =
        getWorkerThread()->Select(-1, 1000000 / mEmulatedFPS);
    if (res == WorkerThread::EXIT_THREAD) {
        LOGV("%s: Worker thread has been terminated.", __FUNCTION__);
        return false;
    }

    /* Lets see if we need to generate a new frame. */
    if ((systemTime(SYSTEM_TIME_MONOTONIC) - mCurFrameTimestamp) >= mRedrawAfter) {
        /*
         * Time to generate a new frame.
         */

        /* Draw the checker board. */
        drawCheckerboard();

        /* Run the square. */
        int x = ((mCcounter * 3) & 255);
        if(x > 128) x = 255 - x;
        int y = ((mCcounter * 5) & 255);
        if(y > 128) y = 255 - y;
        const int size = mFrameWidth / 10;
        drawSquare(x * size / 32, y * size / 32, (size * 5) >> 1,
                   (mCcounter & 0x100) ? &mRedYUV : &mGreenYUV);
        mCcounter++;
    }

    /* Timestamp the current frame, and notify the camera HAL about new frame. */
    mCurFrameTimestamp = systemTime(SYSTEM_TIME_MONOTONIC);
    mCameraHAL->onNextFrameAvailable(mCurrentFrame, mCurFrameTimestamp, this);

    return true;
}

/****************************************************************************
 * Fake camera device private API
 ***************************************************************************/

void EmulatedFakeCameraDevice::drawCheckerboard()
{
    const int size = mFrameWidth / 10;
    bool black = true;

    if((mCheckX / size) & 1)
        black = false;
    if((mCheckY / size) & 1)
        black = !black;

    int county = mCheckY % size;
    int checkxremainder = mCheckX % size;
    uint8_t* Y = mCurrentFrame;
    uint8_t* U_pos = mFrameU;
    uint8_t* V_pos = mFrameV;
    uint8_t* U = U_pos;
    uint8_t* V = V_pos;

    for(int y = 0; y < mFrameHeight; y++) {
        int countx = checkxremainder;
        bool current = black;
        for(int x = 0; x < mFrameWidth; x += 2) {
            if (current) {
                mBlackYUV.get(Y, U, V);
            } else {
                mWhiteYUV.get(Y, U, V);
            }
            Y[1] = *Y;
            Y += 2; U++; V++;
            countx += 2;
            if(countx >= size) {
                countx = 0;
                current = !current;
            }
        }
        if (y & 0x1) {
            U_pos = U;
            V_pos = V;
        } else {
            U = U_pos;
            V = V_pos;
        }
        if(county++ >= size) {
            county = 0;
            black = !black;
        }
    }
    mCheckX += 3;
    mCheckY++;
}

void EmulatedFakeCameraDevice::drawSquare(int x,
                                          int y,
                                          int size,
                                          const YUVPixel* color)
{
    const int half_x = x / 2;
    const int square_xstop = min(mFrameWidth, x+size);
    const int square_ystop = min(mFrameHeight, y+size);
    uint8_t* Y_pos = mCurrentFrame + y * mFrameWidth + x;

    // Draw the square.
    for (; y < square_ystop; y++) {
        const int iUV = (y / 2) * mHalfWidth + half_x;
        uint8_t* sqU = mFrameU + iUV;
        uint8_t* sqV = mFrameV + iUV;
        uint8_t* sqY = Y_pos;
        for (int i = x; i < square_xstop; i += 2) {
            color->get(sqY, sqU, sqV);
            sqY[1] = *sqY;
            sqY += 2; sqU++; sqV++;
        }
        Y_pos += mFrameWidth;
    }
}

}; /* namespace android */
