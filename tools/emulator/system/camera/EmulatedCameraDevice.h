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

#ifndef HW_EMULATOR_CAMERA_EMULATED_CAMERA_DEVICE_H
#define HW_EMULATOR_CAMERA_EMULATED_CAMERA_DEVICE_H

/*
 * Contains declaration of an abstract class EmulatedCameraDevice that defines
 * functionality expected from an emulated physical camera device:
 *  - Obtaining and setting camera device parameters
 *  - Capturing frames
 *  - Streaming video
 *  - etc.
 */

#include <utils/threads.h>
#include "EmulatedCameraCommon.h"

namespace android {

class EmulatedCamera;

/* Encapsulates an abstract class EmulatedCameraDevice that defines
 * functionality expected from an emulated physical camera device:
 *  - Obtaining and setting camera device parameters
 *  - Capturing frames
 *  - Streaming video
 *  - etc.
 */
class EmulatedCameraDevice {
public:
    /* Constructs EmulatedCameraDevice instance.
     * Param:
     *  camera_hal - Emulated camera that implements the camera HAL API, and
     *      manages (contains) this object.
     */
    explicit EmulatedCameraDevice(EmulatedCamera* camera_hal);

    /* Destructs EmulatedCameraDevice instance. */
    virtual ~EmulatedCameraDevice();

    /***************************************************************************
     * Emulated camera device abstract interface
     **************************************************************************/

public:
    /* Connects to the camera device.
     * This method must be called on an initialized instance of this class.
     * Return:
     *  NO_ERROR on success, or an appropriate error status.
     */
    virtual status_t connectDevice() = 0;

    /* Disconnects from the camera device.
     * Return:
     *  NO_ERROR on success, or an appropriate error status. If this method is
     *  called for already disconnected, or uninitialized instance of this class,
     *  a successful status must be returned from this method. If this method is
     *  called for an instance that is in "capturing" state, this method must
     *  return a failure.
     */
    virtual status_t disconnectDevice() = 0;

protected:
    /* Starts capturing frames from the camera device.
     *
     * Typically, this method initializes the camera device with the settings
     * requested by the framework through the camera HAL, and starts a worker
     * thread that will listen to the physical device for available frames. When
     * new frame becomes available, it will be cached in current_framebuffer_,
     * and the containing emulated camera object will be notified via call to
     * its onNextFrameAvailable method. This method must be called on a
     * connected instance of this class. If it is called on a disconnected
     * instance, this method must return a failure.
     * Return:
     *  NO_ERROR on success, or an appropriate error status.
     */
    virtual status_t startDevice() = 0;

    /* Stops capturing frames from the camera device.
     * Return:
     *  NO_ERROR on success, or an appropriate error status. If this method is
     *  called for an object that is not capturing frames, or is disconnected,
     *  or is uninitialized, a successful status must be returned from this
     *  method.
     */
    virtual status_t stopDevice() = 0;

    /***************************************************************************
     * Emulated camera device public API
     **************************************************************************/

public:
    /* Initializes EmulatedCameraDevice instance.
     * Derived classes should override this method in order to cache static
     * properties of the physical device (list of supported pixel formats, frame
     * sizes, etc.) If this method is called on an already initialized instance,
     * it must return a successful status.
     * Return:
     *  NO_ERROR on success, or an appropriate error status.
     */
    virtual status_t Initialize();

    /* Starts capturing frames from the camera device.
     *
     * Typically, this method caches desired frame parameters, and calls
     * startDevice method to start capturing video frames from the camera
     * device. This method must be called on a connected instance of this class.
     * If it is called on a disconnected instance, this method must return a
     * failure.
     * Return:
     *  NO_ERROR on success, or an appropriate error status.
     */
    virtual status_t startCapturing(int width, int height, uint32_t pix_fmt);

    /* Stops capturing frames from the camera device.
     *
     * Typically, this method calls stopDevice method of this class, and
     * uninitializes frame properties, saved in StartCapturing method of this
     * class.
     * This method must be called on a connected instance of this class. If it
     * is called on a disconnected instance, this method must return a failure.
     * Return:
     *  NO_ERROR on success, or an appropriate error status.
     */
    virtual status_t stopCapturing();

