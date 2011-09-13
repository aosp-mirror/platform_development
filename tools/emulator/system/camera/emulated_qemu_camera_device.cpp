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
#include "emulated_qemu_camera.h"
#include "emulated_qemu_camera_device.h"

namespace android {

EmulatedQemuCameraDevice::EmulatedQemuCameraDevice(EmulatedQemuCamera* camera_hal)
    : EmulatedCameraDevice(camera_hal),
      qemu_client_(),
      preview_frame_(NULL)
{
}

EmulatedQemuCameraDevice::~EmulatedQemuCameraDevice()
{
    if (preview_frame_ != NULL) {
        delete[] preview_frame_;
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
    status_t res = qemu_client_.Connect(connect_str);
    if (res != NO_ERROR) {
        return res;
    }

    /* Initialize base class. */
    res = EmulatedCameraDevice::Initialize();
    if (res == NO_ERROR) {
        LOGV("%s: Connected to the emulated camera service '%s'",
             __FUNCTION__, device_name);
        device_name_ = device_name;
    } else {
        qemu_client_.Disconnect();
    }

    return res;
}

/****************************************************************************
 * Emulated camera device abstract interface implementation.
 ***************************************************************************/

status_t EmulatedQemuCameraDevice::Connect()
{
    LOGV("%s", __FUNCTION__);

    Mutex::Autolock locker(&object_lock_);
    if (!IsInitialized()) {
        LOGE("%s: Qemu camera device is not initialized.", __FUNCTION__);
        return EINVAL;
    }
    if (IsConnected()) {
        LOGW("%s: Qemu camera device is already connected.", __FUNCTION__);
        return NO_ERROR;
    }

    const status_t res = qemu_client_.QueryConnect();
    if (res == NO_ERROR) {
        LOGV("%s: Connected", __FUNCTION__);
        state_ = ECDS_CONNECTED;
    } else {
        LOGE("%s: Connection failed", __FUNCTION__);
    }

    return res;
}

status_t EmulatedQemuCameraDevice::Disconnect()
{
    LOGV("%s", __FUNCTION__);

    Mutex::Autolock locker(&object_lock_);
    if (!IsConnected()) {
        LOGW("%s: Qemu camera device is already disconnected.", __FUNCTION__);
        return NO_ERROR;
    }
    if (IsCapturing()) {
        LOGE("%s: Cannot disconnect while in the capturing state.", __FUNCTION__);
        return EINVAL;
    }

    const status_t res = qemu_client_.QueryDisconnect();
    if (res == NO_ERROR) {
        LOGV("%s: Disonnected", __FUNCTION__);
        state_ = ECDS_INITIALIZED;
    } else {
        LOGE("%s: Disconnection failed", __FUNCTION__);
    }

    return res;
}

status_t EmulatedQemuCameraDevice::StartCamera()
{
    LOGV("%s", __FUNCTION__);

    Mutex::Autolock locker(&object_lock_);
    if (!IsConnected()) {
        LOGE("%s: Qemu camera device is not connected.", __FUNCTION__);
        return EINVAL;
    }
    if (IsCapturing()) {
        LOGW("%s: Qemu camera device is already capturing.", __FUNCTION__);
        return NO_ERROR;
    }

    /* Allocate preview frame buffer. */
    /* TODO: Watch out for preview format changes! At this point we implement
     * RGB32 only.*/
    preview_frame_ = new uint16_t[total_pixels_ * 4];
    if (preview_frame_ == NULL) {
        LOGE("%s: Unable to allocate %d bytes for preview frame",
             __FUNCTION__, total_pixels_ * 4);
        return ENOMEM;
    }
    memset(preview_frame_, 0, total_pixels_ * 4);

    /* Start the actual camera device. */
    status_t res =
        qemu_client_.QueryStart(pixel_format_, frame_width_, frame_height_);
    if (res == NO_ERROR) {
        /* Start the worker thread. */
        res = StartWorkerThread();
        if (res == NO_ERROR) {
            state_ = ECDS_CAPTURING;
        } else {
            qemu_client_.QueryStop();
        }
    } else {
        LOGE("%s: Start failed", __FUNCTION__);
    }

    return res;
}

status_t EmulatedQemuCameraDevice::StopCamera()
{
    LOGV("%s", __FUNCTION__);

    Mutex::Autolock locker(&object_lock_);
    if (!IsCapturing()) {
        LOGW("%s: Qemu camera device is not capturing.", __FUNCTION__);
        return NO_ERROR;
    }

    /* Stop the actual camera device. */
    status_t res = qemu_client_.QueryStop();
    if (res == NO_ERROR) {
        /* Stop the worker thread. */
        res = StopWorkerThread();
        if (res == NO_ERROR) {
            if (preview_frame_ == NULL) {
                delete[] preview_frame_;
                preview_frame_ = NULL;
            }
            state_ = ECDS_CONNECTED;
            LOGV("%s: Stopped", __FUNCTION__);
        }
    } else {
        LOGE("%s: Stop failed", __FUNCTION__);
    }

    return res;
}

/****************************************************************************
 * EmulatedCameraDevice virtual overrides
 ***************************************************************************/

status_t EmulatedQemuCameraDevice::GetCurrentPreviewFrame(void* buffer)
{
    LOGW_IF(preview_frame_ == NULL, "%s: No preview frame", __FUNCTION__);
    if (preview_frame_ != NULL) {
        memcpy(buffer, preview_frame_, total_pixels_ * 4);
        return 0;
    } else {
        return EmulatedCameraDevice::GetCurrentPreviewFrame(buffer);
    }
}

/****************************************************************************
 * Worker thread management overrides.
 ***************************************************************************/

bool EmulatedQemuCameraDevice::InWorkerThread()
{
    /* Wait till FPS timeout expires, or thread exit message is received. */
    WorkerThread::SelectRes res =
        worker_thread()->Select(-1, 1000000 / emulated_fps_);
    if (res == WorkerThread::EXIT_THREAD) {
        LOGV("%s: Worker thread has been terminated.", __FUNCTION__);
        return false;
    }

    /* Query frames from the service. */
    status_t query_res = qemu_client_.QueryFrame(current_frame_, preview_frame_,
                                                 framebuffer_size_,
                                                 total_pixels_ * 4);
    if (query_res == NO_ERROR) {
        /* Timestamp the current frame, and notify the camera HAL. */
        timestamp_ = systemTime(SYSTEM_TIME_MONOTONIC);
        camera_hal_->OnNextFrameAvailable(current_frame_, timestamp_, this);
    } else {
        LOGE("%s: Unable to get current video frame: %s",
             __FUNCTION__, strerror(query_res));
    }

    return true;
}

}; /* namespace android */
