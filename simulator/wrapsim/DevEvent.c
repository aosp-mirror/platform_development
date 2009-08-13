/*
 * Copyright 2007 The Android Open Source Project
 *
 * Input event device.
 */
#include "Common.h"

#include <stdlib.h>
#include <string.h>

#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/input.h>


/*
 * Input event device state.
 */
typedef struct EventState {
    struct input_id ident;

    char*   name;
    char*   location;
    char*   idstr;
    int     protoVersion;
} EventState;

/*
 * Key bit mask, for EVIOCGBIT(EV_KEY).
 *
 * (For now, just pretend to be a "goldfish" like the emulator.)
 */
static const unsigned char gKeyBitMask[64] = {
    // These bits indicate which keys the device has
    0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 
    0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 
    0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 
    0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 
    // These bits indicate other capabilities, such
    // as whether it's a trackball or a touchscreen
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // touchscreen
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
};

/*
 * Abs bit mask, for EVIOCGBIT(EV_ABS).
 *
 * Pretend to be a normal single touch panel
 */
static const unsigned char gAbsBitMask[64] = {
    // these bits indicate the capabilities of the touch screen
    0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // ABS_X, ABS_Y
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
};

/*
 * Set some stuff up.
 */
static void configureInitialState(const char* pathName, EventState* eventState)
{
    /*
     * Swim like a goldfish.
     */
    eventState->ident.bustype = 0;
    eventState->ident.vendor = 0;
    eventState->ident.product = 0;
    eventState->ident.version = 0;

    eventState->name = strdup(gWrapSim.keyMap);
    eventState->location = strdup("");
    eventState->idstr = strdup("");
    eventState->protoVersion = 0x010000;
}

/*
 * Free up the state structure.
 */
static void freeState(EventState* eventState)
{
    if (eventState != NULL) {
        free(eventState->name);
        free(eventState->location);
        free(eventState->idstr);
        free(eventState);
    }
}

/*
 * Handle one of the EVIOCGABS requests.
 *
 * Currently not doing much here.
 */
static void handleAbsGet(int reqIdx, void* argp)
{
    struct input_absinfo info;

    switch (reqIdx) {
    case ABS_X:
        wsLog("  req for abs X\n");
        break;
    case ABS_Y:
        wsLog("  req for abs Y\n");
        break;
    case ABS_PRESSURE:
        wsLog("  req for abs PRESSURE\n");
        break;
    case ABS_TOOL_WIDTH:
        wsLog("  req for abs TOOL_WIDTH\n");
        break;
    default:
        wsLog("  req for unexpected event abs 0x%02x\n", reqIdx);
        break;
    }

    memset(&info, 0, sizeof(info));
    memcpy(argp, &info, sizeof(struct input_absinfo));
}

/*
 * Return the next available input event.
 *
 * We just pass this through to the real "read", since "fd" is real.
 */
static ssize_t readEvent(FakeDev* dev, int fd, void* buf, size_t count)
{
    return _ws_read(fd, buf, count);
}

/*
 * Somebody is trying to write to the event pipe.  This can be used to set
 * the state of LED.
 */
static ssize_t writeEvent(FakeDev* dev, int fd, const void* buf, size_t count)
{
    const struct input_event* piev;

    if (count == sizeof(*piev)) {
        piev = (const struct input_event*) buf;

        if (piev->type == EV_LED) {
            wsLog("%s: set LED code=%d value=%d\n",
                dev->debugName, piev->code, piev->value);
        } else {
            wsLog("%s: writeEvent got %d bytes, type=%d\n",
                dev->debugName, count, piev->type);
        }
    } else {
        wsLog("%s: warning: writeEvent got %d bytes, not sure why\n",
            dev->debugName, count);
    }

    return count;
}

/*
 * Handle event ioctls.
 */
