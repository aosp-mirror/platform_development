/*
* Copyright (C) 2011 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#include <string.h>
#include <pthread.h>
#ifdef HAVE_ANDROID_OS      // just want PAGE_SIZE define
# include <asm/page.h>
#else
# include <sys/user.h>
#endif
#include <cutils/ashmem.h>
#include <unistd.h>
#include <errno.h>
#include <dlfcn.h>
#include <sys/mman.h>
#include "gralloc_cb.h"
#include "HostConnection.h"
#include "glUtils.h"
#include <cutils/log.h>
#include <cutils/properties.h>

/* Set to 1 or 2 to enable debug traces */
#define DEBUG  0

#if DEBUG >= 1
#  define D(...)   LOGD(__VA_ARGS__)
#else
#  define D(...)   ((void)0)
#endif

#if DEBUG >= 2
#  define DD(...)  LOGD(__VA_ARGS__)
#else
#  define DD(...)  ((void)0)
#endif

#define DBG_FUNC DBG("%s\n", __FUNCTION__)
//
// our private gralloc module structure
//
struct private_module_t {
    gralloc_module_t base;
};

/* If not NULL, this is a pointer to the fallback module.
 * This really is gralloc.default, which we'll use if we detect
 * that the emulator we're running in does not support GPU emulation.
 */
static gralloc_module_t*  sFallback;
static pthread_once_t     sFallbackOnce = PTHREAD_ONCE_INIT;

static void fallback_init(void);  // forward


typedef struct _alloc_list_node {
    buffer_handle_t handle;
    _alloc_list_node *next;
    _alloc_list_node *prev;
} AllocListNode;

//
// Our gralloc device structure (alloc interface)
//
struct gralloc_device_t {
    alloc_device_t  device;

    AllocListNode *allocListHead;    // double linked list of allocated buffers
    pthread_mutex_t lock;
};

//
// Our framebuffer device structure
//
struct fb_device_t {
    framebuffer_device_t  device;
};

static int map_buffer(cb_handle_t *cb, void **vaddr)
{
    if (cb->fd < 0 || cb->ashmemSize <= 0) {
        return -EINVAL;
    }

    void *addr = mmap(0, cb->ashmemSize, PROT_READ | PROT_WRITE,
                      MAP_SHARED, cb->fd, 0);
    if (addr == MAP_FAILED) {
        return -errno;
    }

    cb->ashmemBase = intptr_t(addr);
    cb->ashmemBasePid = getpid();

    *vaddr = addr;
    return 0;
}

#define DEFINE_HOST_CONNECTION \
    HostConnection *hostCon = HostConnection::get(); \
    renderControl_encoder_context_t *rcEnc = (hostCon ? hostCon->rcEncoder() : NULL)

#define DEFINE_AND_VALIDATE_HOST_CONNECTION \
    HostConnection *hostCon = HostConnection::get(); \
    if (!hostCon) { \
        LOGE("gralloc: Failed to get host connection\n"); \
        return -EIO; \
    } \
    renderControl_encoder_context_t *rcEnc = hostCon->rcEncoder(); \
    if (!rcEnc) { \
        LOGE("gralloc: Failed to get renderControl encoder context\n"); \
        return -EIO; \
    }


