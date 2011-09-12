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
 * Contains implementation of a class EmulatedCameraFactory that manages cameras
 * available for emulation.
 */

#define LOG_NDEBUG 0
#define LOG_TAG "EmulatedCamera_Factory"
#include <cutils/log.h>
#include "emulated_fake_camera.h"
#include "emulated_camera_factory.h"

extern camera_module_t HAL_MODULE_INFO_SYM;

/* A global instance of EmulatedCameraFactory is statically instantiated and
 * initialized when camera emulation HAL is loaded.
 */
android::EmulatedCameraFactory  _emulated_camera_factory;

namespace android {

EmulatedCameraFactory::EmulatedCameraFactory()
        : emulated_cameras_(NULL),
          emulated_camera_num_(0),
          fake_camera_id_(-1),
          constructed_ok_(false)

{
    /* Obtain number of 'qemud' cameras from the emulator. */
    const int qemud_num = GetQemudCameraNumber();
    if (qemud_num < 0) {
        return;
    }
    LOGI("Emulating %d QEMUD cameras.", qemud_num);

    /* ID fake camera with the number of 'qemud' cameras. */
    fake_camera_id_ = qemud_num;
    LOGI("Fake camera ID is %d", fake_camera_id_);

    emulated_camera_num_ = qemud_num + 1;   // Including the 'fake' camera.
    LOGI("%d cameras are being emulated", emulated_camera_num_);

    /* Allocate the array for emulated camera instances. */
    emulated_cameras_ = new EmulatedCamera*[emulated_camera_num_];
    if (emulated_cameras_ == NULL) {
        LOGE("%s: Unable to allocate emulated camera array for %d entries",
             __FUNCTION__, emulated_camera_num_);
        return;
    }

    /* Setup the 'qemud' cameras. */
    if (!CreateQemudCameras(qemud_num)) {
        return;
    }

    /* Create, and initialize the fake camera */
    emulated_cameras_[fake_camera_id_] =
        new EmulatedFakeCamera(fake_camera_id_, &HAL_MODULE_INFO_SYM.common);
    if (emulated_cameras_[fake_camera_id_] == NULL) {
        LOGE("%s: Unable to instantiate fake camera class", __FUNCTION__);
        return;
    }
    if (emulated_cameras_[fake_camera_id_]->Initialize() != NO_ERROR) {
        return;
    }

    constructed_ok_ = true;
}

EmulatedCameraFactory::~EmulatedCameraFactory()
{
    if (emulated_cameras_ != NULL) {
        for (int n = 0; n < emulated_camera_num_; n++) {
            if (emulated_cameras_[n] != NULL) {
                delete emulated_cameras_[n];
            }
        }
        delete[] emulated_cameras_;
    }
}

/****************************************************************************
 * Camera HAL API handlers.
 *
 * Each handler simply verifies existence of an appropriate EmulatedCamera
 * instance, and dispatches the call to that instance.
 *
 ***************************************************************************/

int EmulatedCameraFactory::CameraDeviceOpen(int camera_id, hw_device_t** device)
{
    *device = NULL;

    if (!constructed_ok()) {
        LOGE("%s: EmulatedCameraFactory has failed to initialize", __FUNCTION__);
        return -EINVAL;
    }

    if (camera_id >= emulated_camera_num()) {
        LOGE("%s: Camera id %d is out of bounds (%d)",
             __FUNCTION__, camera_id, emulated_camera_num());
        return -EINVAL;
    }

    return emulated_cameras_[camera_id]->Connect(device);
}

int EmulatedCameraFactory::GetCameraInfo(int camera_id, struct camera_info* info)
{
    if (!constructed_ok()) {
        LOGE("%s: EmulatedCameraFactory has failed to initialize", __FUNCTION__);
        return -EINVAL;
    }

    if (camera_id >= emulated_camera_num()) {
        LOGE("%s: Camera id %d is out of bounds (%d)",
             __FUNCTION__, camera_id, emulated_camera_num());
        return -EINVAL;
    }

    return emulated_cameras_[camera_id]->GetCameraInfo(info);
}

/****************************************************************************
 * Camera HAL API callbacks.
 ***************************************************************************/

int EmulatedCameraFactory::device_open(const hw_module_t* module,
                                       const char* name,
                                       hw_device_t** device)
{
    /*
     * Simply verify the parameters, and dispatch the call inside the
     * EmulatedCameraFactory instance.
     */

    if (module != &HAL_MODULE_INFO_SYM.common) {
        LOGE("%s: Invalid module %p expected %p",
             __FUNCTION__, module, &HAL_MODULE_INFO_SYM.common);
        return -EINVAL;
    }
    if (name == NULL) {
        LOGE("%s: NULL name is not expected here", __FUNCTION__);
        return -EINVAL;
    }

    return _emulated_camera_factory.CameraDeviceOpen(atoi(name), device);
}

int EmulatedCameraFactory::get_number_of_cameras(void)
{
    return _emulated_camera_factory.emulated_camera_num();
}

int EmulatedCameraFactory::get_camera_info(int camera_id,
                                           struct camera_info* info)
{
    return _emulated_camera_factory.GetCameraInfo(camera_id, info);
}

/********************************************************************************
 * Internal API
 *******************************************************************************/

int EmulatedCameraFactory::GetQemudCameraNumber()
{
    // TODO: Implement!
    return 0;
}

bool EmulatedCameraFactory::CreateQemudCameras(int num)
{
    // TODO: Implement!
    return true;
}

/********************************************************************************
 * Initializer for the static member structure.
 *******************************************************************************/

/* Entry point for camera HAL API. */
struct hw_module_methods_t EmulatedCameraFactory::camera_module_methods_ = {
    open: EmulatedCameraFactory::device_open
};

}; /* namespace android */
