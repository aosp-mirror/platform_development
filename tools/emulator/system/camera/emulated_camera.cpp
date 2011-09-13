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
 * Contains implementation of a class EmulatedCamera that encapsulates
 * functionality common to all emulated cameras ("fake", "webcam", "video file",
 * etc.). Instances of this class (for each emulated camera) are created during
 * the construction of the EmulatedCameraFactory instance. This class serves as
 * an entry point for all camera API calls that defined by camera_device_ops_t
 * API.
 */

#define LOG_NDEBUG 0
#define LOG_TAG "EmulatedCamera_Camera"
#include <cutils/log.h>
#include <ui/Rect.h>
#include "emulated_camera.h"
#include "emulated_fake_camera_device.h"
#include "converters.h"

/* Defines whether we should trace parameter changes. */
#define DEBUG_PARAM 1

namespace android {

#if DEBUG_PARAM
/* Calculates and logs parameter changes.
 * Param:
 *  current - Current set of camera parameters.
 *  new_par - String representation of new parameters.
 */
static void _PrintParamDiff(const CameraParameters& current, const char* new_par);
#else
#define _PrintParamDiff(current, new_par)   (void(0))
#endif  /* DEBUG_PARAM */

/* A helper routine that adds a value to the camera parameter.
 * Param:
 *  param - Camera parameter to add a value to.
 *  val - Value to add.
 * Return:
 *  A new string containing parameter with the added value on success, or NULL on
 *  a failure. If non-NULL string is returned, the caller is responsible for
 *  freeing it with 'free'.
 */
static char* _AddValue(const char* param, const char* val);

EmulatedCamera::EmulatedCamera(int cameraId, struct hw_module_t* module)
        : preview_window_(),
          callback_notifier_(),
          camera_id_(cameraId)
{
    /*
     * Initialize camera_device descriptor for this object.
     */

    /* Common header */
    common.tag = HARDWARE_DEVICE_TAG;
    common.version = 0;
    common.module = module;
    common.close = EmulatedCamera::close;

    /* camera_device fields. */
    ops = &device_ops_;
    priv = this;
}

EmulatedCamera::~EmulatedCamera()
{
}

/****************************************************************************
 * Public API
 ***************************************************************************/

status_t EmulatedCamera::Initialize()
{
    LOGV("%s", __FUNCTION__);

    /*
     * Fake required parameters.
     */

    /* Only RGBX are supported by the framework for preview window in the emulator! */
    parameters_.set(CameraParameters::KEY_SUPPORTED_PREVIEW_FORMATS, CameraParameters::PIXEL_FORMAT_RGBA8888);
    parameters_.set(CameraParameters::KEY_SUPPORTED_PREVIEW_FRAME_RATES, "60,50,25,15,10");
    parameters_.set(CameraParameters::KEY_SUPPORTED_PREVIEW_FPS_RANGE, "(10,60)");
    parameters_.set(CameraParameters::KEY_PREVIEW_FPS_RANGE, "10,60");
    parameters_.set(CameraParameters::KEY_SUPPORTED_JPEG_THUMBNAIL_SIZES, "320x240,0x0");
    parameters_.set(CameraParameters::KEY_MAX_EXPOSURE_COMPENSATION, "6");
    parameters_.set(CameraParameters::KEY_MIN_EXPOSURE_COMPENSATION, "-6");
    parameters_.set(CameraParameters::KEY_EXPOSURE_COMPENSATION_STEP, "0.5");
    parameters_.set(CameraParameters::KEY_JPEG_THUMBNAIL_WIDTH, "512");
    parameters_.set(CameraParameters::KEY_JPEG_THUMBNAIL_HEIGHT, "384");
    parameters_.set(CameraParameters::KEY_JPEG_QUALITY, "90");
    parameters_.set(CameraParameters::KEY_FOCAL_LENGTH, "4.31");
    parameters_.set(CameraParameters::KEY_HORIZONTAL_VIEW_ANGLE, "54.8");
    parameters_.set(CameraParameters::KEY_VERTICAL_VIEW_ANGLE, "42.5");
    parameters_.set(CameraParameters::KEY_JPEG_THUMBNAIL_QUALITY, "90");

    /* Only RGB formats are supported by preview window in emulator. */
    parameters_.setPreviewFormat(CameraParameters::PIXEL_FORMAT_RGBA8888);

    /* We don't relay on the actual frame rates supported by the camera device,
     * since we will emulate them through timeouts in the emulated camera device
     * worker thread. */
    parameters_.set(CameraParameters::KEY_SUPPORTED_PREVIEW_FRAME_RATES,
                    "30,24,20,15,10,5");
    parameters_.set(CameraParameters::KEY_SUPPORTED_PREVIEW_FPS_RANGE, "(5,30)");
    parameters_.set(CameraParameters::KEY_PREVIEW_FPS_RANGE, "5,30");
    parameters_.setPreviewFrameRate(24);

    /* Only PIXEL_FORMAT_YUV420P is accepted by camera framework in emulator! */
    parameters_.set(CameraParameters::KEY_VIDEO_FRAME_FORMAT,
                    CameraParameters::PIXEL_FORMAT_YUV420P);
    parameters_.set(CameraParameters::KEY_SUPPORTED_PICTURE_FORMATS,
                    CameraParameters::PIXEL_FORMAT_YUV420P);
    parameters_.setPictureFormat(CameraParameters::PIXEL_FORMAT_YUV420P);

    /*
     * Not supported features
     */

    parameters_.set(CameraParameters::KEY_SUPPORTED_FOCUS_MODES, CameraParameters::FOCUS_MODE_FIXED);
    parameters_.set(CameraParameters::KEY_FOCUS_MODE, CameraParameters::FOCUS_MODE_FIXED);

    return NO_ERROR;
}

void EmulatedCamera::OnNextFrameAvailable(const void* frame,
                                          nsecs_t timestamp,
                                          EmulatedCameraDevice* camera_dev)
{
    /* Notify the preview window first. */
    preview_window_.OnNextFrameAvailable(frame, timestamp, camera_dev);

    /* Notify callback notifier next. */
    callback_notifier_.OnNextFrameAvailable(frame, timestamp, camera_dev);
}

/****************************************************************************
 * Camera API implementation.
 ***************************************************************************/

status_t EmulatedCamera::Connect(hw_device_t** device)
{
    LOGV("%s", __FUNCTION__);

    status_t res = EINVAL;
    EmulatedCameraDevice* const camera_dev = GetCameraDevice();
    LOGE_IF(camera_dev == NULL, "%s: No camera device instance.", __FUNCTION__);

    if (camera_dev != NULL) {
        /* Connect to the camera device. */
        res = GetCameraDevice()->Connect();
        if (res == NO_ERROR) {
            *device = &common;
        }
    }

    return -res;
}

status_t EmulatedCamera::Close()
{
    LOGV("%s", __FUNCTION__);

    return Cleanup();
}

status_t EmulatedCamera::GetCameraInfo(struct camera_info* info)
{
    LOGV("%s", __FUNCTION__);

    const char* valstr = NULL;

    valstr = parameters_.get(EmulatedCamera::FACING_KEY);
    if (valstr != NULL) {
        if (strcmp(valstr, EmulatedCamera::FACING_FRONT) == 0) {
            info->facing = CAMERA_FACING_FRONT;
        }
        else if (strcmp(valstr, EmulatedCamera::FACING_BACK) == 0) {
            info->facing = CAMERA_FACING_BACK;
        }
    } else {
        info->facing = CAMERA_FACING_BACK;
    }

    valstr = parameters_.get(EmulatedCamera::ORIENTATION_KEY);
    if (valstr != NULL) {
        info->orientation = atoi(valstr);
    } else {
        info->orientation = 0;
    }

    return NO_ERROR;
}

status_t EmulatedCamera::SetPreviewWindow(struct preview_stream_ops* window)
{
    /* Callback should return a negative errno. */
    return -preview_window_.SetPreviewWindow(window,
                                             parameters_.getPreviewFrameRate());
}

void EmulatedCamera::SetCallbacks(camera_notify_callback notify_cb,
                                  camera_data_callback data_cb,
                                  camera_data_timestamp_callback data_cb_timestamp,
                                  camera_request_memory get_memory,
                                  void* user)
{
    callback_notifier_.SetCallbacks(notify_cb, data_cb, data_cb_timestamp,
                                    get_memory, user);
}

void EmulatedCamera::EnableMsgType(int32_t msg_type)
{
    callback_notifier_.EnableMessage(msg_type);
}

void EmulatedCamera::DisableMsgType(int32_t msg_type)
{
    callback_notifier_.DisableMessage(msg_type);
}

int EmulatedCamera::MsgTypeEnabled(int32_t msg_type)
{
    return callback_notifier_.IsMessageEnabled(msg_type);
}

status_t EmulatedCamera::StartPreview()
{
    LOGV("%s", __FUNCTION__);

    /* Callback should return a negative errno. */
    return -DoStartPreview();
}

void EmulatedCamera::StopPreview()
{
    LOGV("%s", __FUNCTION__);

    DoStopPreview();
}

int EmulatedCamera::PreviewEnabled()
{
    return preview_window_.IsEnabled();
}

status_t EmulatedCamera::StoreMetaDataInBuffers(int enable)
{
    /* Callback should return a negative errno. */
    return -callback_notifier_.StoreMetaDataInBuffers(enable);
}

status_t EmulatedCamera::StartRecording()
{
    /* Callback should return a negative errno. */
    return -callback_notifier_.EnableVideoRecording(parameters_.getPreviewFrameRate());
}

void EmulatedCamera::StopRecording()
{
    callback_notifier_.DisableVideoRecording();
}

int EmulatedCamera::RecordingEnabled()
{
    return callback_notifier_.IsVideoRecordingEnabled();
}

void EmulatedCamera::ReleaseRecordingFrame(const void* opaque)
{
    callback_notifier_.ReleaseRecordingFrame(opaque);
}

status_t EmulatedCamera::AutoFocus()
{
    LOGV("%s", __FUNCTION__);

    /* TODO: Future enhancements. */
    return NO_ERROR;
}

status_t EmulatedCamera::CancelAutoFocus()
{
    LOGV("%s", __FUNCTION__);

    /* TODO: Future enhancements. */
    return NO_ERROR;
}

status_t EmulatedCamera::TakePicture()
{
    LOGV("%s", __FUNCTION__);

    /*
     * Before taking picture, pause the camera (pause worker thread), and pause
     * the preview.
     */

    /*
     * Take the picture now.
     */

    /*
     * After picture has been taken, resume the preview, and the camera (if any
     * has been paused.
     */


    return NO_ERROR;
}

status_t EmulatedCamera::CancelPicture()
{
    LOGV("%s", __FUNCTION__);

    return NO_ERROR;
}

status_t EmulatedCamera::SetParameters(const char* parms)
{
    LOGV("%s", __FUNCTION__);
    _PrintParamDiff(parameters_, parms);

    CameraParameters new_param;
    String8 str8_param(parms);
    new_param.unflatten(str8_param);
    parameters_ = new_param;

    /*
     * In emulation, there are certain parameters that are required by the
     * framework to be exact, and supported by the camera. Since we can't predict
     * the values of such parameters, we must dynamically update them as they
     * are set by the framework.
     */

    /* Supported preview size. */
    const char* check = parameters_.get(CameraParameters::KEY_PREVIEW_SIZE);
    if (check != NULL) {
        const char* current =
            parameters_.get(CameraParameters::KEY_SUPPORTED_PREVIEW_SIZES);
        if (strstr(current, check) == NULL) {
            /* Required size doesn't exist in the list. Add it. */
            char* to_add = _AddValue(current, check);
            if (to_add != NULL) {
                LOGD("+++ %s: Added %s to supported preview sizes",
                     __FUNCTION__, check);
                parameters_.set(CameraParameters::KEY_SUPPORTED_PREVIEW_SIZES, to_add);
                free(to_add);
            }
        }
    }

    /* Supported preview frame rate. */
    check = parameters_.get(CameraParameters::KEY_PREVIEW_FRAME_RATE);
    if (check != NULL) {
        const char* current =
            parameters_.get(CameraParameters::KEY_SUPPORTED_PREVIEW_FRAME_RATES);
        if (strstr(current, check) == NULL) {
            char* to_add = _AddValue(current, check);
            if (to_add != NULL) {
                LOGD("+++ %s: Added %s to supported preview frame rates",
                     __FUNCTION__, check);
                parameters_.set(CameraParameters::KEY_SUPPORTED_PREVIEW_FRAME_RATES, to_add);
                free(to_add);
            }
        }
    }

    /* Supported picture size. */
    check = parameters_.get(CameraParameters::KEY_PICTURE_SIZE);
    if (check != NULL) {
        const char* current =
            parameters_.get(CameraParameters::KEY_SUPPORTED_PICTURE_SIZES);
        if (strstr(current, check) == NULL) {
            char* to_add = _AddValue(current, check);
            if (to_add != NULL) {
                LOGD("+++ %s: Added %s to supported picture sizes",
                     __FUNCTION__, check);
                parameters_.set(CameraParameters::KEY_SUPPORTED_PICTURE_SIZES, to_add);
                free(to_add);
            }
        }
    }

    return NO_ERROR;
}

/* A dumb variable indicating "no params" / error on the exit from
 * EmulatedCamera::GetParameters(). */
static char _no_param = '\0';
char* EmulatedCamera::GetParameters()
{
    String8 params(parameters_.flatten());
    char* ret_str =
        reinterpret_cast<char*>(malloc(sizeof(char) * (params.length()+1)));
    memset(ret_str, 0, params.length()+1);
    if (ret_str != NULL) {
        strncpy(ret_str, params.string(), params.length()+1);
        return ret_str;
    } else {
        LOGE("%s: Unable to allocate string for %s", __FUNCTION__, params.string());
        /* Apparently, we can't return NULL fron this routine. */
        return &_no_param;
    }
}

void EmulatedCamera::PutParameters(char* params)
{
    /* This method simply frees parameters allocated in GetParameters(). */
    if (params != NULL && params != &_no_param) {
        free(params);
    }
}

status_t EmulatedCamera::SendCommand(int32_t cmd, int32_t arg1, int32_t arg2)
{
    LOGV("%s: cmd = %d, arg1 = %d, arg2 = %d", __FUNCTION__, cmd, arg1, arg2);

    /* TODO: Future enhancements. */
    return 0;
}

void EmulatedCamera::Release()
{
    LOGV("%s", __FUNCTION__);

    Cleanup();
}

status_t EmulatedCamera::Dump(int fd)
{
    LOGV("%s", __FUNCTION__);

    /* TODO: Future enhancements. */
    return -EINVAL;
}

/****************************************************************************
 * Preview management.
 ***************************************************************************/

status_t EmulatedCamera::DoStartPreview()
{
    status_t res = preview_window_.Start();

    /* Start the camera. */
    if (res == NO_ERROR && !GetCameraDevice()->IsCapturing()) {
        res = StartCamera();
        if (res != NO_ERROR) {
            /* If camera didn't start, disable the preview window. */
            preview_window_.Stop();
        }
    }

    return res;
}

status_t EmulatedCamera::DoStopPreview()
{
    status_t res = NO_ERROR;
    /* Stop the camera. */
    if (GetCameraDevice()->IsCapturing()) {
        res = StopCamera();
    }

    if (res == NO_ERROR) {
        /* Disable preview as well. */
        preview_window_.Stop();
    }

    return NO_ERROR;
}

status_t EmulatedCamera::StartCamera()
{
    status_t res = EINVAL;
    EmulatedCameraDevice* camera_dev = GetCameraDevice();
    if (camera_dev != NULL) {
        if (!camera_dev->IsConnected()) {
            res = camera_dev->Connect();
            if (res != NO_ERROR) {
                return res;
            }
        }
        if (!camera_dev->IsCapturing()) {
            int width, height;
            /* Lets see what should we use for frame width, and height. */
            if (parameters_.get(CameraParameters::KEY_VIDEO_SIZE) != NULL) {
                parameters_.getVideoSize(&width, &height);
            } else {
                parameters_.getPreviewSize(&width, &height);
            }
            /* Lets see what should we use for the frame pixel format. */
            const char* pix_fmt = parameters_.get(CameraParameters::KEY_VIDEO_FRAME_FORMAT);
            if (pix_fmt == NULL) {
                pix_fmt = parameters_.getPreviewFormat();
            }
            if (pix_fmt == NULL) {
                LOGE("%s: Unable to obtain video format", __FUNCTION__);
                return EINVAL;
            }
            uint32_t org_fmt;
            if (strcmp(pix_fmt, CameraParameters::PIXEL_FORMAT_YUV420P) == 0) {
                org_fmt = V4L2_PIX_FMT_YVU420;
            } else if (strcmp(pix_fmt, CameraParameters::PIXEL_FORMAT_RGBA8888) == 0) {
                org_fmt = V4L2_PIX_FMT_RGB32;
            } else {
                LOGE("%s: Unsupported pixel format %s", __FUNCTION__, pix_fmt);
                return EINVAL;
            }
            LOGD("Starting camera: %dx%d -> %s", width, height, pix_fmt);
            res = camera_dev->StartCapturing(width, height, org_fmt);
            if (res != NO_ERROR) {
                return res;
            }
        }
    }

    return res;
}

status_t EmulatedCamera::StopCamera()
{
    status_t res = NO_ERROR;
    EmulatedCameraDevice* const camera_dev = GetCameraDevice();
    if (camera_dev != NULL) {
        if (camera_dev->IsCapturing()) {
            res = camera_dev->StopCapturing();
            if (res != NO_ERROR) {
                return res;
            }
        }
    }

    return res;
}


/****************************************************************************
 * Private API.
 ***************************************************************************/

status_t EmulatedCamera::Cleanup()
{
    status_t res = NO_ERROR;

    /* If preview is running - stop it. */
    res = DoStopPreview();
    if (res != NO_ERROR) {
        return -res;
    }

    /* Stop and disconnect the camera device. */
    EmulatedCameraDevice* const camera_dev = GetCameraDevice();
    if (camera_dev != NULL) {
        if (camera_dev->IsCapturing()) {
            res = camera_dev->StopCapturing();
            if (res != NO_ERROR) {
                return -res;
            }
        }
        if (camera_dev->IsConnected()) {
            res = camera_dev->Disconnect();
            if (res != NO_ERROR) {
                return -res;
            }
        }
    }

    callback_notifier_.Cleanup();

    return NO_ERROR;
}

/****************************************************************************
 * Camera API callbacks as defined by camera_device_ops structure.
 *
 * Callbacks here simply dispatch the calls to an appropriate method inside
 * EmulatedCamera instance, defined by the 'dev' parameter.
 ***************************************************************************/

int EmulatedCamera::set_preview_window(struct camera_device* dev,
                                       struct preview_stream_ops* window)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return -EINVAL;
    }
    return ec->SetPreviewWindow(window);
}

