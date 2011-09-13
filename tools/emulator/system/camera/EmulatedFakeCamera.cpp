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
 * Contains implementation of a class EmulatedFakeCamera that encapsulates
 * functionality of a fake camera.
 */

#define LOG_NDEBUG 0
#define LOG_TAG "EmulatedCamera_FakeCamera"
#include <cutils/log.h>
#include "EmulatedFakeCamera.h"
#include "EmulatedCameraFactory.h"

namespace android {

EmulatedFakeCamera::EmulatedFakeCamera(int cameraId, struct hw_module_t* module)
        : EmulatedCamera(cameraId, module),
          mFakeCameraDevice(this)
{
}

EmulatedFakeCamera::~EmulatedFakeCamera()
{
}

/****************************************************************************
 * Public API overrides
 ***************************************************************************/

status_t EmulatedFakeCamera::Initialize()
{
    LOGV("%s", __FUNCTION__);

    status_t res = mFakeCameraDevice.Initialize();
    if (res != NO_ERROR) {
        return res;
    }

    const char* facing = EmulatedCamera::FACING_BACK;
    if (gEmulatedCameraFactory.getFakeCameraOrientation() == CAMERA_FACING_FRONT) {
        facing = EmulatedCamera::FACING_FRONT;
    }
    mPparameters.set(EmulatedCamera::FACING_KEY, facing);

    mPparameters.set(EmulatedCamera::ORIENTATION_KEY,
                    gEmulatedCameraFactory.getFakeCameraOrientation());

    res = EmulatedCamera::Initialize();
    if (res != NO_ERROR) {
        return res;
    }

    /*
     * Parameters provided by the camera device.
     */

    mPparameters.set(CameraParameters::KEY_SUPPORTED_PICTURE_SIZES, "640x480");
    mPparameters.set(CameraParameters::KEY_SUPPORTED_PREVIEW_SIZES, "640x480");
    mPparameters.setPreviewSize(640, 480);
    mPparameters.setPictureSize(640, 480);

    return NO_ERROR;
}

EmulatedCameraDevice* EmulatedFakeCamera::getCameraDevice()
{
    return &mFakeCameraDevice;
}

};  /* namespace android */
