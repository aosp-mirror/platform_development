/*
 * Copyright 2007 The Android Open Source Project
 *
 * Vibrating notification device.
 */
#include "Common.h"

#include <stdlib.h>
#include <string.h>
#include <ctype.h>

#include <unistd.h>


/*
 * The user will write a decimal integer indicating the time, in milliseconds,
 * that the device should vibrate.  In current usage, this is either -1
 * (meaning vibrate forever) or 0 (don't vibrate).
 */
static ssize_t writeVibrator(FakeDev* dev, int fd, const void* buf,
    size_t count)
{
    if (count == 2 && memcmp(buf, "0\n", 2) == 0) {
        wsEnableVibration(0);
    } else if (count == 3 && memcmp(buf, "-1\n", 3) == 0) {
        wsEnableVibration(1);
    } else {
        wsLog("%s: got %d bytes: '%*s'\n",
            dev->debugName, count, count, (const char*) buf);
    }

    return count;
}

/*
 * Open the vibration control device.
 */
FakeDev* wsOpenDevVibrator(const char* pathName, int flags)
{
    FakeDev* newDev = wsCreateFakeDev(pathName);
    if (newDev != NULL) {
        newDev->write = writeVibrator;
    }

    return newDev;
}