void EmulatedCamera::set_callbacks(
        struct camera_device* dev,
        camera_notify_callback notify_cb,
        camera_data_callback data_cb,
        camera_data_timestamp_callback data_cb_timestamp,
        camera_request_memory get_memory,
        void* user)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return;
    }
    ec->SetCallbacks(notify_cb, data_cb, data_cb_timestamp, get_memory, user);
}

void EmulatedCamera::enable_msg_type(struct camera_device* dev, int32_t msg_type)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return;
    }
    ec->EnableMsgType(msg_type);
}

void EmulatedCamera::disable_msg_type(struct camera_device* dev, int32_t msg_type)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return;
    }
    ec->DisableMsgType(msg_type);
}

int EmulatedCamera::msg_type_enabled(struct camera_device* dev, int32_t msg_type)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return -EINVAL;
    }
    return ec->MsgTypeEnabled(msg_type);
}

int EmulatedCamera::start_preview(struct camera_device* dev)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return -EINVAL;
    }
    return ec->StartPreview();
}

void EmulatedCamera::stop_preview(struct camera_device* dev)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return;
    }
    ec->StopPreview();
}

int EmulatedCamera::preview_enabled(struct camera_device* dev)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return -EINVAL;
    }
    return ec->PreviewEnabled();
}

int EmulatedCamera::store_meta_data_in_buffers(struct camera_device* dev,
                                               int enable)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return -EINVAL;
    }
    return ec->StoreMetaDataInBuffers(enable);
}

