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
 * Contains implementation of a class CallbackNotifier that manages callbacks set
 * via set_callbacks, enable_msg_type, and disable_msg_type camera HAL API.
 */

#define LOG_NDEBUG 0
#define LOG_TAG "EmulatedCamera_CallbackNotifier"
#include <cutils/log.h>
#include <media/stagefright/MetadataBufferType.h>
#include "emulated_camera_device.h"
#include "callback_notifier.h"

namespace android {

/* Descriptor for video frame metadata. */
struct VideoFrameMetadata
{
    /* Required field that defines metadata buffer type. */
    MetadataBufferType  type;

    /*
     * TODO: This is taken from a sample code. It seems to work, but requires
     * clarifications on what metadata structure should look like!
     */

    const void*         frame;
    int                 offset;
    camera_memory_t*    holder;
};

/* String representation of camera messages. */
static const char* _camera_messages[] =
{
    "CAMERA_MSG_ERROR",
    "CAMERA_MSG_SHUTTER",
    "CAMERA_MSG_FOCUS",
    "CAMERA_MSG_ZOOM",
    "CAMERA_MSG_PREVIEW_FRAME",
    "CAMERA_MSG_VIDEO_FRAME",
    "CAMERA_MSG_POSTVIEW_FRAME",
    "CAMERA_MSG_RAW_IMAGE",
    "CAMERA_MSG_COMPRESSED_IMAGE",
    "CAMERA_MSG_RAW_IMAGE_NOTIFY",
    "CAMERA_MSG_PREVIEW_METADATA"
};
static const int _camera_messages_num = sizeof(_camera_messages) / sizeof(char*);

/* Builds an array of strings for the given set of messages.
 * Param:
 *  msg - Messages to get strings for,
 *  strings - Array where to save strings
 *  max - Maximum number of entries in the array.
 * Return:
 *  Number of strings saved into the 'strings' array.
 */
static int _GetMessageStrings(uint32_t msg, const char** strings, int max)
{
    int index = 0;
    int out = 0;
    while (msg != 0 && out < max && index < _camera_messages_num) {
        while ((msg & 0x1) == 0 && index < _camera_messages_num) {
            msg >>= 1;
            index++;
        }
        if ((msg & 0x1) != 0 && index < _camera_messages_num) {
            strings[out] = _camera_messages[index];
            out++;
            msg >>= 1;
            index++;
        }
    }

    return out;
}

/* Logs messages, enabled by the mask. */
static void _PrintMessages(uint32_t msg)
{
    const char* strs[_camera_messages_num];
    const int translated = _GetMessageStrings(msg, strs, _camera_messages_num);
    for (int n = 0; n < translated; n++) {
        LOGV("    %s", strs[n]);
    }
}

CallbackNotifier::CallbackNotifier()
    : notify_cb_(NULL),
      data_cb_(NULL),
      data_cb_timestamp_(NULL),
      get_memory_(NULL),
      cb_opaque_(NULL),
      last_frame_(0),
      frame_after_(0),
      message_enabler_(0),
      video_recording_enabled_(false),
      store_meta_data_in_buffers_(true)
{
}

CallbackNotifier::~CallbackNotifier()
{
}

/****************************************************************************
 * Camera API
 ***************************************************************************/

void CallbackNotifier::SetCallbacks(camera_notify_callback notify_cb,
                                    camera_data_callback data_cb,
                                    camera_data_timestamp_callback data_cb_timestamp,
                                    camera_request_memory get_memory,
                                    void* user)
{
    LOGV("%s: %p, %p, %p, %p (%p)",
         __FUNCTION__, notify_cb, data_cb, data_cb_timestamp, get_memory, user);

    Mutex::Autolock locker(&object_lock_);
    notify_cb_ = notify_cb;
    data_cb_ = data_cb;
    data_cb_timestamp_ = data_cb_timestamp;
    get_memory_ = get_memory;
    cb_opaque_ = user;
}

void CallbackNotifier::EnableMessage(uint msg_type)
{
    LOGV("%s: msg_type = 0x%x", __FUNCTION__, msg_type);
    _PrintMessages(msg_type);

    Mutex::Autolock locker(&object_lock_);
    message_enabler_ |= msg_type;
    LOGV("**** Currently enabled messages:");
    _PrintMessages(message_enabler_);
}

void CallbackNotifier::DisableMessage(uint msg_type)
{
    LOGV("%s: msg_type = 0x%x", __FUNCTION__, msg_type);
    _PrintMessages(msg_type);

    Mutex::Autolock locker(&object_lock_);
    message_enabler_ &= ~msg_type;
    LOGV("**** Currently enabled messages:");
    _PrintMessages(message_enabler_);
}

int CallbackNotifier::IsMessageEnabled(uint msg_type)
{
    Mutex::Autolock locker(&object_lock_);
    return message_enabler_ & ~msg_type;
}

status_t CallbackNotifier::EnableVideoRecording(int fps)
{
    LOGV("%s: FPS = %d", __FUNCTION__, fps);

    Mutex::Autolock locker(&object_lock_);
    video_recording_enabled_ = true;
    last_frame_ = 0;
    frame_after_ = 1000000 / fps;

    return NO_ERROR;
}

void CallbackNotifier::DisableVideoRecording()
{
    LOGV("%s:", __FUNCTION__);

    Mutex::Autolock locker(&object_lock_);
    video_recording_enabled_ = false;
    last_frame_ = 0;
    frame_after_ = 0;
}

bool CallbackNotifier::IsVideoRecordingEnabled()
{
    Mutex::Autolock locker(&object_lock_);
    return video_recording_enabled_;
}

void CallbackNotifier::ReleaseRecordingFrame(const void* opaque)
{
    LOGV("%s: frame = %p", __FUNCTION__, opaque);

    if (opaque != NULL) {
        const VideoFrameMetadata* meta =
            reinterpret_cast<const VideoFrameMetadata*>(opaque);
        if (meta->type == kMetadataBufferTypeCameraSource &&
            meta->holder != NULL) {
            meta->holder->release(meta->holder);
        }
    }
}

status_t CallbackNotifier::StoreMetaDataInBuffers(bool enable)
{
    LOGV("%s: %s", __FUNCTION__, enable ? "true" : "false");

    Mutex::Autolock locker(&object_lock_);
    store_meta_data_in_buffers_ = enable;

    /* Return INVALID_OPERATION means HAL does not support metadata. So HAL will
     * return actual frame data with CAMERA_MSG_VIDEO_FRRAME. Return
     * INVALID_OPERATION to mean metadata is not supported. */
    return INVALID_OPERATION;
}

/****************************************************************************
 * Public API
 ***************************************************************************/

void CallbackNotifier::Cleanup()
{
    Mutex::Autolock locker(&object_lock_);
    message_enabler_ = 0;
    notify_cb_ = NULL;
    data_cb_ = NULL;
    data_cb_timestamp_ = NULL;
    get_memory_ = NULL;
    cb_opaque_ = NULL;
    last_frame_ = 0;
    frame_after_ = 0;
    video_recording_enabled_ = false;
    store_meta_data_in_buffers_ = true;
}

void CallbackNotifier::OnNextFrameAvailable(const void* frame,
                                            nsecs_t timestamp,
                                            EmulatedCameraDevice* camera_dev)
{
    Mutex::Autolock locker(&object_lock_);

    if ((message_enabler_ & CAMERA_MSG_VIDEO_FRAME) != 0 &&
        data_cb_timestamp_ != NULL && video_recording_enabled_ &&
        IsTimeForNewVideoFrame()) {
        /* Ready for new video frame. Allocate frame holder. */
        camera_memory_t* holder =
            get_memory_(-1, sizeof(VideoFrameMetadata), 1, NULL);
        if (NULL != holder && NULL != holder->data) {
            if (store_meta_data_in_buffers_) {
                VideoFrameMetadata* meta =
                    reinterpret_cast<VideoFrameMetadata*>(holder->data);
                meta->type = kMetadataBufferTypeCameraSource;
                meta->frame = frame;
                meta->offset = 0;
                meta->holder = holder;
                data_cb_timestamp_(timestamp, CAMERA_MSG_VIDEO_FRAME,
                                   holder, 0, cb_opaque_);
                /* Allocated holder will be released by release_recording_frame
                 * call. */
            } else {
                holder->data = const_cast<void*>(frame);
                data_cb_timestamp_(timestamp, CAMERA_MSG_VIDEO_FRAME,
                                   holder, 0, cb_opaque_);
                holder->release(holder);
            }
        } else {
            LOGE("%s: Memory failure", __FUNCTION__);
        }
    }
}

/****************************************************************************
 * Private API
 ***************************************************************************/

bool CallbackNotifier::IsTimeForNewVideoFrame()
{
    timeval cur_time;
    gettimeofday(&cur_time, NULL);
    const uint64_t cur_mks = cur_time.tv_sec * 1000000LL + cur_time.tv_usec;
    if ((cur_mks - last_frame_) >= frame_after_) {
        last_frame_ = cur_mks;
        return true;
    }
    return false;
}

}; /* namespace android */
