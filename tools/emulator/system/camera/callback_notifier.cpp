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
      video_recording_enabled_(false)
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
    frame_after_ = 1000000000LL / fps;

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
    /* We don't really have anything to release here, since we report video
     * frames by copying them directly to the camera memory. */
}

status_t CallbackNotifier::StoreMetaDataInBuffers(bool enable)
{
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
}

void CallbackNotifier::OnNextFrameAvailable(const void* frame,
                                            nsecs_t timestamp,
                                            EmulatedCameraDevice* camera_dev)
{
    Mutex::Autolock locker(&object_lock_);

    if ((message_enabler_ & CAMERA_MSG_VIDEO_FRAME) != 0 &&
            data_cb_timestamp_ != NULL && video_recording_enabled_ &&
            IsTimeForNewVideoFrame(timestamp)) {
        camera_memory_t* cam_buff =
            get_memory_(-1, camera_dev->GetFrameBufferSize(), 1, NULL);
        if (NULL != cam_buff && NULL != cam_buff->data) {
            memcpy(cam_buff->data, frame, camera_dev->GetFrameBufferSize());
            data_cb_timestamp_(timestamp, CAMERA_MSG_VIDEO_FRAME,
                               cam_buff, 0, cb_opaque_);
        } else {
            LOGE("%s: Memory failure in CAMERA_MSG_VIDEO_FRAME", __FUNCTION__);
        }
    }
}

/****************************************************************************
 * Private API
 ***************************************************************************/

bool CallbackNotifier::IsTimeForNewVideoFrame(nsecs_t timestamp)
{
    if ((timestamp - last_frame_) >= frame_after_) {
        last_frame_ = timestamp;
        return true;
    }
    return false;
}

}; /* namespace android */
