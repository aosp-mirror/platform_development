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

#define LOG_NDEBUG 0
#define LOG_TAG "EmulatedCamera2_JpegCompressor"

#include <utils/Log.h>
#include <ui/GraphicBufferMapper.h>

#include "JpegCompressor.h"
#include "../EmulatedFakeCamera2.h"

namespace android {

JpegCompressor::JpegCompressor(EmulatedFakeCamera2 *parent):
        Thread(false),
        mIsBusy(false),
        mParent(parent),
        mBuffers(NULL),
        mCaptureTime(0) {
}

JpegCompressor::~JpegCompressor() {
    Mutex::Autolock lock(mMutex);
}

status_t JpegCompressor::start(Buffers *buffers,
        nsecs_t captureTime) {
    Mutex::Autolock lock(mMutex);
    {
        Mutex::Autolock busyLock(mBusyMutex);

        if (mIsBusy) {
            ALOGE("%s: Already processing a buffer!", __FUNCTION__);
            return INVALID_OPERATION;
        }

        mIsBusy = true;

        mBuffers = buffers;
        mCaptureTime = captureTime;
    }

    status_t res;
    res = run("EmulatedFakeCamera2::JpegCompressor");
    if (res != OK) {
        ALOGE("%s: Unable to start up compression thread: %s (%d)",
                __FUNCTION__, strerror(-res), res);
        delete mBuffers;
    }
    return res;
}

status_t JpegCompressor::cancel() {
    requestExitAndWait();
    return OK;
}

status_t JpegCompressor::readyToRun() {
    return OK;
}

bool JpegCompressor::threadLoop() {
    Mutex::Autolock lock(mMutex);
    ALOGV("%s: Starting compression thread", __FUNCTION__);

    // Find source and target buffers. Assumes only one buffer matches
    // each condition!

    bool foundJpeg = false, mFoundAux = false;
    for (size_t i = 0; i < mBuffers->size(); i++) {
        const StreamBuffer &b = (*mBuffers)[i];
        if (b.format == HAL_PIXEL_FORMAT_BLOB) {
            mJpegBuffer = b;
            mFoundJpeg = true;
        } else if (b.streamId <= 0) {
            mAuxBuffer = b;
            mFoundAux = true;
        }
        if (mFoundJpeg && mFoundAux) break;
    }
    if (!mFoundJpeg || !mFoundAux) {
        ALOGE("%s: Unable to find buffers for JPEG source/destination",
                __FUNCTION__);
        cleanUp();
        return false;
    }

    // Set up error management

    mJpegErrorInfo = NULL;
    JpegError error;
    error.parent = this;

    mCInfo.err = jpeg_std_error(&error);
    mCInfo.err->error_exit = jpegErrorHandler;

    jpeg_create_compress(&mCInfo);
    if (checkError("Error initializing compression")) return false;

    // Route compressed data straight to output stream buffer

    JpegDestination jpegDestMgr;
    jpegDestMgr.parent = this;
    jpegDestMgr.init_destination = jpegInitDestination;
    jpegDestMgr.empty_output_buffer = jpegEmptyOutputBuffer;
    jpegDestMgr.term_destination = jpegTermDestination;

    mCInfo.dest = &jpegDestMgr;

    // Set up compression parameters

    mCInfo.image_width = mAuxBuffer.width;
    mCInfo.image_height = mAuxBuffer.height;
    mCInfo.input_components = 3;
    mCInfo.in_color_space = JCS_RGB;

    jpeg_set_defaults(&mCInfo);
    if (checkError("Error configuring defaults")) return false;

    // Do compression

    jpeg_start_compress(&mCInfo, TRUE);
    if (checkError("Error starting compression")) return false;

    size_t rowStride = mAuxBuffer.stride * 3;
    const size_t kChunkSize = 32;
    while (mCInfo.next_scanline < mCInfo.image_height) {
        JSAMPROW chunk[kChunkSize];
        for (size_t i = 0 ; i < kChunkSize; i++) {
            chunk[i] = (JSAMPROW)
                    (mAuxBuffer.img + (i + mCInfo.next_scanline) * rowStride);
        }
        jpeg_write_scanlines(&mCInfo, chunk, kChunkSize);
        if (checkError("Error while compressing")) return false;
        if (exitPending()) {
            ALOGV("%s: Cancel called, exiting early", __FUNCTION__);
            cleanUp();
            return false;
        }
    }

    jpeg_finish_compress(&mCInfo);
    if (checkError("Error while finishing compression")) return false;

    // Write to JPEG output stream

    ALOGV("%s: Compression complete, pushing to stream %d", __FUNCTION__,
          mJpegBuffer.streamId);

    GraphicBufferMapper::get().unlock(*(mJpegBuffer.buffer));
    status_t res;
    const Stream &s = mParent->getStreamInfo(mJpegBuffer.streamId);
    res = s.ops->enqueue_buffer(s.ops, mCaptureTime, mJpegBuffer.buffer);
    if (res != OK) {
        ALOGE("%s: Error queueing compressed image buffer %p: %s (%d)",
                __FUNCTION__, mJpegBuffer.buffer, strerror(-res), res);
        mParent->signalError();
    }

    // All done

    cleanUp();

    return false;
}

bool JpegCompressor::isBusy() {
    Mutex::Autolock busyLock(mBusyMutex);
    return mIsBusy;
}

bool JpegCompressor::isStreamInUse(uint32_t id) {
    Mutex::Autolock lock(mBusyMutex);

    if (mBuffers && mIsBusy) {
        for (size_t i = 0; i < mBuffers->size(); i++) {
            if ( (*mBuffers)[i].streamId == (int)id ) return true;
        }
    }
    return false;
}

bool JpegCompressor::waitForDone(nsecs_t timeout) {
    Mutex::Autolock lock(mBusyMutex);
    status_t res = OK;
    if (mIsBusy) {
        res = mDone.waitRelative(mBusyMutex, timeout);
    }
    return (res == OK);
}

bool JpegCompressor::checkError(const char *msg) {
    if (mJpegErrorInfo) {
        char errBuffer[JMSG_LENGTH_MAX];
        mJpegErrorInfo->err->format_message(mJpegErrorInfo, errBuffer);
        ALOGE("%s: %s: %s",
                __FUNCTION__, msg, errBuffer);
        cleanUp();
        mJpegErrorInfo = NULL;
        return true;
    }
    return false;
}

void JpegCompressor::cleanUp() {
    status_t res;
    jpeg_destroy_compress(&mCInfo);
    Mutex::Autolock lock(mBusyMutex);

    if (mFoundAux) {
        if (mAuxBuffer.streamId == 0) {
            delete[] mAuxBuffer.img;
        } else {
            GraphicBufferMapper::get().unlock(*(mAuxBuffer.buffer));
            const ReprocessStream &s =
                    mParent->getReprocessStreamInfo(-mAuxBuffer.streamId);
            res = s.ops->release_buffer(s.ops, mAuxBuffer.buffer);
            if (res != OK) {
                ALOGE("Error releasing reprocess buffer %p: %s (%d)",
                        mAuxBuffer.buffer, strerror(-res), res);
                mParent->signalError();
            }
        }
    }
    delete mBuffers;
    mBuffers = NULL;

    mIsBusy = false;
    mDone.signal();
}

void JpegCompressor::jpegErrorHandler(j_common_ptr cinfo) {
    JpegError *error = static_cast<JpegError*>(cinfo->err);
    error->parent->mJpegErrorInfo = cinfo;
}

void JpegCompressor::jpegInitDestination(j_compress_ptr cinfo) {
    JpegDestination *dest= static_cast<JpegDestination*>(cinfo->dest);
    ALOGV("%s: Setting destination to %p, size %d",
            __FUNCTION__, dest->parent->mJpegBuffer.img, kMaxJpegSize);
    dest->next_output_byte = (JOCTET*)(dest->parent->mJpegBuffer.img);
    dest->free_in_buffer = kMaxJpegSize;
}

boolean JpegCompressor::jpegEmptyOutputBuffer(j_compress_ptr cinfo) {
    ALOGE("%s: JPEG destination buffer overflow!",
            __FUNCTION__);
    return true;
}

void JpegCompressor::jpegTermDestination(j_compress_ptr cinfo) {
    ALOGV("%s: Done writing JPEG data. %d bytes left in buffer",
            __FUNCTION__, cinfo->dest->free_in_buffer);
}

} // namespace android