int EmulatedCamera::start_recording(struct camera_device* dev)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return -EINVAL;
    }
    return ec->StartRecording();
}

void EmulatedCamera::stop_recording(struct camera_device* dev)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return;
    }
    ec->StopRecording();
}

int EmulatedCamera::recording_enabled(struct camera_device* dev)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return -EINVAL;
    }
    return ec->RecordingEnabled();
}

void EmulatedCamera::release_recording_frame(struct camera_device* dev,
                                             const void* opaque)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return;
    }
    ec->ReleaseRecordingFrame(opaque);
}

int EmulatedCamera::auto_focus(struct camera_device* dev)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return -EINVAL;
    }
    return ec->AutoFocus();
}

int EmulatedCamera::cancel_auto_focus(struct camera_device* dev)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return -EINVAL;
    }
    return ec->CancelAutoFocus();
}

int EmulatedCamera::take_picture(struct camera_device* dev)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return -EINVAL;
    }
    return ec->TakePicture();
}

int EmulatedCamera::cancel_picture(struct camera_device* dev)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return -EINVAL;
    }
    return ec->CancelPicture();
}

int EmulatedCamera::set_parameters(struct camera_device* dev, const char* parms)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return -EINVAL;
    }
    return ec->SetParameters(parms);
}

char* EmulatedCamera::get_parameters(struct camera_device* dev)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return NULL;
    }
    return ec->GetParameters();
}

