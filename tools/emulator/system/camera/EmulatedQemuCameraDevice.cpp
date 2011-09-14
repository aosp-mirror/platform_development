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
 * Contains implementation of a class EmulatedQemuCameraDevice that encapsulates
 * an emulated camera device connected to the host.
 */

#define LOG_NDEBUG 0
#define LOG_TAG "EmulatedCamera_QemuDevice"
#include <cutils/log.h>
#include "EmulatedQemuCamera.h"
#include "EmulatedQemuCameraDevice.h"

namespace android {

EmulatedQemuCameraDevice::EmulatedQemuCameraDevice(EmulatedQemuCamera* camera_hal)
    : EmulatedCameraDevice(camera_hal),
      mQemuClient(),
      mPreviewFrame(NULL)
{
}

EmulatedQemuCameraDevice::~EmulatedQemuCameraDevice()
{
    if (mPreviewFrame != NULL) {
        delete[] mPreviewFrame;
    }
}

/****************************************************************************
 * Public API
 ***************************************************************************/

status_t EmulatedQemuCameraDevice::Initialize(const char* device_name)
{
    /* Connect to the service. */
    char connect_str[256];
    snprintf(connect_str, sizeof(connect_str), "name=%s", device_name);
    status_t res = mQemuClient.connectClient(connect_str);
    if (res != NO_ERROR) {
        return res;
    }

    /* Initialize base class. */
    res = EmulatedCameraDevice::Initialize();
    if (res == NO_ERROR) {
        LOGV("%s: Connected to the emulated camera service '%s'",
             __FUNCTION__, device_name);
        mDeviceName = device_name;
    } else {
        mQemuClient.queryDisconnect();
    }

    return res;
}

/****************************************************************************
 * Emulated camera device abstract interface implementation.
 ***************************************************************************/

status_t EmulatedQemuCameraDevice::connectDevice()
{
    LOGV("%s", __FUNCTION__);

    Mutex::Autolock locker(&mObjectLock);
    if (!isInitialized()) {
        LOGE("%s: Qemu camera device is not initialized.", __FUNCTION__);
        return EINVAL;
    }
    if (isConnected()) {
        LOGW("%s: Qemu camera device is already connected.", __FUNCTION__);
        return NO_ERROR;
    }

    const status_t res = mQemuClient.queryConnect();
    if (res == NO_ERROR) {
        LOGV("%s: Connected", __FUNCTION__);
        mState = ECDS_CONNECTED;
    } else {
        LOGE("%s: Connection failed", __FUNCTION__);
    }

    return res;
}

status_t EmulatedQemuCameraDevice::disconnectDevice()
{
    LOGV("%s", __FUNCTION__);

    Mutex::Autolock locker(&mObjectLock);
    if (!isConnected()) {
        LOGW("%s: Qemu camera device is already disconnected.", __FUNCTION__);
        return NO_ERROR;
    }
    if (isCapturing()) {
        LOGE("%s: Cannot disconnect while in the capturing state.", __FUNCTION__);
        return EINVAL;
    }

    const status_t res = mQemuClient.queryDisconnect();
    if (res == NO_ERROR) {
        LOGV("%s: Disonnected", __FUNCTION__);
        mState = ECDS_INITIALIZED;
    } else {
        LOGE("%s: Disconnection failed", __FUNCTION__);
    }

    return res;
}

status_t EmulatedQemuCameraDevice::startDevice()
{
    LOGV("%s", __FUNCTION__);

    if (!isConnected()) {
        LOGE("%s: Qemu camera device is not connected.", __FUNCTION__);
        return EINVAL;
    }
    if (isCapturing()) {
        LOGW("%s: Qemu camera device is already capturing.", __FUNCTION__);
        return NO_ERROR;
    }

    /* Allocate preview frame buffer. */
    /* TODO: Watch out for preview format changes! At this point we implement
     * RGB32 only.*/
    mPreviewFrame = new uint16_t[mTotalPixels * 4];
    if (mPreviewFrame == NULL) {
        LOGE("%s: Unable to allocate %d bytes for preview frame",
             __FUNCTION__, mTotalPixels * 4);
        return ENOMEM;
    }
    memset(mPreviewFrame, 0, mTotalPixels * 4);

    /* Start the actual camera device. */
    status_t res =
        mQemuClient.queryStart(mPixelFormat, mFrameWidth, mFrameHeight);
    if (res == NO_ERROR) {
        /* Start the worker thread. */
        res = startWorkerThread();
        if (res == NO_ERROR) {
            mState = ECDS_CAPTURING;
        } else {
            mQemuClient.queryStop();
        }
    } else {
        LOGE("%s: Start failed", __FUNCTION__);
    }

    return res;
}

status_t EmulatedQemuCameraDevice::stopDevice()
{
    LOGV("%s", __FUNCTION__);

    if (!isCapturing()) {
        LOGW("%s: Qemu camera device is not capturing.", __FUNCTION__);
        return NO_ERROR;
    }

    /* Stop the worker thread first. */
    status_t res = stopWorkerThread();
    if (res == NO_ERROR) {
        /* Stop the actual camera device. */
        res = mQemuClient.queryStop();
        if (res == NO_ERROR) {
            if (mPreviewFrame == NULL) {
                delete[] mPreviewFrame;
                mPreviewFrame = NULL;
            }
            mState = ECDS_CONNECTED;
            LOGV("%s: Stopped", __FUNCTION__);
        } else {
            LOGE("%s: Stop failed", __FUNCTION__);
        }
    } else {
        LOGE("%s: Unable to stop worker thread", __FUNCTION__);
    }

    return res;
}

/****************************************************************************
 * EmulatedCameraDevice virtual overrides
 ***************************************************************************/

status_t EmulatedQemuCameraDevice::getCurrentPreviewFrame(void* buffer)
{
    LOGW_IF(mPreviewFrame == NULL, "%s: No preview frame", __FUNCTION__);
    if (mPreviewFrame != NULL) {
        memcpy(buffer, mPreviewFrame, mTotalPixels * 4);
        return 0;
    } else {
        return EmulatedCameraDevice::getCurrentPreviewFrame(buffer);
    }
}

/****************************************************************************
 * Worker thread management overrides.
 ***************************************************************************/

bool EmulatedQemuCameraDevice::inWorkerThread()
{
    /* Wait till FPS timeout expires, or thread exit message is received. */
    WorkerThread::SelectRes res =
        getWorkerThread()->Select(-1, 1000000 / mEmulatedFPS);
    if (res == WorkerThread::EXIT_THREAD) {
        LOGV("%s: Worker thread has been terminated.", __FUNCTION__);
        return false;
    }

    /* Query frames from the service. */
    status_t query_res = mQemuClient.queryFrame(mCurrentFrame, mPreviewFrame,
                                                 mFrameBufferSize,
                                                 mTotalPixels * 4);
    if (query_res == NO_ERROR) {
        /* Timestamp the current frame, and notify the camera HAL. */
        mCurFrameTimestamp = systemTime(SYSTEM_TIME_MONOTONIC);
        mCameraHAL->onNextFrameAvailable(mCurrentFrame, mCurFrameTimestamp, this);
    } else {
        LOGE("%s: Unable to get current video frame: %s",
             __FUNCTION__, strerror(query_res));
    }

    return true;
}

}; /* namespace android */
