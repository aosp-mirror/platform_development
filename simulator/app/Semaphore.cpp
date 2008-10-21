//
// Copyright 2005 The Android Open Source Project
//
// Inter-process semaphores.
//
#include "Semaphore.h"

#if defined(HAVE_MACOSX_IPC)
# include <semaphore.h>
#elif defined(HAVE_SYSV_IPC)
# include <sys/types.h>
# include <sys/ipc.h>
# include <sys/sem.h>
#elif defined(HAVE_WIN32_IPC)
# include <windows.h>
#elif defined(HAVE_ANDROID_IPC)
// not yet
#else
# error "unknown sem config"
#endif

#include <utils/Log.h>

#include <errno.h>
#include <assert.h>

using namespace android;


#if defined(HAVE_ANDROID_IPC) // ----------------------------------------------

Semaphore::Semaphore(void)
    : mHandle(0), mCreator(false), mKey(-1)
{}

Semaphore::~Semaphore(void)
{}

bool Semaphore::create(int key, int initialValue, bool deleteExisting)
{
    return false;
}

bool Semaphore::attach(int key)
{
    return false;
}

void Semaphore::acquire(void)
{}

void Semaphore::release(void)
{}

bool Semaphore::tryAcquire(void)
{
    return false;
}

#elif defined(HAVE_MACOSX_IPC) // ---------------------------------------------

/*
 * The SysV semaphores don't work on all of our machines.  The POSIX
 * named semaphores seem to work better.
 */

#define kInvalidHandle SEM_FAILED

static const char* kSemStr = "/tmp/android-sem-";

/*
 * Constructor.  Just init fields.
 */
Semaphore::Semaphore(void)
    : mHandle((unsigned long) kInvalidHandle), mCreator(false), mKey(-1)
{
}

/*
 * Destructor.  If we created the semaphore, destroy it.
 */
Semaphore::~Semaphore(void)
{
    LOG(LOG_VERBOSE, "sem", "~Semaphore(handle=%ld creator=%d)\n",
        mHandle, mCreator);

    if (mHandle != (unsigned long) kInvalidHandle) {
        sem_close((sem_t*) mHandle);

        if (mCreator) {
            char nameBuf[64];
            int cc;

            snprintf(nameBuf, sizeof(nameBuf), "%s%d", kSemStr, mKey);

            cc = sem_unlink(nameBuf);
            if (cc != 0) {
                LOG(LOG_ERROR, "sem",
                    "Failed to remove sem '%s' (errno=%d)\n", nameBuf, errno);
            }
        }
    }
}

/*
 * Create the semaphore.
 */
bool Semaphore::create(int key, int initialValue, bool deleteExisting)
{
    int cc;
    char nameBuf[64];

    snprintf(nameBuf, sizeof(nameBuf), "%s%d", kSemStr, key);

    if (deleteExisting) {
        cc = sem_unlink(nameBuf);
        if (cc != 0 && errno != ENOENT) {
            LOG(LOG_WARN, "sem", "Warning: failed to remove sem '%s'\n",
                nameBuf);
            /* keep going? */
        }
    }

    /* create and set initial value */
    sem_t* semPtr;
    semPtr = sem_open(nameBuf, O_CREAT | O_EXCL, 0666, 1);
    if (semPtr == (sem_t*)SEM_FAILED) {
        LOG(LOG_ERROR, "sem",
            "ERROR: sem_open failed to create '%s' (errno=%d)\n",
            nameBuf, errno);
        return false;
    }

    mHandle = (unsigned long) semPtr;
    mCreator = true;
    mKey = key;

    return true;
}

/*
 * Attach to an existing semaphore.
 */
bool Semaphore::attach(int key)
{
    char nameBuf[64];

    snprintf(nameBuf, sizeof(nameBuf), "%s%d", kSemStr, key);

    sem_t* semPtr;
    semPtr = sem_open(nameBuf, 0, 0666, 0);
    if (semPtr == (sem_t*) SEM_FAILED) {
        LOG(LOG_ERROR, "sem",
            "ERROR: sem_open failed to attach to '%s' (errno=%d)\n",
            nameBuf, errno);
        return false;
    }

    mHandle = (unsigned long) semPtr;
    assert(mCreator == false);
    mKey = key;

    return true;
}

/*
 * Acquire or release the semaphore.
 */
void Semaphore::acquire(void)
{
    int cc = sem_wait((sem_t*) mHandle);
    if (cc != 0)
        LOG(LOG_WARN, "sem", "acquire failed (errno=%d)\n", errno);
}
void Semaphore::release(void)
{
    int cc = sem_post((sem_t*) mHandle);
    if (cc != 0)
        LOG(LOG_WARN, "sem", "release failed (errno=%d)\n", errno);
}
bool Semaphore::tryAcquire(void)
{
    int cc = sem_trywait((sem_t*) mHandle);
    if (cc != 0) {
        if (errno != EAGAIN)
            LOG(LOG_WARN, "sem", "tryAcquire failed (errno=%d)\n", errno);
        return false;
    }
    return true;
}