void EmulatedCamera::put_parameters(struct camera_device* dev, char* params)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return;
    }
    ec->PutParameters(params);
}

int EmulatedCamera::send_command(struct camera_device* dev,
                                 int32_t cmd,
                                 int32_t arg1,
                                 int32_t arg2)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return -EINVAL;
    }
    return ec->SendCommand(cmd, arg1, arg2);
}

void EmulatedCamera::release(struct camera_device* dev)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return;
    }
    ec->Release();
}

int EmulatedCamera::dump(struct camera_device* dev, int fd)
{
    EmulatedCamera* ec = reinterpret_cast<EmulatedCamera*>(dev->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return -EINVAL;
    }
    return ec->Dump(fd);
}

int EmulatedCamera::close(struct hw_device_t* device)
{
    EmulatedCamera* ec =
        reinterpret_cast<EmulatedCamera*>(reinterpret_cast<struct camera_device*>(device)->priv);
    if (ec == NULL) {
        LOGE("%s: Unexpected NULL camera device", __FUNCTION__);
        return -EINVAL;
    }
    return ec->Close();
}

/****************************************************************************
 * Static initializer for the camera callback API
 ****************************************************************************/

camera_device_ops_t EmulatedCamera::device_ops_ = {
    EmulatedCamera::set_preview_window,
    EmulatedCamera::set_callbacks,
    EmulatedCamera::enable_msg_type,
    EmulatedCamera::disable_msg_type,
    EmulatedCamera::msg_type_enabled,
    EmulatedCamera::start_preview,
    EmulatedCamera::stop_preview,
    EmulatedCamera::preview_enabled,
    EmulatedCamera::store_meta_data_in_buffers,
    EmulatedCamera::start_recording,
    EmulatedCamera::stop_recording,
    EmulatedCamera::recording_enabled,
    EmulatedCamera::release_recording_frame,
    EmulatedCamera::auto_focus,
    EmulatedCamera::cancel_auto_focus,
    EmulatedCamera::take_picture,
    EmulatedCamera::cancel_picture,
    EmulatedCamera::set_parameters,
    EmulatedCamera::get_parameters,
    EmulatedCamera::put_parameters,
    EmulatedCamera::send_command,
    EmulatedCamera::release,
    EmulatedCamera::dump
};

