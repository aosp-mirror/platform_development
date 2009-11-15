/*
 * Copyright 2007 The Android Open Source Project
 *
 * Fake device support.
 */
#include "Common.h"

#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include <sys/mman.h>
#include <sys/ioctl.h>
#include <linux/fb.h>

typedef struct FbState {

    /* refcount for dup() */
    int refCount;

    /* index into gWrapSim.display[] */
    int     displayIdx;

    /* VRAM address, set by mmap() call */
    void*   vramAddr;

    /* kernel data structures */
    struct fb_var_screeninfo    vinfo;
    struct fb_fix_screeninfo    finfo;
} FbState;


/*
 * Set up the initial values of the structs.
 *
 * The FbState struct is zeroed out initially, so we only need to set the
 * fields that don't default to zero.
 */
static void configureInitialState(int displayIdx, FbState* fbState)
{
    int width, height;

    assert(displayIdx >= 0 && displayIdx < gWrapSim.numDisplays);

    width = gWrapSim.display[displayIdx].width;
    height = gWrapSim.display[displayIdx].height;
    wsLog("Configuring FbState for display %d (%dx%x key=0x%08x)\n",
        displayIdx, width, height, gWrapSim.display[displayIdx].shmemKey);

    /* fb_fix_screeninfo */
    strcpy(fbState->finfo.id, "omapfb");
    fbState->finfo.smem_len = (width * 2) * height * 2;
    fbState->finfo.line_length = width * 2;

    /* fb_var_screeninfo */
    fbState->vinfo.xres = width;
    fbState->vinfo.yres = height;
    fbState->vinfo.xres_virtual = width;
    fbState->vinfo.yres_virtual = height * 2;
    fbState->vinfo.bits_per_pixel = 16;

    fbState->vinfo.red.offset = 11;
    fbState->vinfo.red.length = 5;
    fbState->vinfo.green.offset = 5;
    fbState->vinfo.green.length = 6;
    fbState->vinfo.blue.offset = 0;
    fbState->vinfo.blue.length = 5;

    fbState->vinfo.width = 51;           // physical dimension, used for dpi
    fbState->vinfo.height = 76;

    fbState->vinfo.pixclock = 103092;    
    fbState->vinfo.upper_margin = 3;
    fbState->vinfo.lower_margin = 227;
    fbState->vinfo.left_margin = 12;
    fbState->vinfo.right_margin = 8;
}

/*
 * Free allocated state.
 */
static void freeState(FbState* fbState)
{
    int oldcount;

    oldcount = wsAtomicAdd(&fbState->refCount, -1);

    if (oldcount == 0) {
        free(fbState);
    }
}

/*
 * Wait for our synthetic vsync to happen.
 */
static void waitForVsync(FbState* state)
{
    /* TODO: simulate a real interval */
    usleep(1000000/60);
}

/*
 * Forward pixels to the simulator.
 */
static void sendPixelsToSim(FbState* state)
{
    if (state->vramAddr == 0) {
        wsLog("## not sending pixels (no addr yet)\n");
        return;
    }

    //wsLog("+++ sending pixels to sim (disp=%d yoff=%d)\n",
    //    state->displayIdx, state->vinfo.yoffset);

    wsLockDisplay(state->displayIdx);

    uint8_t* dst = gWrapSim.display[state->displayIdx].addr;

    int l,t,r,b,w,h;
    w = gWrapSim.display[state->displayIdx].width;
    h = gWrapSim.display[state->displayIdx].height;

#if 0
    /*
     * TODO: surfaceflinger encodes the dirty region in vinfo.reserved[].  We
     * can use that to perform a partial update.
     */
    const Rect dirty(dirtyReg.bounds());
    l = dirty.left  >=0 ? dirty.left : 0;
    t = dirty.top   >=0 ? dirty.top  : 0;
    r = dirty.right <=w ? dirty.right  : w;
    b = dirty.bottom<=h ? dirty.bottom : h;
#else
    l = t = 0;
    r = w;
    b = h;
#endif

    /* find the right page */
    int ypage = state->vinfo.yoffset;

    int x, y;
    for (y = t ; y < b ; y++) {
        // no "stride" issues with this display
        uint8_t* outPtr = dst + (y*w+l)*3;
        const uint16_t* ptr16 = (uint16_t*)state->vramAddr + ((y+ypage)*w+l);
        for (x = l; x < r; x++) {
            uint16_t in = *ptr16++;
            uint32_t R,G,B;
            R = ((in>>8)&0xF8) | (in>>(8+5));
            G = (in & 0x7E0)>>3;
            G |= G>>6;
            B = (in & 0x1F)<<3;
            B |= B>>5;
            *outPtr++ = R;
            *outPtr++ = G;
            *outPtr++ = B;
        }
    }

    wsUnlockDisplay(state->displayIdx);

    /* notify the simulator */
    wsPostDisplayUpdate(state->displayIdx);
}

