/*
 * Copyright 2009 The Android Open Source Project
 *
 * Magic entries in /sys/power/.
 */
#include "Common.h"

#include <stdlib.h>
#include <string.h>
#include <ctype.h>

/*
 * Map filename to device index.
 *
 * [ not using DeviceIndex -- would be useful if we need to return something
 * other than a static string ]
 */
static const struct {
    const char*     name;
    //DeviceIndex     idx;
    const char*     data;
} gDeviceMap[] = {
    { "state",
        "mem\n" },
    { "wake_lock",
        "\n" },
    { "wake_unlock",
        "KeyEvents PowerManagerService radio-interface\n" },
};

/*
 * Power driver state.
 *
 * Right now we just ignore everything written.
 */
typedef struct PowerState {
    int         which;
} PowerState;


/*
 * Figure out who we are, based on "pathName".
 */
static void configureInitialState(const char* pathName, PowerState* powerState)
{
    const char* cp = pathName + strlen("/sys/power/");
    int i;

    powerState->which = -1;
    for (i = 0; i < (int) (sizeof(gDeviceMap) / sizeof(gDeviceMap[0])); i++) {
        if (strcmp(cp, gDeviceMap[i].name) == 0) {
            powerState->which = i;
            break;
        }
    }

    if (powerState->which == -1) {
        wsLog("Warning: access to unknown power device '%s'\n", pathName);
        return;
    }
}

/*
 * Free up the state structure.
 */
static void freeState(PowerState* powerState)
{
    free(powerState);
}

/*
 * Read data from the device.
 *
 * We don't try to keep track of how much was read -- existing clients just
 * try to read into a large buffer.
 */
static ssize_t readPower(FakeDev* dev, int fd, void* buf, size_t count)
{
    PowerState* state = (PowerState*) dev->state;
    int dataLen;

    wsLog("%s: read %d\n", dev->debugName, count);

    if (state->which < 0 ||
        state->which >= (int) (sizeof(gDeviceMap)/sizeof(gDeviceMap[0])))
    {
        return 0;
    }

    const char* data = gDeviceMap[state->which].data;
    size_t strLen = strlen(data);

    while(strLen == 0)
        sleep(10); // block forever

    ssize_t copyCount = (strLen < count) ? strLen : count;
    memcpy(buf, data, copyCount);
    return copyCount;
}

/*
 * Ignore the request.
 */
static ssize_t writePower(FakeDev* dev, int fd, const void* buf, size_t count)
{
    wsLog("%s: write %d bytes\n", dev->debugName, count);
    return count;
}

/*
 * Free up our state before closing down the fake descriptor.
 */
static int closePower(FakeDev* dev, int fd)
{
    freeState((PowerState*)dev->state);
    dev->state = NULL;
    return 0;
}

/*
 * Open a power device.
 */
FakeDev* wsOpenSysPower(const char* pathName, int flags)
{
    FakeDev* newDev = wsCreateFakeDev(pathName);
    if (newDev != NULL) {
        newDev->read = readPower;
        newDev->write = writePower;
        newDev->ioctl = NULL;
        newDev->close = closePower;

        PowerState* powerState = calloc(1, sizeof(PowerState));

        configureInitialState(pathName, powerState);
        newDev->state = powerState;
    }

    return newDev;
}

