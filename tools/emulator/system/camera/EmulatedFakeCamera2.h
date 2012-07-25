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

#ifndef HW_EMULATOR_CAMERA_EMULATED_FAKE_CAMERA2_H
#define HW_EMULATOR_CAMERA_EMULATED_FAKE_CAMERA2_H

/*
 * Contains declaration of a class EmulatedFakeCamera2 that encapsulates
 * functionality of a fake camera that implements version 2 of the camera device
 * interface.
 */

#include "EmulatedCamera2.h"
#include "fake-pipeline2/Base.h"
#include "fake-pipeline2/Sensor.h"
#include "fake-pipeline2/JpegCompressor.h"
#include <utils/Condition.h>
#include <utils/KeyedVector.h>
#include <utils/String8.h>
#include <utils/String16.h>
#include <utils/Thread.h>

namespace android {

/* Encapsulates functionality of an advanced fake camera.  This camera contains
 * a simple simulation of a scene, sensor, and image processing pipeline.
 */
class EmulatedFakeCamera2 : public EmulatedCamera2 {
public:
    /* Constructs EmulatedFakeCamera instance. */
    EmulatedFakeCamera2(int cameraId, bool facingBack, struct hw_module_t* module);

    /* Destructs EmulatedFakeCamera instance. */
    ~EmulatedFakeCamera2();

    /****************************************************************************
     * EmulatedCamera2 virtual overrides.
     ***************************************************************************/

public:
    /* Initializes EmulatedFakeCamera2 instance. */
    status_t Initialize();

    /****************************************************************************
     * Camera Module API and generic hardware device API implementation
     ***************************************************************************/
public:

    virtual status_t connectCamera(hw_device_t** device);

    virtual status_t closeCamera();

    virtual status_t getCameraInfo(struct camera_info *info);

    /****************************************************************************
     * EmulatedCamera2 abstract API implementation.
     ***************************************************************************/
protected:
    /** Request input queue */

    virtual int requestQueueNotify();

    /** Count of requests in flight */
    virtual int getInProgressCount();

    /** Cancel all captures in flight */
    //virtual int flushCapturesInProgress();

    /** Construct default request */
    virtual int constructDefaultRequest(
            int request_template,
            camera_metadata_t **request);

    virtual int allocateStream(
            uint32_t width,
            uint32_t height,
            int format,
            const camera2_stream_ops_t *stream_ops,
            uint32_t *stream_id,
            uint32_t *format_actual,
            uint32_t *usage,
            uint32_t *max_buffers);

    virtual int registerStreamBuffers(
            uint32_t stream_id,
            int num_buffers,
            buffer_handle_t *buffers);

    virtual int releaseStream(uint32_t stream_id);

    // virtual int allocateReprocessStream(
    //         uint32_t width,
    //         uint32_t height,
    //         uint32_t format,
    //         const camera2_stream_ops_t *stream_ops,
    //         uint32_t *stream_id,
    //         uint32_t *format_actual,
    //         uint32_t *usage,
    //         uint32_t *max_buffers);

    // virtual int releaseReprocessStream(uint32_t stream_id);

    // virtual int triggerAction(uint32_t trigger_id,
    //        int ext1,
    //        int ext2);

    /** Custom tag definitions */
    virtual const char* getVendorSectionName(uint32_t tag);
    virtual const char* getVendorTagName(uint32_t tag);
    virtual int         getVendorTagType(uint32_t tag);

    /** Debug methods */

    virtual int dump(int fd);

public:
    /****************************************************************************
     * Utility methods called by configure/readout threads and pipeline
     ***************************************************************************/

    // Get information about a given stream. Will lock mMutex
    const Stream &getStreamInfo(uint32_t streamId);

    // Notifies rest of camera subsystem of serious error
    void signalError();

private:
    /****************************************************************************
     * Utility methods
     ***************************************************************************/
    /** Construct static camera metadata, two-pass */
    status_t constructStaticInfo(
            camera_metadata_t **info,
            bool sizeRequest) const;

