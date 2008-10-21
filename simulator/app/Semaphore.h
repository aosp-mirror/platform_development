//
// Copyright 2005 The Android Open Source Project
//
// Inter-process semaphore.
//
// These are meant for IPC, not thread management.  The Mutex and Condition
// classes are much lighter weight.
//
#ifndef __LIBS_SEMAPHORE_H
#define __LIBS_SEMAPHORE_H

#ifdef HAVE_ANDROID_OS
#error DO NOT USE THIS FILE IN THE DEVICE BUILD
#endif

namespace android {

/*
 * Platform-independent semaphore class.
 *
 * Each object holds a single semaphore.
 *
 * The "key" is usually the process ID of the process that created the
 * semaphore (following POSIX semantics).  See the comments in shmem.h.
 */
class Semaphore {
public:
    Semaphore(void);
    virtual ~Semaphore(void);

    /*
     * Create a new semaphore, with the specified initial value.  The
     * value indicates the number of resources available.
     */
    bool create(int key, int initialValue, bool deleteExisting);

    /*
     * Attach to an existing semaphore.
     */
    bool attach(int key);

    /*
     * Acquire or release the semaphore.
     */
    void acquire(void);
    void release(void);
    bool tryAcquire(void);      // take a timeout?

private:
    bool adjust(int adj, bool wait);

    unsigned long   mHandle;    // semid(int) or HANDLE
    bool            mCreator;
    int             mKey;
};

}; // namespace android

#endif // __LIBS_SEMAPHORE_H