    /* Gets current fame into provided buffer.
     * Typically, this method is called by the emulated camera (HAL) in response
     * to a callback from the emulated camera device that gets invoked when new
     * captured frame is available.
     * This method must be called on an instance that is capturing frames from
     * the physical device. If this method is called on an instance that is not
     * capturing frames from the physical device, it must return a failure.
     * Param:
     *  buffer - A buffer where to return the frame. Note that the buffer must be
     *      large enough to contain the entire frame, as defined by frame's width,
     *      height, and pixel format that are current for the camera device.
     */
    virtual status_t getCurrentFrame(void* buffer);

    /* Gets current preview fame into provided buffer.
     * Param:
     *  buffer - A buffer where to return the preview frame. Note that the buffer
     *      must be large enough to contain the entire preview frame, as defined
     *      by frame's width, height, and preview pixel format. Note also, that
     *      due to the the limitations of the camera framework in emulator, the
     *      preview frame is always formatted with RGBA8888.
     */
    virtual status_t getCurrentPreviewFrame(void* buffer);

    /* Gets width of the frame obtained from the physical device. */
    inline int getFrameWidth() const
    {
        return mFrameWidth;
    }

    /* Gets height of the frame obtained from the physical device. */
    inline int getFrameHeight() const
    {
        return mFrameHeight;
    }

    /* Gets byte size of the current frame buffer. */
    inline size_t getFrameBufferSize() const
    {
        return mFrameBufferSize;
    }

    /* Gets number of pixels in the current frame buffer. */
    inline int getPixelNum() const
    {
        return mTotalPixels;
    }

    /* Gets pixel format of the frame that physical device streams.
     * Throughout camera framework, there are three different forms of pixel
     * format representation:
     *  - Original format, as reported by the actual camera device. Values for
     *    this format are declared in bionic/libc/kernel/common/linux/videodev2.h
     *  - String representation as defined in CameraParameters::PIXEL_FORMAT_XXX
     *    strings in frameworks/base/include/camera/CameraParameters.h
     *  - HAL_PIXEL_FORMAT_XXX format, as defined in system/core/include/system/graphics.h
     * Since emulated camera device gets its data from the actual device, it gets
     * pixel format in the original form. And that's the pixel format
     * representation that will be returned from this method. HAL components will
     * need to translate value returned from this method to the appropriate form.
     * This method must be called only on connected instance of this class, since
     * it's applicable only when physical device is ready to stream frames. If
     * this method is called on an instance that is not connected, it must return
     * a failure.
     * Param:
     *  pix_fmt - Upon success contains the original pixel format.
     * Return:
     *  Current framebuffer's pixel format.
     */
    inline uint32_t getOriginalPixelFormat() const
    {
        return mPixelFormat;
    }

    /*
     * State checkers.
     */

    inline bool isInitialized() const {
        /* Instance is initialized when the worker thread has been successfuly
         * created (but not necessarily started). */
        return mWorkerThread.get() != NULL && mState != ECDS_CONSTRUCTED;
    }
    inline bool isConnected() const {
        /* Instance is connected when it is initialized and its status is either
         * "connected", or "capturing". */
        return isInitialized() &&
               (mState == ECDS_CONNECTED || mState == ECDS_CAPTURING);
    }
    inline bool isCapturing() const {
        return isInitialized() && mState == ECDS_CAPTURING;
    }

    /****************************************************************************
     * Worker thread management.
     * Typicaly when emulated camera device starts capturing frames from the
     * actual device, it does that in a worker thread created in StartCapturing,
     * and terminated in StopCapturing. Since this is such a typical scenario,
     * it makes sence to encapsulate worker thread management in the base class
     * for all emulated camera devices.
     ***************************************************************************/

protected:
    /* Starts the worker thread.
     * Typically, worker thread is started from StartCamera method of this
     * class.
     * Return:
     *  NO_ERROR on success, or an appropriate error status.
     */
    virtual status_t startWorkerThread();

    /* Stops the worker thread.
     * Note that this method will always wait for the worker thread to terminate.
     * Return:
     *  NO_ERROR on success, or an appropriate error status.
     */
    virtual status_t stopWorkerThread();

    /* Implementation of the worker thread routine.
     * In the default implementation of the worker thread routine we simply
     * return 'false' forcing the thread loop to exit, and the thread to
     * terminate. Derived class should override that method to provide there the
     * actual frame capturing functionality.
     * Return:
     *  true To continue thread loop (this method will be called again), or false
     *  to exit the thread loop and to terminate the thread.
     */
    virtual bool inWorkerThread();

    /* Encapsulates a worker thread used by the emulated camera device.
     */
    friend class WorkerThread;
    class WorkerThread : public Thread {

