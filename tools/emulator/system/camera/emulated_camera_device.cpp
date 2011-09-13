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
#include "emulated_camera_device.h"
#include "converters.h"

namespace android {

EmulatedCameraDevice::EmulatedCameraDevice(EmulatedCamera* camera_hal)
    : object_lock_(),
      timestamp_(0),
      camera_hal_(camera_hal),
      current_frame_(NULL),
      state_(ECDS_CONSTRUCTED)
{
}

EmulatedCameraDevice::~EmulatedCameraDevice()
{
    if (current_frame_ != NULL) {
        delete[] current_frame_;
    }
}

/****************************************************************************
 * Emulated camera device public API
 ***************************************************************************/

status_t EmulatedCameraDevice::Initialize()
{
    LOGV("%s", __FUNCTION__);

    if (IsInitialized()) {
        LOGW("%s: Emulated camera device is already initialized: state_ = %d",
             __FUNCTION__, state_);
        return NO_ERROR;
    }

    /* Instantiate worker thread object. */
    worker_thread_ = new WorkerThread(this);
    if (worker_thread() == NULL) {
        LOGE("%s: Unable to instantiate worker thread object", __FUNCTION__);
        return ENOMEM;
    }

    state_ = ECDS_INITIALIZED;

    return NO_ERROR;
}

status_t EmulatedCameraDevice::StartCapturing(int width,
                                              int height,
                                              uint32_t pix_fmt)
{
    LOGV("%s", __FUNCTION__);

    /* Validate pixel format, and calculate framebuffer size at the same time. */
    switch (pix_fmt) {
        case V4L2_PIX_FMT_YVU420:
            framebuffer_size_ = (width * height * 12) / 8;
            break;

        default:
            LOGE("%s: Unknown pixel format %.4s",
                 __FUNCTION__, reinterpret_cast<const char*>(&pix_fmt));
            return EINVAL;
    }

    /* Cache framebuffer info. */
    frame_width_ = width;
    frame_height_ = height;
    pixel_format_ = pix_fmt;
    total_pixels_ = width * height;

    /* Allocate framebuffer. */
    current_frame_ = new uint8_t[framebuffer_size_];
    if (current_frame_ == NULL) {
        LOGE("%s: Unable to allocate framebuffer", __FUNCTION__);
        return ENOMEM;
    }
    /* Calculate Cb/Cr panes inside the framebuffer. */
    frame_Cb_ = current_frame_ + total_pixels_;
    frame_Cr_ = frame_Cb_ + total_pixels_ / 4;

    /* Start the camera. */
    const status_t res = StartCamera();
    if (res == NO_ERROR) {
        LOGD("Camera device is started:\n"
             "      Framebuffer dimensions: %dx%d.\n"
             "      Pixel format: %.4s",
             frame_width_, frame_height_,
             reinterpret_cast<const char*>(&pixel_format_));
    } else {
        delete[] current_frame_;
        current_frame_ = NULL;
    }

    return res;
}

status_t EmulatedCameraDevice::StopCapturing()
{
    LOGV("%s", __FUNCTION__);

    /* Stop the camera. */
    const status_t res = StopCamera();
    if (res == NO_ERROR) {
        /* Release resources allocated for capturing. */
        if (current_frame_ != NULL) {
            delete[] current_frame_;
            current_frame_ = NULL;
        }
    }

    return res;
}

status_t EmulatedCameraDevice::GetCurrentFrame(void* buffer)
{
    Mutex::Autolock locker(&object_lock_);

    if (!IsCapturing() || current_frame_ == NULL) {
        LOGE("%s is called on a device that is not in the capturing state",
            __FUNCTION__);
        return EINVAL;
    }

    memcpy(buffer, current_frame_, framebuffer_size_);

    return NO_ERROR;
}

status_t EmulatedCameraDevice::GetCurrentPreviewFrame(void* buffer)
{
    Mutex::Autolock locker(&object_lock_);

    if (!IsCapturing() || current_frame_ == NULL) {
        LOGE("%s is called on a device that is not in the capturing state",
            __FUNCTION__);
        return EINVAL;
    }

    /* In emulation the framebuffer is never RGB. */
    switch (pixel_format_) {
        case V4L2_PIX_FMT_YVU420:
            YV12ToRGB32(current_frame_, buffer, frame_width_, frame_height_);
            return NO_ERROR;

        default:
            LOGE("%s: Unknown pixel format %d", __FUNCTION__, pixel_format_);
            return EINVAL;
    }
}

/****************************************************************************
 * Worker thread management.
 ***************************************************************************/

status_t EmulatedCameraDevice::StartWorkerThread()
{
    LOGV("%s", __FUNCTION__);

    if (!IsInitialized()) {
        LOGE("%s: Emulated camera device is not initialized", __FUNCTION__);
        return EINVAL;
    }

    const status_t ret = worker_thread()->Start();
    LOGE_IF(ret != NO_ERROR, "%s: Unable to start worker thread: %d -> %s",
            __FUNCTION__, ret, strerror(ret));

    return ret;
}

status_t EmulatedCameraDevice::StopWorkerThread()
{
    LOGV("%s", __FUNCTION__);

    if (!IsInitialized()) {
        LOGE("%s: Emulated camera device is not initialized", __FUNCTION__);
        return EINVAL;
    }

    worker_thread()->Stop();

    return NO_ERROR;
}

bool EmulatedCameraDevice::InWorkerThread()
{
    /* This will end the thread loop, and will terminate the thread. */
    return false;
}

/****************************************************************************
 * Worker thread implementation.
 ***************************************************************************/

status_t EmulatedCameraDevice::WorkerThread::readyToRun()
{
    LOGV("Starting emulated camera device worker thread...");

    LOGW_IF(thread_control_ >= 0 || control_fd_ >= 0,
            "%s: Thread control FDs are opened", __FUNCTION__);
    /* Create a pair of FDs that would be used to control the thread. */
    int thread_fds[2];
    if (pipe(thread_fds) == 0) {
        thread_control_ = thread_fds[1];
        control_fd_ = thread_fds[0];
        LOGV("Emulated device's worker thread has been started.");
        return NO_ERROR;
    } else {
        LOGE("%s: Unable to create thread control FDs: %d -> %s",
             __FUNCTION__, errno, strerror(errno));
        return errno;
    }
}

status_t EmulatedCameraDevice::WorkerThread::Stop()
{
    LOGV("Stopping emulated camera device's worker thread...");

    status_t res = EINVAL;
    if (thread_control_ >= 0) {
        /* Send "stop" message to the thread loop. */
        const ControlMessage msg = THREAD_STOP;
        const int wres =
            TEMP_FAILURE_RETRY(write(thread_control_, &msg, sizeof(msg)));
        if (wres == sizeof(msg)) {
            /* Stop the thread, and wait till it's terminated. */
            res = requestExitAndWait();
            if (res == NO_ERROR) {
                /* Close control FDs. */
                if (thread_control_ >= 0) {
                    close(thread_control_);
                    thread_control_ = -1;
                }
                if (control_fd_ >= 0) {
                    close(control_fd_);
                    control_fd_ = -1;
                }
                LOGV("Emulated camera device's worker thread has been stopped.");
            } else {
                LOGE("%s: requestExitAndWait failed: %d -> %s",
                     __FUNCTION__, res, strerror(res));
            }
        } else {
            LOGE("%s: Unable to send THREAD_STOP: %d -> %s",
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

    const int fd_num = (fd >= 0) ? max(fd, control_fd_) + 1 :
                                   control_fd_ + 1;
    FD_ZERO(fds);
    FD_SET(control_fd_, fds);
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
    } else if (FD_ISSET(control_fd_, fds)) {
        /* A control event. Lets read the message. */
        ControlMessage msg;
        res = TEMP_FAILURE_RETRY(read(control_fd_, &msg, sizeof(msg)));
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