//
// gralloc device functions (alloc interface)
//
static int gralloc_alloc(alloc_device_t* dev,
                         int w, int h, int format, int usage,
                         buffer_handle_t* pHandle, int* pStride)
{
    D("gralloc_alloc w=%d h=%d usage=0x%x\n", w, h, usage);

    gralloc_device_t *grdev = (gralloc_device_t *)dev;
    if (!grdev || !pHandle || !pStride)
        return -EINVAL;

    //
    // Validate usage: buffer cannot be written both by s/w and h/w access.
    //
    bool sw_write = (0 != (usage & GRALLOC_USAGE_SW_WRITE_MASK));
    bool hw_write = (usage & GRALLOC_USAGE_HW_RENDER);
    if (hw_write && sw_write) {
        return -EINVAL;
    }

    int ashmem_size = 0;
    *pStride = 0;
    GLenum glFormat = 0;
    GLenum glType = 0;

    int bpp = 0;
    switch (format) {
        case HAL_PIXEL_FORMAT_RGBA_8888:
        case HAL_PIXEL_FORMAT_RGBX_8888:
        case HAL_PIXEL_FORMAT_BGRA_8888:
            bpp = 4;
            glFormat = GL_RGBA;
            glType = GL_UNSIGNED_BYTE;
            break;
        case HAL_PIXEL_FORMAT_RGB_888:
            bpp = 3;
            glFormat = GL_RGB;
            glType = GL_UNSIGNED_BYTE;
            break;
        case HAL_PIXEL_FORMAT_RGB_565:
            bpp = 2;
            glFormat = GL_RGB;
            glType = GL_UNSIGNED_SHORT_5_6_5;
            break;
        case HAL_PIXEL_FORMAT_RGBA_5551:
            bpp = 2;
            glFormat = GL_RGB5_A1_OES;
            glType = GL_UNSIGNED_SHORT_5_5_5_1;
            break;
        case HAL_PIXEL_FORMAT_RGBA_4444:
            bpp = 2;
            glFormat = GL_RGBA4_OES;
            glType = GL_UNSIGNED_SHORT_4_4_4_4;
            break;

        default:
            return -EINVAL;
    }

    if (usage & GRALLOC_USAGE_HW_FB) {
        // keep space for postCounter
        ashmem_size += sizeof(uint32_t);
    }

    if (usage & (GRALLOC_USAGE_SW_READ_MASK | GRALLOC_USAGE_SW_WRITE_MASK)) {
        // keep space for image on guest memory if SW access is needed
        int align = 1;
        size_t bpr = (w*bpp + (align-1)) & ~(align-1);
        ashmem_size += (bpr * h);
        *pStride = bpr / bpp;
    }

    D("gralloc_alloc ashmem_size=%d, tid %d\n", ashmem_size, gettid());

    //
    // Allocate space in ashmem if needed
    //
    int fd = -1;
    if (ashmem_size > 0) {
        // round to page size;
        ashmem_size = (ashmem_size + (PAGE_SIZE-1)) & ~(PAGE_SIZE-1);

        fd = ashmem_create_region("gralloc-buffer", ashmem_size);
        if (fd < 0) {
            LOGE("gralloc_alloc failed to create ashmem region: %s\n", strerror(errno));
            return -errno;
        }
    }

    cb_handle_t *cb = new cb_handle_t(fd, ashmem_size, usage,
                                      w, h, glFormat, glType);

    if (ashmem_size > 0) {
        //
        // map ashmem region if exist
        //
        void *vaddr;
        int err = map_buffer(cb, &vaddr);
        if (err) {
            close(fd);
            delete cb;
            return err;
        }

        cb->setFd(fd);
    }

    //
    // Allocate ColorBuffer handle on the host (only if h/w access is allowed)
    //
    if (usage & GRALLOC_USAGE_HW_MASK) {
        DEFINE_HOST_CONNECTION;
        if (hostCon && rcEnc) {
            cb->hostHandle = rcEnc->rcCreateColorBuffer(rcEnc, w, h, glFormat);
            D("Created host ColorBuffer 0x%x\n", cb->hostHandle);
        }

        if (!cb->hostHandle) {
           // Could not create colorbuffer on host !!!
           close(fd);
           delete cb;
           return -EIO;
        }
    }

    //
    // alloc succeeded - insert the allocated handle to the allocated list
    //
    AllocListNode *node = new AllocListNode();
    pthread_mutex_lock(&grdev->lock);
    node->handle = cb;
    node->next =  grdev->allocListHead;
    node->prev =  NULL;
    if (grdev->allocListHead) {
        grdev->allocListHead->prev = node;
    }
    grdev->allocListHead = node;
    pthread_mutex_unlock(&grdev->lock);

    *pHandle = cb;
    return 0;
}

static int gralloc_free(alloc_device_t* dev,
                        buffer_handle_t handle)
{
    const cb_handle_t *cb = (const cb_handle_t *)handle;
    if (!cb_handle_t::validate((cb_handle_t*)cb)) {
        ERR("gralloc_free: invalid handle");
        return -EINVAL;
    }

    if (cb->hostHandle != 0) {
        DEFINE_AND_VALIDATE_HOST_CONNECTION;
        D("Closing host ColorBuffer 0x%x\n", cb->hostHandle);
        rcEnc->rcCloseColorBuffer(rcEnc, cb->hostHandle);
    }

    //
    // detach and unmap ashmem area if present
    //
    if (cb->fd > 0) {
        if (cb->ashmemSize > 0 && cb->ashmemBase) {
            munmap((void *)cb->ashmemBase, cb->ashmemSize);
        }
        close(cb->fd);
    }

    // remove it from the allocated list
    gralloc_device_t *grdev = (gralloc_device_t *)dev;
    pthread_mutex_lock(&grdev->lock);
    AllocListNode *n = grdev->allocListHead;
    while( n && n->handle != cb ) {
        n = n->next;
    }
    if (n) {
       // buffer found on list - remove it from list
       if (n->next) {
           n->next->prev = n->prev;
       }
       if (n->prev) {
           n->prev->next = n->next;
       }
       else {
           grdev->allocListHead = n->next;
       }

       delete n;
    }
    pthread_mutex_unlock(&grdev->lock);

    delete cb;

    return 0;
}