#elif defined(HAVE_SYSV_IPC) // -----------------------------------------------

/*
 * Basic SysV semaphore stuff.
 */

#define kInvalidHandle  ((unsigned long)-1)

#if defined(_SEM_SEMUN_UNDEFINED)
/* according to X/OPEN we have to define it ourselves */
union semun {
    int val;                  /* value for SETVAL */
    struct semid_ds *buf;     /* buffer for IPC_STAT, IPC_SET */
    unsigned short *array;    /* array for GETALL, SETALL */
                              /* Linux specific part: */
    struct seminfo *__buf;    /* buffer for IPC_INFO */
};
#endif

/*
 * Constructor.  Just init fields.
 */
Semaphore::Semaphore(void)
    : mHandle(kInvalidHandle), mCreator(false)
{
}

/*
 * Destructor.  If we created the semaphore, destroy it.
 */
Semaphore::~Semaphore(void)
{
    LOG(LOG_VERBOSE, "sem", "~Semaphore(handle=%ld creator=%d)\n",
        mHandle, mCreator);

    if (mCreator && mHandle != kInvalidHandle) {
        int cc;

        cc = semctl((int) mHandle, 0, IPC_RMID);
        if (cc != 0) {
            LOG(LOG_WARN, "sem",
                "Destructor failed to destroy key=%ld\n", mHandle);
        }
    }
}

/*
 * Create the semaphore.
 */
bool Semaphore::create(int key, int initialValue, bool deleteExisting)
{
    int semid, cc;

    if (deleteExisting) {
        semid = semget(key, 1, 0);
        if (semid != -1) {
            LOG(LOG_DEBUG, "sem", "Key %d exists (semid=%d), removing\n",
                key, semid);
            cc = semctl(semid, 0, IPC_RMID);
            if (cc != 0) {
                LOG(LOG_ERROR, "sem", "Failed to remove key=%d semid=%d\n",
                    key, semid);
                return false;
            } else {
                LOG(LOG_DEBUG, "sem",
                    "Removed previous semaphore with key=%d\n", key);
            }
        }
    }

    semid = semget(key, 1, 0600 | IPC_CREAT | IPC_EXCL);
    if (semid == -1) {
        LOG(LOG_ERROR, "sem", "Failed to create key=%d (errno=%d)\n",
            key, errno);
        return false;
    }

    mHandle = semid;
    mCreator = true;
    mKey = key;

    /*
     * Set initial value.
     */
    union semun init;
    init.val = initialValue;
    cc = semctl(semid, 0, SETVAL, init);
    if (cc == -1) {
        LOG(LOG_ERROR, "sem",
            "Unable to initialize semaphore, key=%d iv=%d (errno=%d)\n",
            key, initialValue, errno);
        return false;
    }

    return true;
}

/*
 * Attach to an existing semaphore.
 */
bool Semaphore::attach(int key)
{
    int semid;

    semid = semget(key, 0, 0);
    if (semid == -1) {
        LOG(LOG_ERROR, "sem", "Failed to find key=%d\n", key);
        return false;
    }

    mHandle = semid;
    assert(mCreator == false);
    mKey = key;

    return true;
}

/*
 * Acquire or release the semaphore.
 */
void Semaphore::acquire(void)
{
    assert(mHandle != kInvalidHandle);
    adjust(-1, true);
}
void Semaphore::release(void)
{
    assert(mHandle != kInvalidHandle);
    adjust(1, true);
}
bool Semaphore::tryAcquire(void)
{
    assert(mHandle != kInvalidHandle);
    return adjust(-1, false);
}

/*
 * Do the actual semaphore manipulation.
 *
 * The semaphore's value indicates the number of free resources.  Pass
 * in a negative value for "adj" to acquire resources, or a positive
 * value to free resources.
 *
 * Returns true on success, false on failure.
 */
bool Semaphore::adjust(int adj, bool wait)
{
    struct sembuf op;
    int cc;

    op.sem_num = 0;
    op.sem_op = adj;
    op.sem_flg = SEM_UNDO;
    if (!wait)
        op.sem_flg |= IPC_NOWAIT;

    cc = semop((int) mHandle, &op, 1);
    if (cc != 0) {
        if (wait || errno != EAGAIN) {
            LOG(LOG_WARN, "sem",
                "semaphore adjust by %d failed for semid=%ld (errno=%d)\n",
                adj, mHandle, errno);
        }
        return false;
    }

    //LOG(LOG_VERBOSE, "sem",
    //    "adjusted semaphore by %d (semid=%ld)\n", adj, mHandle);

    return true;
}


