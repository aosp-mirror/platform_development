//
// Copyright 2005 The Android Open Source Project
//
// Miscellaneous definitions and declarations used for interaction
// between the device and the simulator.
//
// This header is included on both sides, so try not to include
// any other headers from here.
//
#ifndef _RUNTIME_SIMULATOR_H
#define _RUNTIME_SIMULATOR_H

#include "MessageStream.h"
#include "Shmem.h"
//#include "utils/RefBase.h"
#include "utils/Log.h"

namespace android {

#define ANDROID_PIPE_NAME "runtime"

/*
 * Hold simulator state.
 */
class Simulator {
public:
    Simulator(void);
    ~Simulator(void);

    /*
     * Commands exchanged between simulator and runtime.
     */
    typedef enum Command {
        kCommandUnknown = 0,

        /* sent from sim to runtime */
        kCommandGoAway,             // sim says: go away, I'm busy
        kCommandConfigDone,         // sim says: done sending config
        kCommandQuit,               // quit nicely
        kCommandNewPGroup,          // process group management
        kCommandKeyDown,            // key has been pressed
        kCommandKeyUp,              // key has been released
        kCommandTouch,              // finger touched/lifted/dragged

        /* sent from runtime to sim */
        kCommandNewPGroupCreated,    // send process group as argument
        kCommandRuntimeReady,       // we're initialized and about to start
        kCommandUpdateDisplay,      // display has been updated
        kCommandVibrate,            // vibrate on or off
    } Command;

    typedef enum TouchMode {
        kTouchDown = 0,
        kTouchUp = 1,
        kTouchDrag = 2
    } TouchMode;

    /*
     * Some parameters for config exchange.
     */
    enum {
        kDisplayConfigMagic = 0x44495350,
        kValuesPerDisplay = 5,
    };

    /*
     * Set up communication with parent process.
     */
    //bool create(ParentProcess* pParent);

    /*
     * Set up communication with detached simulator.
     */
    bool create(Pipe* reader, Pipe* writer);

    /*
     * Tell simulator that we're ready to go.
     */
    void sendRuntimeReady(void);

    /*
     * Tell the simulator that a display has been refreshed.
     */
    void sendDisplayUpdate(int displayIndex);

    /*
     * Tell the simulator to turn the vibrator on or off
     */
    void sendVibrate(int vibrateOn);

    /*
     * Get a pointer to the shared memory for the Nth display.
     */
    Shmem* getGraphicsBuffer(int displayIndex);

    /*
     * Return a copy of our input pipe so the event system can monitor
     * it for pending activity.
     */
    Pipe* getReadPipe(void) { return mStream.getReadPipe(); }

    /*
     * Retrieve the next command from the parent.  Returns NO_ERROR
     * if all is okay, WOULD_BLOCK if blocking is false and there
     * are no pending commands, or INVALID_OPERATION if the simulator
     * has disappeared.
     */
    int getNextKey(int32_t* outKey, bool* outDown);

    /*
     * Log system callback function.
     */
    static void writeLogMsg(const android_LogBundle* pBundle);

private:
    bool finishCreate(void);
    bool handleDisplayConfig(const long* pData, int length);

    MessageStream   mStream;
};

}; // namespace android

#endif // _RUNTIME_SIMULATOR_H
