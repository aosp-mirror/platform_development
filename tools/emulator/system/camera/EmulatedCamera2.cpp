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
 * Contains implementation of a class EmulatedCamera that encapsulates
 * functionality common to all version 2.0 emulated camera devices.  Instances
 * of this class (for each emulated camera) are created during the construction
 * of the EmulatedCameraFactory instance.  This class serves as an entry point
 * for all camera API calls that defined by camera2_device_ops_t API.
 */

#define LOG_NDEBUG 0
#define LOG_TAG "EmulatedCamera2_Camera"
#include <cutils/log.h>

#include "EmulatedCamera2.h"
#include "system/camera_metadata.h"

namespace android {

/* Constructs EmulatedCamera2 instance.
 * Param:
 *  cameraId - Zero based camera identifier, which is an index of the camera
 *      instance in camera factory's array.
 *  module - Emulated camera HAL module descriptor.
 */
EmulatedCamera2::EmulatedCamera2(int cameraId,
        struct hw_module_t* module):
        EmulatedBaseCamera(cameraId,
                CAMERA_DEVICE_API_VERSION_2_0,
                &common,
                module)
{
    common.close = EmulatedCamera2::close;
    ops = &sDeviceOps;
    priv = this;

    mRequestQueueDstOps.notify_queue_not_empty =
            EmulatedCamera2::request_queue_notify_queue_not_empty;
    mRequestQueueDstOps.parent                 = this;

    mRequestQueueDstOps.notify_queue_not_empty =
            EmulatedCamera2::reprocess_queue_notify_queue_not_empty;
    mReprocessQueueDstOps.parent               = this;

    mFrameQueueSrcOps.buffer_count = EmulatedCamera2::frame_queue_buffer_count;
    mFrameQueueSrcOps.dequeue      = EmulatedCamera2::frame_queue_dequeue;
    mFrameQueueSrcOps.free         = EmulatedCamera2::frame_queue_free;
    mFrameQueueSrcOps.parent       = this;

    mReprocessStreamOps.dequeue_buffer =
            EmulatedCamera2::reprocess_stream_dequeue_buffer;
    mReprocessStreamOps.enqueue_buffer =
            EmulatedCamera2::reprocess_stream_enqueue_buffer;
    mReprocessStreamOps.cancel_buffer =
            EmulatedCamera2::reprocess_stream_cancel_buffer;
    mReprocessStreamOps.set_buffer_count =
            EmulatedCamera2::reprocess_stream_set_buffer_count;
    mReprocessStreamOps.set_crop = EmulatedCamera2::reprocess_stream_set_crop;
    mReprocessStreamOps.set_timestamp =
            EmulatedCamera2::reprocess_stream_set_timestamp;
    mReprocessStreamOps.set_usage = EmulatedCamera2::reprocess_stream_set_usage;
    mReprocessStreamOps.get_min_undequeued_buffer_count =
            EmulatedCamera2::reprocess_stream_get_min_undequeued_buffer_count;
    mReprocessStreamOps.lock_buffer =
            EmulatedCamera2::reprocess_stream_lock_buffer;
    mReprocessStreamOps.parent   = this;

    mVendorTagOps.get_camera_vendor_section_name =
            EmulatedCamera2::get_camera_vendor_section_name;
    mVendorTagOps.get_camera_vendor_tag_name =
            EmulatedCamera2::get_camera_vendor_tag_name;
    mVendorTagOps.get_camera_vendor_tag_type =
            EmulatedCamera2::get_camera_vendor_tag_type;
    mVendorTagOps.parent = this;
}

/* Destructs EmulatedCamera2 instance. */
EmulatedCamera2::~EmulatedCamera2() {
}

/****************************************************************************
 * Abstract API
 ***************************************************************************/

/****************************************************************************
 * Public API
 ***************************************************************************/

status_t EmulatedCamera2::Initialize() {
    return NO_ERROR;
}

/****************************************************************************
 * Camera API implementation
 ***************************************************************************/

status_t EmulatedCamera2::connectCamera(hw_device_t** device) {
    return NO_ERROR;
}

status_t EmulatedCamera2::closeCamera() {
    return NO_ERROR;
}

status_t EmulatedCamera2::getCameraInfo(struct camera_info* info) {

    return EmulatedBaseCamera::getCameraInfo(info);
}

/****************************************************************************
 * Camera API implementation.
 * These methods are called from the camera API callback routines.
 ***************************************************************************/

/** Request input queue */

int EmulatedCamera2::setRequestQueueSrcOps(
    camera2_metadata_queue_src_ops *request_queue_src_ops) {
    return NO_ERROR;
}

int EmulatedCamera2::requestQueueNotifyNotEmpty() {
    return NO_ERROR;
}

/** Reprocessing input queue */

int EmulatedCamera2::setReprocessQueueSrcOps(
    camera2_metadata_queue_src_ops *reprocess_queue_src_ops) {
    return NO_ERROR;
}

int EmulatedCamera2::reprocessQueueNotifyNotEmpty() {
    return NO_ERROR;
}

/** Frame output queue */

int EmulatedCamera2::setFrameQueueDstOps(camera2_metadata_queue_dst_ops *frame_queue_dst_ops) {
    return NO_ERROR;
}

int EmulatedCamera2::frameQueueBufferCount() {
    return NO_ERROR;
}
int EmulatedCamera2::frameQueueDequeue(camera_metadata_t **buffer) {
    return NO_ERROR;
}
int EmulatedCamera2::frameQueueFree(camera_metadata_t *old_buffer) {
    return NO_ERROR;
}

/** Notifications to application */
int EmulatedCamera2::setNotifyCallback(camera2_notify_callback notify_cb) {
    return NO_ERROR;
}

/** Count of requests in flight */
int EmulatedCamera2::getInProgressCount() {
    return NO_ERROR;
}

/** Cancel all captures in flight */
int EmulatedCamera2::flushCapturesInProgress() {
    return NO_ERROR;
}

/** Reprocessing input stream management */
int EmulatedCamera2::reprocessStreamDequeueBuffer(buffer_handle_t** buffer,
        int *stride) {
    return NO_ERROR;
}

int EmulatedCamera2::reprocessStreamEnqueueBuffer(buffer_handle_t* buffer) {
    return NO_ERROR;
}

int EmulatedCamera2::reprocessStreamCancelBuffer(buffer_handle_t* buffer) {
    return NO_ERROR;
}

int EmulatedCamera2::reprocessStreamSetBufferCount(int count) {
    return NO_ERROR;
}

int EmulatedCamera2::reprocessStreamSetCrop(int left, int top, int right, int bottom) {
    return NO_ERROR;
}

int EmulatedCamera2::reprocessStreamSetTimestamp(int64_t timestamp) {
    return NO_ERROR;
}

int EmulatedCamera2::reprocessStreamSetUsage(int usage) {
    return NO_ERROR;
}

int EmulatedCamera2::reprocessStreamSetSwapInterval(int interval) {
    return NO_ERROR;
}

int EmulatedCamera2::reprocessStreamGetMinUndequeuedBufferCount(int *count) {
    return NO_ERROR;
}

int EmulatedCamera2::reprocessStreamLockBuffer(buffer_handle_t *buffer) {
    return NO_ERROR;
}

/** Output stream creation and management */

int EmulatedCamera2::getStreamSlotCount() {
    return NO_ERROR;
}

int EmulatedCamera2::allocateStream(uint32_t stream_slot,
        uint32_t width,
        uint32_t height,
        int format,
        camera2_stream_ops_t *stream_ops) {
    return NO_ERROR;
}

int EmulatedCamera2::releaseStream(uint32_t stream_slot) {
    return NO_ERROR;
}

/** Custom tag query methods */

const char* EmulatedCamera2::getVendorSectionName(uint32_t tag) {
    return NULL;
}

const char* EmulatedCamera2::getVendorTagName(uint32_t tag) {
    return NULL;
}

int EmulatedCamera2::getVendorTagType(uint32_t tag) {
    return -1;
}

/** Shutdown and debug methods */

int EmulatedCamera2::release() {
    return NO_ERROR;
}

int EmulatedCamera2::dump(int fd) {
    return NO_ERROR;
}

/****************************************************************************
 * Private API.
 ***************************************************************************/

/****************************************************************************
 * Camera API callbacks as defined by camera2_device_ops structure.  See
 * hardware/libhardware/include/hardware/camera2.h for information on each
 * of these callbacks. Implemented in this class, these callbacks simply
 * dispatch the call into an instance of EmulatedCamera2 class defined by the
 * 'camera_device2' parameter.
 ***************************************************************************/

int EmulatedCamera2::set_request_queue_src_ops(struct camera2_device *d,
        camera2_metadata_queue_src_ops *queue_src_ops) {
    EmulatedCamera2* ec = static_cast<EmulatedCamera2*>(d);
    return ec->setRequestQueueSrcOps(queue_src_ops);
}

int EmulatedCamera2::get_request_queue_dst_ops(struct camera2_device *d,
        camera2_metadata_queue_dst_ops **queue_dst_ops) {
    EmulatedCamera2* ec = static_cast<EmulatedCamera2*>(d);
    *queue_dst_ops = static_cast<camera2_metadata_queue_dst_ops*>(
        &ec->mRequestQueueDstOps);
    return NO_ERROR;
}

int EmulatedCamera2::request_queue_notify_queue_not_empty(
        camera2_metadata_queue_dst_ops *q) {
    EmulatedCamera2* ec = static_cast<QueueDstOps*>(q)->parent;
    return ec->requestQueueNotifyNotEmpty();
}

int EmulatedCamera2::set_reprocess_queue_src_ops(struct camera2_device *d,
        camera2_metadata_queue_src_ops *queue_src_ops) {
    EmulatedCamera2* ec = static_cast<EmulatedCamera2*>(d);
    return ec->setReprocessQueueSrcOps(queue_src_ops);
}

int EmulatedCamera2::get_reprocess_queue_dst_ops(struct camera2_device *d,
        camera2_metadata_queue_dst_ops **queue_dst_ops) {
    EmulatedCamera2* ec = static_cast<EmulatedCamera2*>(d);
    *queue_dst_ops = static_cast<camera2_metadata_queue_dst_ops*>(
        &ec->mReprocessQueueDstOps);
    return NO_ERROR;
}

int EmulatedCamera2::reprocess_queue_notify_queue_not_empty(
        camera2_metadata_queue_dst_ops *q) {
    EmulatedCamera2* ec = static_cast<QueueDstOps*>(q)->parent;
    return ec->reprocessQueueNotifyNotEmpty();
}

int EmulatedCamera2::set_frame_queue_dst_ops(struct camera2_device *d,
        camera2_metadata_queue_dst_ops *queue_dst_ops) {
    EmulatedCamera2* ec = static_cast<EmulatedCamera2*>(d);
    return ec->setFrameQueueDstOps(queue_dst_ops);
}

int EmulatedCamera2::get_frame_queue_src_ops(struct camera2_device *d,
        camera2_metadata_queue_src_ops **queue_src_ops) {
    EmulatedCamera2* ec = static_cast<EmulatedCamera2*>(d);
    *queue_src_ops = static_cast<camera2_metadata_queue_src_ops*>(
        &ec->mFrameQueueSrcOps);
    return NO_ERROR;
}

int EmulatedCamera2::frame_queue_buffer_count(camera2_metadata_queue_src_ops *q) {
    EmulatedCamera2 *ec = static_cast<QueueSrcOps*>(q)->parent;
    return ec->frameQueueBufferCount();
}

int EmulatedCamera2::frame_queue_dequeue(camera2_metadata_queue_src_ops *q,
        camera_metadata_t **buffer) {
    EmulatedCamera2 *ec = static_cast<QueueSrcOps*>(q)->parent;
    return ec->frameQueueDequeue(buffer);
}

int EmulatedCamera2::frame_queue_free(camera2_metadata_queue_src_ops *q,
        camera_metadata_t *old_buffer) {
    EmulatedCamera2 *ec = static_cast<QueueSrcOps*>(q)->parent;
    return ec->frameQueueFree(old_buffer);
}

int EmulatedCamera2::set_notify_callback(struct camera2_device *d,
        camera2_notify_callback notify_cb) {
    EmulatedCamera2* ec = static_cast<EmulatedCamera2*>(d);
    return ec->setNotifyCallback(notify_cb);
}

int EmulatedCamera2::get_in_progress_count(struct camera2_device *d) {
    EmulatedCamera2* ec = static_cast<EmulatedCamera2*>(d);
    return ec->getInProgressCount();
}

int EmulatedCamera2::flush_captures_in_progress(struct camera2_device *d) {
    EmulatedCamera2* ec = static_cast<EmulatedCamera2*>(d);
    return ec->flushCapturesInProgress();
}

int EmulatedCamera2::get_reprocess_stream_ops(camera2_device_t *d,
        camera2_stream_ops **stream) {
    EmulatedCamera2* ec = static_cast<EmulatedCamera2*>(d);
    *stream = static_cast<camera2_stream_ops*>(&ec->mReprocessStreamOps);
    return NO_ERROR;
}

int EmulatedCamera2::reprocess_stream_dequeue_buffer(camera2_stream_ops *s,
        buffer_handle_t** buffer, int *stride) {
    EmulatedCamera2* ec = static_cast<StreamOps*>(s)->parent;
    return ec->reprocessStreamDequeueBuffer(buffer, stride);
}

int EmulatedCamera2::reprocess_stream_enqueue_buffer(camera2_stream_ops *s,
        buffer_handle_t* buffer) {
    EmulatedCamera2* ec = static_cast<StreamOps*>(s)->parent;
    return ec->reprocessStreamEnqueueBuffer(buffer);
}

int EmulatedCamera2::reprocess_stream_cancel_buffer(camera2_stream_ops *s,
        buffer_handle_t* buffer) {
    EmulatedCamera2* ec = static_cast<StreamOps*>(s)->parent;
    return ec->reprocessStreamCancelBuffer(buffer);
}

int EmulatedCamera2::reprocess_stream_set_buffer_count(camera2_stream_ops *s,
        int count) {
    EmulatedCamera2* ec = static_cast<StreamOps*>(s)->parent;
    return ec->reprocessStreamSetBufferCount(count);
}

int EmulatedCamera2::reprocess_stream_set_crop(camera2_stream_ops *s,
        int left, int top, int right, int bottom) {
    EmulatedCamera2* ec = static_cast<StreamOps*>(s)->parent;
    return ec->reprocessStreamSetCrop(left, top, right, bottom);
}

int EmulatedCamera2::reprocess_stream_set_timestamp(camera2_stream_ops *s,
        int64_t timestamp) {
    EmulatedCamera2* ec = static_cast<StreamOps*>(s)->parent;
    return ec->reprocessStreamSetTimestamp(timestamp);
}

int EmulatedCamera2::reprocess_stream_set_usage(camera2_stream_ops *s,
        int usage) {
    EmulatedCamera2* ec = static_cast<StreamOps*>(s)->parent;
    return ec->reprocessStreamSetUsage(usage);
}

int EmulatedCamera2::reprocess_stream_set_swap_interval(camera2_stream_ops *s,
        int interval) {
    EmulatedCamera2* ec = static_cast<StreamOps*>(s)->parent;
    return ec->reprocessStreamSetSwapInterval(interval);
}

int EmulatedCamera2::reprocess_stream_get_min_undequeued_buffer_count(
        const camera2_stream_ops *s,
        int *count) {
    EmulatedCamera2* ec = static_cast<const StreamOps*>(s)->parent;
    return ec->reprocessStreamGetMinUndequeuedBufferCount(count);
}

int EmulatedCamera2::reprocess_stream_lock_buffer(camera2_stream_ops *s,
        buffer_handle_t* buffer) {
    EmulatedCamera2* ec = static_cast<StreamOps*>(s)->parent;
    return ec->reprocessStreamLockBuffer(buffer);
}

int EmulatedCamera2::get_stream_slot_count(struct camera2_device *d) {
    EmulatedCamera2* ec =
            static_cast<EmulatedCamera2*>(d);
    return ec->getStreamSlotCount();
}

int EmulatedCamera2::allocate_stream(struct camera2_device *d,
        uint32_t stream_slot,
        uint32_t width,
        uint32_t height,
        uint32_t format,
        camera2_stream_ops_t *stream_ops) {
    EmulatedCamera2* ec =
            static_cast<EmulatedCamera2*>(d);
    return ec->allocateStream(stream_slot, width, height, format, stream_ops);
}

int EmulatedCamera2::release_stream(struct camera2_device *d,
        uint32_t stream_slot) {
    EmulatedCamera2* ec =
            static_cast<EmulatedCamera2*>(d);
    return ec->releaseStream(stream_slot);
}

void EmulatedCamera2::release(struct camera2_device *d) {
    EmulatedCamera2* ec =
            static_cast<EmulatedCamera2*>(d);
    ec->release();
}

int EmulatedCamera2::dump(struct camera2_device *d, int fd) {
    EmulatedCamera2* ec =
            static_cast<EmulatedCamera2*>(d);
    return ec->dump(fd);
}

int EmulatedCamera2::close(struct hw_device_t* device) {
    EmulatedCamera2* ec =
            static_cast<EmulatedCamera2*>(
                reinterpret_cast<struct camera2_device*>(device) );
    if (ec == NULL) {
        ALOGE("%s: Unexpected NULL camera2 device", __FUNCTION__);
        return -EINVAL;
    }
    return ec->closeCamera();
}

int EmulatedCamera2::get_metadata_vendor_tag_ops(struct camera2_device *d,
        vendor_tag_query_ops_t **ops) {
    EmulatedCamera2* ec = static_cast<EmulatedCamera2*>(d);
    *ops = static_cast<vendor_tag_query_ops_t*>(
            &ec->mVendorTagOps);
    return NO_ERROR;
}

const char* EmulatedCamera2::get_camera_vendor_section_name(
        const vendor_tag_query_ops_t *v,
        uint32_t tag) {
    EmulatedCamera2* ec = static_cast<const TagOps*>(v)->parent;
    return ec->getVendorSectionName(tag);
}

const char* EmulatedCamera2::get_camera_vendor_tag_name(
        const vendor_tag_query_ops_t *v,
        uint32_t tag) {
    EmulatedCamera2* ec = static_cast<const TagOps*>(v)->parent;
    return ec->getVendorTagName(tag);
}

int EmulatedCamera2::get_camera_vendor_tag_type(
        const vendor_tag_query_ops_t *v,
        uint32_t tag)  {
    EmulatedCamera2* ec = static_cast<const TagOps*>(v)->parent;
    return ec->getVendorTagType(tag);
}

camera2_device_ops_t EmulatedCamera2::sDeviceOps = {
    EmulatedCamera2::set_request_queue_src_ops,
    EmulatedCamera2::get_request_queue_dst_ops,
    EmulatedCamera2::set_reprocess_queue_src_ops,
    EmulatedCamera2::get_reprocess_queue_dst_ops,
    EmulatedCamera2::set_frame_queue_dst_ops,
    EmulatedCamera2::get_frame_queue_src_ops,
    EmulatedCamera2::set_notify_callback,
    EmulatedCamera2::get_in_progress_count,
    EmulatedCamera2::flush_captures_in_progress,
    EmulatedCamera2::get_reprocess_stream_ops,
    EmulatedCamera2::get_stream_slot_count,
    EmulatedCamera2::allocate_stream,
    EmulatedCamera2::release_stream,
    EmulatedCamera2::get_metadata_vendor_tag_ops,
    EmulatedCamera2::release,
    EmulatedCamera2::dump
};

}; /* namespace android */