    /** Two-pass implementation of constructDefaultRequest */
    status_t constructDefaultRequest(
            int request_template,
            camera_metadata_t **request,
            bool sizeRequest) const;
    /** Helper function for constructDefaultRequest */
    static status_t addOrSize( camera_metadata_t *request,
            bool sizeRequest,
            size_t *entryCount,
            size_t *dataCount,
            uint32_t tag,
            const void *entry_data,
            size_t entry_count);

    /** Determine if the stream id is listed in any currently-in-flight
     * requests. Assumes mMutex is locked */
    bool isStreamInUse(uint32_t streamId);

    /****************************************************************************
     * Pipeline controller threads
     ***************************************************************************/

    class ConfigureThread: public Thread {
      public:
        ConfigureThread(EmulatedFakeCamera2 *parent);
        ~ConfigureThread();

        status_t waitUntilRunning();
        status_t newRequestAvailable();
        status_t readyToRun();

        bool isStreamInUse(uint32_t id);
        int getInProgressCount();
      private:
        EmulatedFakeCamera2 *mParent;

        bool mRunning;
        bool threadLoop();

        Mutex mInputMutex; // Protects mActive, mRequestCount
        Condition mInputSignal;
        bool mActive; // Whether we're waiting for input requests or actively
                      // working on them
        size_t mRequestCount;

        camera_metadata_t *mRequest;

        Mutex mInternalsMutex; // Lock before accessing below members.
        bool    mNextNeedsJpeg;
        int32_t mNextFrameNumber;
        int64_t mNextExposureTime;
        int64_t mNextFrameDuration;
        int32_t mNextSensitivity;
        Buffers *mNextBuffers;
    };

    class ReadoutThread: public Thread {
      public:
        ReadoutThread(EmulatedFakeCamera2 *parent);
        ~ReadoutThread();

        status_t readyToRun();

        // Input
        status_t waitUntilRunning();
        void setNextCapture(camera_metadata_t *request,
                Buffers *buffers);

        bool isStreamInUse(uint32_t id);
        int getInProgressCount();
      private:
        EmulatedFakeCamera2 *mParent;

        bool mRunning;
        bool threadLoop();

        // Inputs
        Mutex mInputMutex; // Protects mActive, mInFlightQueue, mRequestCount
        Condition mInputSignal;
        bool mActive;

        static const int kInFlightQueueSize = 4;
        struct InFlightQueue {
            camera_metadata_t *request;
            Buffers *buffers;
        } *mInFlightQueue;

        size_t mInFlightHead;
        size_t mInFlightTail;

        size_t mRequestCount;

        // Internals
        Mutex mInternalsMutex;
        camera_metadata_t *mRequest;
        Buffers *mBuffers;

    };

    /****************************************************************************
     * Static configuration information
     ***************************************************************************/
private:
    static const uint32_t kMaxRawStreamCount = 1;
    static const uint32_t kMaxProcessedStreamCount = 3;
    static const uint32_t kMaxJpegStreamCount = 1;
    static const uint32_t kAvailableFormats[];
    static const uint32_t kAvailableRawSizes[];
    static const uint64_t kAvailableRawMinDurations[];
    static const uint32_t kAvailableProcessedSizes[];
    static const uint64_t kAvailableProcessedMinDurations[];
    static const uint32_t kAvailableJpegSizes[];
    static const uint64_t kAvailableJpegMinDurations[];

    /****************************************************************************
     * Data members.
     ***************************************************************************/

protected:
    /* Facing back (true) or front (false) switch. */
    bool mFacingBack;

private:
    /** Mutex for calls through camera2 device interface */
    Mutex mMutex;

    /** Stream manipulation */
    uint32_t mNextStreamId;
    uint32_t mRawStreamCount;
    uint32_t mProcessedStreamCount;
    uint32_t mJpegStreamCount;

    KeyedVector<uint32_t, Stream> mStreams;

    /** Simulated hardware interfaces */
    sp<Sensor> mSensor;
    sp<JpegCompressor> mJpegCompressor;

    /** Pipeline control threads */
    sp<ConfigureThread> mConfigureThread;
    sp<ReadoutThread>   mReadoutThread;
};

}; /* namespace android */

#endif  /* HW_EMULATOR_CAMERA_EMULATED_FAKE_CAMERA2_H */
