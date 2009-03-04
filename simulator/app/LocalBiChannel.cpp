//
// Copyright 2005 The Android Open Source Project
//
// Local named bi-directional communication channel.
//
#include "LocalBiChannel.h"
#include "utils/Log.h"

#if defined(HAVE_WIN32_IPC)
# define _WIN32_WINNT 0x0500
# include <windows.h>
#else
# include <sys/types.h>
# include <sys/socket.h>
# include <sys/stat.h>
# include <sys/un.h>
#endif

#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <assert.h>

#ifndef SUN_LEN
/*
 * Our current set of ARM header files don't define this.
 */
# define SUN_LEN(ptr) ((size_t) (((struct sockaddr_un *) 0)->sun_path)        \
                      + strlen ((ptr)->sun_path))
#endif

using namespace android;

const unsigned long kInvalidHandle = (unsigned long) -1;

/*
 * Initialize data fields.
 */
LocalBiChannel::LocalBiChannel(void)
    : mFileName(NULL), mIsListener(false), mHandle(kInvalidHandle)
{
}

#if defined(HAVE_WIN32_IPC)
/*
 * Implementation for Win32, using named pipes.
 *
 * Cygwin actually supports UNIX-domain sockets, but we want to stuff
 * the file handles into a Pipe, which uses HANDLE under Win32.
 */

const int kPipeSize = 4096;

/*
 * Destructor.  If we're the server side, we may need to clean up after
 * ourselves.
 */
LocalBiChannel::~LocalBiChannel(void)
{
    if (mHandle != kInvalidHandle)
        CloseHandle((HANDLE)mHandle);

    delete[] mFileName;
}

/*
 * Construct the full path.  The caller must delete[] the return value.
 */
static char* makeFilename(const char* name)
{
    static const char* kBasePath = "\\\\.\\pipe\\android-";
    char* fileName;

    assert(name != NULL && name[0] != '\0');

    fileName = new char[strlen(kBasePath) + strlen(name) + 1];
    strcpy(fileName, kBasePath);
    strcat(fileName, name);

    return fileName;
}

/*
 * Create a named pipe, so the client has something to connect to.
 */
bool LocalBiChannel::create(const char* name)
{
    delete[] mFileName;
    mFileName = makeFilename(name);

#if 0
    HANDLE hPipe;

    hPipe = CreateNamedPipe(
                    mFileName,              // unique pipe name
                    PIPE_ACCESS_DUPLEX |    // open mode
                        FILE_FLAG_FIRST_PIPE_INSTANCE,
                    0,                      // pipe mode (byte, blocking)
                    1,                      // max instances
                    kPipeSize,              // output buffer
                    kPipeSize,              // input buffer
                    NMPWAIT_USE_DEFAULT_WAIT,   // client time-out
                    NULL);                  // security

    if (hPipe == 0) {
        LOG(LOG_ERROR, "lbicomm",
            "CreateNamedPipe failed (err=%ld)\n", GetLastError());
        return false;
    }

    mHandle = (unsigned long) hPipe;
#endif

    return true;
}

/*
 * Attach to an existing named pipe.
 */
bool LocalBiChannel::attach(const char* name, Pipe** ppReadPipe,
    Pipe** ppWritePipe)
{
    HANDLE hPipe, dupHandle;

    delete[] mFileName;
    mFileName = makeFilename(name);

    hPipe = CreateFile(
                mFileName,                      // filename
                GENERIC_READ | GENERIC_WRITE,   // access
                0,                              // no sharing
                NULL,                           // security
                OPEN_EXISTING,                  // don't create
                0,                              // attributes
                NULL);                          // template
    if (hPipe == INVALID_HANDLE_VALUE) {
        LOG(LOG_ERROR, "lbicomm",
            "CreateFile on pipe '%s' failed (err=%ld)\n", name, GetLastError());
        return false;
    }

    assert(mHandle == kInvalidHandle);

    /*
     * Set up the pipes.  Use the new handle for one, and a duplicate
     * of it for the other, in case we decide to only close one side.
     */
    *ppReadPipe = new Pipe();
    (*ppReadPipe)->createReader((unsigned long) hPipe);

    DuplicateHandle(
            GetCurrentProcess(),
            hPipe,
            GetCurrentProcess(),
            &dupHandle,
            0,
            FALSE,
            DUPLICATE_SAME_ACCESS);
    *ppWritePipe = new Pipe();
    (*ppWritePipe)->createWriter((unsigned long) dupHandle);

    return true;
}