#elif defined(HAVE_WIN32_IPC) // ----------------------------------------------

/*
 * Win32 semaphore implementation.
 *
 * Pretty straightforward.
 */

static const char* kSemStr = "android-sem-";

/*
 * Constructor.  Just init fields.
 */
Semaphore::Semaphore(void)
    : mHandle((unsigned long) INVALID_HANDLE_VALUE), mCreator(false)
{
}

/*
 * Destructor.  Just close the semaphore handle.
 */
Semaphore::~Semaphore(void)
{
    LOG(LOG_DEBUG, "sem", "~Semaphore(handle=%ld creator=%d)\n",
        mHandle, mCreator);

    if (mHandle != (unsigned long) INVALID_HANDLE_VALUE)
        CloseHandle((HANDLE) mHandle);
}

/*
 * Create the semaphore.
 */
bool Semaphore::create(int key, int initialValue, bool deleteExisting)
{
    char keyBuf[64];
    HANDLE hSem;
    long max;

    snprintf(keyBuf, sizeof(keyBuf), "%s%d", kSemStr, key);

    if (initialValue == 0)
        max = 1;
    else
        max = initialValue;

    hSem = CreateSemaphore(
            NULL,                       // security attributes
            initialValue,               // initial count
            max,                        // max count, must be >= initial
            keyBuf);                    // object name
    if (hSem == NULL) {
        DWORD err = GetLastError();
        if (err == ERROR_ALREADY_EXISTS) {
            LOG(LOG_ERROR, "sem", "Semaphore '%s' already exists\n", keyBuf);
        } else {
            LOG(LOG_ERROR, "sem", "CreateSemaphore(%s) failed (err=%ld)\n",
                keyBuf, err);
        }
        return false;
    }

    mHandle = (unsigned long) hSem;
    mCreator = true;
    mKey = key;

    //LOG(LOG_DEBUG, "sem", "Semaphore '%s' created (handle=0x%08lx)\n",
    //    keyBuf, mHandle);

    return true;
}

/*
 * Attach to an existing semaphore.
 */
bool Semaphore::attach(int key)
{
    char keyBuf[64];
    HANDLE hSem;

    snprintf(keyBuf, sizeof(keyBuf), "%s%d", kSemStr, key);

    hSem = OpenSemaphore(
            //SEMAPHORE_MODIFY_STATE,   // mostly-full access
            SEMAPHORE_ALL_ACCESS,       // full access
            FALSE,                      // don't let kids inherit handle
            keyBuf);                    // object name
    if (hSem == NULL) {
        LOG(LOG_ERROR, "sem", "OpenSemaphore(%s) failed (err=%ld)\n",
            keyBuf, GetLastError());
        return false;
    }

    mHandle = (unsigned long) hSem;
    assert(mCreator == false);
    mKey = key;

    return true;
}

/*
 * Acquire or release the semaphore.
 */
void Semaphore::acquire(void)
{
    DWORD result;

    assert(mHandle != (unsigned long) INVALID_HANDLE_VALUE);

    result = WaitForSingleObject((HANDLE) mHandle, INFINITE);
    if (result != WAIT_OBJECT_0) {
        LOG(LOG_WARN, "sem",
            "WaitForSingleObject(INF) on semaphore returned %ld (err=%ld)\n",
            result, GetLastError());
    }
}
void Semaphore::release(void)
{
    DWORD result;

    assert(mHandle != (unsigned long) INVALID_HANDLE_VALUE);

    result = ReleaseSemaphore((HANDLE) mHandle, 1, NULL);    // incr by 1
    if (result == 0) {
        LOG(LOG_WARN, "sem", "ReleaseSemaphore failed (err=%ld)\n",
            GetLastError());
    }
}
bool Semaphore::tryAcquire(void)
{
    DWORD result;

    assert(mHandle != (unsigned long) INVALID_HANDLE_VALUE);
    result = WaitForSingleObject((HANDLE) mHandle, 0);
    if (result == WAIT_OBJECT_0)
        return true;        // grabbed it
    else if (result == WAIT_TIMEOUT)
        return false;       // not available
    else if (result == WAIT_FAILED) {
        LOG(LOG_WARN, "sem", "WaitForSingleObject(0) on sem failed (err=%ld)\n",
            GetLastError());
        return false;
    } else {
        LOG(LOG_WARN, "sem",
            "WaitForSingleObject(0) on sem returned %ld (err=%ld)\n",
            result, GetLastError());
        return false;
    }
}

#endif // ---------------------------------------------------------------------
