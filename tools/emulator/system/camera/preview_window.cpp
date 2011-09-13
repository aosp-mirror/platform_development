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
 * Contains implementation of a class PreviewWindow that encapsulates
 * functionality of a preview window set via set_preview_window camera HAL API.
 */

#define LOG_NDEBUG 0
#define LOG_TAG "EmulatedCamera_Preview"
#include <cutils/log.h>
#include <ui/Rect.h>
#include <ui/GraphicBufferMapper.h>
#include "emulated_camera_device.h"
#include "preview_window.h"

namespace android {

PreviewWindow::PreviewWindow()
    : preview_window_(NULL),
      last_previewed_(0),
      preview_frame_width_(0),
      preview_frame_height_(0),
      preview_enabled_(false)
{
}

PreviewWindow::~PreviewWindow()
{
}

/****************************************************************************
 * Camera API
 ***************************************************************************/

status_t PreviewWindow::SetPreviewWindow(struct preview_stream_ops* window,
                                         int preview_fps)
{
    LOGV("%s: current: %p -> new: %p", __FUNCTION__, preview_window_, window);

    status_t res = NO_ERROR;
    Mutex::Autolock locker(&object_lock_);

    /* Reset preview info. */
    preview_frame_width_ = preview_frame_height_ = 0;
    preview_after_ = 0;
    last_previewed_ = 0;

    if (window != NULL) {
        /* The CPU will write each frame to the preview window buffer.
         * Note that we delay setting preview window buffer geometry until
         * frames start to come in. */
        res = window->set_usage(window, GRALLOC_USAGE_SW_WRITE_OFTEN);
        if (res == NO_ERROR) {
            /* Set preview frequency. */
            preview_after_ = 1000000 / preview_fps;
        } else {
            window = NULL;
            res = -res; // set_usage returns a negative errno.
            LOGE("%s: Error setting preview window usage %d -> %s",
                 __FUNCTION__, res, strerror(res));
        }
    }
    preview_window_ = window;

    return res;
}

status_t PreviewWindow::Start()
{
    LOGV("%s", __FUNCTION__);

    Mutex::Autolock locker(&object_lock_);
    preview_enabled_ = true;

    return NO_ERROR;
}

void PreviewWindow::Stop()
{
    LOGV("%s", __FUNCTION__);

    Mutex::Autolock locker(&object_lock_);
    preview_enabled_ = false;
}

bool PreviewWindow::IsEnabled()
{
    Mutex::Autolock locker(&object_lock_);
    return preview_enabled_;
}

/****************************************************************************
 * Public API
 ***************************************************************************/

void PreviewWindow::OnNextFrameAvailable(const void* frame,
                                         nsecs_t timestamp,
                                         EmulatedCameraDevice* camera_dev)
{
    int res;
    Mutex::Autolock locker(&object_lock_);

    if (!preview_enabled_ || preview_window_ == NULL || !IsTimeToPreview()) {
        return;
    }

    /* Make sure that preview window dimensions are OK with the camera device. */
    if (AdjustPreviewDimensions(camera_dev)) {
        /* Need to set / adjust buffer geometry for the preview window.
         * Note that in the emulator preview window uses only RGB for pixel
         * formats. */
        LOGV("%s: Adjusting preview windows %p geometry to %dx%d",
             __FUNCTION__, preview_window_, preview_frame_width_,
             preview_frame_height_);
        res = preview_window_->set_buffers_geometry(preview_window_,
                                                    preview_frame_width_,
                                                    preview_frame_height_,
                                                    HAL_PIXEL_FORMAT_RGBA_8888);
        if (res != NO_ERROR) {
            LOGE("%s: Error in set_buffers_geometry %d -> %s",
                 __FUNCTION__, -res, strerror(-res));
            return;
        }
    }

    /*
     * Push new frame to the preview window.
     */

    /* Dequeue preview window buffer for the frame. */
    buffer_handle_t* buffer = NULL;
    int stride = 0;
    res = preview_window_->dequeue_buffer(preview_window_, &buffer, &stride);
    if (res != NO_ERROR || buffer == NULL) {
        LOGE("%s: Unable to dequeue preview window buffer: %d -> %s",
            __FUNCTION__, -res, strerror(-res));
        return;
    }

    /* Let the preview window to lock the buffer. */
    res = preview_window_->lock_buffer(preview_window_, buffer);
    if (res != NO_ERROR) {
        LOGE("%s: Unable to lock preview window buffer: %d -> %s",
             __FUNCTION__, -res, strerror(-res));
        preview_window_->cancel_buffer(preview_window_, buffer);
        return;
    }

    /* Now let the graphics framework to lock the buffer, and provide
     * us with the framebuffer data address. */
    void* img = NULL;
    const Rect rect(preview_frame_width_, preview_frame_height_);
    GraphicBufferMapper& grbuffer_mapper(GraphicBufferMapper::get());
    res = grbuffer_mapper.lock(*buffer, GRALLOC_USAGE_SW_WRITE_OFTEN, rect, &img);
    if (res != NO_ERROR) {
        LOGE("%s: grbuffer_mapper.lock failure: %d -> %s",
             __FUNCTION__, res, strerror(res));
        preview_window_->cancel_buffer(preview_window_, buffer);
        return;
    }

    /* Frames come in in YV12/NV12/NV21 format. Since preview window doesn't
     * supports those formats, we need to obtain the frame in RGB565. */
    res = camera_dev->GetCurrentPreviewFrame(img);
    if (res == NO_ERROR) {
        /* Show it. */
        preview_window_->enqueue_buffer(preview_window_, buffer);
    } else {
        LOGE("%s: Unable to obtain preview frame: %d", __FUNCTION__, res);
        preview_window_->cancel_buffer(preview_window_, buffer);
    }
    grbuffer_mapper.unlock(*buffer);
}

bool PreviewWindow::AdjustPreviewDimensions(EmulatedCameraDevice* camera_dev)
{
    /* Match the cached frame dimensions against the actual ones. */
    if (preview_frame_width_ == camera_dev->GetFrameWidth() &&
        preview_frame_height_ == camera_dev->GetFrameHeight()) {
        /* They match. */
        return false;
    }

    /* They don't match: adjust the cache. */
    preview_frame_width_ = camera_dev->GetFrameWidth();
    preview_frame_height_ = camera_dev->GetFrameHeight();

    return true;
}

bool PreviewWindow::IsTimeToPreview()
{
    timeval cur_time;
    gettimeofday(&cur_time, NULL);
    const uint64_t cur_mks = cur_time.tv_sec * 1000000LL + cur_time.tv_usec;
    if ((cur_mks - last_previewed_) >= preview_after_) {
        last_previewed_ = cur_mks;
        return true;
    }
    return false;
}

}; /* namespace android */
