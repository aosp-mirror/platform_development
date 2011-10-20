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
#include <cutils/properties.h>
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
    status_t res = mFakeCameraDevice.Initialize();
    if (res != NO_ERROR) {
        return res;
    }

    /* Fake camera facing is defined by the qemu.sf.fake_camera boot property. */
    char prop[PROPERTY_VALUE_MAX];
    property_get("qemu.sf.fake_camera", prop, EmulatedCamera::FACING_BACK);
    const char* facing = prop;

    mParameters.set(EmulatedCamera::FACING_KEY, facing);
    LOGD("%s: Fake camera is facing %s", __FUNCTION__, facing);

    mParameters.set(EmulatedCamera::ORIENTATION_KEY,
                    gEmulatedCameraFactory.getFakeCameraOrientation());

    res = EmulatedCamera::Initialize();

    mParameters.set(CameraParameters::KEY_MAX_EXPOSURE_COMPENSATION, "6");
    mParameters.set(CameraParameters::KEY_MIN_EXPOSURE_COMPENSATION, "-6");
    mParameters.set(CameraParameters::KEY_EXPOSURE_COMPENSATION_STEP, "0.5");
    mParameters.set(CameraParameters::KEY_EXPOSURE_COMPENSATION, "0");
    ALOGV("Set camera supported exposure values");

    // Sets the white balance modes and the device-dependent scale factors.
    mFakeCameraDevice.initializeWhiteBalanceModes(
            CameraParameters::WHITE_BALANCE_INCANDESCENT, 1.38f, 0.60f);
    mFakeCameraDevice.initializeWhiteBalanceModes(
            CameraParameters::WHITE_BALANCE_DAYLIGHT, 1.09f, 0.92f);
    mFakeCameraDevice.initializeWhiteBalanceModes(
            CameraParameters::WHITE_BALANCE_TWILIGHT, 0.92f, 1.22f);

    char supported_white_balance[1024];
    snprintf(supported_white_balance, sizeof(supported_white_balance),
             "%s,%s,%s,%s",
             mParameters.get(CameraParameters::KEY_SUPPORTED_WHITE_BALANCE),
             CameraParameters::WHITE_BALANCE_INCANDESCENT,
             CameraParameters::WHITE_BALANCE_DAYLIGHT,
             CameraParameters::WHITE_BALANCE_TWILIGHT);
    mParameters.set(CameraParameters::KEY_SUPPORTED_WHITE_BALANCE,
                    supported_white_balance);

    ALOGV("Set camera supported white balance modes");

    if (res != NO_ERROR) {
        return res;
    }

    /*
     * Parameters provided by the camera device.
     */

    mParameters.set(CameraParameters::KEY_SUPPORTED_PICTURE_SIZES, "640x480");
    mParameters.set(CameraParameters::KEY_SUPPORTED_PREVIEW_SIZES, "640x480");
    mParameters.setPreviewSize(640, 480);
    mParameters.setPictureSize(640, 480);

    return NO_ERROR;
}

EmulatedCameraDevice* EmulatedFakeCamera::getCameraDevice()
{
    return &mFakeCameraDevice;
}

};  /* namespace android */
