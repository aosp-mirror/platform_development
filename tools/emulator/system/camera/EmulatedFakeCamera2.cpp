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
#include <utils/Log.h>

#include "EmulatedFakeCamera2.h"
#include "EmulatedCameraFactory.h"
#include <ui/Rect.h>
#include <ui/GraphicBufferMapper.h>

namespace android {

const uint32_t EmulatedFakeCamera2::kAvailableFormats[1] = {
    HAL_PIXEL_FORMAT_RAW_SENSOR
};

const uint32_t EmulatedFakeCamera2::kAvailableSizesPerFormat[1] = {
    1
};

const uint32_t EmulatedFakeCamera2::kAvailableSizes[2] = {
    640, 480
    //    Sensor::kResolution[0], Sensor::kResolution[1]
};

const uint64_t EmulatedFakeCamera2::kAvailableMinFrameDurations[1] = {
    Sensor::kFrameDurationRange[0]
};

EmulatedFakeCamera2::EmulatedFakeCamera2(int cameraId,
        bool facingBack,
        struct hw_module_t* module)
        : EmulatedCamera2(cameraId,module),
          mFacingBack(facingBack)
{
    ALOGD("Constructing emulated fake camera 2 facing %s",
            facingBack ? "back" : "front");
}

EmulatedFakeCamera2::~EmulatedFakeCamera2() {
    if (mCameraInfo != NULL) {
        free_camera_metadata(mCameraInfo);
    }
}

/****************************************************************************
 * Public API overrides
 ***************************************************************************/

status_t EmulatedFakeCamera2::Initialize() {
    status_t res;
    mCameraInfo = allocate_camera_metadata(10,100);

    res = add_camera_metadata_entry(mCameraInfo,
            ANDROID_SENSOR_EXPOSURE_TIME_RANGE,
            Sensor::kExposureTimeRange, 2);

    res = add_camera_metadata_entry(mCameraInfo,
            ANDROID_SENSOR_MAX_FRAME_DURATION,
            &Sensor::kFrameDurationRange[1], 1);

    res = add_camera_metadata_entry(mCameraInfo,
            ANDROID_SENSOR_AVAILABLE_SENSITIVITIES,
            Sensor::kAvailableSensitivities,
            sizeof(Sensor::kAvailableSensitivities)
            /sizeof(uint32_t));

    res = add_camera_metadata_entry(mCameraInfo,
            ANDROID_SENSOR_COLOR_FILTER_ARRANGEMENT,
            &Sensor::kColorFilterArrangement, 1);

    res = add_camera_metadata_entry(mCameraInfo,
            ANDROID_SENSOR_PIXEL_ARRAY_SIZE,
            Sensor::kResolution, 2);

    res = add_camera_metadata_entry(mCameraInfo,
            ANDROID_SENSOR_ACTIVE_ARRAY_SIZE,
            Sensor::kResolution, 2);

    res = add_camera_metadata_entry(mCameraInfo,
            ANDROID_SCALER_AVAILABLE_FORMATS,
            kAvailableFormats,
            sizeof(kAvailableFormats)/sizeof(uint32_t));

    res = add_camera_metadata_entry(mCameraInfo,
            ANDROID_SCALER_AVAILABLE_SIZES_PER_FORMAT,
            kAvailableSizesPerFormat,
            sizeof(kAvailableSizesPerFormat)/sizeof(uint32_t));

    res = add_camera_metadata_entry(mCameraInfo,
            ANDROID_SCALER_AVAILABLE_SIZES,
            kAvailableSizes,
            sizeof(kAvailableSizes)/sizeof(uint32_t));

    res = add_camera_metadata_entry(mCameraInfo,
            ANDROID_SCALER_AVAILABLE_MIN_FRAME_DURATIONS,
            kAvailableMinFrameDurations,
            sizeof(kAvailableMinFrameDurations)/sizeof(uint32_t));

    // TODO: Add all the others

    return NO_ERROR;
}

/****************************************************************************
 * Camera module API overrides
 ***************************************************************************/

status_t EmulatedFakeCamera2::connectCamera(hw_device_t** device) {
    status_t res;
    ALOGV("%s", __FUNCTION__);

    mConfigureThread = new ConfigureThread(this);
    mReadoutThread = new ReadoutThread(this);
    mSensor = new Sensor();

    mNextStreamId = 0;
    mRawStreamOps = NULL;

    res = mSensor->startUp();
    if (res != NO_ERROR) return res;

    res = mConfigureThread->run("EmulatedFakeCamera2::configureThread");
    if (res != NO_ERROR) return res;

    res = mReadoutThread->run("EmulatedFakeCamera2::readoutThread");
    if (res != NO_ERROR) return res;

    return EmulatedCamera2::connectCamera(device);
}

status_t EmulatedFakeCamera2::closeCamera() {
    Mutex::Autolock l(mMutex);

    status_t res;
    ALOGV("%s", __FUNCTION__);

    res = mSensor->shutDown();
    if (res != NO_ERROR) {
        ALOGE("%s: Unable to shut down sensor: %d", __FUNCTION__, res);
        return res;
    }

    mConfigureThread->requestExit();
    mReadoutThread->requestExit();

    mConfigureThread->join();
    mReadoutThread->join();

    ALOGV("%s exit", __FUNCTION__);
    return NO_ERROR;
}

status_t EmulatedFakeCamera2::getCameraInfo(struct camera_info *info) {
    info->facing = mFacingBack ? CAMERA_FACING_BACK : CAMERA_FACING_FRONT;
    info->orientation = 0;
    return EmulatedCamera2::getCameraInfo(info);
}

/****************************************************************************
 * Camera device API overrides
 ***************************************************************************/

/** Request input queue */

int EmulatedFakeCamera2::requestQueueNotify() {
    ALOGV("Request queue notification received");

    ALOG_ASSERT(mRequestQueueSrc != NULL,
            "%s: Request queue src not set, but received queue notification!",
            __FUNCTION__);
    ALOG_ASSERT(mFrameQueueDst != NULL,
            "%s: Request queue src not set, but received queue notification!",
            __FUNCTION__);
    ALOG_ASSERT(mRawStreamOps != NULL,
            "%s: No raw stream allocated, but received queue notification!",
            __FUNCTION__);
    return mConfigureThread->newRequestAvailable();
}

int EmulatedFakeCamera2::allocateStream(
        uint32_t width,
        uint32_t height,
        int format,
        camera2_stream_ops_t *stream_ops,
        uint32_t *stream_id,
        uint32_t *format_actual,
        uint32_t *usage,
        uint32_t *max_buffers) {
    Mutex::Autolock l(mMutex);

    if (mNextStreamId > 0) {
        // TODO: Support more than one stream
        ALOGW("%s: Only one stream supported", __FUNCTION__);
        return BAD_VALUE;
    }

    unsigned int numFormats = sizeof(kAvailableFormats) / sizeof(uint32_t);
    unsigned int formatIdx = 0;
    unsigned int sizeOffsetIdx = 0;
    for (; formatIdx < numFormats; formatIdx++) {
        if (format == (int)kAvailableFormats[formatIdx]) break;
        sizeOffsetIdx += kAvailableSizesPerFormat[formatIdx];
    }

    if (formatIdx == numFormats) {
        ALOGW("%s: Format 0x%x is not supported", __FUNCTION__, format);
        return BAD_VALUE;
    }
    unsigned int resIdx = 0;

    for (; resIdx < kAvailableSizesPerFormat[formatIdx]; resIdx++) {
        uint32_t widthMatch  = kAvailableSizes[ (sizeOffsetIdx + resIdx)*2 + 0];
        uint32_t heightMatch = kAvailableSizes[ (sizeOffsetIdx + resIdx)*2 + 1];
        if (width == widthMatch && height == heightMatch) break;
    }
    if (resIdx == kAvailableSizesPerFormat[formatIdx]) {
        ALOGW("%s: Format 0x%x does not support resolution %d, %d", __FUNCTION__,
                format, width, height);
        return BAD_VALUE;
    }

    // TODO: Generalize below to work for variable types of streams, etc.
    // Currently only correct for raw sensor format, sensor resolution.

    ALOG_ASSERT(format == HAL_PIXEL_FORMAT_RAW_SENSOR,
            "%s: TODO: Only supporting raw sensor format right now", __FUNCTION__);
    ALOG_ASSERT(width == Sensor::kResolution[0],
            "%s: TODO: Only supporting raw sensor size right now", __FUNCTION__);
    ALOG_ASSERT(height == Sensor::kResolution[1],
            "%s: TODO: Only supporting raw sensor size right now", __FUNCTION__);

    mRawStreamOps = stream_ops;

    *stream_id = mNextStreamId;
    if (format_actual) *format_actual = format;
    *usage = GRALLOC_USAGE_SW_WRITE_OFTEN;
    *max_buffers = 4;

    ALOGV("Stream allocated: %d, %d x %d, 0x%x. U: %x, B: %d",
            *stream_id, width, height, format, *usage, *max_buffers);

    mNextStreamId++;
    return NO_ERROR;
}

int EmulatedFakeCamera2::registerStreamBuffers(
            uint32_t stream_id,
            int num_buffers,
            buffer_handle_t *buffers) {
    // Emulator doesn't need to register these with V4L2, etc.
    ALOGV("%s: Stream %d registering %d buffers", __FUNCTION__,
            stream_id, num_buffers);
    return NO_ERROR;
}

int EmulatedFakeCamera2::releaseStream(uint32_t stream_id) {
    Mutex::Autolock l(mMutex);
    ALOG_ASSERT(stream_id == 0,
            "%s: TODO: Only one stream supported", __FUNCTION__);

    // TODO: Need to clean up better than this - in-flight buffers likely
    mRawStreamOps = NULL;

    return NO_ERROR;
}

/** Custom tag definitions */

// Emulator camera metadata sections
enum {
    EMULATOR_SCENE = VENDOR_SECTION,
    END_EMULATOR_SECTIONS
};

enum {
    EMULATOR_SCENE_START = EMULATOR_SCENE << 16,
};

// Emulator camera metadata tags
enum {
    // Hour of day to use for lighting calculations (0-23). Default: 12
    EMULATOR_SCENE_HOUROFDAY = EMULATOR_SCENE_START,
    EMULATOR_SCENE_END
};

unsigned int emulator_metadata_section_bounds[END_EMULATOR_SECTIONS -
        VENDOR_SECTION][2] = {
    { EMULATOR_SCENE_START, EMULATOR_SCENE_END }
};

const char *emulator_metadata_section_names[END_EMULATOR_SECTIONS -
        VENDOR_SECTION] = {
    "com.android.emulator.scene"
};

typedef struct emulator_tag_info {
    const char *tag_name;
    uint8_t     tag_type;
} emulator_tag_info_t;

emulator_tag_info_t emulator_scene[EMULATOR_SCENE_END - EMULATOR_SCENE_START] = {
    { "hourOfDay", TYPE_INT32 }
};

emulator_tag_info_t *tag_info[END_EMULATOR_SECTIONS -
        VENDOR_SECTION] = {
    emulator_scene
};

const char* EmulatedFakeCamera2::getVendorSectionName(uint32_t tag) {
    ALOGV("%s", __FUNCTION__);
    uint32_t section = tag >> 16;
    if (section < VENDOR_SECTION || section > END_EMULATOR_SECTIONS) return NULL;
    return emulator_metadata_section_names[section - VENDOR_SECTION];
}

const char* EmulatedFakeCamera2::getVendorTagName(uint32_t tag) {
    ALOGV("%s", __FUNCTION__);
    uint32_t section = tag >> 16;
    if (section < VENDOR_SECTION || section > END_EMULATOR_SECTIONS) return NULL;
    uint32_t section_index = section - VENDOR_SECTION;
    if (tag >= emulator_metadata_section_bounds[section_index][1]) {
        return NULL;
    }
    uint32_t tag_index = tag & 0xFFFF;
    return tag_info[section_index][tag_index].tag_name;
}

int EmulatedFakeCamera2::getVendorTagType(uint32_t tag) {
    ALOGV("%s", __FUNCTION__);
    uint32_t section = tag >> 16;
    if (section < VENDOR_SECTION || section > END_EMULATOR_SECTIONS) return -1;
    uint32_t section_index = section - VENDOR_SECTION;
    if (tag >= emulator_metadata_section_bounds[section_index][1]) {
        return -1;
    }
    uint32_t tag_index = tag & 0xFFFF;
    return tag_info[section_index][tag_index].tag_type;
}

/** Shutdown and debug methods */

int EmulatedFakeCamera2::dump(int fd) {
    return NO_ERROR;
}

void EmulatedFakeCamera2::signalError() {
    // TODO: Let parent know so we can shut down cleanly
    ALOGE("Worker thread is signaling a serious error");
}

/** Pipeline control worker thread methods */

EmulatedFakeCamera2::ConfigureThread::ConfigureThread(EmulatedFakeCamera2 *parent):
        Thread(false),
        mParent(parent) {
    mRunning = false;
}

EmulatedFakeCamera2::ConfigureThread::~ConfigureThread() {
}

status_t EmulatedFakeCamera2::ConfigureThread::readyToRun() {
    Mutex::Autolock lock(mInputMutex);

    ALOGV("Starting up ConfigureThread");
    mRequest = NULL;
    mActive  = false;
    mRunning = true;

    mInputSignal.signal();
    return NO_ERROR;
}

status_t EmulatedFakeCamera2::ConfigureThread::waitUntilRunning() {
    Mutex::Autolock lock(mInputMutex);
    if (!mRunning) {
        ALOGV("Waiting for configure thread to start");
        mInputSignal.wait(mInputMutex);
    }
    return OK;
}

status_t EmulatedFakeCamera2::ConfigureThread::newRequestAvailable() {
    waitUntilRunning();

    Mutex::Autolock lock(mInputMutex);

    mActive = true;
    mInputSignal.signal();

    return OK;
}

bool EmulatedFakeCamera2::ConfigureThread::threadLoop() {
    static const nsecs_t kWaitPerLoop = 10000000L; // 10 ms
    status_t res;

    // Check if we're currently processing or just waiting
    {
        Mutex::Autolock lock(mInputMutex);
        if (!mActive) {
            // Inactive, keep waiting until we've been signaled
            status_t res;
            res = mInputSignal.waitRelative(mInputMutex, kWaitPerLoop);
            if (res != NO_ERROR && res != TIMED_OUT) {
                ALOGE("%s: Error waiting for input requests: %d",
                        __FUNCTION__, res);
                return false;
            }
            if (!mActive) return true;
            ALOGV("New request available");
        }
        // Active
    }
    if (mRequest == NULL) {
        ALOGV("Getting next request");
        res = mParent->mRequestQueueSrc->dequeue_request(
            mParent->mRequestQueueSrc,
            &mRequest);
        if (res != NO_ERROR) {
            ALOGE("%s: Error dequeuing next request: %d", __FUNCTION__, res);
            mParent->signalError();
            return false;
        }
        if (mRequest == NULL) {
            ALOGV("Request queue empty, going inactive");
            // No requests available, go into inactive mode
            Mutex::Autolock lock(mInputMutex);
            mActive = false;
            return true;
        }
        // Get necessary parameters for sensor config

        sort_camera_metadata(mRequest);

        camera_metadata_entry_t streams;
        res = find_camera_metadata_entry(mRequest,
                ANDROID_REQUEST_OUTPUT_STREAMS,
                &streams);
        if (res != NO_ERROR) {
            ALOGE("%s: error reading output stream tag", __FUNCTION__);
            mParent->signalError();
            return false;
        }
        // TODO: Only raw stream supported
        if (streams.count != 1 || streams.data.u8[0] != 0) {
            ALOGE("%s: TODO: Only raw stream supported", __FUNCTION__);
            mParent->signalError();
            return false;
        }

        camera_metadata_entry_t e;
        res = find_camera_metadata_entry(mRequest,
                ANDROID_REQUEST_FRAME_COUNT,
                &e);
        if (res != NO_ERROR) {
            ALOGE("%s: error reading frame count tag", __FUNCTION__);
            mParent->signalError();
            return false;
        }
        mNextFrameNumber = *e.data.i32;

        res = find_camera_metadata_entry(mRequest,
                ANDROID_SENSOR_EXPOSURE_TIME,
                &e);
        if (res != NO_ERROR) {
            ALOGE("%s: error reading exposure time tag", __FUNCTION__);
            mParent->signalError();
            return false;
        }
        mNextExposureTime = *e.data.i64;

        res = find_camera_metadata_entry(mRequest,
                ANDROID_SENSOR_FRAME_DURATION,
                &e);
        if (res != NO_ERROR) {
            ALOGE("%s: error reading frame duration tag", __FUNCTION__);
            mParent->signalError();
            return false;
        }
        mNextFrameDuration = *e.data.i64;

        if (mNextFrameDuration <
                mNextExposureTime + Sensor::kMinVerticalBlank) {
            mNextFrameDuration = mNextExposureTime + Sensor::kMinVerticalBlank;
        }
        res = find_camera_metadata_entry(mRequest,
                ANDROID_SENSOR_SENSITIVITY,
                &e);
        if (res != NO_ERROR) {
            ALOGE("%s: error reading sensitivity tag", __FUNCTION__);
            mParent->signalError();
            return false;
        }
        mNextSensitivity = *e.data.i32;

        res = find_camera_metadata_entry(mRequest,
                EMULATOR_SCENE_HOUROFDAY,
                &e);
        if (res == NO_ERROR) {
            ALOGV("Setting hour: %d", *e.data.i32);
            mParent->mSensor->getScene().setHour(*e.data.i32);
        }

        // TODO: Fetch stride from gralloc
        mNextBufferStride = Sensor::kResolution[0];

        // Start waiting on sensor
        ALOGV("Waiting for sensor");
    }
    bool vsync = mParent->mSensor->waitForVSync(kWaitPerLoop);

    if (vsync) {
        ALOGV("Configuring sensor for frame %d", mNextFrameNumber);
        mParent->mSensor->setExposureTime(mNextExposureTime);
        mParent->mSensor->setFrameDuration(mNextFrameDuration);
        mParent->mSensor->setSensitivity(mNextSensitivity);

        /** Get buffer to fill for this frame */
        // TODO: Only does raw stream

        /* Get next buffer from raw stream */
        mNextBuffer = NULL;
        res = mParent->mRawStreamOps->dequeue_buffer(mParent->mRawStreamOps,
            &mNextBuffer);
        if (res != NO_ERROR || mNextBuffer == NULL) {
            ALOGE("%s: Unable to dequeue buffer from stream %d: %d",
                    __FUNCTION__, 0, res);
            mParent->signalError();
            return false;
        }

        /* Lock the buffer from the perspective of the graphics mapper */
        uint8_t *img;
        const Rect rect(Sensor::kResolution[0], Sensor::kResolution[1]);

        res = GraphicBufferMapper::get().lock(*mNextBuffer,
                GRALLOC_USAGE_SW_WRITE_OFTEN,
                rect, (void**)&img);

        if (res != NO_ERROR) {
            ALOGE("%s: grbuffer_mapper.lock failure: %d", __FUNCTION__, res);
            mParent->mRawStreamOps->cancel_buffer(mParent->mRawStreamOps,
                    mNextBuffer);
            mParent->signalError();
            return false;
        }
        mParent->mSensor->setDestinationBuffer(img, mNextBufferStride);
        mParent->mReadoutThread->setNextCapture(mRequest, mNextBuffer);

        mRequest = NULL;
    }

    return true;
}

EmulatedFakeCamera2::ReadoutThread::ReadoutThread(EmulatedFakeCamera2 *parent):
        Thread(false),
        mParent(parent),
        mRunning(false),
        mActive(false),
        mRequest(NULL),
        mBuffer(NULL)
{
    mInFlightQueue = new InFlightQueue[kInFlightQueueSize];
    mInFlightHead = 0;
    mInFlightTail = 0;
}

EmulatedFakeCamera2::ReadoutThread::~ReadoutThread() {
    delete mInFlightQueue;
}

status_t EmulatedFakeCamera2::ReadoutThread::readyToRun() {
    Mutex::Autolock lock(mInputMutex);
    ALOGV("Starting up ReadoutThread");
    mRunning = true;
    mInputSignal.signal();
    return NO_ERROR;
}

status_t EmulatedFakeCamera2::ReadoutThread::waitUntilRunning() {
    Mutex::Autolock lock(mInputMutex);
    if (!mRunning) {
        ALOGV("Waiting for readout thread to start");
        mInputSignal.wait(mInputMutex);
    }
    return OK;
}

void EmulatedFakeCamera2::ReadoutThread::setNextCapture(camera_metadata_t *request,
        buffer_handle_t *buffer) {
    Mutex::Autolock lock(mInputMutex);
    if ( (mInFlightTail + 1) % kInFlightQueueSize == mInFlightHead) {
        ALOGE("In flight queue full, dropping captures");
        mParent->signalError();
        return;
    }
    mInFlightQueue[mInFlightTail].request = request;
    mInFlightQueue[mInFlightTail].buffer = buffer;
    mInFlightTail = (mInFlightTail + 1) % kInFlightQueueSize;

    if (!mActive) {
        mActive = true;
        mInputSignal.signal();
    }
}

bool EmulatedFakeCamera2::ReadoutThread::threadLoop() {
    static const nsecs_t kWaitPerLoop = 10000000L; // 10 ms
    status_t res;

    // Check if we're currently processing or just waiting
    {
        Mutex::Autolock lock(mInputMutex);
        if (!mActive) {
            // Inactive, keep waiting until we've been signaled
            res = mInputSignal.waitRelative(mInputMutex, kWaitPerLoop);
            if (res != NO_ERROR && res != TIMED_OUT) {
                ALOGE("%s: Error waiting for capture requests: %d",
                        __FUNCTION__, res);
                mParent->signalError();
                return false;
            }
            if (!mActive) return true;
        }
        // Active, see if we need a new request
        if (mRequest == NULL) {
            if (mInFlightHead == mInFlightTail) {
                // Go inactive
                ALOGV("Waiting for sensor data");
                mActive = false;
                return true;
            } else {
                mRequest = mInFlightQueue[mInFlightHead].request;
                mBuffer  = mInFlightQueue[mInFlightHead].buffer;
                mInFlightQueue[mInFlightHead].request = NULL;
                mInFlightQueue[mInFlightHead].buffer = NULL;
                mInFlightHead = (mInFlightHead + 1) % kInFlightQueueSize;
            }
        }
    }

    // Active with request, wait on sensor to complete

    nsecs_t captureTime;

    bool gotFrame;
    gotFrame = mParent->mSensor->waitForNewFrame(kWaitPerLoop,
            &captureTime);

    if (!gotFrame) return true;

    // Got sensor data, construct frame and send it out
    ALOGV("Readout: Constructing metadata and frames");

    camera_metadata_entry_t metadataMode;
    res = find_camera_metadata_entry(mRequest,
            ANDROID_REQUEST_METADATA_MODE,
            &metadataMode);

    if (*metadataMode.data.u8 == ANDROID_REQUEST_METADATA_FULL) {
        ALOGV("Metadata requested, constructing");

        camera_metadata_t *frame = NULL;

        size_t frame_entries = get_camera_metadata_entry_count(mRequest);
        size_t frame_data    = get_camera_metadata_data_count(mRequest);

        frame_entries += 2;
        frame_data += 8;

        res = mParent->mFrameQueueDst->dequeue_frame(mParent->mFrameQueueDst,
                frame_entries, frame_data, &frame);

        if (res != NO_ERROR || frame == NULL) {
            ALOGE("%s: Unable to dequeue frame metadata buffer", __FUNCTION__);
            mParent->signalError();
            return false;
        }

        res = append_camera_metadata(frame, mRequest);
        if (res != NO_ERROR) {
            ALOGE("Unable to append request metadata");
        }

        add_camera_metadata_entry(frame,
                ANDROID_SENSOR_TIMESTAMP,
                &captureTime,
                1);

        int32_t hourOfDay = (int32_t)mParent->mSensor->getScene().getHour();
        camera_metadata_entry_t requestedHour;
        res = find_camera_metadata_entry(frame,
                EMULATOR_SCENE_HOUROFDAY,
                &requestedHour);
        if (res == NAME_NOT_FOUND) {
            ALOGV("Adding vendor tag");
            res = add_camera_metadata_entry(frame,
                    EMULATOR_SCENE_HOUROFDAY,
                    &hourOfDay, 1);
            if (res != NO_ERROR) {
                ALOGE("Unable to add vendor tag");
            }
        } else if (res == OK) {
            ALOGV("Replacing value in vendor tag");
            *requestedHour.data.i32 = hourOfDay;
        } else {
            ALOGE("Error looking up vendor tag");
        }

        // TODO: Collect all final values used from sensor in addition to timestamp

        mParent->mFrameQueueDst->enqueue_frame(mParent->mFrameQueueDst,
                frame);
    }

    res = mParent->mRequestQueueSrc->free_request(mParent->mRequestQueueSrc, mRequest);
    if (res != NO_ERROR) {
        ALOGE("%s: Unable to return request buffer to queue: %d",
                __FUNCTION__, res);
        mParent->signalError();
        return false;
    }
    mRequest = NULL;

    ALOGV("Sending image buffer to output stream.");
    GraphicBufferMapper::get().unlock(*mBuffer);
    mParent->mRawStreamOps->enqueue_buffer(mParent->mRawStreamOps,
            captureTime, mBuffer);
    mBuffer = NULL;

    return true;
}

};  /* namespace android */
