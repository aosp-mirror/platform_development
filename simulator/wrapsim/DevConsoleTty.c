/*
 * Copyright 2007 The Android Open Source Project
 *
 * Console tty device.
 */
#include "Common.h"

#include <string.h>

#include <sys/ioctl.h>
#include <linux/vt.h>


/*
 * Handle the various console ioctls, most of which we can just ignore.
 */
static int ioctlConsoleTty(FakeDev* dev, int fd, int request, void* argp)
{
    wsLog("%s: ioctl(0x%x, %p)\n", dev->debugName, request, argp);
    switch (request) {
    case VT_GETSTATE:       // struct vt_stat*
        /*
         * Looks like they want vs.v_active.  This just gets fed back into
         * another console ioctl, so we don't really need to do anything.
         * We zero out the struct so the data will at least appear to be
         * initialized.
         */
        memset(argp, 0, sizeof(struct vt_stat));
        break;
    case VT_OPENQRY:        // int*
        /* they want the console number */
        *(int*)argp = 123;
        break;
    default:
        /* ignore anything we don't understand */
        break;
    }

    return 0;
}

/*
 * Open the console TTY device, which responds to a collection of ioctl()s.
 */
FakeDev* wsOpenDevConsoleTty(const char* pathName, int flags)
{
    FakeDev* newDev = wsCreateFakeDev(pathName);
    if (newDev != NULL) {
        newDev->ioctl = ioctlConsoleTty;
    }
    return newDev;
}

