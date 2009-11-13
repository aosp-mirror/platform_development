/*
 * Copyright 2007 The Android Open Source Project
 *
 * Fake device support.
 */
/*
Implementation notes:

There are a couple of basic scenarios, exemplified by the "fb" and
"events" devices.  The framebuffer driver is pretty simple, handling a
few ioctl()s and managing a stretch of memory.  We can just intercept a
few calls.  The input event driver can be used in a select() or poll()
call with other file descriptors, which either requires us to do some
fancy tricks with select() and poll(), or requires that we return a real
file descriptor (perhaps based on a socketpair).

We have three basic approaches to dealing with "fake" file descriptors:

(1) Always use real fds.  We can dup() an open /dev/null to get a number
    for the cases where we don't need a socketpair.
(2) Always use fake fds with absurdly high numeric values.  Testing to see
    if the fd is one we handle is trivial (range check).  This doesn't
    work for select(), which uses fd bitmaps accessed through macros.
(3) Use a mix of real and fake fds, in a high range (512-1023).  Because
    it's in the "real" range, we can pass real fds around for things that
    are handed to poll() and select(), but because of the high numeric
    value we *should* be able to get away with a trivial range check.

Approach (1) is the most portable and least likely to break, but the
efficiencies gained in approach (2) make it more desirable.  There is
a small risk of application fds wandering into our range, but we can
minimize that by asserting on a "guard zone" and/or obstructing dup2().
(We can also dup2(/dev/null) to "reserve" our fds, but that wastes
resources.)
*/

#include "Common.h"

#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <assert.h>
#include <fnmatch.h>

/*
 * Devices we intercept.
 *
 * Needed:
 *  /dev/alarm
 *  radio
 */
typedef FakeDev* (*wsFileHook)(const char *path, int flags);

typedef struct FakedPath {
    const char *pathexpr;
    wsFileHook hook;
} FakedPath;

FakedPath fakedpaths[] =
{
    { "/dev/graphics/fb0",      wsOpenDevFb },
    { "/dev/hw3d",              NULL },
    { "/dev/eac",               wsOpenDevAudio },
    { "/dev/tty0",              wsOpenDevConsoleTty },
    { "/dev/input/event0",      wsOpenDevEvent },
    { "/dev/input/*",           NULL },
    { "/dev/log/*",             wsOpenDevLog },
    { "/sys/class/power_supply/*", wsOpenDevPower },
    { "/sys/power/state",       wsOpenSysPower },
    { "/sys/power/wake_lock",   wsOpenSysPower },
    { "/sys/power/wake_unlock", wsOpenSysPower },
    { "/sys/devices/platform/android-vibrator/enable",  wsOpenDevVibrator },
    { "/sys/qemu_trace/*",      NULL },
    { NULL,                     NULL }
};


/*
 * Generic drop-in for an unimplemented call.
 *
 * Returns -1, which conveniently is the same as MAP_FAILED for mmap.
 */
static int notImplemented(FakeDev* dev, const char* callName)
{
    wsLog("WARNING: unimplemented %s() on '%s' %p\n",
        callName, dev->debugName, dev->state);
    errno = kNoHandlerError;
    return -1;
}

/*
 * Default implementations.  We want to log as much information as we can
 * so that we can fill in the missing implementation.
 *
 * TODO: for some or all of these we will want to display the full arg list.
 */
static int noClose(FakeDev* dev, ...)
{
    return 0;
}
static FakeDev* noDup(FakeDev* dev, ...)
{
    notImplemented(dev, "dup");
    return NULL;
}
static int noRead(FakeDev* dev, ...)
{
    return notImplemented(dev, "read");
}
static int noReadv(FakeDev* dev, ...)
{
    return notImplemented(dev, "readv");
}
static int noWrite(FakeDev* dev, ...)
{
    return notImplemented(dev, "write");
}
static int noWritev(FakeDev* dev, ...)
{
    return notImplemented(dev, "writev");
}
static int noMmap(FakeDev* dev, ...)
{
    return notImplemented(dev, "mmap");
}
static int noIoctl(FakeDev* dev, ...)
{
    return notImplemented(dev, "ioctl");
}


/*
 * Create a new FakeDev entry.
 *
 * We mark the fd slot as "used" in the bitmap, but don't add it to the
 * table yet since the entry is not fully prepared.
 */
