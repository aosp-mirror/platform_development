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

#ifndef HW_EMULATOR_CAMERA_EMULATED_CAMERA2_H
#define HW_EMULATOR_CAMERA_EMULATED_CAMERA2_H

/*
 * Contains declaration of a class EmulatedCamera that encapsulates
 * functionality common to all version 2.0 emulated camera devices.  Instances
 * of this class (for each emulated camera) are created during the construction
 * of the EmulatedCameraFactory instance.  This class serves as an entry point
 * for all camera API calls that defined by camera2_device_ops_t API.
 */

#include "hardware/camera2.h"
#include "system/camera_metadata.h"
#include "EmulatedBaseCamera.h"

namespace android {

/* Encapsulates functionality common to all version 2.0 emulated camera devices
 *
 * Note that EmulatedCameraFactory instantiates object of this class just once,
 * when EmulatedCameraFactory instance gets constructed. Connection to /
 * disconnection from the actual camera device is handled by calls to
 * connectDevice(), and closeCamera() methods of this class that are invoked in
 * response to hw_module_methods_t::open, and camera_device::close callbacks.
 */
class EmulatedCamera2 : public camera2_device, public EmulatedBaseCamera {
public:
    /* Constructs EmulatedCamera2 instance.
     * Param:
     *  cameraId - Zero based camera identifier, which is an index of the camera
     *      instance in camera factory's array.
     *  module - Emulated camera HAL module descriptor.
     */
    EmulatedCamera2(int cameraId,
            struct hw_module_t* module);

    /* Destructs EmulatedCamera2 instance. */
    virtual ~EmulatedCamera2();

    /****************************************************************************
     * Abstract API
     ***************************************************************************/

public:

    /****************************************************************************
     * Public API
     ***************************************************************************/

public:
    virtual status_t Initialize();

    /****************************************************************************
     * Camera API implementation
     ***************************************************************************/

public:
    virtual status_t connectCamera(hw_device_t** device);

    virtual status_t closeCamera();

    virtual status_t getCameraInfo(struct camera_info* info);

    /****************************************************************************
     * Camera API implementation.
     * These methods are called from the camera API callback routines.
     ***************************************************************************/

protected:
    /** Request input queue */

    int setRequestQueueSrcOps(
        camera2_metadata_queue_src_ops *request_queue_src_ops);

    int requestQueueNotifyNotEmpty();

    /** Reprocessing input queue */

    int setReprocessQueueSrcOps(
        camera2_metadata_queue_src_ops *reprocess_queue_src_ops);

    int reprocessQueueNotifyNotEmpty();

    /** Frame output queue */

    int setFrameQueueDstOps(camera2_metadata_queue_dst_ops *frame_queue_dst_ops);

    int frameQueueBufferCount();
    int frameQueueDequeue(camera_metadata_t **buffer);
    int frameQueueFree(camera_metadata_t *old_buffer);

    /** Notifications to application */
    int setNotifyCallback(camera2_notify_callback notify_cb);

    /** Count of requests in flight */
    int getInProgressCount();

    /** Cancel all captures in flight */
    int flushCapturesInProgress();

    /** Reprocessing input stream management */
    int reprocessStreamDequeueBuffer(buffer_handle_t** buffer,
            int *stride);

    int reprocessStreamEnqueueBuffer(buffer_handle_t* buffer);

    int reprocessStreamCancelBuffer(buffer_handle_t* buffer);

    int reprocessStreamSetBufferCount(int count);

    int reprocessStreamSetCrop(int left, int top, int right, int bottom);

    int reprocessStreamSetTimestamp(int64_t timestamp);

    int reprocessStreamSetUsage(int usage);

    int reprocessStreamSetSwapInterval(int interval);

    int reprocessStreamGetMinUndequeuedBufferCount(int *count);

    int reprocessStreamLockBuffer(buffer_handle_t *buffer);

    /** Output stream creation and management */

    int getStreamSlotCount();

    int allocateStream(uint32_t stream_slot,
            uint32_t width,
            uint32_t height,
            int format,
            camera2_stream_ops_t *stream_ops);

    int releaseStream(uint32_t stream_slot);

    /** Custom tag definitions */
    const char* getVendorSectionName(uint32_t tag);
    const char* getVendorTagName(uint32_t tag);
    int         getVendorTagType(uint32_t tag);

    /** Shutdown and debug methods */

    int release();

    int dump(int fd);

    int close();

    /****************************************************************************
     * Camera API callbacks as defined by camera2_device_ops structure.  See
     * hardware/libhardware/include/hardware/camera2.h for information on each
     * of these callbacks. Implemented in this class, these callbacks simply
     * dispatch the call into an instance of EmulatedCamera2 class defined in the
     * 'camera_device2' parameter.
     ***************************************************************************/

private:
    /** Input request queue */
    static int set_request_queue_src_ops(camera2_device_t *,
            camera2_metadata_queue_src_ops *queue_src_ops);
    static int get_request_queue_dst_ops(camera2_device_t *,
            camera2_metadata_queue_dst_ops **queue_dst_ops);
    // for get_request_queue_dst_ops
    static int request_queue_notify_queue_not_empty(
        camera2_metadata_queue_dst_ops *);