static int ioctlEvent(FakeDev* dev, int fd, int request, void* argp)
{
    EventState* state = (EventState*) dev->state;
    unsigned int urequest = (unsigned int) request;

    wsLog("%s: ioctl(0x%x, %p)\n", dev->debugName, urequest, argp);

    if (_IOC_TYPE(urequest) != _IOC_TYPE(EVIOCGVERSION)) {
        wsLog("%s: inappropriate ioctl 0x%08x\n", dev->debugName, urequest);
        return -1;
    }

    if (urequest == EVIOCGVERSION) {
        *(int*)argp = state->protoVersion;
    } else if (urequest == EVIOCGID) {
        memcpy(argp, &state->ident, sizeof(struct input_id));
    } else if (_IOC_NR(urequest) == _IOC_NR(EVIOCGNAME(0))) {
        int maxLen = _IOC_SIZE(urequest);
        int strLen = (int) strlen(state->name);
        if (strLen >= maxLen) {
            errno = EINVAL;
            return -1;
        }
        memcpy(argp, state->name, strLen+1);
        return strLen;
    } else if (_IOC_NR(urequest) == _IOC_NR(EVIOCGPHYS(0))) {
        int maxLen = _IOC_SIZE(urequest);
        int strLen = (int) strlen(state->location);
        if (strLen >= maxLen) {
            errno = EINVAL;
            return -1;
        }
        memcpy(argp, state->location, strLen+1);
        return strLen;
    } else if (_IOC_NR(urequest) == _IOC_NR(EVIOCGUNIQ(0))) {
        /* device doesn't seem to support this, neither will we */
        return -1;
    } else if (_IOC_NR(urequest) == _IOC_NR(EVIOCGBIT(EV_KEY,0))) {
        /* keys */
        int maxLen = _IOC_SIZE(urequest);
        if (maxLen > (int) sizeof(gKeyBitMask))
            maxLen = sizeof(gKeyBitMask);
        memcpy(argp, gKeyBitMask, maxLen);
    } else if (_IOC_NR(urequest) == _IOC_NR(EVIOCGBIT(EV_REL,0))) {
        /* relative controllers (trackball) */
        int maxLen = _IOC_SIZE(urequest);
        memset(argp, 0xff, maxLen);
    } else if (!getenv("NOTOUCH") && _IOC_NR(urequest) == _IOC_NR(EVIOCGBIT(EV_ABS,0))) {
        // absolute controllers (touch screen)
        int maxLen = _IOC_SIZE(urequest);
        if (maxLen > (int) sizeof(gAbsBitMask))
            maxLen = sizeof(gAbsBitMask);
        memcpy(argp, gAbsBitMask, maxLen);

    } else if (_IOC_NR(urequest) >= _IOC_NR(EVIOCGABS(ABS_X)) &&
               _IOC_NR(urequest) <= _IOC_NR(EVIOCGABS(ABS_MAX)))
    {
        /* get abs value / limits */
        int reqIdx = _IOC_NR(urequest) - _IOC_NR(EVIOCGABS(ABS_X));
        handleAbsGet(reqIdx, argp);
    } else {
        wsLog("GLITCH: UNKNOWN ioctl request 0x%x on %s\n",
            urequest, dev->debugName);
        return -1;
    }

    return 0;
}

/*
 * Free up our state before closing down the fake descriptor.
 */
static int closeEvent(FakeDev* dev, int fd)
{
    freeState((EventState*)dev->state);
    dev->state = NULL;
    if (gWrapSim.keyInputDevice == dev) {
        gWrapSim.keyInputDevice = NULL;
        wsLog("Sim input device closed\n");
    }
    return 0;
}

/*
 * Open an input event device.
 */
FakeDev* wsOpenDevEvent(const char* pathName, int flags)
{
    FakeDev* newDev = wsCreateRealFakeDev(pathName);
    if (newDev != NULL) {
        newDev->read = readEvent;
        newDev->write = writeEvent;
        newDev->ioctl = ioctlEvent;
        newDev->close = closeEvent;

        EventState* eventState = calloc(1, sizeof(EventState));

        configureInitialState(pathName, eventState);
        newDev->state = eventState;

        /*
         * First one opened becomes the place where we queue up input
         * events from the simulator.  This approach will fail if the
         * app opens the device, then opens it a second time for input,
         * then closes the first.  The app doesn't currently do this (though
         * it does do quick opens to fiddle with LEDs).
         */
        if (gWrapSim.keyInputDevice == NULL) {
            gWrapSim.keyInputDevice = newDev;
            wsLog("Device %p / %d will receive sim input events\n",
                newDev, newDev->fd);
        }
    }

    return newDev;
}