FakeDev* wsCreateFakeDev(const char* debugName)
{
    FakeDev* newDev;
    int cc;

    assert(debugName != NULL);

    newDev = (FakeDev*) calloc(1, sizeof(FakeDev));
    if (newDev == NULL)
        return NULL;

    newDev->debugName = strdup(debugName);
    newDev->state = NULL;

    newDev->close = (Fake_close) noClose;
    newDev->dup = (Fake_dup) noDup;
    newDev->read = (Fake_read) noRead;
    newDev->readv = (Fake_readv) noReadv;
    newDev->write = (Fake_write) noWrite;
    newDev->writev = (Fake_writev) noWritev;
    newDev->mmap = (Fake_mmap) noMmap;
    newDev->ioctl = (Fake_ioctl) noIoctl;

    /*
     * Allocate a new entry.  The bit vector map is really only used as a
     * performance boost in the current implementation.
     */
    cc = pthread_mutex_lock(&gWrapSim.fakeFdLock); assert(cc == 0);
    int newfd = wsAllocBit(gWrapSim.fakeFdMap);
    cc = pthread_mutex_unlock(&gWrapSim.fakeFdLock); assert(cc == 0);

    if (newfd < 0) {
        wsLog("WARNING: ran out of 'fake' file descriptors\n");
        free(newDev);
        return NULL;
    }
    newDev->fd = newfd + kFakeFdBase;
    newDev->otherFd = -1;
    assert(gWrapSim.fakeFdList[newDev->fd - kFakeFdBase] == NULL);

    return newDev;
}

/*
 * Create a new FakeDev entry, and open a file descriptor that actually
 * works.
 */
FakeDev* wsCreateRealFakeDev(const char* debugName)
{
    FakeDev* newDev = wsCreateFakeDev(debugName);
    if (newDev == NULL)
        return newDev;
    
    int fds[2];

    if (socketpair(AF_UNIX, SOCK_STREAM, 0, fds) < 0) {
        wsLog("socketpair() failed: %s\n", strerror(errno));
        wsFreeFakeDev(newDev);
        return NULL;
    }

    if (dup2(fds[0], newDev->fd) < 0) {
        wsLog("dup2(%d,%d) failed: %s\n",
            fds[0], newDev->fd, strerror(errno));
        wsFreeFakeDev(newDev);
        return NULL;
    }
    close(fds[0]);

    /* okay to leave this one in the "normal" range; not visible to app */
    newDev->otherFd = fds[1];

    return newDev;
}

/*
 * Free fake device entry.
 */
void wsFreeFakeDev(FakeDev* dev)
{
    if (dev == NULL)
        return;

    wsLog("## closing/freeing '%s' (%d/%d)\n",
        dev->debugName, dev->fd, dev->otherFd);

    /*
     * If we assigned a file descriptor slot, free it up.
     */
    if (dev->fd >= 0) {
        int cc;

        gWrapSim.fakeFdList[dev->fd - kFakeFdBase] = NULL;

        cc = pthread_mutex_lock(&gWrapSim.fakeFdLock); assert(cc == 0);
        wsFreeBit(gWrapSim.fakeFdMap, dev->fd - kFakeFdBase);
        cc = pthread_mutex_unlock(&gWrapSim.fakeFdLock); assert(cc == 0);
    }
    if (dev->otherFd >= 0)
        close(dev->otherFd);

    if (dev->debugName) free(dev->debugName);
    free(dev);
}

/*
 * Map a file descriptor to a fake device.
 *
 * Returns NULL if there's no corresponding entry.
 */
FakeDev* wsFakeDevFromFd(int fd)
{
    /* quick range test */
    if (fd < kFakeFdBase || fd >= kFakeFdBase + kMaxFakeFdCount)
        return NULL;

    return gWrapSim.fakeFdList[fd - kFakeFdBase];
}


/*
 * Check to see if we're opening a device that we want to fake out.
 *
 * We return a file descriptor >= 0 on success, -1 if we're not interested,
 * or -2 if we explicitly want to pretend that the device doesn't exist.
 */
int wsInterceptDeviceOpen(const char* pathName, int flags)
{
    FakedPath* p = fakedpaths;

    while (p->pathexpr) {
        if (fnmatch(p->pathexpr, pathName, 0) == 0) {
            if (p->hook != NULL) {
                FakeDev* dev = p->hook(pathName, flags);
                if (dev != NULL) {
                    /*
                     * Now that the device entry is ready, add it to the list.
                     */
                    wsLog("## created fake dev %d: '%s' %p\n",
                        dev->fd, dev->debugName, dev->state);
                    gWrapSim.fakeFdList[dev->fd - kFakeFdBase] = dev;
                    return dev->fd;
                }
            } else {
                wsLog("## rejecting attempt to open %s\n", pathName);
                errno = ENOENT;
                return -2;
            }
            break;
        }
        p++;
    }
    return -1;
}

/*
 * Check to see if we're accessing a device that we want to fake out.
 * Returns 0 if the device can be (fake) opened with the given mode,
 * -1 if it can't, -2 if it can't and we don't want to allow fallback
 * to the host-device either.
 * TODO: actually check the mode.
 */
int wsInterceptDeviceAccess(const char *pathName, int mode)
{
    FakedPath *p = fakedpaths;

    while (p->pathexpr) {
        if (fnmatch(p->pathexpr, pathName, 0) == 0) {
            if (p->hook) {
                return 0;
            } else {
                wsLog("## rejecting attempt to open %s\n", pathName);
                errno = ENOENT;
                return -2;
            }
            break;
        }
        p++;
    }
    errno = ENOENT;
    return -1;
}