        /****************************************************************************
         * Public API
         ***************************************************************************/

        public:
            inline explicit WorkerThread(EmulatedCameraDevice* camera_dev)
                : Thread(true),   // Callbacks may involve Java calls.
                  mCameraDevice(camera_dev),
                  mThreadControl(-1),
                  mControlFD(-1)
            {
            }

            inline ~WorkerThread()
            {
                LOGW_IF(mThreadControl >= 0 || mControlFD >= 0,
                        "%s: Control FDs are opened in the destructor",
                        __FUNCTION__);
                if (mThreadControl >= 0) {
                    close(mThreadControl);
                }
                if (mControlFD >= 0) {
                    close(mControlFD);
                }
            }

            /* Starts the thread */
            inline status_t startThread()
            {
                return run(NULL, ANDROID_PRIORITY_URGENT_DISPLAY, 0);
            }

            /* Overriden base class method.
             * It is overriden in order to provide one-time initialization just
             * prior to starting the thread routine.
             */
            status_t readyToRun();

            /* Stops the thread. */
            status_t stopThread();

            /* Values returned from the Select method of this class. */
            enum SelectRes {
                /* A timeout has occurred. */
                TIMEOUT,
                /* Data are available for read on the provided FD. */
                READY,
                /* Thread exit request has been received. */
                EXIT_THREAD,
                /* An error has occurred. */
                ERROR
            };

            /* Select on an FD event, keeping in mind thread exit message.
             * Param:
             *  fd - File descriptor on which to wait for an event. This
             *      parameter may be negative. If it is negative this method will
             *      only wait on a control message to the thread.
             *  timeout - Timeout in microseconds. 0 indicates no timeout (wait
             *      forever).
             * Return:
             *  See SelectRes enum comments.
             */
            SelectRes Select(int fd, int timeout);

        /****************************************************************************
         * Private API
         ***************************************************************************/

        private:
            /* Implements abstract method of the base Thread class. */
            inline bool threadLoop()
            {
                /* Simply dispatch the call to the containing camera device. */
                return mCameraDevice->inWorkerThread();
            }

            /* Containing camera device object. */
            EmulatedCameraDevice*   mCameraDevice;

            /* FD that is used to send control messages into the thread. */
            int                     mThreadControl;

            /* FD that thread uses to receive control messages. */
            int                     mControlFD;

            /* Enumerates control messages that can be sent into the thread. */
            enum ControlMessage {
                /* Stop the thread. */
                THREAD_STOP
            };
    };

    /* Worker thread accessor. */
    inline WorkerThread* getWorkerThread() const
    {
        return mWorkerThread.get();
    }

    /****************************************************************************
     * Data members
     ***************************************************************************/

protected:
    /* Locks this instance for parameters, state, etc. change. */
    Mutex                       mObjectLock;

    /* Worker thread that is used in frame capturing. */
    sp<WorkerThread>            mWorkerThread;

    /* Timestamp of the current frame. */
    nsecs_t                     mCurFrameTimestamp;

    /* Emulated camera object containing this instance. */
    EmulatedCamera*             mCameraHAL;

    /* Framebuffer containing the current frame. */
    uint8_t*                    mCurrentFrame;

    /* U panel inside the framebuffer. */
    uint8_t*                    mFrameU;

    /* V panel inside the framebuffer. */
    uint8_t*                    mFrameV;

    /*
     * Framebuffer properties.
     */

    /* Byte size of the framebuffer. */
    size_t                      mFrameBufferSize;

    /* Original pixel format (one of the V4L2_PIX_FMT_XXX values, as defined in
     * bionic/libc/kernel/common/linux/videodev2.h */
    uint32_t                    mPixelFormat;

    /* Frame width */
    int                         mFrameWidth;

    /* Frame height */
    int                         mFrameHeight;

    /* Total number of pixels */
    int                         mTotalPixels;

    /* Defines possible states of the emulated camera device object.
     */
    enum EmulatedCameraDeviceState {
        /* Object has been constructed. */
        ECDS_CONSTRUCTED,
        /* Object has been initialized. */
        ECDS_INITIALIZED,
        /* Object has been connected to the physical device. */
        ECDS_CONNECTED,
        /* Frames are being captured. */
        ECDS_CAPTURING,
    };

    /* Object state. */
    EmulatedCameraDeviceState   mState;
};

}; /* namespace android */

#endif  /* HW_EMULATOR_CAMERA_EMULATED_CAMERA_DEVICE_H */