static int gralloc_device_close(struct hw_device_t *dev)
{
    gralloc_device_t* d = reinterpret_cast<gralloc_device_t*>(dev);
    if (d) {

        // free still allocated buffers
        while( d->allocListHead != NULL ) {
            gralloc_free(&d->device, d->allocListHead->handle);
        }

        // free device
        free(d);
    }
    return 0;
}

static int fb_compositionComplete(struct framebuffer_device_t* dev)
{
    return 0;
}

//
// Framebuffer device functions
//
static int fb_post(struct framebuffer_device_t* dev, buffer_handle_t buffer)
{
    fb_device_t *fbdev = (fb_device_t *)dev;
    cb_handle_t *cb = (cb_handle_t *)buffer;

    if (!fbdev || !cb_handle_t::validate(cb) || !cb->canBePosted()) {
        return -EINVAL;
    }

    // Make sure we have host connection
    DEFINE_AND_VALIDATE_HOST_CONNECTION;

    // increment the post count of the buffer
    uint32_t *postCountPtr = (uint32_t *)cb->ashmemBase;
    if (!postCountPtr) {
        // This should not happen
        return -EINVAL;
    }
    (*postCountPtr)++;

    // send post request to host
    rcEnc->rcFBPost(rcEnc, cb->hostHandle);
    hostCon->flush();

    return 0;
}

static int fb_setUpdateRect(struct framebuffer_device_t* dev,
        int l, int t, int w, int h)
{
    fb_device_t *fbdev = (fb_device_t *)dev;

    if (!fbdev) {
        return -EINVAL;
    }

    // Make sure we have host connection
    DEFINE_AND_VALIDATE_HOST_CONNECTION;

    // send request to host
    // TODO: XXX - should be implemented
    //rcEnc->rc_XXX

    return 0;
}

static int fb_setSwapInterval(struct framebuffer_device_t* dev,
            int interval)
{
    fb_device_t *fbdev = (fb_device_t *)dev;

    if (!fbdev) {
        return -EINVAL;
    }

    // Make sure we have host connection
    DEFINE_AND_VALIDATE_HOST_CONNECTION;

    // send request to host
    rcEnc->rcFBSetSwapInterval(rcEnc, interval);
    hostCon->flush();

    return 0;
}

static int fb_close(struct hw_device_t *dev)
{
    fb_device_t *fbdev = (fb_device_t *)dev;

    delete fbdev;

    return 0;
}


//
// gralloc module functions - refcount + locking interface
//
static int gralloc_register_buffer(gralloc_module_t const* module,
                                   buffer_handle_t handle)
{
    pthread_once(&sFallbackOnce, fallback_init);
    if (sFallback != NULL) {
        return sFallback->registerBuffer(sFallback, handle);
    }

    D("gralloc_register_buffer(%p) called", handle);

    private_module_t *gr = (private_module_t *)module;
    cb_handle_t *cb = (cb_handle_t *)handle;
    if (!gr || !cb_handle_t::validate(cb)) {
        ERR("gralloc_register_buffer(%p): invalid buffer", cb);
        return -EINVAL;
    }

    if (cb->hostHandle != 0) {
        DEFINE_AND_VALIDATE_HOST_CONNECTION;
        D("Opening host ColorBuffer 0x%x\n", cb->hostHandle);
        rcEnc->rcOpenColorBuffer(rcEnc, cb->hostHandle);
    }

    //
    // if the color buffer has ashmem region and it is not mapped in this
    // process map it now.
    //
    if (cb->ashmemSize > 0 && cb->mappedPid != getpid()) {
        void *vaddr;
        int err = map_buffer(cb, &vaddr);
        if (err) {
            ERR("gralloc_register_buffer(%p): map failed: %s", cb, strerror(-err));
            return -err;
        }
        cb->mappedPid = getpid();
    }

    return 0;
}

