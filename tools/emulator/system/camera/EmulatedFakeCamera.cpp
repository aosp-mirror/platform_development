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
    if (res != NO_ERROR) {
        return res;
    }

    /*
     * Parameters provided by the camera device.
     */

    /* 352x288 and 320x240 frame dimensions are required by the framework for
     * video mode preview and video recording. */
    mParameters.set(CameraParameters::KEY_SUPPORTED_PICTURE_SIZES,
                    "640x480,352x288,320x240");
    mParameters.set(CameraParameters::KEY_SUPPORTED_PREVIEW_SIZES,
                    "640x480,352x288,320x240");
    mParameters.setPreviewSize(640, 480);
    mParameters.setPictureSize(640, 480);

    return NO_ERROR;
}

EmulatedCameraDevice* EmulatedFakeCamera::getCameraDevice()
{
    return &mFakeCameraDevice;
}

};  /* namespace android */
