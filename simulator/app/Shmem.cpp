//
// Copyright 2005 The Android Open Source Project
//
// Shared memory interface.
//
#include "Shmem.h"
#include "utils/Log.h"

#if defined(HAVE_MACOSX_IPC) || defined(HAVE_ANDROID_IPC)
#  include <sys/mman.h>
#  include <fcntl.h>
#  include <unistd.h>
#elif defined(HAVE_SYSV_IPC)
# include <sys/types.h>
# include <sys/ipc.h>
# include <sys/shm.h>
#elif defined(HAVE_WIN32_IPC)
# include <windows.h>
#else
# error "unknown shm config"
#endif

#include <errno.h>
#include <assert.h>

using namespace android;


#if defined(HAVE_MACOSX_IPC) || defined(HAVE_ANDROID_IPC)

/*
 * SysV IPC under Mac OS X seems to have problems.  It works fine on
 * some machines but totally fails on others.  We're working around it
 * here by using mmap().
 */

#define kInvalidHandle  ((unsigned long)-1)

static const char* kShmemFile = "/tmp/android-";

/*
 * Constructor.  Just set up the fields.
 */
Shmem::Shmem(void)
    : mHandle(kInvalidHandle), mAddr(MAP_FAILED), mLength(-1), mCreator(false),
      mKey(-1)
{
}

/*
 * Destructor.  Detach and, if we created it, mark the segment for
 * destruction.
 */
Shmem::~Shmem(void)
{
    if (mAddr != MAP_FAILED)
        munmap(mAddr, mLength);
    if ((long)mHandle >= 0) {
        close(mHandle);

        if (mCreator) {
            char nameBuf[64];
            int cc;

            snprintf(nameBuf, sizeof(nameBuf), "%s%d", kShmemFile, mKey);
            cc = unlink(nameBuf);
            if (cc != 0) {
                LOG(LOG_WARN, "shmem", "Couldn't clean up '%s'\n", nameBuf);
                /* oh well */
            }
        }
    }
}

/*
 * Create the segment and attach ourselves to it.
 */
bool Shmem::create(int key, long size, bool deleteExisting)
{
    char nameBuf[64];
    int fd, cc;

    snprintf(nameBuf, sizeof(nameBuf), "%s%d", kShmemFile, key);

    if (deleteExisting) {
        cc = unlink(nameBuf);
        if (cc != 0 && errno != ENOENT) {
            LOG(LOG_ERROR, "shmem", "Failed to remove old map file '%s'\n",
                nameBuf);
            return false;
        }
    }

    fd = open(nameBuf, O_CREAT|O_EXCL|O_RDWR, 0600);
    if (fd < 0) {
        LOG(LOG_ERROR, "shmem", "Unable to create map file '%s' (errno=%d)\n",
            nameBuf, errno);
        return false;
    }

    /*
     * Set the file size by seeking and writing.
     */
    if (ftruncate(fd, size) == -1) {
        LOG(LOG_ERROR, "shmem", "Unable to set file size in '%s' (errno=%d)\n",
            nameBuf, errno);
        close(fd);
        return false;
    }

    mAddr = mmap(NULL, size, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0);
    if (mAddr == MAP_FAILED) {
        LOG(LOG_ERROR, "shmem", "mmap failed (errno=%d)\n", errno);
        close(fd);
        return false;
    }

    mHandle = fd;
    mLength = size;
    mCreator = true;
    mKey = key;

    /* done with shmem, create the associated semaphore */
    if (!mSem.create(key, 1, true)) {
        LOG(LOG_ERROR, "shmem",
            "Failed creating semaphore for Shmem (key=%d)\n", key);
        return false;
    }

    return true;
}

/*
 * Attach ourselves to an existing segment.
 */
bool Shmem::attach(int key)
{
    char nameBuf[64];
    int fd;

    snprintf(nameBuf, sizeof(nameBuf), "%s%d", kShmemFile, key);
    fd = open(nameBuf, O_RDWR, 0600);
    if (fd < 0) {
        LOG(LOG_ERROR, "shmem", "Unable to open map file '%s' (errno=%d)\n",
            nameBuf, errno);
        return false;
    }

    off_t len;
    len = lseek(fd, 0, SEEK_END);
    if (len == (off_t) -1) {
        LOG(LOG_ERROR, "shmem",
            "Could not determine file size of '%s' (errno=%d)\n",
            nameBuf, errno);
        close(fd);
        return false;
    }

    mAddr = mmap(NULL, len, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0);
    if (mAddr == MAP_FAILED) {
        LOG(LOG_ERROR, "shmem", "mmap failed (errno=%d)\n", errno);
        close(fd);
        return false;
    }

    mHandle = fd;
    mLength = len;
    assert(mCreator == false);
    mKey = key;

    /* done with shmem, attach to associated semaphore */
    if (!mSem.attach(key)) {
        LOG(LOG_ERROR, "shmem",
            "Failed to attach to semaphore for Shmem (key=%d)\n", key);
        return false;
    }

    return true;
}

