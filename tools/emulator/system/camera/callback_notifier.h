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

#ifndef HW_EMULATOR_CAMERA_CALLBACK_NOTIFIER_H
#define HW_EMULATOR_CAMERA_CALLBACK_NOTIFIER_H

/*
 * Contains declaration of a class CallbackNotifier that manages callbacks set
 * via set_callbacks, enable_msg_type, and disable_msg_type camera HAL API.
 */

namespace android {

class EmulatedCameraDevice;

/* Manages callbacks set via set_callbacks, enable_msg_type, and disable_msg_type
 * camera HAL API.
 *
 * Objects of this class are contained in EmulatedCamera objects, and handle
 * relevant camera API callbacks.
 */
class CallbackNotifier {
public:
    /* Constructs CallbackNotifier instance. */
    CallbackNotifier();

    /* Destructs CallbackNotifier instance. */
    ~CallbackNotifier();

    /****************************************************************************
     * Camera API
     ***************************************************************************/

public:
    /* Actual handler for camera_device_ops_t::set_callbacks callback.
     * This method is called by the containing emulated camera object when it is
     * handing the camera_device_ops_t::set_callbacks callback.
     */
    void SetCallbacks(camera_notify_callback notify_cb,
                      camera_data_callback data_cb,
                      camera_data_timestamp_callback data_cb_timestamp,
                      camera_request_memory get_memory,
                      void* user);

    /* Actual handler for camera_device_ops_t::enable_msg_type callback.
     * This method is called by the containing emulated camera object when it is
     * handing the camera_device_ops_t::enable_msg_type callback.
     */
    void EnableMessage(uint msg_type);

    /* Actual handler for camera_device_ops_t::disable_msg_type callback.
     * This method is called by the containing emulated camera object when it is
     * handing the camera_device_ops_t::disable_msg_type callback.
     */
    void DisableMessage(uint msg_type);

    /* Actual handler for camera_device_ops_t::msg_type_enabled callback.
     * This method is called by the containing emulated camera object when it is
     * handing the camera_device_ops_t::msg_type_enabled callback.
     * Return:
     *  0 if message is disabled, or non-zero value, if message is enabled.
     */
    int IsMessageEnabled(uint msg_type);

    /* Actual handler for camera_device_ops_t::store_meta_data_in_buffers
     * callback. This method is called by the containing emulated camera object
     * when it is handing the camera_device_ops_t::store_meta_data_in_buffers
     * callback.
     * Return:
     *  NO_ERROR on success, or an appropriate error status.
     */
    status_t StoreMetaDataInBuffers(bool enable);

    /* Enables video recording.
     * This method is called by the containing emulated camera object when it is
     * handing the camera_device_ops_t::start_recording callback.
     * Param:
     *  fps - Video frame frequency. This parameter determins when a frame
     *      received via OnNextFrameAvailable call will be pushed through the
     *      callback.
     * Return:
     *  NO_ERROR on success, or an appropriate error status.
     */
    status_t EnableVideoRecording(int fps);

    /* Disables video recording.
     * This method is called by the containing emulated camera object when it is
     * handing the camera_device_ops_t::stop_recording callback.
     */
    void DisableVideoRecording();

    /* Checks id video recording is enabled.
     * This method is called by the containing emulated camera object when it is
     * handing the camera_device_ops_t::recording_enabled callback.
     * Return:
     *  true if video recording is enabled, or false if it is disabled.
     */
    bool IsVideoRecordingEnabled();

    /* Releases video frame, sent to the framework.
     * This method is called by the containing emulated camera object when it is
     * handing the camera_device_ops_t::release_recording_frame callback.
     */
    void ReleaseRecordingFrame(const void* opaque);

    /****************************************************************************
     * Public API
     ***************************************************************************/

public:
    /* Resets the callback notifier. */
    void Cleanup();

    /* Next frame is available in the camera device.
     * This is a notification callback that is invoked by the camera device when
     * a new frame is available.
     * Note that most likely this method is called in context of a worker thread
     * that camera device has created for frame capturing.
     * Param:
     *  frame - Captured frame, or NULL if camera device didn't pull the frame
     *      yet. If NULL is passed in this parameter use GetCurrentFrame method
     *      of the camera device class to obtain the next frame. Also note that
     *      the size of the frame that is passed here (as well as the frame
     *      returned from the GetCurrentFrame method) is defined by the current
     *      frame settings (width + height + pixel format) for the camera device.
     * timestamp - Frame's timestamp.
     * camera_dev - Camera device instance that delivered the frame.
     */
    void OnNextFrameAvailable(const void* frame,
                              nsecs_t timestamp,
                              EmulatedCameraDevice* camera_dev);

    /****************************************************************************
     * Private API
     ***************************************************************************/

protected:
    /* Checks if it's time to push new video frame.
     * Note that this method must be called while object is locked.
     * Param:
     *  timestamp - Timestamp for the new frame. */
    bool IsTimeForNewVideoFrame(nsecs_t timestamp);

    /****************************************************************************
     * Data members
     ***************************************************************************/

protected:
    /* Locks this instance for data change. */
    Mutex                           object_lock_;

    /*
     * Callbacks, registered in set_callbacks.
     */

    camera_notify_callback          notify_cb_;
    camera_data_callback            data_cb_;
    camera_data_timestamp_callback  data_cb_timestamp_;
    camera_request_memory           get_memory_;
    void*                           cb_opaque_;

    /* Timestamp when last frame has been delivered to the framework. */
    nsecs_t                         last_frame_;

    /* Video frequency in nanosec. */
    nsecs_t                         frame_after_;

    /* Message enabler. */
    uint32_t                        message_enabler_;

    /* Video recording status. */
    bool                            video_recording_enabled_;
};

}; /* namespace android */

#endif  /* HW_EMULATOR_CAMERA_CALLBACK_NOTIFIER_H */