/*
 * Listen for a new connection, discarding any existing connection.
 */
bool LocalBiChannel::listen(Pipe** ppReadPipe, Pipe** ppWritePipe)
{
    BOOL connected;
    HANDLE hPipe;

    /*
     * Create up to 3 instances of the named pipe:
     * - currently active connection
     * - connection currently being rejected because one is already active
     * - a new listener to wait for the next round
     */
    hPipe = CreateNamedPipe(
                    mFileName,              // unique pipe name
                    PIPE_ACCESS_DUPLEX      // open mode
                        /*| FILE_FLAG_FIRST_PIPE_INSTANCE*/,
                    0,                      // pipe mode (byte, blocking)
                    3,                      // max instances
                    kPipeSize,              // output buffer
                    kPipeSize,              // input buffer
                    NMPWAIT_USE_DEFAULT_WAIT,   // client time-out
                    NULL);                  // security

    if (hPipe == 0) {
        LOG(LOG_ERROR, "lbicomm",
            "CreateNamedPipe failed (err=%ld)\n", GetLastError());
        return false;
    }

    /*
     * If a client is already connected to us, this fails with
     * ERROR_PIPE_CONNECTED.  It returns success if we had to wait
     * a little bit before the connection happens.
     */
    connected = ConnectNamedPipe(hPipe, NULL) ?
        TRUE : (GetLastError() == ERROR_PIPE_CONNECTED);

    if (connected) {
        /*
         * Create the pipes.  Give one a duplicated handle so that,
         * when one closes, we don't lose both.
         */
        HANDLE dupHandle;

        *ppReadPipe = new Pipe();
        (*ppReadPipe)->createReader((unsigned long) hPipe);

        DuplicateHandle(
                GetCurrentProcess(),
                hPipe,
                GetCurrentProcess(),
                &dupHandle,
                0,
                FALSE,
                DUPLICATE_SAME_ACCESS);
        *ppWritePipe = new Pipe();
        (*ppWritePipe)->createWriter((unsigned long) dupHandle);

        return true;
    } else {
        LOG(LOG_WARN, "lbicomm",
            "ConnectNamedPipe failed (err=%ld)\n", GetLastError());
#ifdef HAVE_WIN32_THREADS
        Sleep(500); /* 500 ms */
#else            
        usleep(500000);     // DEBUG DEBUG
#endif        
        return false;
    }
}

#else

/*
 * Implementation for Linux and Darwin, using UNIX-domain sockets.
 */

/*
 * Destructor.  If we're the server side, blow away the socket file.
 */
LocalBiChannel::~LocalBiChannel(void)
{
    if (mHandle != kInvalidHandle)
        close((int) mHandle);

    if (mIsListener && mFileName != NULL) {
        LOG(LOG_DEBUG, "lbicomm", "Removing '%s'\n", mFileName);
        (void) unlink(mFileName);
    }
    delete[] mFileName;
}

/*
 * Construct the full path.  The caller must delete[] the return value.
 */
static char* makeFilename(const char* name)
{
    static const char* kBasePath = "/tmp/android-";
    char* fileName;

    assert(name != NULL && name[0] != '\0');

    fileName = new char[strlen(kBasePath) + strlen(name) + 1];
    strcpy(fileName, kBasePath);
    strcat(fileName, name);

    return fileName;
}

/*
 * Create a UNIX domain socket, carefully removing it if it already
 * exists.
 */
