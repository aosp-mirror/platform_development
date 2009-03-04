//
// Copyright 2005 The Android Open Source Project
//
// Class that manages the simulated device.
//
#ifndef _SIM_DEVICE_MANAGER_H
#define _SIM_DEVICE_MANAGER_H

#include "UserEvent.h"

#include "Shmem.h"
#include "MessageStream.h"
#include "SimRuntime.h"

#include "ui/PixelFormat.h"
#include "ui/KeycodeLabels.h"

#include <sys/stat.h>

/*
 * Manage the simulated device.  This includes starting/stopping as well
 * as sending messages to it and receiving events from it.
 *
 * The object may span multiple invocations of a specific device.  If
 * the simulator is reconfigured to use a device with different
 * characteristics, the object should be destroyed and recreated (which
 * guarantees that the runtime is restarted).
 */
class DeviceManager {
public:
    DeviceManager(void);
    virtual ~DeviceManager(void);

    /*
     * Initialize the object.  Call this once.
     *
     * "numDisplays" is the number of displays that the simulated hardware
     * supports.  The displays themselves are configured with separate calls.
     *
     * "statusWindow" should be the main frame.  Messages indicating runtime
     * startup/shutdown are sent, as well as error messages that should be
     * displayed in message boxes.
     */
    bool Init(int numDisplays, wxWindow* statusWindow);
    bool IsInitialized(void) const;

    /*
     * Tell the device manager that the windows used to display its output
     * are closing down.
     */
    void WindowsClosing(void);

    /*
     * "displayWindow" is the window to notify when a new frame of graphics
     * data is available.  This can be set independently for each display.
     */
    bool SetDisplayConfig(int displayIndex, wxWindow* window,
        int width, int height, android::PixelFormat format, int refresh);

    /*
     * set the key map
     */
    bool SetKeyboardConfig(const char *keymap);

    /*
     * Return the number of displays we're configured for.
     */
    int GetNumDisplays(void) const { return mNumDisplays; }

    /*
     * Return the shmem key for the Nth display.
     */
    //int GetShmemKey(int displayIndex);

    /*
     * Is the runtime process still running?
     */
    bool IsRunning(void) const {
        if (mThread != NULL)
            return mThread->IsRunning();
        return false;
    }
    bool IsKillable(void) const {
        return true;
    }

    // (Re-)configure the device, e.g. when #of displays changes because
    // a different phone model has been selected.  Call this before doing
    // any display-specific setup.  DO NOT call this if the runtime is active.
//    void Configure(int numDisplays);

    // start the runtime, acting as parent
    bool StartRuntime(void);
    // start the runtime, acting as peer
    bool StartRuntime(android::Pipe* reader, android::Pipe* writer);
    // politely ask the runtime to stop
    bool StopRuntime(void);
    // kill the runtime with extreme prejudice
    void KillRuntime(void);

#if 0
    // Returns if the executable is new
    bool RefreshRuntime(void);
    // Update the time of the current runtime because the user cancelled a
    // refresh
    void UserCancelledRefresh(void);
#endif

    // send a key-up or key-down event to the runtime
    void SendKeyEvent(KeyCode keyCode, bool down);
    // send touch-screen events
    void SendTouchEvent(android::Simulator::TouchMode mode, int x, int y);

    wxBitmap* GetImageData(int displayIndex);
    
    void BroadcastEvent(UserEvent &userEvent);

private:
    /*
     * Threads in wxWidgets use sub-classing to define interfaces and
     * entry points.  We use this to create the thread that interacts
     * with the runtime.
     *
     * The "reader" and "writer" arguments may be NULL.  If they are,
     * we will launch the runtime ourselves.  If not, we will use them
     * to speak with an externally-launched runtime process.  The thread
     * will own the pipes, shutting them down when it exits.
     */
    class DeviceThread : public wxThread {
    public:
        DeviceThread(DeviceManager* pDM, wxWindow* pStatusWindow,
            android::Pipe* reader, android::Pipe* writer)
            : wxThread(wxTHREAD_JOINABLE), mpStatusWindow(pStatusWindow),
              mReader(reader), mWriter(writer),
              mpDeviceManager(pDM), /*mTerminalFollowsChild(false),
              mSlowExit(false), mIsExternal(false), mLastModified(0),*/
              mRuntimeProcessGroup(0)
            {}
        virtual ~DeviceThread(void) {
            delete mReader;
            delete mWriter;
        }

