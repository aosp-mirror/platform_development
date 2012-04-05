/*
 * Copyright (C) 2012 The Android Open Source Project
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
 * Contains implementation of a class EmulatedFakeCamera2 that encapsulates
 * functionality of an advanced fake camera.
 */

#define LOG_NDEBUG 0
#define LOG_TAG "EmulatedCamera_FakeCamera2"
#include <cutils/log.h>
#include <cutils/properties.h>
#include "EmulatedFakeCamera2.h"
#include "EmulatedCameraFactory.h"

namespace android {

EmulatedFakeCamera2::EmulatedFakeCamera2(int cameraId,
        bool facingBack,
        struct hw_module_t* module)
        : EmulatedCamera2(cameraId,module),
          mFacingBack(facingBack)
{
    ALOGD("Constructing emulated fake camera 2 facing %s",
            facingBack ? "back" : "front");
}

EmulatedFakeCamera2::~EmulatedFakeCamera2()
{
}

/****************************************************************************
 * Public API overrides
 ***************************************************************************/

status_t EmulatedFakeCamera2::Initialize()
{
    return NO_ERROR;
}

};  /* namespace android */