/*
 * Get address.
 */
void* Shmem::getAddr(void)
{
    assert(mAddr != MAP_FAILED);
    return mAddr;
}

/*
 * Return the length of the segment.
 *
 * Returns -1 on failure.
 */
long Shmem::getLength(void)
{
    if (mLength >= 0)
        return mLength;

    // we should always have it by now
    assert(false);
    return -1;
}


#elif defined(HAVE_SYSV_IPC) // ----------------------------------------------

/*
 * SysV-style IPC.  The SysV shared memory API is fairly annoying to
 * deal with, but it's present on many UNIX-like systems.
 */

#define kInvalidHandle  ((unsigned long)-1)

/*
 * Constructor.  Just set up the fields.
 */
Shmem::Shmem(void)
    : mHandle(kInvalidHandle), mAddr(NULL), mLength(-1), mCreator(false),
      mKey(-1)
{
}

/*
 * Destructor.  Detach and, if we created it, mark the segment for
 * destruction.
 */
Shmem::~Shmem(void)
{
    int cc;

    //LOG(LOG_DEBUG, "shmem", "~Shmem(handle=%ld creator=%d)",
    //    mHandle, mCreator);

    if (mAddr != NULL)
        cc = shmdt(mAddr);

    if (mCreator && mHandle != kInvalidHandle) {
        cc = shmctl((int) mHandle, IPC_RMID, NULL);
        if (cc != 0) {
            LOG(LOG_WARN, "shmem",
                "Destructor failed to remove shmid=%ld (errno=%d)\n",
                mHandle, errno);
        }
    }
}

/*
 * Create the segment and attach ourselves to it.
 */
bool Shmem::create(int key, long size, bool deleteExisting)
{
    int shmid, cc;

    if (deleteExisting) {
        shmid = shmget(key, size, 0);
        if (shmid != -1) {
            LOG(LOG_DEBUG, "shmem",
                "Key %d exists (shmid=%d), marking for destroy", key, shmid);
            cc = shmctl(shmid, IPC_RMID, NULL);
            if (cc != 0) {
                LOG(LOG_ERROR, "shmem",
                    "Failed to remove key=%d shmid=%d (errno=%d)\n",
                    key, shmid, errno);
                return false;   // IPC_CREAT | IPC_EXCL will fail, so bail now
            } else {
                LOG(LOG_DEBUG, "shmem",
                    "Removed previous segment with key=%d\n", key);
            }
        }
    }

    shmid = shmget(key, size, 0600 | IPC_CREAT | IPC_EXCL);
    if (shmid == -1) {
        LOG(LOG_ERROR, "shmem", "Failed to create key=%d (errno=%d)\n",
            key, errno);
        return false;
    }

    mHandle = shmid;
    mCreator = true;
    mKey = key;

    void* addr = shmat(shmid, NULL, 0);
    if (addr == (void*) -1) {
        LOG(LOG_ERROR, "shmem",
            "Could not attach to key=%d shmid=%d (errno=%d)\n",
            key, shmid, errno);
        return false;
    }

    mAddr = addr;
    mLength = size;

    /* done with shmem, create the associated semaphore */
    if (!mSem.create(key, 1, true)) {
        LOG(LOG_ERROR, "shmem",
            "Failed creating semaphore for Shmem (key=%d)\n", key);
        return false;
    }

    return true;
}

/*
 * Attach ourselves to an existing segment.
 */
bool Shmem::attach(int key)
{
    int shmid;

    shmid = shmget(key, 0, 0);
    if (shmid == -1) {
        LOG(LOG_ERROR, "shmem", "Failed to find key=%d\n", key);
        return false;
    }

    mHandle = shmid;
    assert(mCreator == false);
    mKey = key;

    void* addr = shmat(shmid, NULL, 0);
    if (addr == (void*) -1) {
        LOG(LOG_ERROR, "shmem", "Could not attach to key=%d shmid=%d\n",
            key, shmid);
        return false;
    }

    mAddr = addr;

    /* done with shmem, attach to associated semaphore */
    if (!mSem.attach(key)) {
        LOG(LOG_ERROR, "shmem",
            "Failed to attach to semaphore for Shmem (key=%d)\n", key);
        return false;
    }

    return true;
}

/*
 * Get address.
 */
void* Shmem::getAddr(void)
{
    assert(mAddr != NULL);
    return mAddr;
}

/*
 * Return the length of the segment.
 *
 * Returns -1 on failure.
 */
long Shmem::getLength(void)
{
    if (mLength >= 0)
        return mLength;

    assert(mHandle != kInvalidHandle);

    struct shmid_ds shmids;
    int cc;

    cc = shmctl((int) mHandle, IPC_STAT, &shmids);
    if (cc != 0) {
        LOG(LOG_ERROR, "shmem", "Could not IPC_STAT shmid=%ld\n", mHandle);
        return -1;
    }
    mLength = shmids.shm_segsz;     // save a copy to avoid future lookups

    return mLength;
}


#elif defined(HAVE_WIN32_IPC) // ---------------------------------------------