        /* thread entry point */
        virtual void* Entry(void);

        // wxThread class supplies an IsRunning() method

        /*
         * This kills the runtime process to force this thread to exit.
         * If the thread doesn't exit after a short period of time, it
         * is forcibly terminated.
         */
        void KillChildProcesses(void);

#if 0
        /*
         * Return if the runtime executable is new
         */
        bool IsRuntimeNew(void);

        void UpdateLastModified(void);
#endif

        android::MessageStream* GetStream(void) { return &mStream; }

        static bool LaunchProcess(wxWindow* statusWindow);

    private:
        void WaitForDeath(int delay);
        void ResetProperties(void);

        android::MessageStream  mStream;
        wxWindow*       mpStatusWindow;
        android::Pipe*  mReader;
        android::Pipe*  mWriter;
        DeviceManager*  mpDeviceManager;
        pid_t           mRuntimeProcessGroup;
        //time_t          mLastModified;
        wxString        mRuntimeExe;
    };

    friend class DeviceThread;

    /*
     * We need one of these for each display on the device.  Most devices
     * only have one, but some flip phones have two.
     */
    class Display {
    public:
        Display(void)
            : mDisplayWindow(NULL), mpShmem(NULL), mShmemKey(0),
              mImageData(NULL), mDisplayNum(-1), mWidth(-1), mHeight(-1),
              mFormat(android::PIXEL_FORMAT_UNKNOWN), mRefresh(0)
            {}
        ~Display() {
            delete mpShmem;
            delete[] mImageData;
        }

        /* initialize goodies */
        bool Create(int displayNum, wxWindow* window, int width, int height,
            android::PixelFormat format, int refresh);

        /* call this if we're shutting down soon */
        void Uncreate(void);

        /* copy & convert data from shared memory */
        void CopyFromShared(void);

        /* get image data in the form of a 24bpp bitmap */
        wxBitmap* GetImageData(void);

        /* get a pointer to our display window */
        wxWindow* GetWindow(void) const { return mDisplayWindow; }

        /* get our shared memory key */
        int GetShmemKey(void) const { return mShmemKey; }

        int GetWidth(void) const { return mWidth; }
        int GetHeight(void) const { return mHeight; }
        android::PixelFormat GetFormat(void) const { return mFormat; }
        int GetRefresh(void) const { return mRefresh; }

    private:
        int GenerateKey(int displayNum) {
            return 0x41544d00 | displayNum;
        }

        // control access to image data shared between runtime mgr and UI
        wxMutex         mImageDataLock;
        // we send an event here when we get stuff to display
        wxWindow*       mDisplayWindow;

        // shared memory segment
        android::Shmem* mpShmem;
        int             mShmemKey;

        // local copy of data from shared mem, converted to 24bpp
        unsigned char*  mImageData;

        // mainly for debugging -- which display are we?
        int             mDisplayNum;

        // display characteristics
        int             mWidth;
        int             mHeight;
        android::PixelFormat mFormat;
        int             mRefresh;       // fps
    };

    Display* GetDisplay(int dispNum) { return &mDisplay[dispNum]; }

    const char* GetKeyMap() { return mKeyMap ? mKeyMap : "qwerty"; }

    void ShowFrame(int displayIndex);

    void Vibrate(int vibrateOn);

    // get the message stream from the device thread
    android::MessageStream* GetStream(void);

    // send a request to set the visible layers
    void SendSetVisibleLayers(void);

    // points at the runtime's thread (while it's running)
    DeviceThread*   mThread;

    // array of Displays, one per display on the device
    Display*        mDisplay;
    int             mNumDisplays;

    // the key map
    const char * mKeyMap;

    // which graphics layers are visible?
    int             mVisibleLayers;

    // where to send status messages
    wxWindow*       mpStatusWindow;

};

#endif // _SIM_DEVICE_MANAGER_H