/****************************************************************************
 * Common keys
 ***************************************************************************/

const char EmulatedCamera::FACING_KEY[]       = "prop-facing";
const char EmulatedCamera::ORIENTATION_KEY[]  = "prop-orientation";

/****************************************************************************
 * Common string values
 ***************************************************************************/

const char EmulatedCamera::FACING_BACK[]      = "back";
const char EmulatedCamera::FACING_FRONT[]     = "front";

/****************************************************************************
 * Helper routines
 ***************************************************************************/

static char* _AddValue(const char* param, const char* val)
{
    const size_t len1 = strlen(param);
    const size_t len2 = strlen(val);
    char* ret = reinterpret_cast<char*>(malloc(len1 + len2 + 2));
    LOGE_IF(ret == NULL, "%s: Memory failure", __FUNCTION__);
    if (ret != NULL) {
        memcpy(ret, param, len1);
        ret[len1] = ',';
        memcpy(ret + len1 + 1, val, len2);
        ret[len1 + len2 + 1] = '\0';
    }
    return ret;
}

/****************************************************************************
 * Parameter debugging helpers
 ***************************************************************************/

#if DEBUG_PARAM
static void _PrintParamDiff(const CameraParameters& current,
                            const char* new_par)
{
    char tmp[2048];
    const char* wrk = new_par;

    /* Divided with ';' */
    const char* next = strchr(wrk, ';');
    while (next != NULL) {
        snprintf(tmp, sizeof(tmp), "%.*s", next-wrk, wrk);
        /* in the form key=value */
        char* val = strchr(tmp, '=');
        if (val != NULL) {
            *val = '\0'; val++;
            const char* in_current = current.get(tmp);
            if (in_current != NULL) {
                if (strcmp(in_current, val)) {
                    LOGD("=== Value changed: %s: %s -> %s", tmp, in_current, val);
                }
            } else {
                LOGD("+++ New parameter: %s=%s", tmp, val);
            }
        } else {
            LOGW("No value separator in %s", tmp);
        }
        wrk = next + 1;
        next = strchr(wrk, ';');
    }
}
#endif  /* DEBUG_PARAM */

}; /* namespace android */
