/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * This file defines an NDK API.
 * Do not remove methods.
 * Do not change method signatures.
 * Do not change the value of constants.
 * Do not change the size of any of the classes defined in here.
 * Do not reference types that are not part of the NDK.
 * Do not #include files that aren't part of the NDK.
 */

#ifndef _NDK_MEDIA_CODEC_H
#define _NDK_MEDIA_CODEC_H

#include <android/native_window.h>

#include "NdkMediaCrypto.h"
#include "NdkMediaError.h"
#include "NdkMediaFormat.h"

#ifdef __cplusplus
extern "C" {
#endif


struct AMediaCodec;
typedef struct AMediaCodec AMediaCodec;

struct AMediaCodecBufferInfo {
    int32_t offset;
    int32_t size;
    int64_t presentationTimeUs;
    uint32_t flags;
};
typedef struct AMediaCodecBufferInfo AMediaCodecBufferInfo;
typedef struct AMediaCodecCryptoInfo AMediaCodecCryptoInfo;

enum {
    AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM = 4,
    AMEDIACODEC_CONFIGURE_FLAG_ENCODE = 1,
    AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED = -3,
    AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED = -2,
    AMEDIACODEC_INFO_TRY_AGAIN_LATER = -1
};

/**
 * Create codec by name. Use this if you know the exact codec you want to use.
 * When configuring, you will need to specify whether to use the codec as an
 * encoder or decoder.
 */
AMediaCodec* AMediaCodec_createCodecByName(const char *name);

/**
 * Create codec by mime type. Most applications will use this, specifying a
 * mime type obtained from media extractor.
 */
AMediaCodec* AMediaCodec_createDecoderByType(const char *mime_type);

/**
 * Create encoder by name.
 */
AMediaCodec* AMediaCodec_createEncoderByType(const char *mime_type);

/**
 * delete the codec and free its resources
 */
media_status_t AMediaCodec_delete(AMediaCodec*);

/**
 * Configure the codec. For decoding you would typically get the format from an extractor.
 */
media_status_t AMediaCodec_configure(
        AMediaCodec*,
        const AMediaFormat* format,
        ANativeWindow* surface,
        AMediaCrypto *crypto,
        uint32_t flags);

/**
 * Start the codec. A codec must be configured before it can be started, and must be started
 * before buffers can be sent to it.
 */
media_status_t AMediaCodec_start(AMediaCodec*);

/**
 * Stop the codec.
 */
media_status_t AMediaCodec_stop(AMediaCodec*);

/*
 * Flush the codec's input and output. All indices previously returned from calls to
 * AMediaCodec_dequeueInputBuffer and AMediaCodec_dequeueOutputBuffer become invalid.
 */
media_status_t AMediaCodec_flush(AMediaCodec*);

/**
 * Get an input buffer. The specified buffer index must have been previously obtained from
 * dequeueInputBuffer, and not yet queued.
 */
uint8_t* AMediaCodec_getInputBuffer(AMediaCodec*, size_t idx, size_t *out_size);

/**
 * Get an output buffer. The specified buffer index must have been previously obtained from
 * dequeueOutputBuffer, and not yet queued.
 */
uint8_t* AMediaCodec_getOutputBuffer(AMediaCodec*, size_t idx, size_t *out_size);

/**
 * Get the index of the next available input buffer. An app will typically use this with
 * getInputBuffer() to get a pointer to the buffer, then copy the data to be encoded or decoded
 * into the buffer before passing it to the codec.
 */
ssize_t AMediaCodec_dequeueInputBuffer(AMediaCodec*, int64_t timeoutUs);

/**
 * Send the specified buffer to the codec for processing.
 */
media_status_t AMediaCodec_queueInputBuffer(AMediaCodec*,
        size_t idx, off_t offset, size_t size, uint64_t time, uint32_t flags);

/**
 * Send the specified buffer to the codec for processing.
 */
media_status_t AMediaCodec_queueSecureInputBuffer(AMediaCodec*,
        size_t idx, off_t offset, AMediaCodecCryptoInfo*, uint64_t time, uint32_t flags);

/**
 * Get the index of the next available buffer of processed data.
 */
ssize_t AMediaCodec_dequeueOutputBuffer(AMediaCodec*, AMediaCodecBufferInfo *info, int64_t timeoutUs);
AMediaFormat* AMediaCodec_getOutputFormat(AMediaCodec*);

/**
 * If you are done with a buffer, use this call to return the buffer to
 * the codec. If you previously specified a surface when configuring this
 * video decoder you can optionally render the buffer.
 */
media_status_t AMediaCodec_releaseOutputBuffer(AMediaCodec*, size_t idx, bool render);

/**
 * If you are done with a buffer, use this call to update its surface timestamp
 * and return it to the codec to render it on the output surface. If you
 * have not specified an output surface when configuring this video codec,
 * this call will simply return the buffer to the codec.
 *
 * For more details, see the Java documentation for MediaCodec.releaseOutputBuffer.
 */
media_status_t AMediaCodec_releaseOutputBufferAtTime(
        AMediaCodec *mData, size_t idx, int64_t timestampNs);


typedef enum {
    AMEDIACODECRYPTOINFO_MODE_CLEAR = 0,
    AMEDIACODECRYPTOINFO_MODE_AES_CTR = 1
} cryptoinfo_mode_t;

/**
 * Create an AMediaCodecCryptoInfo from scratch. Use this if you need to use custom
 * crypto info, rather than one obtained from AMediaExtractor.
 *
 * AMediaCodecCryptoInfo describes the structure of an (at least
 * partially) encrypted input sample.
 * A buffer's data is considered to be partitioned into "subsamples",
 * each subsample starts with a (potentially empty) run of plain,
 * unencrypted bytes followed by a (also potentially empty) run of
 * encrypted bytes.
 * numBytesOfClearData can be null to indicate that all data is encrypted.
 * This information encapsulates per-sample metadata as outlined in
 * ISO/IEC FDIS 23001-7:2011 "Common encryption in ISO base media file format files".
 */
AMediaCodecCryptoInfo *AMediaCodecCryptoInfo_new(
        int numsubsamples,
        uint8_t key[16],
        uint8_t iv[16],
        cryptoinfo_mode_t mode,
        size_t *clearbytes,
        size_t *encryptedbytes);

/**
 * delete an AMediaCodecCryptoInfo created previously with AMediaCodecCryptoInfo_new, or
 * obtained from AMediaExtractor
 */
media_status_t AMediaCodecCryptoInfo_delete(AMediaCodecCryptoInfo*);

/**
 * The number of subsamples that make up the buffer's contents.
 */
size_t AMediaCodecCryptoInfo_getNumSubSamples(AMediaCodecCryptoInfo*);

/**
 * A 16-byte opaque key
 */
media_status_t AMediaCodecCryptoInfo_getKey(AMediaCodecCryptoInfo*, uint8_t *dst);

/**
 * A 16-byte initialization vector
 */
media_status_t AMediaCodecCryptoInfo_getIV(AMediaCodecCryptoInfo*, uint8_t *dst);

/**
 * The type of encryption that has been applied,
 * one of AMEDIACODECRYPTOINFO_MODE_CLEAR or AMEDIACODECRYPTOINFO_MODE_AES_CTR.
 */
cryptoinfo_mode_t AMediaCodecCryptoInfo_getMode(AMediaCodecCryptoInfo*);

/**
 * The number of leading unencrypted bytes in each subsample.
 */
media_status_t AMediaCodecCryptoInfo_getClearBytes(AMediaCodecCryptoInfo*, size_t *dst);

/**
 * The number of trailing encrypted bytes in each subsample.
 */
media_status_t AMediaCodecCryptoInfo_getEncryptedBytes(AMediaCodecCryptoInfo*, size_t *dst);

#ifdef __cplusplus
} // extern "C"
#endif

#endif //_NDK_MEDIA_CODEC_H