/*
 * Provide a memory-mapped region for framebuffer data.  We want to use a
 * real mmap() call, not fake it with a malloc, so that related calls
 * (munmap, madvise) will just work.
 */
static void* mmapFb(FakeDev* dev, void* start, size_t length, int prot,
    int flags, int fd, __off_t offset)
{
    FbState* state = (FbState*) dev->state;
    void* map;

    /* be reasonable */
    if (length > (640*480*2)*4) {
        errno = EINVAL;
        return MAP_FAILED;
    }

    /* this is supposed to be VRAM, so just map a chunk */
    map = mmap(start, length, prot, MAP_PRIVATE | MAP_ANON, -1, 0);

    /* update our "VRAM address"; this feels a bit fragile */
    if (state->vramAddr != NULL) {
        wsLog("%s: NOTE: changing vram address from %p\n",
            dev->debugName, state->vramAddr);
    }
    state->vramAddr = map;

    wsLog("%s: mmap %u bytes --> %p\n", dev->debugName, length, map);
    return map;
}

/*
 * Handle framebuffer ioctls.
 */
static int ioctlFb(FakeDev* dev, int fd, int request, void* argp)
{
    FbState* state = (FbState*) dev->state;

    wsLog("%s: ioctl(0x%x, %p)\n", dev->debugName, request, argp);

    switch (request) {
    case FBIOGET_FSCREENINFO:       // struct fb_fix_screeninfo*
        memcpy(argp, &state->finfo, sizeof(struct fb_fix_screeninfo));
        break;
    case FBIOGET_VSCREENINFO:       // struct fb_var_screeninfo*
        memcpy(argp, &state->vinfo, sizeof(struct fb_var_screeninfo));
        break;
    case FBIOPUT_VSCREENINFO:       // struct fb_var_screeninfo*
        memcpy(&state->vinfo, argp, sizeof(struct fb_var_screeninfo));
        if (state->vinfo.activate == FB_ACTIVATE_NOW) {
            //wsLog("%s: activate now\n", dev->debugName);
            sendPixelsToSim(state);
        } else if (state->vinfo.activate == FB_ACTIVATE_VBL) {
            //wsLog("%s: activate on VBL\n", dev->debugName);
            sendPixelsToSim(state);
            /* we wait *after* so other process gets scheduled to draw */
            waitForVsync(state);
        } else {
            wsLog("%s: activate value is %d\n",
                dev->debugName, state->vinfo.activate);
        }
        break;
    case FBIOGET_VBLANK:            // struct fb_vblank*
        /* the device doesn't actually implement this */
        //memset(argp, 0, sizeof(struct fb_vblank));
        errno = EINVAL;
        return -1;
    default:
    /*case FBIO_WAITFORVSYNC:*/
        wsLog("GLITCH: UNKNOWN ioctl request 0x%x on %s\n",
            request, dev->debugName);
        return -1;
    }

    return 0;
}

/*
 * Free up our state before closing down the fake descriptor.
 */
static int closeFb(FakeDev* dev, int fd)
{
    freeState((FbState*)dev->state);
    dev->state = NULL;
    return 0;
}

/*
 * dup() an existing fake descriptor
 */
static FakeDev* dupFb(FakeDev* dev, int fd)
{
    FakeDev* newDev = wsCreateFakeDev(dev->debugName);
    if (newDev != NULL) {
        newDev->mmap = mmapFb;
        newDev->ioctl = ioctlFb;
        newDev->close = closeFb;
        newDev->dup = dupFb;

        /* use state from existing FakeDev */
        FbState* fbState = dev->state;
        wsAtomicAdd(&fbState->refCount, 1);

        newDev->state = fbState;
    }

    return newDev;
}

/*
 * Open the console TTY device, which responds to a collection of ioctl()s.
 */
FakeDev* wsOpenDevFb(const char* pathName, int flags)
{
    FakeDev* newDev = wsCreateFakeDev(pathName);
    if (newDev != NULL) {
        newDev->mmap = mmapFb;
        newDev->ioctl = ioctlFb;
        newDev->close = closeFb;
        newDev->dup = dupFb;

        FbState* fbState = calloc(1, sizeof(FbState));

        /* establish a connection to the front-end if necessary */
        /* (also gets display configuration) */
        wsSimConnect();

        configureInitialState(0, fbState);  // always use display 0 for now
        newDev->state = fbState;
    }

    return newDev;
}

