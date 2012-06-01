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

//#define LOG_NDEBUG 0
#define LOG_TAG "EmulatedCamera_FakeCamera2"
#include <utils/Log.h>

#include "EmulatedFakeCamera2.h"
#include "EmulatedCameraFactory.h"
#include <ui/Rect.h>
#include <ui/GraphicBufferMapper.h>

namespace android {

const uint32_t EmulatedFakeCamera2::kAvailableFormats[3] = {
        HAL_PIXEL_FORMAT_RAW_SENSOR,
        HAL_PIXEL_FORMAT_YV12,
        HAL_PIXEL_FORMAT_YCrCb_420_SP
};

const uint32_t EmulatedFakeCamera2::kAvailableRawSizes[2] = {
    640, 480
    //    Sensor::kResolution[0], Sensor::kResolution[1]
};

const uint64_t EmulatedFakeCamera2::kAvailableRawMinDurations[1] = {
    Sensor::kFrameDurationRange[0]
};

const uint32_t EmulatedFakeCamera2::kAvailableProcessedSizes[2] = {
    640, 480
    //    Sensor::kResolution[0], Sensor::kResolution[1]
};

const uint64_t EmulatedFakeCamera2::kAvailableProcessedMinDurations[1] = {
    Sensor::kFrameDurationRange[0]
};

const uint32_t EmulatedFakeCamera2::kAvailableJpegSizes[2] = {
    640, 480
    //    Sensor::kResolution[0], Sensor::kResolution[1]
};

const uint64_t EmulatedFakeCamera2::kAvailableJpegMinDurations[1] = {
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

    res = constructStaticInfo(&mCameraInfo, true);
    if (res != OK) {
        ALOGE("%s: Unable to allocate static info: %s (%d)",
                __FUNCTION__, strerror(-res), res);
        return res;
    }
    res = constructStaticInfo(&mCameraInfo, false);
    if (res != OK) {
        ALOGE("%s: Unable to fill in static info: %s (%d)",
                __FUNCTION__, strerror(-res), res);
        return res;
    }
    if (res != OK) return res;

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

int EmulatedFakeCamera2::constructDefaultRequest(
        int request_template,
        camera_metadata_t **request) {

    if (request == NULL) return BAD_VALUE;
    if (request_template < 0 || request_template >= CAMERA2_TEMPLATE_COUNT) {
        return BAD_VALUE;
    }
    status_t res;
    // Pass 1, calculate size and allocate
    res = constructDefaultRequest(request_template,
            request,
            true);
    if (res != OK) {
        return res;
    }
    // Pass 2, build request
    res = constructDefaultRequest(request_template,
            request,
            false);
    if (res != OK) {
        ALOGE("Unable to populate new request for template %d",
                request_template);
    }

    return res;
}

int EmulatedFakeCamera2::allocateStream(
        uint32_t width,
        uint32_t height,
        int format,
        const camera2_stream_ops_t *stream_ops,
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

    if (format != CAMERA2_HAL_PIXEL_FORMAT_OPAQUE) {
        unsigned int numFormats = sizeof(kAvailableFormats) / sizeof(uint32_t);
        unsigned int formatIdx = 0;
        unsigned int sizeOffsetIdx = 0;
        for (; formatIdx < numFormats; formatIdx++) {
            if (format == (int)kAvailableFormats[formatIdx]) break;
        }
        if (formatIdx == numFormats) {
            ALOGE("%s: Format 0x%x is not supported", __FUNCTION__, format);
            return BAD_VALUE;
        }
    } else {
        // Emulator's opaque format is RGBA
        format = HAL_PIXEL_FORMAT_RGBA_8888;
    }

    const uint32_t *availableSizes;
    size_t availableSizeCount;
    if (format == HAL_PIXEL_FORMAT_RAW_SENSOR) {
        availableSizes = kAvailableRawSizes;
        availableSizeCount = sizeof(kAvailableRawSizes)/sizeof(uint32_t);
    } else {
        availableSizes = kAvailableProcessedSizes;
        availableSizeCount = sizeof(kAvailableProcessedSizes)/sizeof(uint32_t);
    }
    // TODO: JPEG sizes

    unsigned int resIdx = 0;
    for (; resIdx < availableSizeCount; resIdx++) {
        if (availableSizes[resIdx * 2] == width &&
                availableSizes[resIdx * 2 + 1] == height) break;
    }
    if (resIdx == availableSizeCount) {
        ALOGE("%s: Format 0x%x does not support resolution %d, %d", __FUNCTION__,
                format, width, height);
        return BAD_VALUE;
    }

    // TODO: Generalize below to work for variable types of streams, etc.
    // Currently only correct for raw sensor format, sensor resolution.

    ALOG_ASSERT(width == Sensor::kResolution[0],
            "%s: TODO: Only supporting raw sensor size right now", __FUNCTION__);
    ALOG_ASSERT(height == Sensor::kResolution[1],
            "%s: TODO: Only supporting raw sensor size right now", __FUNCTION__);

    mStreamFormat = format;
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
            ALOGE("%s: error reading frame count tag: %s (%d)",
                    __FUNCTION__, strerror(-res), res);
            mParent->signalError();
            return false;
        }
        mNextFrameNumber = *e.data.i32;

        res = find_camera_metadata_entry(mRequest,
                ANDROID_SENSOR_EXPOSURE_TIME,
                &e);
        if (res != NO_ERROR) {
            ALOGE("%s: error reading exposure time tag: %s (%d)",
                    __FUNCTION__, strerror(-res), res);
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
        mParent->mSensor->setDestinationBuffer(img, mParent->mStreamFormat,
                mNextBufferStride);
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

/** Private methods */

status_t EmulatedFakeCamera2::constructStaticInfo(
        camera_metadata_t **info,
        bool sizeRequest) {

    size_t entryCount = 0;
    size_t dataCount = 0;
    status_t ret;

#define ADD_OR_SIZE( tag, data, count ) \
    if ( ( ret = addOrSize(*info, sizeRequest, &entryCount, &dataCount, \
            tag, data, count) ) != OK ) return ret

    // android.lens

    static const float minFocusDistance = 0;
    ADD_OR_SIZE(ANDROID_LENS_MINIMUM_FOCUS_DISTANCE,
            &minFocusDistance, 1);
    ADD_OR_SIZE(ANDROID_LENS_HYPERFOCAL_DISTANCE,
            &minFocusDistance, 1);

    static const float focalLength = 3.30f; // mm
    ADD_OR_SIZE(ANDROID_LENS_AVAILABLE_FOCAL_LENGTHS,
            &focalLength, 1);
    static const float aperture = 2.8f;
    ADD_OR_SIZE(ANDROID_LENS_AVAILABLE_APERTURES,
            &aperture, 1);
    static const float filterDensity = 0;
    ADD_OR_SIZE(ANDROID_LENS_AVAILABLE_FILTER_DENSITY,
            &filterDensity, 1);
    static const uint8_t availableOpticalStabilization =
            ANDROID_LENS_OPTICAL_STABILIZATION_OFF;
    ADD_OR_SIZE(ANDROID_LENS_AVAILABLE_OPTICAL_STABILIZATION,
            &availableOpticalStabilization, 1);

    static const int32_t lensShadingMapSize[] = {1, 1};
    ADD_OR_SIZE(ANDROID_LENS_SHADING_MAP_SIZE, lensShadingMapSize,
            sizeof(lensShadingMapSize)/sizeof(int32_t));

    static const float lensShadingMap[3 * 1 * 1 ] =
            { 1.f, 1.f, 1.f };
    ADD_OR_SIZE(ANDROID_LENS_SHADING_MAP, lensShadingMap,
            sizeof(lensShadingMap)/sizeof(float));

    // Identity transform
    static const int32_t geometricCorrectionMapSize[] = {2, 2};
    ADD_OR_SIZE(ANDROID_LENS_GEOMETRIC_CORRECTION_MAP_SIZE,
            geometricCorrectionMapSize,
            sizeof(geometricCorrectionMapSize)/sizeof(int32_t));

    static const float geometricCorrectionMap[2 * 3 * 2 * 2] = {
            0.f, 0.f,  0.f, 0.f,  0.f, 0.f,
            1.f, 0.f,  1.f, 0.f,  1.f, 0.f,
            0.f, 1.f,  0.f, 1.f,  0.f, 1.f,
            1.f, 1.f,  1.f, 1.f,  1.f, 1.f};
    ADD_OR_SIZE(ANDROID_LENS_GEOMETRIC_CORRECTION_MAP,
            geometricCorrectionMap,
            sizeof(geometricCorrectionMap)/sizeof(float));

    int32_t lensFacing = mFacingBack ?
            ANDROID_LENS_FACING_BACK : ANDROID_LENS_FACING_FRONT;
    ADD_OR_SIZE(ANDROID_LENS_FACING, &lensFacing, 1);

    float lensPosition[3];
    if (mFacingBack) {
        // Back-facing camera is center-top on device
        lensPosition[0] = 0;
        lensPosition[1] = 20;
        lensPosition[2] = -5;
    } else {
        // Front-facing camera is center-right on device
        lensPosition[0] = 20;
        lensPosition[1] = 20;
        lensPosition[2] = 0;
    }
    ADD_OR_SIZE(ANDROID_LENS_POSITION, lensPosition, sizeof(lensPosition)/
            sizeof(float));

    // android.sensor

    ADD_OR_SIZE(ANDROID_SENSOR_EXPOSURE_TIME_RANGE,
            Sensor::kExposureTimeRange, 2);

    ADD_OR_SIZE(ANDROID_SENSOR_MAX_FRAME_DURATION,
            &Sensor::kFrameDurationRange[1], 1);

    ADD_OR_SIZE(ANDROID_SENSOR_AVAILABLE_SENSITIVITIES,
            Sensor::kAvailableSensitivities,
            sizeof(Sensor::kAvailableSensitivities)
            /sizeof(uint32_t));

    ADD_OR_SIZE(ANDROID_SENSOR_COLOR_FILTER_ARRANGEMENT,
            &Sensor::kColorFilterArrangement, 1);

    static const float sensorPhysicalSize[2] = {3.20f, 2.40f}; // mm
    ADD_OR_SIZE(ANDROID_SENSOR_PHYSICAL_SIZE,
            sensorPhysicalSize, 2);

    ADD_OR_SIZE(ANDROID_SENSOR_PIXEL_ARRAY_SIZE,
            Sensor::kResolution, 2);

    ADD_OR_SIZE(ANDROID_SENSOR_ACTIVE_ARRAY_SIZE,
            Sensor::kResolution, 2);

    ADD_OR_SIZE(ANDROID_SENSOR_WHITE_LEVEL,
            &Sensor::kMaxRawValue, 1);

    static const int32_t blackLevelPattern[4] = {
            Sensor::kBlackLevel, Sensor::kBlackLevel,
            Sensor::kBlackLevel, Sensor::kBlackLevel
    };
    ADD_OR_SIZE(ANDROID_SENSOR_BLACK_LEVEL_PATTERN,
            blackLevelPattern, sizeof(blackLevelPattern)/sizeof(int32_t));

    //TODO: sensor color calibration fields

    // android.flash
    static const uint8_t flashAvailable = 0;
    ADD_OR_SIZE(ANDROID_FLASH_AVAILABLE, &flashAvailable, 1);

    static const int64_t flashChargeDuration = 0;
    ADD_OR_SIZE(ANDROID_FLASH_CHARGE_DURATION, &flashChargeDuration, 1);

    // android.tonemap

    static const int32_t tonemapCurvePoints = 128;
    ADD_OR_SIZE(ANDROID_TONEMAP_MAX_CURVE_POINTS, &tonemapCurvePoints, 1);

    // android.scaler

    ADD_OR_SIZE(ANDROID_SCALER_AVAILABLE_FORMATS,
            kAvailableFormats,
            sizeof(kAvailableFormats)/sizeof(uint32_t));

    ADD_OR_SIZE(ANDROID_SCALER_AVAILABLE_RAW_SIZES,
            kAvailableRawSizes,
            sizeof(kAvailableRawSizes)/sizeof(uint32_t));

    ADD_OR_SIZE(ANDROID_SCALER_AVAILABLE_RAW_MIN_DURATIONS,
            kAvailableRawMinDurations,
            sizeof(kAvailableRawMinDurations)/sizeof(uint64_t));

    ADD_OR_SIZE(ANDROID_SCALER_AVAILABLE_PROCESSED_SIZES,
            kAvailableProcessedSizes,
            sizeof(kAvailableProcessedSizes)/sizeof(uint32_t));

    ADD_OR_SIZE(ANDROID_SCALER_AVAILABLE_PROCESSED_MIN_DURATIONS,
            kAvailableProcessedMinDurations,
            sizeof(kAvailableProcessedMinDurations)/sizeof(uint64_t));

    ADD_OR_SIZE(ANDROID_SCALER_AVAILABLE_JPEG_SIZES,
            kAvailableJpegSizes,
            sizeof(kAvailableJpegSizes)/sizeof(uint32_t));

    ADD_OR_SIZE(ANDROID_SCALER_AVAILABLE_JPEG_MIN_DURATIONS,
            kAvailableJpegMinDurations,
            sizeof(kAvailableJpegMinDurations)/sizeof(uint64_t));

    static const float maxZoom = 10;
    ADD_OR_SIZE(ANDROID_SCALER_AVAILABLE_MAX_ZOOM,
            &maxZoom, 1);

    // android.jpeg

    static const int32_t jpegThumbnailSizes[] = {
            160, 120,
            320, 240,
            640, 480
    };
    ADD_OR_SIZE(ANDROID_JPEG_AVAILABLE_THUMBNAIL_SIZES,
            jpegThumbnailSizes, sizeof(jpegThumbnailSizes)/sizeof(int32_t));

    // android.stats

    static const uint8_t availableFaceDetectModes[] = {
            ANDROID_STATS_FACE_DETECTION_OFF
    };
    ADD_OR_SIZE(ANDROID_STATS_AVAILABLE_FACE_DETECT_MODES,
            availableFaceDetectModes,
            sizeof(availableFaceDetectModes));

    static const int32_t maxFaceCount = 0;
    ADD_OR_SIZE(ANDROID_STATS_MAX_FACE_COUNT,
            &maxFaceCount, 1);

    static const int32_t histogramSize = 64;
    ADD_OR_SIZE(ANDROID_STATS_HISTOGRAM_BUCKET_COUNT,
            &histogramSize, 1);

    static const int32_t maxHistogramCount = 1000;
    ADD_OR_SIZE(ANDROID_STATS_MAX_HISTOGRAM_COUNT,
            &maxHistogramCount, 1);

    static const int32_t sharpnessMapSize[2] = {64, 64};
    ADD_OR_SIZE(ANDROID_STATS_SHARPNESS_MAP_SIZE,
            sharpnessMapSize, sizeof(sharpnessMapSize)/sizeof(int32_t));

    static const int32_t maxSharpnessMapValue = 1000;
    ADD_OR_SIZE(ANDROID_STATS_MAX_SHARPNESS_MAP_VALUE,
            &maxSharpnessMapValue, 1);

    // android.control

    static const uint8_t availableSceneModes[] = {
            ANDROID_CONTROL_SCENE_MODE_UNSUPPORTED
    };
    ADD_OR_SIZE(ANDROID_CONTROL_AVAILABLE_SCENE_MODES,
            availableSceneModes, sizeof(availableSceneModes));

    static const uint8_t availableEffects[] = {
            ANDROID_CONTROL_EFFECT_OFF
    };
    ADD_OR_SIZE(ANDROID_CONTROL_AVAILABLE_EFFECTS,
            availableEffects, sizeof(availableEffects));

    int32_t max3aRegions = 0;
    ADD_OR_SIZE(ANDROID_CONTROL_MAX_REGIONS,
            &max3aRegions, 1);

    static const uint8_t availableAeModes[] = {
            ANDROID_CONTROL_AE_OFF,
            ANDROID_CONTROL_AE_ON
    };
    ADD_OR_SIZE(ANDROID_CONTROL_AE_AVAILABLE_MODES,
            availableAeModes, sizeof(availableAeModes));

    static const camera_metadata_rational exposureCompensationStep = {
            1, 3
    };
    ADD_OR_SIZE(ANDROID_CONTROL_AE_EXP_COMPENSATION_STEP,
            &exposureCompensationStep, 1);

    int32_t exposureCompensationRange[] = {-9, 9};
    ADD_OR_SIZE(ANDROID_CONTROL_AE_EXP_COMPENSATION_RANGE,
            exposureCompensationRange,
            sizeof(exposureCompensationRange)/sizeof(int32_t));

    static const int32_t availableTargetFpsRanges[] = {
            5, 30
    };
    ADD_OR_SIZE(ANDROID_CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES,
            availableTargetFpsRanges,
            sizeof(availableTargetFpsRanges)/sizeof(int32_t));

    static const uint8_t availableAntibandingModes[] = {
            ANDROID_CONTROL_AE_ANTIBANDING_OFF,
            ANDROID_CONTROL_AE_ANTIBANDING_AUTO
    };
    ADD_OR_SIZE(ANDROID_CONTROL_AE_AVAILABLE_ANTIBANDING_MODES,
            availableAntibandingModes, sizeof(availableAntibandingModes));

    static const uint8_t availableAwbModes[] = {
            ANDROID_CONTROL_AWB_OFF,
            ANDROID_CONTROL_AWB_AUTO,
            ANDROID_CONTROL_AWB_INCANDESCENT,
            ANDROID_CONTROL_AWB_FLUORESCENT,
            ANDROID_CONTROL_AWB_DAYLIGHT,
            ANDROID_CONTROL_AWB_SHADE
    };
    ADD_OR_SIZE(ANDROID_CONTROL_AWB_AVAILABLE_MODES,
            availableAwbModes, sizeof(availableAwbModes));

    static const uint8_t availableAfModes[] = {
            ANDROID_CONTROL_AF_OFF
    };
    ADD_OR_SIZE(ANDROID_CONTROL_AF_AVAILABLE_MODES,
            availableAfModes, sizeof(availableAfModes));

    static const uint8_t availableVstabModes[] = {
            ANDROID_CONTROL_VIDEO_STABILIZATION_OFF
    };
    ADD_OR_SIZE(ANDROID_CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES,
            availableVstabModes, sizeof(availableVstabModes));

#undef ADD_OR_SIZE
    /** Allocate metadata if sizing */
    if (sizeRequest) {
        ALOGV("Allocating %d entries, %d extra bytes for "
                "static camera info",
                entryCount, dataCount);
        *info = allocate_camera_metadata(entryCount, dataCount);
        if (*info == NULL) {
            ALOGE("Unable to allocate camera static info"
                    "(%d entries, %d bytes extra data)",
                    entryCount, dataCount);
            return NO_MEMORY;
        }
    }
    return OK;
}

status_t EmulatedFakeCamera2::constructDefaultRequest(
        int request_template,
        camera_metadata_t **request,
        bool sizeRequest) {

    size_t entryCount = 0;
    size_t dataCount = 0;
    status_t ret;

#define ADD_OR_SIZE( tag, data, count ) \
    if ( ( ret = addOrSize(*request, sizeRequest, &entryCount, &dataCount, \
            tag, data, count) ) != OK ) return ret

    static const int64_t USEC = 1000LL;
    static const int64_t MSEC = USEC * 1000LL;
    static const int64_t SEC = MSEC * 1000LL;

    /** android.request */

    static const uint8_t metadataMode = ANDROID_REQUEST_METADATA_NONE;
    ADD_OR_SIZE(ANDROID_REQUEST_METADATA_MODE, &metadataMode, 1);

    static const int32_t id = 0;
    ADD_OR_SIZE(ANDROID_REQUEST_ID, &id, 1);

    static const int32_t frameCount = 0;
    ADD_OR_SIZE(ANDROID_REQUEST_FRAME_COUNT, &frameCount, 1);

    // OUTPUT_STREAMS set by user
    entryCount += 1;
    dataCount += 5; // TODO: Should be maximum stream number

    /** android.lens */

    static const float focusDistance = 0;
    ADD_OR_SIZE(ANDROID_LENS_FOCUS_DISTANCE, &focusDistance, 1);

    static const float aperture = 2.8f;
    ADD_OR_SIZE(ANDROID_LENS_APERTURE, &aperture, 1);

    static const float focalLength = 5.0f;
    ADD_OR_SIZE(ANDROID_LENS_FOCAL_LENGTH, &focalLength, 1);

    static const float filterDensity = 0;
    ADD_OR_SIZE(ANDROID_LENS_FILTER_DENSITY, &filterDensity, 1);

    static const uint8_t opticalStabilizationMode =
            ANDROID_LENS_OPTICAL_STABILIZATION_OFF;
    ADD_OR_SIZE(ANDROID_LENS_OPTICAL_STABILIZATION_MODE,
            &opticalStabilizationMode, 1);

    // FOCUS_RANGE set only in frame

    /** android.sensor */

    static const int64_t exposureTime = 30 * MSEC;
    ADD_OR_SIZE(ANDROID_SENSOR_EXPOSURE_TIME, &exposureTime, 1);

    static const int64_t frameDuration = 33333333L; // 1/30 s
    ADD_OR_SIZE(ANDROID_SENSOR_FRAME_DURATION, &frameDuration, 1);

    static const int32_t sensitivity = 400;
    ADD_OR_SIZE(ANDROID_SENSOR_SENSITIVITY, &sensitivity, 1);

    // TIMESTAMP set only in frame

    /** android.flash */

    static const uint8_t flashMode = ANDROID_FLASH_OFF;
    ADD_OR_SIZE(ANDROID_FLASH_MODE, &flashMode, 1);

    static const uint8_t flashPower = 10;
    ADD_OR_SIZE(ANDROID_FLASH_FIRING_POWER, &flashPower, 1);

    static const int64_t firingTime = 0;
    ADD_OR_SIZE(ANDROID_FLASH_FIRING_TIME, &firingTime, 1);

    /** Processing block modes */
    uint8_t hotPixelMode = 0;
    uint8_t demosaicMode = 0;
    uint8_t noiseMode = 0;
    uint8_t shadingMode = 0;
    uint8_t geometricMode = 0;
    uint8_t colorMode = 0;
    uint8_t tonemapMode = 0;
    uint8_t edgeMode = 0;
    switch (request_template) {
      case CAMERA2_TEMPLATE_PREVIEW:
        hotPixelMode = ANDROID_PROCESSING_FAST;
        demosaicMode = ANDROID_PROCESSING_FAST;
        noiseMode = ANDROID_PROCESSING_FAST;
        shadingMode = ANDROID_PROCESSING_FAST;
        geometricMode = ANDROID_PROCESSING_FAST;
        colorMode = ANDROID_PROCESSING_FAST;
        tonemapMode = ANDROID_PROCESSING_FAST;
        edgeMode = ANDROID_PROCESSING_FAST;
        break;
      case CAMERA2_TEMPLATE_STILL_CAPTURE:
        hotPixelMode = ANDROID_PROCESSING_HIGH_QUALITY;
        demosaicMode = ANDROID_PROCESSING_HIGH_QUALITY;
        noiseMode = ANDROID_PROCESSING_HIGH_QUALITY;
        shadingMode = ANDROID_PROCESSING_HIGH_QUALITY;
        geometricMode = ANDROID_PROCESSING_HIGH_QUALITY;
        colorMode = ANDROID_PROCESSING_HIGH_QUALITY;
        tonemapMode = ANDROID_PROCESSING_HIGH_QUALITY;
        edgeMode = ANDROID_PROCESSING_HIGH_QUALITY;
        break;
      case CAMERA2_TEMPLATE_VIDEO_RECORD:
        hotPixelMode = ANDROID_PROCESSING_FAST;
        demosaicMode = ANDROID_PROCESSING_FAST;
        noiseMode = ANDROID_PROCESSING_FAST;
        shadingMode = ANDROID_PROCESSING_FAST;
        geometricMode = ANDROID_PROCESSING_FAST;
        colorMode = ANDROID_PROCESSING_FAST;
        tonemapMode = ANDROID_PROCESSING_FAST;
        edgeMode = ANDROID_PROCESSING_FAST;
        break;
      case CAMERA2_TEMPLATE_VIDEO_SNAPSHOT:
        hotPixelMode = ANDROID_PROCESSING_HIGH_QUALITY;
        demosaicMode = ANDROID_PROCESSING_HIGH_QUALITY;
        noiseMode = ANDROID_PROCESSING_HIGH_QUALITY;
        shadingMode = ANDROID_PROCESSING_HIGH_QUALITY;
        geometricMode = ANDROID_PROCESSING_HIGH_QUALITY;
        colorMode = ANDROID_PROCESSING_HIGH_QUALITY;
        tonemapMode = ANDROID_PROCESSING_HIGH_QUALITY;
        edgeMode = ANDROID_PROCESSING_HIGH_QUALITY;
        break;
      case CAMERA2_TEMPLATE_ZERO_SHUTTER_LAG:
        hotPixelMode = ANDROID_PROCESSING_HIGH_QUALITY;
        demosaicMode = ANDROID_PROCESSING_HIGH_QUALITY;
        noiseMode = ANDROID_PROCESSING_HIGH_QUALITY;
        shadingMode = ANDROID_PROCESSING_HIGH_QUALITY;
        geometricMode = ANDROID_PROCESSING_HIGH_QUALITY;
        colorMode = ANDROID_PROCESSING_HIGH_QUALITY;
        tonemapMode = ANDROID_PROCESSING_HIGH_QUALITY;
        edgeMode = ANDROID_PROCESSING_HIGH_QUALITY;
        break;
      default:
        hotPixelMode = ANDROID_PROCESSING_FAST;
        demosaicMode = ANDROID_PROCESSING_FAST;
        noiseMode = ANDROID_PROCESSING_FAST;
        shadingMode = ANDROID_PROCESSING_FAST;
        geometricMode = ANDROID_PROCESSING_FAST;
        colorMode = ANDROID_PROCESSING_FAST;
        tonemapMode = ANDROID_PROCESSING_FAST;
        edgeMode = ANDROID_PROCESSING_FAST;
        break;
    }
    ADD_OR_SIZE(ANDROID_HOT_PIXEL_MODE, &hotPixelMode, 1);
    ADD_OR_SIZE(ANDROID_DEMOSAIC_MODE, &demosaicMode, 1);
    ADD_OR_SIZE(ANDROID_NOISE_MODE, &noiseMode, 1);
    ADD_OR_SIZE(ANDROID_SHADING_MODE, &shadingMode, 1);
    ADD_OR_SIZE(ANDROID_GEOMETRIC_MODE, &geometricMode, 1);
    ADD_OR_SIZE(ANDROID_COLOR_MODE, &colorMode, 1);
    ADD_OR_SIZE(ANDROID_TONEMAP_MODE, &tonemapMode, 1);
    ADD_OR_SIZE(ANDROID_EDGE_MODE, &edgeMode, 1);

    /** android.noise */
    static const uint8_t noiseStrength = 5;
    ADD_OR_SIZE(ANDROID_NOISE_STRENGTH, &noiseStrength, 1);

    /** android.color */
    static const float colorTransform[9] = {
        1.0f, 0.f, 0.f,
        0.f, 1.f, 0.f,
        0.f, 0.f, 1.f
    };
    ADD_OR_SIZE(ANDROID_COLOR_TRANSFORM, colorTransform, 9);

    /** android.tonemap */
    static const float tonemapCurve[4] = {
        0.f, 0.f,
        1.f, 1.f
    };
    ADD_OR_SIZE(ANDROID_TONEMAP_CURVE_RED, tonemapCurve, 4);
    ADD_OR_SIZE(ANDROID_TONEMAP_CURVE_GREEN, tonemapCurve, 4);
    ADD_OR_SIZE(ANDROID_TONEMAP_CURVE_BLUE, tonemapCurve, 4);

    /** android.edge */
    static const uint8_t edgeStrength = 5;
    ADD_OR_SIZE(ANDROID_EDGE_STRENGTH, &edgeStrength, 1);

    /** android.scaler */
    static const int32_t cropRegion[3] = {
        0, 0, Sensor::kResolution[0]
    };
    ADD_OR_SIZE(ANDROID_SCALER_CROP_REGION, cropRegion, 3);

    /** android.jpeg */
    static const int32_t jpegQuality = 80;
    ADD_OR_SIZE(ANDROID_JPEG_QUALITY, &jpegQuality, 1);

    static const int32_t thumbnailSize[2] = {
        640, 480
    };
    ADD_OR_SIZE(ANDROID_JPEG_THUMBNAIL_SIZE, thumbnailSize, 2);

    static const int32_t thumbnailQuality = 80;
    ADD_OR_SIZE(ANDROID_JPEG_THUMBNAIL_QUALITY, &thumbnailQuality, 1);

    static const double gpsCoordinates[2] = {
        0, 0
    };
    ADD_OR_SIZE(ANDROID_JPEG_GPS_COORDINATES, gpsCoordinates, 2);

    static const uint8_t gpsProcessingMethod[32] = "None";
    ADD_OR_SIZE(ANDROID_JPEG_GPS_PROCESSING_METHOD, gpsProcessingMethod, 32);

    static const int64_t gpsTimestamp = 0;
    ADD_OR_SIZE(ANDROID_JPEG_GPS_TIMESTAMP, &gpsTimestamp, 1);

    static const int32_t jpegOrientation = 0;
    ADD_OR_SIZE(ANDROID_JPEG_ORIENTATION, &jpegOrientation, 1);

    /** android.stats */

    static const uint8_t faceDetectMode = ANDROID_STATS_FACE_DETECTION_OFF;
    ADD_OR_SIZE(ANDROID_STATS_FACE_DETECT_MODE, &faceDetectMode, 1);

    static const uint8_t histogramMode = ANDROID_STATS_OFF;
    ADD_OR_SIZE(ANDROID_STATS_HISTOGRAM_MODE, &histogramMode, 1);

    static const uint8_t sharpnessMapMode = ANDROID_STATS_OFF;
    ADD_OR_SIZE(ANDROID_STATS_SHARPNESS_MAP_MODE, &sharpnessMapMode, 1);

    // faceRectangles, faceScores, faceLandmarks, faceIds, histogram,
    // sharpnessMap only in frames

    /** android.control */

    uint8_t controlIntent = 0;
    switch (request_template) {
      case CAMERA2_TEMPLATE_PREVIEW:
        controlIntent = ANDROID_CONTROL_INTENT_PREVIEW;
        break;
      case CAMERA2_TEMPLATE_STILL_CAPTURE:
        controlIntent = ANDROID_CONTROL_INTENT_STILL_CAPTURE;
        break;
      case CAMERA2_TEMPLATE_VIDEO_RECORD:
        controlIntent = ANDROID_CONTROL_INTENT_VIDEO_RECORD;
        break;
      case CAMERA2_TEMPLATE_VIDEO_SNAPSHOT:
        controlIntent = ANDROID_CONTROL_INTENT_VIDEO_SNAPSHOT;
        break;
      case CAMERA2_TEMPLATE_ZERO_SHUTTER_LAG:
        controlIntent = ANDROID_CONTROL_INTENT_ZERO_SHUTTER_LAG;
        break;
      default:
        controlIntent = ANDROID_CONTROL_INTENT_CUSTOM;
        break;
    }
    ADD_OR_SIZE(ANDROID_CONTROL_CAPTURE_INTENT, &controlIntent, 1);

    static const uint8_t controlMode = ANDROID_CONTROL_AUTO;
    ADD_OR_SIZE(ANDROID_CONTROL_MODE, &controlMode, 1);

    static const uint8_t effectMode = ANDROID_CONTROL_EFFECT_OFF;
    ADD_OR_SIZE(ANDROID_CONTROL_EFFECT_MODE, &effectMode, 1);

    static const uint8_t sceneMode = ANDROID_CONTROL_SCENE_MODE_FACE_PRIORITY;
    ADD_OR_SIZE(ANDROID_CONTROL_SCENE_MODE, &sceneMode, 1);

    static const uint8_t aeMode = ANDROID_CONTROL_AE_ON_AUTO_FLASH;
    ADD_OR_SIZE(ANDROID_CONTROL_AE_MODE, &aeMode, 1);

    static const int32_t controlRegions[5] = {
        0, 0, Sensor::kResolution[0], Sensor::kResolution[1], 1000
    };
    ADD_OR_SIZE(ANDROID_CONTROL_AE_REGIONS, controlRegions, 5);

    static const int32_t aeExpCompensation = 0;
    ADD_OR_SIZE(ANDROID_CONTROL_AE_EXP_COMPENSATION, &aeExpCompensation, 1);

    static const int32_t aeTargetFpsRange[2] = {
        10, 30
    };
    ADD_OR_SIZE(ANDROID_CONTROL_AE_TARGET_FPS_RANGE, aeTargetFpsRange, 2);

    static const uint8_t aeAntibandingMode =
            ANDROID_CONTROL_AE_ANTIBANDING_AUTO;
    ADD_OR_SIZE(ANDROID_CONTROL_AE_ANTIBANDING_MODE, &aeAntibandingMode, 1);

    static const uint8_t awbMode =
            ANDROID_CONTROL_AWB_AUTO;
    ADD_OR_SIZE(ANDROID_CONTROL_AWB_MODE, &awbMode, 1);

    ADD_OR_SIZE(ANDROID_CONTROL_AWB_REGIONS, controlRegions, 5);

    uint8_t afMode = 0;
    switch (request_template) {
      case CAMERA2_TEMPLATE_PREVIEW:
        afMode = ANDROID_CONTROL_AF_AUTO;
        break;
      case CAMERA2_TEMPLATE_STILL_CAPTURE:
        afMode = ANDROID_CONTROL_AF_AUTO;
        break;
      case CAMERA2_TEMPLATE_VIDEO_RECORD:
        afMode = ANDROID_CONTROL_AF_CONTINUOUS_VIDEO;
        break;
      case CAMERA2_TEMPLATE_VIDEO_SNAPSHOT:
        afMode = ANDROID_CONTROL_AF_CONTINUOUS_VIDEO;
        break;
      case CAMERA2_TEMPLATE_ZERO_SHUTTER_LAG:
        afMode = ANDROID_CONTROL_AF_CONTINUOUS_PICTURE;
        break;
      default:
        afMode = ANDROID_CONTROL_AF_AUTO;
        break;
    }
    ADD_OR_SIZE(ANDROID_CONTROL_AF_MODE, &afMode, 1);

    ADD_OR_SIZE(ANDROID_CONTROL_AF_REGIONS, controlRegions, 5);

    static const uint8_t vstabMode = ANDROID_CONTROL_VIDEO_STABILIZATION_OFF;
    ADD_OR_SIZE(ANDROID_CONTROL_VIDEO_STABILIZATION_MODE, &vstabMode, 1);

    // aeState, awbState, afState only in frame

    /** Allocate metadata if sizing */
    if (sizeRequest) {
        ALOGV("Allocating %d entries, %d extra bytes for "
                "request template type %d",
                entryCount, dataCount, request_template);
        *request = allocate_camera_metadata(entryCount, dataCount);
        if (*request == NULL) {
            ALOGE("Unable to allocate new request template type %d "
                    "(%d entries, %d bytes extra data)", request_template,
                    entryCount, dataCount);
            return NO_MEMORY;
        }
    }
    return OK;
#undef ADD_OR_SIZE
}

status_t EmulatedFakeCamera2::addOrSize(camera_metadata_t *request,
        bool sizeRequest,
        size_t *entryCount,
        size_t *dataCount,
        uint32_t tag,
        const void *entryData,
        size_t entryDataCount) {
    status_t res;
    if (!sizeRequest) {
        return add_camera_metadata_entry(request, tag, entryData,
                entryDataCount);
    } else {
        int type = get_camera_metadata_tag_type(tag);
        if (type < 0 ) return BAD_VALUE;
        (*entryCount)++;
        (*dataCount) += calculate_camera_metadata_entry_data_size(type,
                entryDataCount);
        return OK;
    }
}


};  /* namespace android */
