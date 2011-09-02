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
#include "emulated_fake_camera.h"
#include "emulated_fake_camera_device.h"

namespace android {

EmulatedFakeCameraDevice::EmulatedFakeCameraDevice(EmulatedFakeCamera* camera_hal)
    : EmulatedCameraDevice(camera_hal),
      last_redrawn_(0),
      black_YCbCr_(0),
      white_YCbCr_(0xffff),
      red_YCbCr_(kRed),
      green_YCbCr_(kGreen),
      blue_YCbCr_(kBlue),
      check_x_(0),
      check_y_(0),
      counter_(0)
{
}

EmulatedFakeCameraDevice::~EmulatedFakeCameraDevice()
{
}

/****************************************************************************
 * Emulated camera device abstract interface implementation.
 ***************************************************************************/

status_t EmulatedFakeCameraDevice::Connect()
{
    LOGV("%s", __FUNCTION__);

    Mutex::Autolock locker(&object_lock_);
    if (!IsInitialized()) {
        LOGE("%s: Fake camera device is not initialized.", __FUNCTION__);
        return EINVAL;
    }
    if (IsConnected()) {
        LOGW("%s: Fake camera device is already connected.", __FUNCTION__);
        return NO_ERROR;
    }

    state_ = ECDS_CONNECTED;

    return NO_ERROR;
}

status_t EmulatedFakeCameraDevice::Disconnect()
{
    LOGV("%s", __FUNCTION__);

    Mutex::Autolock locker(&object_lock_);
    if (!IsConnected()) {
        LOGW("%s: Fake camera device is already disconnected.", __FUNCTION__);
        return NO_ERROR;
    }
    if (IsCapturing()) {
        LOGE("%s: Cannot disconnect while in the capturing state.", __FUNCTION__);
        return EINVAL;
    }

    state_ = ECDS_INITIALIZED;

    return NO_ERROR;
}

status_t EmulatedFakeCameraDevice::StartCamera()
{
    LOGV("%s", __FUNCTION__);

    Mutex::Autolock locker(&object_lock_);
    if (!IsConnected()) {
        LOGE("%s: Fake camera device is not connected.", __FUNCTION__);
        return EINVAL;
    }
    if (IsCapturing()) {
        LOGW("%s: Fake camera device is already capturing.", __FUNCTION__);
        return NO_ERROR;
    }

    /* Used in calculating Cb/Cr position when drawing the square. */
    half_width_ = frame_width_ / 2;

    /* Just start the worker thread: there is no real device to deal with. */
    const status_t ret = StartWorkerThread();
    if (ret == NO_ERROR) {
        state_ = ECDS_CAPTURING;
    }

    return ret;
}

status_t EmulatedFakeCameraDevice::StopCamera()
{
    LOGV("%s", __FUNCTION__);

    Mutex::Autolock locker(&object_lock_);
    if (!IsCapturing()) {
        LOGW("%s: Fake camera device is not capturing.", __FUNCTION__);
        return NO_ERROR;
    }

    /* Just stop the worker thread: there is no real device to deal with. */
    const status_t ret = StopWorkerThread();
    if (ret == NO_ERROR) {
        state_ = ECDS_CONNECTED;
    }

    return ret;
}

/****************************************************************************
 * Worker thread management overrides.
 ***************************************************************************/

bool EmulatedFakeCameraDevice::InWorkerThread()
{
    /* Wait till FPS timeout expires, or thread exit message is received. */
    WorkerThread::SelectRes res =
        worker_thread()->Select(-1, 1000000 / emulated_fps_);
    if (res == WorkerThread::EXIT_THREAD) {
        LOGV("%s: Worker thread has been terminated.", __FUNCTION__);
        return false;
    }

    /* Lets see if we need to generate a new frame. */
    timeval cur_time;
    gettimeofday(&cur_time, NULL);
    const uint64_t cur_mks = cur_time.tv_sec * 1000000LL + cur_time.tv_usec;
    if ((cur_mks - last_redrawn_) >= redraw_after_) {
        /*
         * Time to generate a new frame.
         */

        /* Draw the checker board. */
        DrawCheckerboard();

        /* Run the square. */
        int x = ((counter_ * 3) & 255);
        if(x > 128) x = 255 - x;
        int y = ((counter_ * 5) & 255);
        if(y > 128) y = 255 - y;
        const int size = frame_width_ / 10;
        DrawSquare(x * size / 32, y * size / 32, (size * 5) >> 1,
                   (counter_ & 0x100) ? &red_YCbCr_ : &green_YCbCr_);
        counter_++;
        last_redrawn_ = cur_mks;
    }

    /* Notify the camera HAL about new frame. */
    camera_hal_->OnNextFrameAvailable(current_frame_,
                                      systemTime(SYSTEM_TIME_MONOTONIC), this);

    return true;
}

/****************************************************************************
 * Fake camera device private API
 ***************************************************************************/

void EmulatedFakeCameraDevice::DrawCheckerboard()
{
    const int size = frame_width_ / 10;
    bool black = true;

    if((check_x_ / size) & 1)
        black = false;
    if((check_y_ / size) & 1)
        black = !black;

    int county = check_y_ % size;
    int checkxremainder = check_x_ % size;
    uint8_t* Y = current_frame_;
    uint8_t* Cb_pos = frame_Cb_;
    uint8_t* Cr_pos = frame_Cr_;
    uint8_t* Cb = Cb_pos;
    uint8_t* Cr = Cr_pos;

    for(int y = 0; y < frame_height_; y++) {
        int countx = checkxremainder;
        bool current = black;
        for(int x = 0; x < frame_width_; x += 2) {
            if (current) {
                black_YCbCr_.get(Y, Cb, Cr);
            } else {
                white_YCbCr_.get(Y, Cb, Cr);
            }
            Y[1] = *Y;
            Y += 2; Cb++; Cr++;
            countx += 2;
            if(countx >= size) {
                countx = 0;
                current = !current;
            }
        }
        if (y & 0x1) {
            Cb_pos = Cb;
            Cr_pos = Cr;
        } else {
            Cb = Cb_pos;
            Cr = Cr_pos;
        }
        if(county++ >= size) {
            county = 0;
            black = !black;
        }
    }
    check_x_ += 3;
    check_y_++;
}

void EmulatedFakeCameraDevice::DrawSquare(int x,
                                          int y,
                                          int size,
                                          const YCbCrPixel* color)
{
    const int half_x = x / 2;
    const int square_xstop = min(frame_width_, x+size);
    const int square_ystop = min(frame_height_, y+size);
    uint8_t* Y_pos = current_frame_ + y * frame_width_ + x;

    // Draw the square.
    for (; y < square_ystop; y++) {
        const int iCbCr = (y / 2) * half_width_ + half_x;
        uint8_t* sqCb = frame_Cb_ + iCbCr;
        uint8_t* sqCr = frame_Cr_ + iCbCr;
        uint8_t* sqY = Y_pos;
        for (int i = x; i < square_xstop; i += 2) {
            color->get(sqY, sqCb, sqCr);
            sqY[1] = *sqY;
            sqY += 2; sqCb++; sqCr++;
        }
        Y_pos += frame_width_;
    }
}

}; /* namespace android */