bool LocalBiChannel::create(const char* name)
{
    struct stat sb;
    bool result = false;
    int sock = -1;
    int cc;

    delete[] mFileName;
    mFileName = makeFilename(name);

    cc = stat(mFileName, &sb);
    if (cc < 0) {
        if (errno != ENOENT) {
            LOG(LOG_ERROR, "lbicomm",
                "Unable to stat '%s' (errno=%d)\n", mFileName, errno);
            goto bail;
        }
    } else {
        /* don't touch it if it's not a socket */
        if (!(S_ISSOCK(sb.st_mode))) {
            LOG(LOG_ERROR, "lbicomm",
                "File '%s' exists and is not a socket\n", mFileName);
            goto bail;
        }

        /* remove the cruft */
        if (unlink(mFileName) < 0) {
            LOG(LOG_ERROR, "lbicomm",
                "Unable to remove '%s' (errno=%d)\n", mFileName, errno);
            goto bail;
        }
    }

    struct sockaddr_un addr;

    sock = ::socket(AF_UNIX, SOCK_STREAM, 0);
    if (sock < 0) {
        LOG(LOG_ERROR, "lbicomm",
            "UNIX domain socket create failed (errno=%d)\n", errno);
        goto bail;
    }

    /* bind the socket; this creates the file on disk */
    strcpy(addr.sun_path, mFileName);    // max 108 bytes
    addr.sun_family = AF_UNIX;
    cc = ::bind(sock, (struct sockaddr*) &addr, SUN_LEN(&addr));
    if (cc < 0) {
        LOG(LOG_ERROR, "lbicomm",
            "AF_UNIX bind failed for '%s' (errno=%d)\n", mFileName, errno);
        goto bail;
    }

    mHandle = (unsigned long) sock;
    sock = -1;
    mIsListener = true;
    result = true;

bail:
    if (sock >= 0)
        close(sock);
    return result;
}

/*
 * Attach to an existing UNIX domain socket.
 */
bool LocalBiChannel::attach(const char* name, Pipe** ppReadPipe,
    Pipe** ppWritePipe)
{
    bool result = false;
    int sock = -1;
    int cc;

    assert(ppReadPipe != NULL);
    assert(ppWritePipe != NULL);

    delete[] mFileName;
    mFileName = makeFilename(name);

    struct sockaddr_un addr;

    sock = ::socket(AF_UNIX, SOCK_STREAM, 0);
    if (sock < 0) {
        LOG(LOG_ERROR, "lbicomm",
            "UNIX domain socket create failed (errno=%d)\n", errno);
        goto bail;
    }

    /* connect to socket; fails if file doesn't exist */
    strcpy(addr.sun_path, mFileName);    // max 108 bytes
    addr.sun_family = AF_UNIX;
    cc = ::connect(sock, (struct sockaddr*) &addr, SUN_LEN(&addr));
    if (cc < 0) {
        // ENOENT means socket file doesn't exist
        // ECONNREFUSED means socket exists but nobody is listening
        LOG(LOG_ERROR, "lbicomm",
            "AF_UNIX connect failed for '%s': %s\n", mFileName,strerror(errno));
        goto bail;
    }

    /*
     * Create the two halves.  We dup() the sock so that closing one side
     * does not hose the other.
     */
    *ppReadPipe = new Pipe();
    (*ppReadPipe)->createReader(sock);
    *ppWritePipe = new Pipe();
    (*ppWritePipe)->createWriter(dup(sock));

    assert(mHandle == kInvalidHandle);
    sock = -1;
    mIsListener = false;

    result = true;

bail:
    if (sock >= 0)
        close(sock);
    return result;
}

/*
 * Listen for a new connection.
 */
bool LocalBiChannel::listen(Pipe** ppReadPipe, Pipe** ppWritePipe)
{
    bool result = false;
    struct sockaddr_un from;
    socklen_t fromlen;
    int sock, lsock;
    int cc;

    assert(mHandle != kInvalidHandle);
    lsock = (int) mHandle;

    LOG(LOG_DEBUG, "lbicomm", "AF_UNIX listening\n");
    cc = ::listen(lsock, 5);
    if (cc < 0) {
        LOG(LOG_ERROR, "lbicomm", "AF_UNIX listen failed (errno=%d)\n", errno);
        goto bail;
    }

    fromlen = sizeof(from);     // not SUN_LEN()
    sock = ::accept(lsock, (struct sockaddr*) &from, &fromlen);
    if (sock < 0) {
        LOG(LOG_WARN, "lbicomm", "AF_UNIX accept failed (errno=%d)\n", errno);
        goto bail;
    }

    /*
     * Create the two halves.  We dup() the sock so that closing one side
     * does not hose the other.
     */
    *ppReadPipe = new Pipe();
    (*ppReadPipe)->createReader(sock);
    *ppWritePipe = new Pipe();
    (*ppWritePipe)->createWriter(dup(sock));
    result = true;

bail:
    return result;
}

#endif
