/*
 * Copyright 2007 The Android Open Source Project
 *
 * Simulator interactions.
 */
#ifndef _WRAPSIM_SIMULATOR_H
#define _WRAPSIM_SIMULATOR_H

/*
 * Commands exchanged between simulator and runtime.
 *
 * NOTE: this is cloned from SimRuntime.h -- fix this.
 */
typedef enum SimCommand {
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
    kCommandNewPGroupCreated,   // send process group as argument
    kCommandRuntimeReady,       // we're initialized and about to start
    kCommandUpdateDisplay,      // display has been updated
    kCommandVibrate,            // vibrate on or off
} SimCommand;

/*
 * Touch screen action; also clined from SimRuntime.h.
 */
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
 * UNIX domain socket name.
 */
#define kAndroidPipeName        "runtime"

int wsSimConnect(void);

/*
 * Display management.
 */
void wsLockDisplay(int displayIdx);
void wsUnlockDisplay(int displayIdx);
void wsPostDisplayUpdate(int displayIdx);

/*
 * Send a log message.
 */
void wsPostLogMessage(int logPrio, const char* tag, const char* msg);

/*
 * Change the state of the vibration device.
 */
void wsEnableVibration(int vibrateOn);

#endif /*_WRAPSIM_SIMULATOR_H*/
