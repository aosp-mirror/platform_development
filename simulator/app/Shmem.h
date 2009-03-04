//
// Copyright 2005 The Android Open Source Project
//
// Inter-process shared memory.
//
#ifndef __LIBS_SHMEM_H
#define __LIBS_SHMEM_H

#ifdef HAVE_ANDROID_OS
#error DO NOT USE THIS FILE IN THE DEVICE BUILD
#endif

#include "Semaphore.h"

namespace android {

/*
 * Platform-independent shared memory.  Each object can be used to
 * create a chunk of memory that is shared between processes.
 *
 * For convenience, a semaphore is associated with each segment.
 * (Whether this should have been done in a subclass is debatable.)
 *
 * The "key" is usually the process ID of the process that created the
 * segment.  The goal is to avoid clashing with other processes that are
 * trying to do the same thing.  It's a little awkward to use when you
 * want to have multiple shared segments created in one process.  In
 * SysV you can work around this by using a "private" key and sharing
 * the shmid with your friends, in Win32 you can use a string, but we're
 * in lowest-common-denominator mode here.  Assuming we have 16-bit PIDs,
 * the upper 16 bits can be used to serialize keys.
 *
 * When the object goes out of scope, the shared memory segment is
 * detached from the process.  If the object was responsible for creating
 * the segment, it is also marked for destruction on SysV systems.  This
 * will make it impossible for others to attach to.
 *
 * On some systems, the length returned by getLength() may be different
 * for parent and child due to page size rounding.
 */
class Shmem {
public:
    Shmem(void);
    virtual ~Shmem(void);

    /*
     * Create a new shared memory segment, with the specified size.  If
     * "deleteExisting" is set, any existing segment will be deleted first
     * (useful for SysV IPC).
     *
     * Returns "true" on success, "false" on failure.
     */
    bool create(int key, long size, bool deleteExisting);

    /*
     * Attach to a shared memory segment.  Use this from the process that
     * didn't create the segment.
     *
     * Returns "true" on success, "false" on failure.
     */
    bool attach(int key);

    /*
     * Get the memory segment address and length.  These will not change
     * for the lifetime of the object, so it's okay to cache the results.
     *
     * On failure, getAddr() returns NULL and getLength() returns -1.
     */
    void* getAddr(void);
    long getLength(void);

    /*
     * Lock or unlock the shared memory segment.  This is useful if you
     * are updating pieces of shared data.  The segment is initially
     * "unlocked".
     *
     * This does *not* lock down the segment in the virtual paging system.
     * It's just a mutex.
     */
    void lock(void);
    void unlock(void);
    bool tryLock(void);

private:
    Semaphore       mSem;       // uses the same value for "key"
    unsigned long   mHandle;    // shmid(int) or HANDLE
    void*           mAddr;      // address
    long            mLength;    // length of segment (cached)
    bool            mCreator;   // true if we created the segment
    int             mKey;       // key passed in as arg
};

}; // namespace android

#endif // __LIBS_SHMEM_H