/*
 * Win32 shared memory implementation.
 *
 * Shared memory is implemented as an "anonymous" file mapping, using the
 * memory-mapped I/O interfaces.
 */

static const char* kShmemStr = "android-shmem-";

/*
 * Constructor.  Just set up the fields.
 */
Shmem::Shmem(void)
    : mHandle((unsigned long) INVALID_HANDLE_VALUE),
      mAddr(NULL), mLength(-1), mCreator(false), mKey(-1)
{
}

/*
 * Destructor.  The Win32 API doesn't require a distinction between
 * the "creator" and other mappers.
 */
Shmem::~Shmem(void)
{
    LOG(LOG_DEBUG, "shmem", "~Shmem(handle=%ld creator=%d)",
        mHandle, mCreator);

    if (mAddr != NULL)
        UnmapViewOfFile(mAddr);
    if (mHandle != (unsigned long) INVALID_HANDLE_VALUE)
        CloseHandle((HANDLE) mHandle);
}

/*
 * Create the segment and map it.
 */
bool Shmem::create(int key, long size, bool deleteExisting)
{
    char keyBuf[64];
    HANDLE hMapFile;

    snprintf(keyBuf, sizeof(keyBuf), "%s%d", kShmemStr, key);

    hMapFile = CreateFileMapping(
                INVALID_HANDLE_VALUE,       // use paging file, not actual file
                NULL,                       // default security
                PAGE_READWRITE,             // read/write access
                0,                          // max size; no need to cap
                size,                       // min size
                keyBuf);                    // mapping name
    if (hMapFile == NULL || hMapFile == INVALID_HANDLE_VALUE) {
        LOG(LOG_ERROR, "shmem",
            "Could not create mapping object '%s' (err=%ld)\n",
            keyBuf, GetLastError());
        return false;
    }

    mHandle = (unsigned long) hMapFile;
    mCreator = true;
    mKey = key;

    mAddr = MapViewOfFile(
                hMapFile,                   // handle to map object
                FILE_MAP_ALL_ACCESS,        // read/write
                0,                          // offset (hi)
                0,                          // offset (lo)
                size);                      // #of bytes to map
    if (mAddr == NULL) {
        LOG(LOG_ERROR, "shmem", "Could not map shared area (err=%ld)\n",
            GetLastError());
        return false;
    }
    mLength = size;

    /* done with shmem, create the associated semaphore */
    if (!mSem.create(key, 1, true)) {
        LOG(LOG_ERROR, "shmem",
            "Failed creating semaphore for Shmem (key=%d)\n", key);
        return false;
    }

    return true;
}

/*
 * Attach ourselves to an existing segment.
 */
bool Shmem::attach(int key)
{
    char keyBuf[64];
    HANDLE hMapFile;

    snprintf(keyBuf, sizeof(keyBuf), "%s%d", kShmemStr, key);

    hMapFile = OpenFileMapping(
                FILE_MAP_ALL_ACCESS,        // read/write
                FALSE,                      // don't let kids inherit handle
                keyBuf);                    // mapping name
    if (hMapFile == NULL) {
        LOG(LOG_ERROR, "shmem",
            "Could not open mapping object '%s' (err=%ld)\n",
            keyBuf, GetLastError());
        return false;
    }

    mHandle = (unsigned long) hMapFile;
    assert(mCreator == false);
    mKey = key;

    mAddr = MapViewOfFile(
                hMapFile,                   // handle to map object
                FILE_MAP_ALL_ACCESS,        // read/write
                0,                          // offset (hi)
                0,                          // offset (lo)
                0);                        // #of bytes to map
    if (mAddr == NULL) {
        LOG(LOG_ERROR, "shmem", "Could not map shared area (err=%ld)\n",
            GetLastError());
        return false;
    }

    /* done with shmem, attach to associated semaphore */
    if (!mSem.attach(key)) {
        LOG(LOG_ERROR, "shmem",
            "Failed to attach to semaphore for Shmem (key=%d)\n", key);
        return false;
    }

    return true;
}

/*
 * Get address.
 */
void* Shmem::getAddr(void)
{
    assert(mAddr != NULL);
    return mAddr;
}

/*
 * Get the length of the segment.
 */
long Shmem::getLength(void)
{
    SIZE_T size;
    MEMORY_BASIC_INFORMATION mbInfo;

    if (mLength >= 0)
        return mLength;

    assert(mAddr != NULL);

    size = VirtualQuery(mAddr, &mbInfo, sizeof(mbInfo));
    if (size == 0) {
        LOG(LOG_WARN, "shmem", "VirtualQuery returned no data\n");
        return -1;
    }

    mLength = mbInfo.RegionSize;
    return mLength;
}

#endif // --------------------------------------------------------------------

/*
 * Semaphore operations.
 */
void Shmem::lock(void)
{
    mSem.acquire();
}
void Shmem::unlock(void)
{
    mSem.release();
}
bool Shmem::tryLock(void)
{
    return mSem.tryAcquire();
}