    /** Input reprocess queue */
    static int set_reprocess_queue_src_ops(camera2_device_t *,
            camera2_metadata_queue_src_ops *reprocess_queue_src_ops);
    static int get_reprocess_queue_dst_ops(camera2_device_t *,
            camera2_metadata_queue_dst_ops **queue_dst_ops);
    // for reprocess_queue_dst_ops
    static int reprocess_queue_notify_queue_not_empty(
            camera2_metadata_queue_dst_ops *);

    /** Output frame queue */
    static int set_frame_queue_dst_ops(camera2_device_t *,
            camera2_metadata_queue_dst_ops *queue_dst_ops);
    static int get_frame_queue_src_ops(camera2_device_t *,
            camera2_metadata_queue_src_ops **queue_src_ops);
    // for get_frame_queue_src_ops
    static int frame_queue_buffer_count(camera2_metadata_queue_src_ops *);
    static int frame_queue_dequeue(camera2_metadata_queue_src_ops *,
            camera_metadata_t **buffer);
    static int frame_queue_free(camera2_metadata_queue_src_ops *,
            camera_metadata_t *old_buffer);

    /** Notifications to application */
    static int set_notify_callback(camera2_device_t *,
            camera2_notify_callback notify_cb);

    /** In-progress request management */
    static int get_in_progress_count(camera2_device_t *);

    static int flush_captures_in_progress(camera2_device_t *);

    /** Input reprocessing stream */
    static int get_reprocess_stream_ops(camera2_device_t *,
            camera2_stream_ops_t **stream);
    // for get_reprocess_stream_ops
    static int reprocess_stream_dequeue_buffer(camera2_stream_ops *,
            buffer_handle_t** buffer, int *stride);
    static int reprocess_stream_enqueue_buffer(camera2_stream_ops *,
            buffer_handle_t* buffer);
    static int reprocess_stream_cancel_buffer(camera2_stream_ops *,
            buffer_handle_t* buffer);
    static int reprocess_stream_set_buffer_count(camera2_stream_ops *,
            int count);
    static int reprocess_stream_set_crop(camera2_stream_ops *,
            int left, int top, int right, int bottom);
    static int reprocess_stream_set_timestamp(camera2_stream_ops *,
            int64_t timestamp);
    static int reprocess_stream_set_usage(camera2_stream_ops *,
            int usage);
    static int reprocess_stream_set_swap_interval(camera2_stream_ops *,
            int interval);
    static int reprocess_stream_get_min_undequeued_buffer_count(
            const camera2_stream_ops *,
            int *count);
    static int reprocess_stream_lock_buffer(camera2_stream_ops *,
            buffer_handle_t* buffer);

    /** Output stream allocation and management */

    static int get_stream_slot_count(camera2_device_t *);

    static int allocate_stream(camera2_device_t *,
            uint32_t stream_slot,
            uint32_t width,
            uint32_t height,
            uint32_t format,
            camera2_stream_ops_t *stream_ops);

    static int release_stream(camera2_device_t *,
            uint32_t stream_slot);

    static void release(camera2_device_t *);

    /** Vendor metadata registration */

    static int get_metadata_vendor_tag_ops(camera2_device_t *,
            vendor_tag_query_ops_t **ops);
    // for get_metadata_vendor_tag_ops
    static const char* get_camera_vendor_section_name(
            const vendor_tag_query_ops_t *,
            uint32_t tag);
    static const char* get_camera_vendor_tag_name(
            const vendor_tag_query_ops_t *,
            uint32_t tag);
    static int get_camera_vendor_tag_type(
            const vendor_tag_query_ops_t *,
            uint32_t tag);

    static int dump(camera2_device_t *, int fd);

    static int close(struct hw_device_t* device);

    /****************************************************************************
     * Data members
     ***************************************************************************/

  private:
    static camera2_device_ops_t sDeviceOps;

    struct QueueDstOps : public camera2_metadata_queue_dst_ops {
        EmulatedCamera2 *parent;
    };

    struct QueueSrcOps : public camera2_metadata_queue_src_ops {
        EmulatedCamera2 *parent;
    };

    struct StreamOps : public camera2_stream_ops {
        EmulatedCamera2 *parent;
    };

    struct TagOps : public vendor_tag_query_ops {
        EmulatedCamera2 *parent;
    };

    QueueDstOps mRequestQueueDstOps;
    QueueDstOps mReprocessQueueDstOps;
    QueueSrcOps mFrameQueueSrcOps;
    StreamOps   mReprocessStreamOps;
    TagOps      mVendorTagOps;
};

}; /* namespace android */

#endif  /* HW_EMULATOR_CAMERA_EMULATED_CAMERA2_H */