/*
 * Write a key event.
 */
static int sendKeyEvent(FakeDev* dev, int code, int isDown)
{
    struct input_event iev;
    ssize_t actual;

    gettimeofday(&iev.time, NULL);
    iev.type = EV_KEY;
    iev.code = code;
    iev.value = (isDown != 0) ? 1 : 0;

    actual = _ws_write(dev->otherFd, &iev, sizeof(iev));
    if (actual != (ssize_t) sizeof(iev)) {
        wsLog("WARNING: send key event partial write (%d of %d)\n",
            actual, sizeof(iev));
        return -1;
    }

    return 0;
}

/*
 * Write an absolute (touch screen) event.
 */
static int sendAbsButton(FakeDev* dev, int x, int y, int isDown)
{
    struct input_event iev;
    ssize_t actual;

    wsLog("absButton x=%d y=%d down=%d\n", x, y, isDown);

    gettimeofday(&iev.time, NULL);
    iev.type = EV_KEY;
    iev.code = BTN_TOUCH;
    iev.value = (isDown != 0) ? 1 : 0;

    actual = _ws_write(dev->otherFd, &iev, sizeof(iev));
    if (actual != (ssize_t) sizeof(iev)) {
        wsLog("WARNING: send touch event partial write (%d of %d)\n",
            actual, sizeof(iev));
        return -1;
    }

    return 0;
}

/*
 * Write an absolute (touch screen) event.
 */
static int sendAbsMovement(FakeDev* dev, int x, int y)
{
    struct input_event iev;
    ssize_t actual;

    wsLog("absMove x=%d y=%d\n", x, y);

    gettimeofday(&iev.time, NULL);
    iev.type = EV_ABS;
    iev.code = ABS_X;
    iev.value = x;

    actual = _ws_write(dev->otherFd, &iev, sizeof(iev));
    if (actual != (ssize_t) sizeof(iev)) {
        wsLog("WARNING: send abs movement event partial X write (%d of %d)\n",
            actual, sizeof(iev));
        return -1;
    }

    iev.code = ABS_Y;
    iev.value = y;

    actual = _ws_write(dev->otherFd, &iev, sizeof(iev));
    if (actual != (ssize_t) sizeof(iev)) {
        wsLog("WARNING: send abs movement event partial Y write (%d of %d)\n",
            actual, sizeof(iev));
        return -1;
    }

    return 0;
}

/*
 * Not quite sure what this is for, but the emulator does it.
 */
static int sendAbsSyn(FakeDev* dev)
{
    struct input_event iev;
    ssize_t actual;

    gettimeofday(&iev.time, NULL);
    iev.type = EV_SYN;
    iev.code = 0;
    iev.value = 0;

    actual = _ws_write(dev->otherFd, &iev, sizeof(iev));
    if (actual != (ssize_t) sizeof(iev)) {
        wsLog("WARNING: send abs movement syn (%d of %d)\n",
            actual, sizeof(iev));
        return -1;
    }

    return 0;
}

/*
 * Send a key event to the fake key event device.
 *
 * We have to translate the simulator key event into one or more device
 * key events.
 */
void wsSendSimKeyEvent(int key, int isDown)
{
    FakeDev* dev;
    EventState* state;

    dev = gWrapSim.keyInputDevice;
    if (dev == NULL)
        return;

    sendKeyEvent(dev, key, isDown);
}

/*
 * Send a touch-screen event to the fake key event device.
 *
 * We have to translate the simulator key event into one or more device
 * key events.
 */
void wsSendSimTouchEvent(int action, int x, int y)
{
    FakeDev* dev;
    EventState* state;

    dev = gWrapSim.keyInputDevice;
    if (dev == NULL)
        return;

    if (action == kTouchDown) {
        sendAbsMovement(dev, x, y);
        sendAbsButton(dev, x, y, 1);
        sendAbsSyn(dev);
    } else if (action == kTouchUp) {
        sendAbsButton(dev, x, y, 0);
        sendAbsSyn(dev);
    } else if (action == kTouchDrag) {
        sendAbsMovement(dev, x, y);
        sendAbsSyn(dev);
    } else {
        wsLog("WARNING: unexpected sim touch action  %d\n", action);
    }
}

