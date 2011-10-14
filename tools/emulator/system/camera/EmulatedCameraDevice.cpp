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
 * Contains implementation of an abstract class EmulatedCameraDevice that defines
 * functionality expected from an emulated physical camera device:
 *  - Obtaining and setting camera parameters
 *  - Capturing frames
 *  - Streaming video
 *  - etc.
 */

#define LOG_NDEBUG 0
#define LOG_TAG "EmulatedCamera_Device"
#include <cutils/log.h>
#include <sys/select.h>
#include <cmath>
#include "EmulatedCameraDevice.h"
#include "Converters.h"

namespace android {

EmulatedCameraDevice::EmulatedCameraDevice(EmulatedCamera* camera_hal)
    : mObjectLock(),
      mCurFrameTimestamp(0),
      mCameraHAL(camera_hal),
      mCurrentFrame(NULL),
      mExposureCompensation(1.0f),
      mState(ECDS_CONSTRUCTED)
{
}

EmulatedCameraDevice::~EmulatedCameraDevice()
{
    if (mCurrentFrame != NULL) {
        delete[] mCurrentFrame;
    }
}

/****************************************************************************
 * Emulated camera device public API
 ***************************************************************************/

status_t EmulatedCameraDevice::Initialize()
{
    if (isInitialized()) {
        LOGW("%s: Emulated camera device is already initialized: mState = %d",
             __FUNCTION__, mState);
        return NO_ERROR;
    }

    /* Instantiate worker thread object. */
    mWorkerThread = new WorkerThread(this);
    if (getWorkerThread() == NULL) {
        LOGE("%s: Unable to instantiate worker thread object", __FUNCTION__);
        return ENOMEM;
    }

    mState = ECDS_INITIALIZED;

    return NO_ERROR;
}

status_t EmulatedCameraDevice::startDeliveringFrames(bool one_burst)
{
    LOGV("%s", __FUNCTION__);

    if (!isStarted()) {
        LOGE("%s: Device is not started", __FUNCTION__);
        return EINVAL;
    }

    /* Frames will be delivered from the thread routine. */
    const status_t res = startWorkerThread(one_burst);
    LOGE_IF(res != NO_ERROR, "%s: startWorkerThread failed", __FUNCTION__);
    return res;
}

status_t EmulatedCameraDevice::stopDeliveringFrames()
{
    LOGV("%s", __FUNCTION__);

    if (!isStarted()) {
        LOGW("%s: Device is not started", __FUNCTION__);
        return NO_ERROR;
    }

    const status_t res = stopWorkerThread();
    LOGE_IF(res != NO_ERROR, "%s: startWorkerThread failed", __FUNCTION__);
    return res;
}

void EmulatedCameraDevice::setExposureCompensation(const float ev) {
    LOGV("%s", __FUNCTION__);

    if (!isStarted()) {
        LOGW("%s: Fake camera device is not started.", __FUNCTION__);
    }

    mExposureCompensation = std::pow(2.0f, ev);
    LOGV("New exposure compensation is %f", mExposureCompensation);
}

status_t EmulatedCameraDevice::getCurrentPreviewFrame(void* buffer)
{
    if (!isStarted()) {
        LOGE("%s: Device is not started", __FUNCTION__);
        return EINVAL;
    }
    if (mCurrentFrame == NULL || buffer == NULL) {
        LOGE("%s: No framebuffer", __FUNCTION__);
        return EINVAL;
    }

    /* In emulation the framebuffer is never RGB. */
    switch (mPixelFormat) {
        case V4L2_PIX_FMT_YVU420:
            YV12ToRGB32(mCurrentFrame, buffer, mFrameWidth, mFrameHeight);
            return NO_ERROR;
        case V4L2_PIX_FMT_YUV420:
            YU12ToRGB32(mCurrentFrame, buffer, mFrameWidth, mFrameHeight);
            return NO_ERROR;
        case V4L2_PIX_FMT_NV21:
            NV21ToRGB32(mCurrentFrame, buffer, mFrameWidth, mFrameHeight);
            return NO_ERROR;
        case V4L2_PIX_FMT_NV12:
            NV12ToRGB32(mCurrentFrame, buffer, mFrameWidth, mFrameHeight);
            return NO_ERROR;

        default:
            LOGE("%s: Unknown pixel format %.4s",
                 __FUNCTION__, reinterpret_cast<const char*>(&mPixelFormat));
            return EINVAL;
    }
}

/****************************************************************************
 * Emulated camera device private API
 ***************************************************************************/

status_t EmulatedCameraDevice::commonStartDevice(int width,
                                                 int height,
                                                 uint32_t pix_fmt)
{
    /* Validate pixel format, and calculate framebuffer size at the same time. */
    switch (pix_fmt) {
        case V4L2_PIX_FMT_YVU420:
        case V4L2_PIX_FMT_YUV420:
        case V4L2_PIX_FMT_NV21:
        case V4L2_PIX_FMT_NV12:
            mFrameBufferSize = (width * height * 12) / 8;
            break;

        default:
            LOGE("%s: Unknown pixel format %.4s",
                 __FUNCTION__, reinterpret_cast<const char*>(&pix_fmt));
            return EINVAL;
    }

    /* Cache framebuffer info. */
    mFrameWidth = width;
    mFrameHeight = height;
    mPixelFormat = pix_fmt;
    mTotalPixels = width * height;

    /* Allocate framebuffer. */
    mCurrentFrame = new uint8_t[mFrameBufferSize];
    if (mCurrentFrame == NULL) {
        LOGE("%s: Unable to allocate framebuffer", __FUNCTION__);
        return ENOMEM;
    }
    LOGV("%s: Allocated %p %d bytes for %d pixels in %.4s[%dx%d] frame",
         __FUNCTION__, mCurrentFrame, mFrameBufferSize, mTotalPixels,
         reinterpret_cast<const char*>(&mPixelFormat), mFrameWidth, mFrameHeight);
    return NO_ERROR;
}

void EmulatedCameraDevice::commonStopDevice()
{
    mFrameWidth = mFrameHeight = mTotalPixels = 0;
    mPixelFormat = 0;

    if (mCurrentFrame != NULL) {
        delete[] mCurrentFrame;
        mCurrentFrame = NULL;
    }
}

/****************************************************************************
 * Worker thread management.
 ***************************************************************************/

status_t EmulatedCameraDevice::startWorkerThread(bool one_burst)
{
    LOGV("%s", __FUNCTION__);

    if (!isInitialized()) {
        LOGE("%s: Emulated camera device is not initialized", __FUNCTION__);
        return EINVAL;
    }

    const status_t res = getWorkerThread()->startThread(one_burst);
    LOGE_IF(res != NO_ERROR, "%s: Unable to start worker thread", __FUNCTION__);
    return res;
}

status_t EmulatedCameraDevice::stopWorkerThread()
{
    LOGV("%s", __FUNCTION__);

    if (!isInitialized()) {
        LOGE("%s: Emulated camera device is not initialized", __FUNCTION__);
        return EINVAL;
    }

    const status_t res = getWorkerThread()->stopThread();
    LOGE_IF(res != NO_ERROR, "%s: Unable to stop worker thread", __FUNCTION__);
    return res;
}

bool EmulatedCameraDevice::inWorkerThread()
{
    /* This will end the thread loop, and will terminate the thread. Derived
     * classes must override this method. */
    return false;
}

/****************************************************************************
 * Worker thread implementation.
 ***************************************************************************/

status_t EmulatedCameraDevice::WorkerThread::readyToRun()
{
    LOGV("Starting emulated camera device worker thread...");

    LOGW_IF(mThreadControl >= 0 || mControlFD >= 0,
            "%s: Thread control FDs are opened", __FUNCTION__);
    /* Create a pair of FDs that would be used to control the thread. */
    int thread_fds[2];
    if (pipe(thread_fds) == 0) {
        mThreadControl = thread_fds[1];
        mControlFD = thread_fds[0];
        LOGV("Emulated device's worker thread has been started.");
        return NO_ERROR;
    } else {
        LOGE("%s: Unable to create thread control FDs: %d -> %s",
             __FUNCTION__, errno, strerror(errno));
        return errno;
    }
}

status_t EmulatedCameraDevice::WorkerThread::stopThread()
{
    LOGV("Stopping emulated camera device's worker thread...");

    status_t res = EINVAL;
    if (mThreadControl >= 0) {
        /* Send "stop" message to the thread loop. */
        const ControlMessage msg = THREAD_STOP;
        const int wres =
            TEMP_FAILURE_RETRY(write(mThreadControl, &msg, sizeof(msg)));
        if (wres == sizeof(msg)) {
            /* Stop the thread, and wait till it's terminated. */
            res = requestExitAndWait();
            if (res == NO_ERROR) {
                /* Close control FDs. */
                if (mThreadControl >= 0) {
                    close(mThreadControl);
                    mThreadControl = -1;
                }
                if (mControlFD >= 0) {
                    close(mControlFD);
                    mControlFD = -1;
                }
                LOGV("Emulated camera device's worker thread has been stopped.");
            } else {
                LOGE("%s: requestExitAndWait failed: %d -> %s",
                     __FUNCTION__, res, strerror(-res));
            }
        } else {
            LOGE("%s: Unable to send THREAD_STOP message: %d -> %s",
                 __FUNCTION__, errno, strerror(errno));
            res = errno ? errno : EINVAL;
        }
    } else {
        LOGE("%s: Thread control FDs are not opened", __FUNCTION__);
    }

    return res;
}

EmulatedCameraDevice::WorkerThread::SelectRes
EmulatedCameraDevice::WorkerThread::Select(int fd, int timeout)
{
    fd_set fds[1];
    struct timeval tv, *tvp = NULL;

    const int fd_num = (fd >= 0) ? max(fd, mControlFD) + 1 :
                                   mControlFD + 1;
    FD_ZERO(fds);
    FD_SET(mControlFD, fds);
    if (fd >= 0) {
        FD_SET(fd, fds);
    }
    if (timeout) {
        tv.tv_sec = timeout / 1000000;
        tv.tv_usec = timeout % 1000000;
        tvp = &tv;
    }
    int res = TEMP_FAILURE_RETRY(select(fd_num, fds, NULL, NULL, tvp));
    if (res < 0) {
        LOGE("%s: select returned %d and failed: %d -> %s",
             __FUNCTION__, res, errno, strerror(errno));
        return ERROR;
    } else if (res == 0) {
        /* Timeout. */
        return TIMEOUT;
    } else if (FD_ISSET(mControlFD, fds)) {
        /* A control event. Lets read the message. */
        ControlMessage msg;
        res = TEMP_FAILURE_RETRY(read(mControlFD, &msg, sizeof(msg)));
        if (res != sizeof(msg)) {
            LOGE("%s: Unexpected message size %d, or an error %d -> %s",
                 __FUNCTION__, res, errno, strerror(errno));
            return ERROR;
        }
        /* THREAD_STOP is the only message expected here. */
        if (msg == THREAD_STOP) {
            LOGV("%s: THREAD_STOP message is received", __FUNCTION__);
            return EXIT_THREAD;
        } else {
            LOGE("Unknown worker thread message %d", msg);
            return ERROR;
        }
    } else {
        /* Must be an FD. */
        LOGW_IF(fd < 0 || !FD_ISSET(fd, fds), "%s: Undefined 'select' result",
                __FUNCTION__);
        return READY;
    }
}

};  /* namespace android */
