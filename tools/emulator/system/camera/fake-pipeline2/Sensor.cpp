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

//#define LOG_NDEBUG 0
//#define LOG_NNDEBUG 0
#define LOG_TAG "EmulatedCamera2_Sensor"

#ifdef LOG_NNDEBUG
#define ALOGVV(...) ALOGV(__VA_ARGS__)
#else
#define ALOGVV(...) ((void)0)
#endif

#include <utils/Log.h>

#include "Sensor.h"
#include <cmath>
#include <cstdlib>
#include "system/camera_metadata.h"

namespace android {

const unsigned int Sensor::kResolution[2]  = {640, 480};

const nsecs_t Sensor::kExposureTimeRange[2] =
    {1000L, 30000000000L} ; // 1 us - 30 sec
const nsecs_t Sensor::kFrameDurationRange[2] =
    {33331760L, 30000000000L}; // ~1/30 s - 30 sec
const nsecs_t Sensor::kMinVerticalBlank = 10000L;

const uint8_t Sensor::kColorFilterArrangement = ANDROID_SENSOR_RGGB;

// Output image data characteristics
const uint32_t Sensor::kMaxRawValue = 4000;
const uint32_t Sensor::kBlackLevel  = 1000;

// Sensor sensitivity
const float Sensor::kSaturationVoltage      = 0.520f;
const uint32_t Sensor::kSaturationElectrons = 2000;
const float Sensor::kVoltsPerLuxSecond      = 0.100f;

const float Sensor::kElectronsPerLuxSecond =
        Sensor::kSaturationElectrons / Sensor::kSaturationVoltage
        * Sensor::kVoltsPerLuxSecond;

const float Sensor::kBaseGainFactor = (float)Sensor::kMaxRawValue /
            Sensor::kSaturationElectrons;

const float Sensor::kReadNoiseStddevBeforeGain = 1.177; // in electrons
const float Sensor::kReadNoiseStddevAfterGain =  2.100; // in digital counts
const float Sensor::kReadNoiseVarBeforeGain =
            Sensor::kReadNoiseStddevBeforeGain *
            Sensor::kReadNoiseStddevBeforeGain;
const float Sensor::kReadNoiseVarAfterGain =
            Sensor::kReadNoiseStddevAfterGain *
            Sensor::kReadNoiseStddevAfterGain;

// While each row has to read out, reset, and then expose, the (reset +
// expose) sequence can be overlapped by other row readouts, so the final
// minimum frame duration is purely a function of row readout time, at least
// if there's a reasonable number of rows.
const nsecs_t Sensor::kRowReadoutTime =
            Sensor::kFrameDurationRange[0] / Sensor::kResolution[1];

const uint32_t Sensor::kAvailableSensitivities[5] =
    {100, 200, 400, 800, 1600};
const uint32_t Sensor::kDefaultSensitivity = 100;

/** A few utility functions for math, normal distributions */

// Take advantage of IEEE floating-point format to calculate an approximate
// square root. Accurate to within +-3.6%
float sqrtf_approx(float r) {
    // Modifier is based on IEEE floating-point representation; the
    // manipulations boil down to finding approximate log2, dividing by two, and
    // then inverting the log2. A bias is added to make the relative error
    // symmetric about the real answer.
    const int32_t modifier = 0x1FBB4000;

    int32_t r_i = *(int32_t*)(&r);
    r_i = (r_i >> 1) + modifier;

    return *(float*)(&r_i);
}



Sensor::Sensor():
        Thread(false),
        mGotVSync(false),
        mExposureTime(kFrameDurationRange[0]-kMinVerticalBlank),
        mFrameDuration(kFrameDurationRange[0]),
        mGainFactor(kDefaultSensitivity),
        mNextBuffer(NULL),
        mCapturedBuffer(NULL),
        mScene(kResolution[0], kResolution[1], kElectronsPerLuxSecond)
{

}

Sensor::~Sensor() {
    shutDown();
}

status_t Sensor::startUp() {
    ALOGV("%s: E", __FUNCTION__);

    int res;
    mCapturedBuffer = NULL;
    res = readyToRun();
    if (res != OK) {
        ALOGE("Unable to prepare sensor capture thread to run: %d", res);
        return res;
    }
    res = run("EmulatedFakeCamera2::Sensor",
            ANDROID_PRIORITY_URGENT_DISPLAY);

    if (res != OK) {
        ALOGE("Unable to start up sensor capture thread: %d", res);
    }
    return res;
}

status_t Sensor::shutDown() {
    ALOGV("%s: E", __FUNCTION__);

    int res;
    res = requestExitAndWait();
    if (res != OK) {
        ALOGE("Unable to shut down sensor capture thread: %d", res);
    }
    return res;
}

Scene &Sensor::getScene() {
    return mScene;
}

void Sensor::setExposureTime(uint64_t ns) {
    Mutex::Autolock lock(mControlMutex);
    ALOGVV("Exposure set to %f", ns/1000000.f);
    mExposureTime = ns;
}

void Sensor::setFrameDuration(uint64_t ns) {
    Mutex::Autolock lock(mControlMutex);
    ALOGVV("Frame duration set to %f", ns/1000000.f);
    mFrameDuration = ns;
}

void Sensor::setSensitivity(uint32_t gain) {
    Mutex::Autolock lock(mControlMutex);
    ALOGVV("Gain set to %d", gain);
    mGainFactor = gain;
}

void Sensor::setDestinationBuffer(uint8_t *buffer,
        uint32_t format, uint32_t stride) {
    Mutex::Autolock lock(mControlMutex);
    mNextBuffer = buffer;
    mNextBufferFmt = format;
    mNextStride = stride;
}

bool Sensor::waitForVSync(nsecs_t reltime) {
    int res;
    Mutex::Autolock lock(mControlMutex);

    mGotVSync = false;
    res = mVSync.waitRelative(mControlMutex, reltime);
    if (res != OK && res != TIMED_OUT) {
        ALOGE("%s: Error waiting for VSync signal: %d", __FUNCTION__, res);
        return false;
    }
    return mGotVSync;
}

bool Sensor::waitForNewFrame(nsecs_t reltime,
        nsecs_t *captureTime) {
    Mutex::Autolock lock(mReadoutMutex);
    uint8_t *ret;
    if (mCapturedBuffer == NULL) {
        int res;
        res = mReadoutComplete.waitRelative(mReadoutMutex, reltime);
        if (res == TIMED_OUT) {
            return false;
        } else if (res != OK || mCapturedBuffer == NULL) {
            ALOGE("Error waiting for sensor readout signal: %d", res);
            return false;
        }
    }
    *captureTime = mCaptureTime;
    mCapturedBuffer = NULL;
    return true;
}

status_t Sensor::readyToRun() {
    ALOGV("Starting up sensor thread");
    mStartupTime = systemTime();
    mNextCaptureTime = 0;
    mNextCapturedBuffer = NULL;
    return OK;
}

bool Sensor::threadLoop() {
    /**
     * Sensor capture operation main loop.
     *
     * Stages are out-of-order relative to a single frame's processing, but
     * in-order in time.
     */

    /**
     * Stage 1: Read in latest control parameters
     */
    uint64_t exposureDuration;
    uint64_t frameDuration;
    uint32_t gain;
    uint8_t *nextBuffer;
    uint32_t nextBufferFmt;
    uint32_t stride;
    {
        Mutex::Autolock lock(mControlMutex);
        exposureDuration = mExposureTime;
        frameDuration    = mFrameDuration;
        gain             = mGainFactor;
        nextBuffer       = mNextBuffer;
        nextBufferFmt    = mNextBufferFmt;
        stride           = mNextStride;
        // Don't reuse a buffer
        mNextBuffer = NULL;

        // Signal VSync for start of readout
        ALOGVV("Sensor VSync");
        mGotVSync = true;
        mVSync.signal();
    }

    /**
     * Stage 3: Read out latest captured image
     */

    uint8_t *capturedBuffer = NULL;
    nsecs_t captureTime = 0;

    nsecs_t startRealTime  = systemTime();
    nsecs_t simulatedTime    = startRealTime - mStartupTime;
    nsecs_t frameEndRealTime = startRealTime + frameDuration;
    nsecs_t frameReadoutEndRealTime = startRealTime +
            kRowReadoutTime * kResolution[1];

    if (mNextCapturedBuffer != NULL) {
        ALOGVV("Sensor starting readout");
        // Pretend we're doing readout now; will signal once enough time has elapsed
        capturedBuffer = mNextCapturedBuffer;
        captureTime    = mNextCaptureTime;
    }
    simulatedTime += kRowReadoutTime + kMinVerticalBlank;

    /**
     * Stage 2: Capture new image
     */

    mNextCaptureTime = simulatedTime;
    mNextCapturedBuffer = nextBuffer;

    if (mNextCapturedBuffer != NULL) {
        ALOGVV("Sensor capturing image (%d x %d) stride %d",
                kResolution[0], kResolution[1], stride);
        ALOGVV("Exposure: %f ms, gain: %d", (float)exposureDuration/1e6, gain);
        mScene.setExposureDuration((float)exposureDuration/1e9);
        mScene.calculateScene(mNextCaptureTime);

        switch(nextBufferFmt) {
            case HAL_PIXEL_FORMAT_RAW_SENSOR:
                captureRaw(gain, stride, &capturedBuffer,
                        captureTime, frameEndRealTime);
                break;
            case HAL_PIXEL_FORMAT_RGBA_8888:
                captureRGBA(gain, stride, &capturedBuffer,
                        captureTime, frameEndRealTime);
                break;
            default:
                ALOGE("%s: Unknown format %x, no output", __FUNCTION__,
                        nextBufferFmt);
                break;
        }
    }
    // No capture done, or finished image generation before readout was completed
    if (capturedBuffer != NULL) {
        ALOGVV("Sensor readout complete");
        Mutex::Autolock lock(mReadoutMutex);
        mCapturedBuffer = capturedBuffer;
        mCaptureTime = captureTime;
        mReadoutComplete.signal();
        capturedBuffer = NULL;
    }

    ALOGVV("Sensor vertical blanking interval");
    nsecs_t workDoneRealTime = systemTime();
    const nsecs_t timeAccuracy = 2e6; // 2 ms of imprecision is ok
    if (workDoneRealTime < frameEndRealTime - timeAccuracy) {
        timespec t;
        t.tv_sec = (frameEndRealTime - workDoneRealTime)  / 1000000000L;
        t.tv_nsec = (frameEndRealTime - workDoneRealTime) % 1000000000L;

        int ret;
        do {
            ret = nanosleep(&t, &t);
        } while (ret != 0);
    }
    nsecs_t endRealTime = systemTime();
    ALOGVV("Frame cycle took %d ms, target %d ms",
            (int)((endRealTime - startRealTime)/1000000),
            (int)(frameDuration / 1000000));
    return true;
};

void Sensor::captureRaw(uint32_t gain, uint32_t stride,
        uint8_t **capturedBuffer, nsecs_t captureTime, nsecs_t frameReadoutTime) {
    float totalGain = gain/100.0 * kBaseGainFactor;
    float noiseVarGain =  totalGain * totalGain;
    float readNoiseVar = kReadNoiseVarBeforeGain * noiseVarGain
            + kReadNoiseVarAfterGain;

    int bayerSelect[4] = {Scene::R, Scene::Gr, Scene::Gb, Scene::B}; // RGGB

    for (unsigned int y = 0; y < kResolution[1]; y++ ) {
        int *bayerRow = bayerSelect + (y & 0x1) * 2;
        uint16_t *px = (uint16_t*)mNextCapturedBuffer + y * stride;
        for (unsigned int x = 0; x < kResolution[0]; x++) {
            uint32_t electronCount;
            electronCount = mScene.getPixelElectrons()[bayerRow[x & 0x1]];

            // TODO: Better pixel saturation curve?
            electronCount = (electronCount < kSaturationElectrons) ?
                    electronCount : kSaturationElectrons;

            // TODO: Better A/D saturation curve?
            uint16_t rawCount = electronCount * totalGain;
            rawCount = (rawCount < kMaxRawValue) ? rawCount : kMaxRawValue;

            // Calculate noise value
            // TODO: Use more-correct Gaussian instead of uniform noise
            float photonNoiseVar = electronCount * noiseVarGain;
            float noiseStddev = sqrtf_approx(readNoiseVar + photonNoiseVar);
            // Scaled to roughly match gaussian/uniform noise stddev
            float noiseSample = std::rand() * (2.5 / (1.0 + RAND_MAX)) - 1.25;

            rawCount += kBlackLevel;
            rawCount += noiseStddev * noiseSample;

            *px++ = rawCount;
        }
        // TODO: Handle this better
        //simulatedTime += kRowReadoutTime;

        // If enough time has elapsed to complete readout, signal done frame
        // Only check every so often, though
        if ((*capturedBuffer != NULL) &&
                ((y & 63) == 0) &&
                (systemTime() >= frameReadoutTime) ) {
            ALOGV("Sensor readout complete");
            Mutex::Autolock lock(mReadoutMutex);
            mCapturedBuffer = *capturedBuffer;
            mCaptureTime = captureTime;
            mReadoutComplete.signal();
            *capturedBuffer = NULL;
        }
    }
    ALOGVV("Raw sensor image captured");
}

void Sensor::captureRGBA(uint32_t gain, uint32_t stride,
        uint8_t **capturedBuffer, nsecs_t captureTime, nsecs_t frameReadoutTime) {
    float totalGain = gain/100.0 * kBaseGainFactor;
    float noiseVarGain =  totalGain * totalGain;
    float readNoiseVar = kReadNoiseVarBeforeGain * noiseVarGain
            + kReadNoiseVarAfterGain;

    for (unsigned int y = 0; y < kResolution[1]; y++ ) {
        uint8_t *px = (uint8_t*)mNextCapturedBuffer + y * stride * 4;
        for (unsigned int x = 0; x < kResolution[0]; x++) {
            uint32_t rCount, gCount, bCount;
            // TODO: Perfect demosaicing is a cheat
            const uint32_t *pixel = mScene.getPixelElectrons();
            rCount = pixel[Scene::R]  * totalGain;
            gCount = pixel[Scene::Gr] * totalGain;
            bCount = pixel[Scene::B]  * totalGain;

            *px++ = rCount / (kMaxRawValue / 255);
            *px++ = gCount / (kMaxRawValue / 255);
            *px++ = bCount / (kMaxRawValue / 255);
            *px++ = 255;
        }
        // TODO: Handle this better
        //simulatedTime += kRowReadoutTime;

        // If enough time has elapsed to complete readout, signal done frame
        // Only check every so often, though
        if ((*capturedBuffer != NULL) &&
                ((y & 63) == 0) &&
                (systemTime() >= frameReadoutTime) ) {
            ALOGV("Sensor readout complete");
            Mutex::Autolock lock(mReadoutMutex);
            mCapturedBuffer = *capturedBuffer;
            mCaptureTime = captureTime;
            mReadoutComplete.signal();
            *capturedBuffer = NULL;
        }
    }
    ALOGVV("RGBA sensor image captured");
}

} // namespace android
