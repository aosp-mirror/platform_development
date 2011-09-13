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
#include "emulated_qemu_camera.h"
#include "emulated_fake_camera.h"
#include "emulated_camera_factory.h"

extern camera_module_t HAL_MODULE_INFO_SYM;

/* A global instance of EmulatedCameraFactory is statically instantiated and
 * initialized when camera emulation HAL is loaded.
 */
android::EmulatedCameraFactory  _emulated_camera_factory;

namespace android {

EmulatedCameraFactory::EmulatedCameraFactory()
        : qemu_client_(),
          emulated_cameras_(NULL),
          emulated_camera_num_(0),
          fake_camera_id_(-1),
          constructed_ok_(false)

{
    /* If qemu camera emulation is on, try to connect to the factory service in
     * the emulator. */
    if (IsQemuCameraEmulationOn() && qemu_client_.Connect(NULL) == NO_ERROR) {
        /* Connection has succeeded. Create emulated cameras for each camera
         * device, reported by the service. */
        CreateQemuCameras();
    }

    if (IsFakeCameraEmulationOn()) {
        /* ID fake camera with the number of created 'qemud' cameras. */
        fake_camera_id_ = emulated_camera_num_;
        emulated_camera_num_++;

        /* Make sure that array is allocated (in case there were no 'qemu'
         * cameras created. */
        if (emulated_cameras_ == NULL) {
            emulated_cameras_ = new EmulatedCamera*[emulated_camera_num_];
            if (emulated_cameras_ == NULL) {
                LOGE("%s: Unable to allocate emulated camera array for %d entries",
                     __FUNCTION__, emulated_camera_num_);
                return;
            }
            memset(emulated_cameras_, 0, emulated_camera_num_ * sizeof(EmulatedCamera*));
        }

        /* Create, and initialize the fake camera */
        emulated_cameras_[fake_camera_id_] =
            new EmulatedFakeCamera(fake_camera_id_, &HAL_MODULE_INFO_SYM.common);
        if (emulated_cameras_[fake_camera_id_] != NULL) {
            if (emulated_cameras_[fake_camera_id_]->Initialize() != NO_ERROR) {
                delete emulated_cameras_[fake_camera_id_];
                emulated_cameras_--;
                fake_camera_id_ = -1;
            }
        } else {
            emulated_cameras_--;
            fake_camera_id_ = -1;
            LOGE("%s: Unable to instantiate fake camera class", __FUNCTION__);
        }
    }

    LOGV("%d cameras are being emulated. Fake camera ID is %d",
         emulated_camera_num_, fake_camera_id_);

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

/*
 * Camera information tokens passed in response to the "list" factory query.
 */

/* Device name token. */
static const char _list_name_token[]    = "name=";
/* Frame dimensions token. */
static const char _list_dims_token[]    = "framedims=";

void EmulatedCameraFactory::CreateQemuCameras()
{
    /* Obtain camera list. */
    char* camera_list = NULL;
    status_t res = qemu_client_.ListCameras(&camera_list);
    /* Empty list, or list containing just an EOL means that there were no
     * connected cameras found. */
    if (res != NO_ERROR || camera_list == NULL || *camera_list == '\0' ||
        *camera_list == '\n') {
        if (camera_list != NULL) {
            free(camera_list);
        }
        return;
    }

    /*
     * Calculate number of connected cameras. Number of EOLs in the camera list
     * is the number of the connected cameras.
     */

    int num = 0;
    const char* eol = strchr(camera_list, '\n');
    while (eol != NULL) {
        num++;
        eol = strchr(eol + 1, '\n');
    }

    /* Allocate the array for emulated camera instances. Note that we allocate
     * one more entry for the fake camera emulation. */
    emulated_cameras_ = new EmulatedCamera*[num + 1];
    if (emulated_cameras_ == NULL) {
        LOGE("%s: Unable to allocate emulated camera array for %d entries",
             __FUNCTION__, num + 1);
        free(camera_list);
        return;
    }
    memset(emulated_cameras_, 0, sizeof(EmulatedCamera*) * (num + 1));

    /*
     * Iterate the list, creating, and initializin emulated qemu cameras for each
     * entry (line) in the list.
     */

    int index = 0;
    char* cur_entry = camera_list;
    while (cur_entry != NULL && *cur_entry != '\0' && index < num) {
        /* Find the end of the current camera entry, and terminate it with zero
         * for simpler string manipulation. */
        char* next_entry = strchr(cur_entry, '\n');
        if (next_entry != NULL) {
            *next_entry = '\0';
            next_entry++;   // Start of the next entry.
        }

        /* Find 'name', and 'framedims' tokens that are required here. */
        char* name_start = strstr(cur_entry, _list_name_token);
        char* dim_start = strstr(cur_entry, _list_dims_token);
        if (name_start != NULL && dim_start != NULL) {
            /* Advance to the token values. */
            name_start += strlen(_list_name_token);
            dim_start += strlen(_list_dims_token);

            /* Terminate token values with zero. */
            char* s = strchr(name_start, ' ');
            if (s != NULL) {
                *s = '\0';
            }
            s = strchr(dim_start, ' ');
            if (s != NULL) {
                *s = '\0';
            }

            /* Create and initialize qemu camera. */
            EmulatedQemuCamera* qemu_cam =
                new EmulatedQemuCamera(index, &HAL_MODULE_INFO_SYM.common);
            if (NULL != qemu_cam) {
                res = qemu_cam->Initialize(name_start, dim_start);
                if (res == NO_ERROR) {
                    emulated_cameras_[index] = qemu_cam;
                    index++;
                } else {
                    delete qemu_cam;
                }
            } else {
                LOGE("%s: Unable to instantiate EmulatedQemuCamera",
                     __FUNCTION__);
            }
        } else {
            LOGW("%s: Bad camera information: %s", __FUNCTION__, cur_entry);
        }

        cur_entry = next_entry;
    }

    emulated_camera_num_ = index;
}

bool EmulatedCameraFactory::IsQemuCameraEmulationOn()
{
    /* TODO: Have a boot property that controls that! */
    return true;
}

bool EmulatedCameraFactory::IsFakeCameraEmulationOn()
{
    /* TODO: Have a boot property that controls that! */
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