static int gralloc_unregister_buffer(gralloc_module_t const* module,
                                     buffer_handle_t handle)
{
    if (sFallback != NULL) {
        return sFallback->unregisterBuffer(sFallback, handle);
    }

    private_module_t *gr = (private_module_t *)module;
    cb_handle_t *cb = (cb_handle_t *)handle;
    if (!gr || !cb_handle_t::validate(cb)) {
        ERR("gralloc_unregister_buffer(%p): invalid buffer", cb);
        return -EINVAL;
    }

    if (cb->hostHandle != 0) {
        DEFINE_AND_VALIDATE_HOST_CONNECTION;
        D("Closing host ColorBuffer 0x%x\n", cb->hostHandle);
        rcEnc->rcCloseColorBuffer(rcEnc, cb->hostHandle);
    }

    //
    // unmap ashmem region if it was previously mapped in this process
    // (through register_buffer)
    //
    if (cb->ashmemSize > 0 && cb->mappedPid == getpid()) {
        void *vaddr;
        int err = munmap((void *)cb->ashmemBase, cb->ashmemSize);
        if (err) {
            ERR("gralloc_unregister_buffer(%p): unmap failed", cb);
            return -EINVAL;
        }
        cb->ashmemBase = NULL;
        cb->mappedPid = 0;
    }

    D("gralloc_unregister_buffer(%p) done\n", cb);

    return 0;
}

static int gralloc_lock(gralloc_module_t const* module,
                        buffer_handle_t handle, int usage,
                        int l, int t, int w, int h,
                        void** vaddr)
{
    if (sFallback != NULL) {
        return sFallback->lock(sFallback, handle, usage, l, t, w, h, vaddr);
    }

    private_module_t *gr = (private_module_t *)module;
    cb_handle_t *cb = (cb_handle_t *)handle;
    if (!gr || !cb_handle_t::validate(cb)) {
        LOGE("gralloc_lock bad handle\n");
        return -EINVAL;
    }

    // Validate usage,
    //   1. cannot be locked for hw access
    //   2. lock for either sw read or write.
    //   3. locked sw access must match usage during alloc time.
    bool sw_read = (0 != (usage & GRALLOC_USAGE_SW_READ_MASK));
    bool sw_write = (0 != (usage & GRALLOC_USAGE_SW_WRITE_MASK));
    bool hw_read = (usage & GRALLOC_USAGE_HW_TEXTURE);
    bool hw_write = (usage & GRALLOC_USAGE_HW_RENDER);
    bool sw_read_allowed = (0 != (cb->usage & GRALLOC_USAGE_SW_READ_MASK));
    bool sw_write_allowed = (0 != (cb->usage & GRALLOC_USAGE_SW_WRITE_MASK));

    if ( (hw_read || hw_write) ||
         (!sw_read && !sw_write) ||
         (sw_read && !sw_read_allowed) ||
         (sw_write && !sw_write_allowed) ) {
        LOGE("gralloc_lock usage mismatch usage=0x%x cb->usage=0x%x\n", usage, cb->usage);
        return -EINVAL;
    }

    EGLint postCount = 0;
    void *cpu_addr = NULL;

    //
    // make sure ashmem area is mapped if needed
    //
    if (cb->canBePosted() || sw_read || sw_write) {
        if (cb->ashmemBasePid != getpid() || !cb->ashmemBase) {
            return -EACCES;
        }

        if (cb->canBePosted()) {
            postCount = *((int *)cb->ashmemBase);
            cpu_addr = (void *)(cb->ashmemBase + sizeof(int));
        }
        else {
            cpu_addr = (void *)(cb->ashmemBase);
        }
    }

    if (cb->hostHandle) {
        // Make sure we have host connection
        DEFINE_AND_VALIDATE_HOST_CONNECTION;

        //
        // flush color buffer write cache on host and get its sync status.
        //
        int hostSyncStatus = rcEnc->rcColorBufferCacheFlush(rcEnc, cb->hostHandle,
                                                            postCount,
                                                            sw_read);
        if (hostSyncStatus < 0) {
            // host failed the color buffer sync - probably since it was already
            // locked for write access. fail the lock.
            LOGE("gralloc_lock cacheFlush failed postCount=%d sw_read=%d\n",
                 postCount, sw_read);
            return -EBUSY;
        }

        //
        // is virtual address required ?
        //
        if (sw_read || sw_write) {
            *vaddr = cpu_addr;
        }
    }

    if (sw_write) {
        //
        // Keep locked region if locked for s/w write access.
        //
        cb->lockedLeft = l;
        cb->lockedTop = t;
        cb->lockedWidth = w;
        cb->lockedHeight = h;
    }

    return 0;
}

