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

#ifndef HW_EMULATOR_CAMERA_EMULATED_FAKE_CAMERA_DEVICE_H
#define HW_EMULATOR_CAMERA_EMULATED_FAKE_CAMERA_DEVICE_H

/*
 * Contains declaration of a class EmulatedFakeCameraDevice that encapsulates
 * a fake camera device.
 */

#include "Converters.h"
#include "EmulatedCameraDevice.h"

namespace android {

class EmulatedFakeCamera;

/* Encapsulates a fake camera device.
 * Fake camera device emulates a camera device by providing frames containing
 * a black and white checker board, moving diagonally towards the 0,0 corner.
 * There is also a green, or red square that bounces inside the frame, changing
 * its color when bouncing off the 0,0 corner.
 */
class EmulatedFakeCameraDevice : public EmulatedCameraDevice {
public:
    /* Constructs EmulatedFakeCameraDevice instance. */
    explicit EmulatedFakeCameraDevice(EmulatedFakeCamera* camera_hal);

    /* Destructs EmulatedFakeCameraDevice instance. */
    ~EmulatedFakeCameraDevice();

    /***************************************************************************
     * Emulated camera device abstract interface implementation.
     * See declarations of these methods in EmulatedCameraDevice class for
     * information on each of these methods.
     **************************************************************************/

public:
    /* Connects to the camera device.
     * Since there is no real device to connect to, this method does nothing,
     * but changes the state.
     */
    status_t connectDevice();

    /* Disconnects from the camera device.
     * Since there is no real device to disconnect from, this method does
     * nothing, but changes the state.
     */
    status_t disconnectDevice();

protected:
    /* Starts capturing frames from the camera device.
     * Since there is no real device to control, this method simply starts the
     * worker thread, and changes the state.
     */
    status_t startDevice();

    /* Stops capturing frames from the camera device.
     * Since there is no real device to control, this method simply stops the
     * worker thread, and changes the state.
     */
    status_t stopDevice();

    /***************************************************************************
     * Worker thread management overrides.
     * See declarations of these methods in EmulatedCameraDevice class for
     * information on each of these methods.
     **************************************************************************/

protected:
    /* Implementation of the worker thread routine.
     * This method simply sleeps for a period of time defined by FPS property of
     * the fake camera (simulating frame frequency), and then calls emulated
     * camera's onNextFrameAvailable method.
     */
    bool inWorkerThread();

    /****************************************************************************
     * Fake camera device private API
     ***************************************************************************/

private:
    /* Draws a black and white checker board in the current frame buffer. */
    void drawCheckerboard();

    /* Draws a square of the given color in the current frame buffer.
     * Param:
     *  x, y - Coordinates of the top left corner of the square in the buffer.
     *  size - Size of the square's side.
     *  color - Square's color.
     */
    void drawSquare(int x, int y, int size, const YUVPixel* color);

    /****************************************************************************
     * Fake camera device data members
     ***************************************************************************/

private:
    /*
     * Pixel colors in YUV format used when drawing the checker board.
     */

    YUVPixel    mBlackYUV;
    YUVPixel    mWhiteYUV;
    YUVPixel    mRedYUV;
    YUVPixel    mGreenYUV;
    YUVPixel    mBlueYUV;

    /*
     * Drawing related stuff
     */

    int         mCheckX;
    int         mCheckY;
    int         mCcounter;
    int         mHalfWidth;

    /* Emulated FPS (frames per second).
     * We will emulate 50 FPS. */
    static const int        mEmulatedFPS = 50;

    /* Defines time (in nanoseconds) between redrawing the checker board.
     * We will redraw the checker board every 15 milliseconds. */
    static const nsecs_t    mRedrawAfter = 15000000LL;
};

}; /* namespace android */

#endif  /* HW_EMULATOR_CAMERA_EMULATED_FAKE_CAMERA_DEVICE_H */