static int gralloc_unlock(gralloc_module_t const* module,
                          buffer_handle_t handle)
{
    if (sFallback != NULL) {
        return sFallback->unlock(sFallback, handle);
    }

    private_module_t *gr = (private_module_t *)module;
    cb_handle_t *cb = (cb_handle_t *)handle;
    if (!gr || !cb_handle_t::validate(cb)) {
        return -EINVAL;
    }

    //
    // if buffer was locked for s/w write, we need to update the host with
    // the updated data
    //
    if (cb->lockedWidth > 0 && cb->lockedHeight > 0 && cb->hostHandle) {

        // Make sure we have host connection
        DEFINE_AND_VALIDATE_HOST_CONNECTION;

        void *cpu_addr;
        if (cb->canBePosted()) {
            cpu_addr = (void *)(cb->ashmemBase + sizeof(int));
        }
        else {
            cpu_addr = (void *)(cb->ashmemBase);
        }

        if (cb->lockedWidth < cb->width || cb->lockedHeight < cb->height) {
            int bpp = glUtilsPixelBitSize(cb->glFormat, cb->glType) >> 3;
            char *tmpBuf = new char[cb->lockedWidth * cb->lockedHeight * bpp];

            int dst_line_len = cb->lockedWidth * bpp;
            int src_line_len = cb->width * bpp;
            char *src = (char *)cpu_addr + cb->lockedTop*src_line_len + cb->lockedLeft*bpp;
            char *dst = tmpBuf;
            for (int y=0; y<cb->lockedHeight; y++) {
                memcpy(dst, src, dst_line_len);
                src += src_line_len;
                dst += dst_line_len;
            }

            rcEnc->rcUpdateColorBuffer(rcEnc, cb->hostHandle,
                                       cb->lockedLeft, cb->lockedTop,
                                       cb->lockedWidth, cb->lockedHeight,
                                       cb->glFormat, cb->glType,
                                       tmpBuf);

            delete [] tmpBuf;
        }
        else {
            rcEnc->rcUpdateColorBuffer(rcEnc, cb->hostHandle, 0, 0,
                                       cb->width, cb->height,
                                       cb->glFormat, cb->glType,
                                       cpu_addr);
        }
    }

    cb->lockedWidth = cb->lockedHeight = 0;
    return 0;
}


static int gralloc_device_open(const hw_module_t* module,
                               const char* name,
                               hw_device_t** device)
{
    int status = -EINVAL;

    D("gralloc_device_open %s\n", name);

    pthread_once( &sFallbackOnce, fallback_init );
    if (sFallback != NULL) {
        return sFallback->common.methods->open(&sFallback->common, name, device);
    }

    if (!strcmp(name, GRALLOC_HARDWARE_GPU0)) {

        // Create host connection and keep it in the TLS.
        // return error if connection with host can not be established
        HostConnection *hostCon = HostConnection::get();
        if (!hostCon) {
            LOGE("gralloc: failed to get host connection while opening %s\n", name);
            return -EIO;
        }

        //
        // Allocate memory for the gralloc device (alloc interface)
        //
        gralloc_device_t *dev;
        dev = (gralloc_device_t*)malloc(sizeof(gralloc_device_t));
        if (NULL == dev) {
            return -ENOMEM;
        }

        // Initialize our device structure
        //
        dev->device.common.tag = HARDWARE_DEVICE_TAG;
        dev->device.common.version = 0;
        dev->device.common.module = const_cast<hw_module_t*>(module);
        dev->device.common.close = gralloc_device_close;

        dev->device.alloc   = gralloc_alloc;
        dev->device.free    = gralloc_free;
        dev->allocListHead  = NULL;
        pthread_mutex_init(&dev->lock, NULL);

        *device = &dev->device.common;
        status = 0;
    }
    else if (!strcmp(name, GRALLOC_HARDWARE_FB0)) {

        // return error if connection with host can not be established
        DEFINE_AND_VALIDATE_HOST_CONNECTION;

        //
        // Query the host for Framebuffer attributes
        //
        D("gralloc: query Frabuffer attribs\n");
        EGLint width = rcEnc->rcGetFBParam(rcEnc, FB_WIDTH);
        D("gralloc: width=%d\n", width);
        EGLint height = rcEnc->rcGetFBParam(rcEnc, FB_HEIGHT);
        D("gralloc: height=%d\n", height);
        EGLint xdpi = rcEnc->rcGetFBParam(rcEnc, FB_XDPI);
        D("gralloc: xdpi=%d\n", xdpi);
        EGLint ydpi = rcEnc->rcGetFBParam(rcEnc, FB_YDPI);
        D("gralloc: ydpi=%d\n", ydpi);
        EGLint fps = rcEnc->rcGetFBParam(rcEnc, FB_FPS);
        D("gralloc: fps=%d\n", fps);
        EGLint min_si = rcEnc->rcGetFBParam(rcEnc, FB_MIN_SWAP_INTERVAL);
        D("gralloc: min_swap=%d\n", min_si);
        EGLint max_si = rcEnc->rcGetFBParam(rcEnc, FB_MAX_SWAP_INTERVAL);
        D("gralloc: max_swap=%d\n", max_si);

        //
        // Allocate memory for the framebuffer device
        //
        fb_device_t *dev;
        dev = (fb_device_t*)malloc(sizeof(fb_device_t));
        if (NULL == dev) {
            return -ENOMEM;
        }
        memset(dev, 0, sizeof(fb_device_t));

        // Initialize our device structure
        //
        dev->device.common.tag = HARDWARE_DEVICE_TAG;
        dev->device.common.version = 0;
        dev->device.common.module = const_cast<hw_module_t*>(module);
        dev->device.common.close = fb_close;
        dev->device.setSwapInterval = fb_setSwapInterval;
        dev->device.post            = fb_post;
        dev->device.setUpdateRect   = 0; //fb_setUpdateRect;
        dev->device.compositionComplete = fb_compositionComplete; //XXX: this is a dummy

        const_cast<uint32_t&>(dev->device.flags) = 0;
        const_cast<uint32_t&>(dev->device.width) = width;
        const_cast<uint32_t&>(dev->device.height) = height;
        const_cast<int&>(dev->device.stride) = width;
        const_cast<int&>(dev->device.format) = HAL_PIXEL_FORMAT_RGBA_8888;
        const_cast<float&>(dev->device.xdpi) = xdpi;
        const_cast<float&>(dev->device.ydpi) = ydpi;
        const_cast<float&>(dev->device.fps) = fps;
        const_cast<int&>(dev->device.minSwapInterval) = min_si;
        const_cast<int&>(dev->device.maxSwapInterval) = max_si;
        *device = &dev->device.common;

        status = 0;
    }

    return status;
}

//
// define the HMI symbol - our module interface
//
static struct hw_module_methods_t gralloc_module_methods = {
        open: gralloc_device_open
};

struct private_module_t HAL_MODULE_INFO_SYM = {
    base: {
        common: {
            tag: HARDWARE_MODULE_TAG,
            version_major: 1,
            version_minor: 0,
            id: GRALLOC_HARDWARE_MODULE_ID,
            name: "Graphics Memory Allocator Module",
            author: "The Android Open Source Project",
            methods: &gralloc_module_methods,
            dso: NULL,
            reserved: {0, }
        },
        registerBuffer: gralloc_register_buffer,
        unregisterBuffer: gralloc_unregister_buffer,
        lock: gralloc_lock,
        unlock: gralloc_unlock,
        perform: NULL,
        reserved_proc : {NULL, }
    }
};

/* This function is called once to detect whether the emulator supports
 * GPU emulation (this is done by looking at the qemu.gles kernel
 * parameter, which must be > 0 if this is the case).
 *
 * If not, then load gralloc.default instead as a fallback.
 */
static void
fallback_init(void)
{
    char  prop[PROPERTY_VALUE_MAX];
    void* module;

    property_get("ro.kernel.qemu.gles", prop, "0");
    if (atoi(prop) > 0) {
        return;
    }
    LOGD("Emulator without GPU emulation detected.");
    module = dlopen("/system/lib/hw/gralloc.default.so", RTLD_LAZY|RTLD_LOCAL);
    if (module != NULL) {
        sFallback = reinterpret_cast<gralloc_module_t*>(dlsym(module, HAL_MODULE_INFO_SYM_AS_STR));
        if (sFallback == NULL) {
            dlclose(module);
        }
    }
    if (sFallback == NULL) {
        LOGE("Could not find software fallback module!?");
    }
}
