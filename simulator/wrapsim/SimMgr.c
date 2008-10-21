/*
 * Copyright 2007 The Android Open Source Project
 *
 * Simulator interactions.
 *
 * TODO: for multi-process we probably need to switch to a new process
 * group if we are the first process (could be runtime, could be gdb),
 * rather than wait for the simulator to tell us to switch.
 */
#include "Common.h"

#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/ipc.h>
#include <sys/shm.h>
#include <sys/sem.h>
#include <sys/un.h>
#include <signal.h>
#include <assert.h>

// fwd
static int connectToSim(void);
static void listenToSim(void);

/*
 * Env var to restrict who tries to talk to the front end.
 */
#define kWrapSimConnectedEnv    "WRAP_SIM_CONNECTED"


/*
 * Signal the main thread that we're ready to continue.
 */
static void signalMainThread(void)
{
    int cc;

    cc = pthread_mutex_lock(&gWrapSim.startLock);
    assert(cc == 0);

    gWrapSim.startReady = 1;

    cc = pthread_cond_signal(&gWrapSim.startCond);
    assert(cc == 0);

    cc = pthread_mutex_unlock(&gWrapSim.startLock);
    assert(cc == 0);
}


/*
 * Entry point for the sim management thread.
 *
 * Once we have established a connection to the simulator and are ready
 * for other threads to send messages, we signal the main thread.
 */
static void* simThreadEntry(void* arg)
{
    wsLog("--- sim manager thread started\n");

    /*
     * Establish a connection to the simulator front-end.  If we can't do
     * that, we have no access to input or output devices, and we might
     * as well give up.
     */
    if (connectToSim() != 0) {
        signalMainThread();
        return NULL;
    }

    /* success! */
    wsLog("--- sim manager thread ready\n");
    gWrapSim.simulatorInitFailed = 0;
    signalMainThread();

    listenToSim();

    wsLog("--- sim manager thread exiting\n");

    return NULL;
}

/*
 * If we think we're not yet connected to the sim, do so now.  We only
 * want to do this once per process *group*, so we control access with
 * an environment variable.
 */
int wsSimConnect(void)
{
    /*
     * If the environment variable hasn't been set, assume we're the first
     * to get here, and should attach to the simulator.  We set the env
     * var here whether or not we succeed in connecting to the sim.
     *
     * (For correctness we should wrap the getenv/setenv in a semaphore.)
     */
    if (getenv(kWrapSimConnectedEnv) == NULL) {
        pthread_attr_t threadAttr;
        pthread_t threadHandle;
        int cc;

        gWrapSim.simulatorInitFailed = 1;
        setenv(kWrapSimConnectedEnv, "1", 1);

        cc = pthread_mutex_lock(&gWrapSim.startLock);
        assert(cc == 0);

        pthread_attr_init(&threadAttr);
        pthread_attr_setdetachstate(&threadAttr, PTHREAD_CREATE_DETACHED);
        cc = pthread_create(&threadHandle, &threadAttr, simThreadEntry, NULL);
        if (cc != 0) {
            wsLog("Unable to create new thread: %s\n", strerror(errno));
            abort();
        }

        while (!gWrapSim.startReady) {
            cc = pthread_cond_wait(&gWrapSim.startCond, &gWrapSim.startLock);
            assert(cc == 0);
        }

        cc = pthread_mutex_unlock(&gWrapSim.startLock);
        assert(cc == 0);

        if (gWrapSim.simulatorInitFailed) {
            wsLog("Simulator initialization failed, bailing\n");

            /* this *should* be okay to do */
            fprintf(stderr, "Fatal error:"
                " unable to connect to sim front-end (not running?)\n");
            abort();
        }
    }

    wsLog("+++ continuing\n");
    return 0;
}


/*
 * ===========================================================================
 *      Message / MessageStream
 * ===========================================================================
 */

/*
 * This is a quick & dirty rewrite of the C++ Message and MessageStream
 * classes, ported to C, reduced in generality, with syscalls stubbed
 * where necessary.  I didn't fix the API to make it more sensible in C
 * (which lacks destructors), so some of this is a little fragile or
 * awkward.
 */

/* values for message type byte; must match android::Message constants */
typedef enum MessageType {
    kTypeUnknown = 0,
    kTypeRaw,           // chunk of raw data
    kTypeConfig,        // send a name=value pair to peer
    kTypeCommand,       // simple command w/arg
    kTypeCommandExt,    // slightly more complicated command
    kTypeLogBundle,     // multi-part log message
} MessageType;

/*
 * Reusable message object.
 */
typedef struct Message {
    MessageType     mType;
    unsigned char*  mData;
    int             mLength;
} Message;

/* magic init messages; must match android::MessageStream constants */
enum {
    kHelloMsg       = 0x4e303047,       // 'N00G'
    kHelloAckMsg    = 0x31455221,       // '1ER!'
};


/*
 * Clear out a Message.
 */
static void Message_clear(Message* msg)
{
    memset(msg, 0, sizeof(Message));
}

/*
 * Keep reading until we get all bytes or hit EOF/error.  "fd" is expected
 * to be in blocking mode.
 *
 * Returns 0 on success.
 */
static int readAll(int fd, void* buf, size_t count)
{
    ssize_t actual;
    ssize_t have;

    have = 0;
    while (have != (ssize_t) count) {
        actual = _ws_read(fd, ((char*) buf) + have, count - have);
        if (actual < 0) {
            if (errno == EINTR)
                continue;
            wsLog("read %d failed: %s\n", fd, strerror(errno));
        } else if (actual == 0) {
            wsLog("early EOF on %d\n", fd);
            return -1;
        } else {
            have += actual;
        }

        assert(have <= (ssize_t)count);
    }

    return 0;
}

#if 0
/*
 * Keep writing until we put all bytes or hit an error.  "fd" is expected
 * to be in blocking mode.
 *
 * Returns 0 on success.
 */
static int writeAll(int fd, const void* buf, size_t count)
{
    ssize_t actual;
    ssize_t have;

    have = 0;
    while (have != count) {
        actual = _ws_write(fd, ((const char*) buf) + have, count - have);
        if (actual < 0) {
            if (errno == EINTR)
                continue;
            wsLog("write %d failed: %s\n", fd, strerror(errno));
        } else if (actual == 0) {
            wsLog("wrote zero on %d\n", fd);
            return -1;
        } else {
            have += actual;
        }

        assert(have <= count);
    }

    return 0;
}
#endif

/*
 * Read a message from the specified file descriptor.
 *
 * The caller must Message_release(&msg).
 *
 * We guarantee 32-bit alignment for msg->mData.
 */
static int Message_read(Message* msg, int fd)
{
    unsigned char header[4];

    readAll(fd, header, 4);

    msg->mType = (MessageType) header[2];
    msg->mLength = header[0] | header[1] << 8;
    msg->mLength -= 2;   // we already read two of them in the header

    if (msg->mLength > 0) {
        int actual;

        /* Linux malloc guarantees at least 32-bit alignment */
        msg->mData = (unsigned char*) malloc(msg->mLength);
        if (msg->mData == NULL) {
            wsLog("alloc %d failed\n", msg->mLength);
            return -1;
        }

        if (readAll(fd, msg->mData, msg->mLength) != 0) {
            wsLog("failed reading message body (wanted %d)\n", msg->mLength);
            return -1;
        }
    } else {
        msg->mData = NULL;
    }

    return 0;
}

/*
 * Write a message to the specified file descriptor.
 *
 * The caller must Message_release(&msg).
 */
static int Message_write(Message* msg, int fd)
{
    struct iovec writeVec[2];
    unsigned char header[4];
    int len, numVecs;
    ssize_t actual;

    len = msg->mLength + 2;
    header[0] = len & 0xff;
    header[1] = (len >> 8) & 0xff;
    header[2] = msg->mType;
    header[3] = 0;
    writeVec[0].iov_base = header;
    writeVec[0].iov_len = 4;
    numVecs = 1;

    if (msg->mLength > 0) {
        assert(msg->mData != NULL);
        writeVec[1].iov_base = msg->mData;
        writeVec[1].iov_len = msg->mLength;
        numVecs++;
    }

    /* write it all in one shot; not worrying about partial writes for now */
    actual = _ws_writev(fd, writeVec, numVecs);
    if (actual != len+2) {
        wsLog("failed writing message to fd %d: %d of %d %s\n",
            fd, actual, len+2, strerror(errno));
        return -1;
    }

    return 0;
}

/*
 * Release storage associated with a Message.
 */
static void Message_release(Message* msg)
{
    free(msg->mData);
    msg->mData = NULL;
}

/*
 * Extract a name/value pair from a message.
 */
static int getConfig(const Message* msg, const char** name, const char** val)
{
    if (msg->mLength < 2) {
        wsLog("message len (%d) is too short\n", msg->mLength);
        return -1;
    }
    const char* ptr = (const char*) msg->mData;

    *name = (const char*) ptr;
    *val = (const char*) (ptr + strlen((char*)ptr) +1);
    return 0;
}

/*
 * Extract a command from a message.
 */
static int getCommand(const Message* msg, int* pCmd, int* pArg)
{
    if (msg->mLength != sizeof(int) * 2) {
        wsLog("message len (%d) is wrong for cmd (%d)\n",
            msg->mLength, sizeof(int) * 2);
        return -1;
    }

    /* this assumes 32-bit alignment on mData */
    const int* ptr = (const int*) msg->mData;

    *pCmd = ptr[0];
    *pArg = ptr[1];

    return 0;
}

/*
 * Extract an extended command from a message.
 */
static int getCommandExt(const Message* msg, int* pCmd, int* pArg0,
    int* pArg1, int* pArg2)
{
    if (msg->mLength != sizeof(int) * 4) {
        wsLog("message len (%d) is wrong for cmd (%d)\n",
            msg->mLength, sizeof(int) * 4);
        return -1;
    }

    /* this assumes 32-bit alignment on mData */
    const int* ptr = (const int*) msg->mData;

    *pCmd = ptr[0];
    *pArg0 = ptr[1];
    *pArg1 = ptr[2];
    *pArg2 = ptr[3];

    return 0;
}

/*
 * Attach 8 bytes of data with "cmd" and "arg" to "msg".
 *
 * "msg->mData" will need to be freed by the caller.  (This approach made
 * more sense when C++ destructors were available, but it's just not worth
 * reworking it.)
 */
static int setCommand(Message* msg, int cmd, int arg)
{
    Message_clear(msg);

    msg->mLength = 8;
    msg->mData = malloc(msg->mLength);
    msg->mType = kTypeCommand;

    /* assumes 32-bit alignment on malloc blocks */
    int* pInt = (int*) msg->mData;
    pInt[0] = cmd;
    pInt[1] = arg;

    return 0;
}

/*
 * Construct the full path.  The caller must free() the return value.
 */
static char* makeFilename(const char* name)
{
    static const char* kBasePath = "/tmp/android-";
    char* fileName;

    assert(name != NULL && name[0] != '\0');

    fileName = (char*) malloc(strlen(kBasePath) + strlen(name) + 1);
    strcpy(fileName, kBasePath);
    strcat(fileName, name);

    return fileName;
}

/*
 * Attach to a SysV shared memory segment.
 */
static int attachToShmem(int key, int* pShmid, void** pAddr, long* pLength)
{
    int shmid;

    shmid = shmget(key, 0, 0);
    if (shmid == -1) {
        wsLog("ERROR: failed to find shmem key=%d\n", key);
        return -1;
    }

    void* addr = shmat(shmid, NULL, 0);
    if (addr == (void*) -1) {
        wsLog("ERROR: could not attach to key=%d shmid=%d\n", key, shmid);
        return -1;
    }

    struct shmid_ds shmids;
    int cc;

    cc = shmctl(shmid, IPC_STAT, &shmids);
    if (cc != 0) {
        wsLog("ERROR: could not IPC_STAT shmid=%d\n", shmid);
        return -1;
    }
    *pLength = shmids.shm_segsz;

    *pAddr = addr;
    *pShmid = shmid;
    return 0;
}

/*
 * Attach to a SysV semaphore.
 */
static int attachToSem(int key, int* pSemid)
{
    int semid;

    semid = semget(key, 0, 0);
    if (semid == -1) {
        wsLog("ERROR: failed to attach to semaphore key=%d\n", key);
        return -1;
    }

    *pSemid = semid;
    return 0;
}

/*
 * "Adjust" a semaphore.
 */
static int adjustSem(int semid, int adj)
{
    const int wait = 1;
    struct sembuf op;
    int cc;

    op.sem_num = 0;
    op.sem_op = adj;
    op.sem_flg = SEM_UNDO;
    if (!wait)
        op.sem_flg |= IPC_NOWAIT;

    cc = semop(semid, &op, 1);
    if (cc != 0) {
        if (wait || errno != EAGAIN) {
            wsLog("Warning:"
                " semaphore adjust by %d failed for semid=%d (errno=%d)\n",
                adj, semid, errno);
        }
        return -1;
    }

    return 0;
}

/*
 * Acquire the semaphore associated with a display.
 */
void wsLockDisplay(int displayIdx)
{
    assert(displayIdx >= 0 && displayIdx < gWrapSim.numDisplays);
    int semid = gWrapSim.display[displayIdx].semid;

    (void) adjustSem(semid, -1);
}

/*
 * Acquire the semaphore associated with a display.
 */
void wsUnlockDisplay(int displayIdx)
{
    assert(displayIdx >= 0 && displayIdx < gWrapSim.numDisplays);
    int semid = gWrapSim.display[displayIdx].semid;

    (void) adjustSem(semid, 1);
}

/*
 * Process the display config from the simulator
 *
 * Right now this is a blob of raw data that looks like this:
 *  +00 magic number
 *  +04 #of displays
 *  +08 display 0:
 *      +00 width
 *      +04 height
 *      +08 format
 *      +0c refresh rate
 *      +10 shmem key
 *  +1c display 1...
 */
static int handleDisplayConfig(const int* pData, int length)
{
    int numDisplays;

    if (length < 8) {
        wsLog("Bad display config: length is %d\n", length);
        return -1;
    }
    assert(*pData == kDisplayConfigMagic);

    /*
     * Pull out the #of displays.  If it looks valid, configure the runtime.
     */
    pData++;        // skip over magic
    numDisplays = *pData++;

    if (numDisplays <= 0 || numDisplays > kMaxDisplays) {
        wsLog("Bizarre display count %d\n", numDisplays);
        return -1;
    }
    if (length != 8 + numDisplays * kValuesPerDisplay * (int)sizeof(int)) {
        wsLog("Bad display config: length is %d (expected %d)\n",
            length, 8 + numDisplays * kValuesPerDisplay * (int)sizeof(int));
        return -1;
    }

    /*
     * Extract the config values.
     *
     * The runtime doesn't support multiple devices, so we don't either.
     */
    int i;
    for (i = 0; i < numDisplays; i++) {
        gWrapSim.display[i].width = pData[0];
        gWrapSim.display[i].height = pData[1];
        gWrapSim.display[i].shmemKey = pData[4];
        /* format/refresh no longer needed */

        void* addr;
        int shmid, semid;
        long length;
        if (attachToShmem(gWrapSim.display[i].shmemKey, &shmid, &addr,
                &length) != 0)
        {
            wsLog("Unable to connect to shared memory\n");
            return -1;
        }

        if (attachToSem(gWrapSim.display[i].shmemKey, &semid) != 0) {
            wsLog("Unable to attach to sempahore\n");
            return -1;
        }

        gWrapSim.display[i].shmid = shmid;
        gWrapSim.display[i].addr = addr;
        gWrapSim.display[i].length = length;
        gWrapSim.display[i].semid = semid;

        wsLog("Display %d: width=%d height=%d\n",
            i,
            gWrapSim.display[i].width,
            gWrapSim.display[i].height);
        wsLog("  shmem=0x%08x addr=%p len=%ld semid=%d\n",
            gWrapSim.display[i].shmemKey,
            gWrapSim.display[i].addr,
            gWrapSim.display[i].length,
            gWrapSim.display[i].semid);

        pData += kValuesPerDisplay;
    }
    gWrapSim.numDisplays = numDisplays;

    return 0;
}


/*
 * Initialize our connection to the simulator, which will be listening on
 * a UNIX domain socket.
 *
 * On success, this configures gWrapSim.simulatorFd and returns 0.
 */
static int openSimConnection(const char* name)
{
    int result = -1;
    char* fileName = NULL;
    int sock = -1;
    int cc;

    assert(gWrapSim.simulatorFd == -1);

    fileName = makeFilename(name);

    struct sockaddr_un addr;
    
    sock = socket(AF_UNIX, SOCK_STREAM, 0);
    if (sock < 0) {
        wsLog("UNIX domain socket create failed (errno=%d)\n", errno);
        goto bail;
    }

    /* connect to socket; fails if file doesn't exist */
    strcpy(addr.sun_path, fileName);    // max 108 bytes
    addr.sun_family = AF_UNIX;
    cc = connect(sock, (struct sockaddr*) &addr, SUN_LEN(&addr));
    if (cc < 0) {
        // ENOENT means socket file doesn't exist
        // ECONNREFUSED means socket exists but nobody is listening
        wsLog("AF_UNIX connect failed for '%s': %s\n",
            fileName, strerror(errno));
        goto bail;
    }

    gWrapSim.simulatorFd = sock;
    sock = -1;

    result = 0;
    wsLog("+++ connected to '%s'\n", fileName);

bail:
    if (sock >= 0)
        _ws_close(sock);
    free(fileName);
    return result;
}

/*
 * Prepare communication with the front end.  We wait for a "hello" from
 * the other side, and respond in kind.
 */
static int prepSimConnection(void)
{
    /* NOTE: this is endian-specific; we're x86 Linux only, so no problem */
    static const unsigned int hello = kHelloMsg;
    static const unsigned int helloAck = kHelloAckMsg;
    Message msg;

    if (Message_read(&msg, gWrapSim.simulatorFd) != 0) {
        wsLog("hello read failed\n");
        return -1;
    }

    if (memcmp(msg.mData, &hello, 4) != 0) {
        wsLog("Got bad hello from peer\n");
        return -1;
    }

    Message_release(&msg);

    msg.mType = kTypeRaw;
    msg.mData = (unsigned char*) &helloAck;
    msg.mLength = 4;

    if (Message_write(&msg, gWrapSim.simulatorFd) != 0) {
        wsLog("hello ack write failed\n");
        return -1;
    }

    return 0;
}

/*
 * Get the sim front-end configuration.  We loop here until the sim claims
 * to be done with us.
 */
static int getSimConfig(void)
{
    Message msg;
    int joinNewGroup, grabTerminal, done;
    int result = -1;

    joinNewGroup = grabTerminal = done = 0;
    Message_clear(&msg);        // clear out msg->mData

    wsLog("+++ awaiting hardware configuration\n");
    while (!done) {
        if (Message_read(&msg, gWrapSim.simulatorFd) != 0) {
            wsLog("failed receiving config from parent\n");
            goto bail;
        }

        if (msg.mType == kTypeCommand) {
            int cmd, arg;

            if (getCommand(&msg, &cmd, &arg) != 0)
                goto bail;

            switch (cmd) {
            case kCommandGoAway:
                wsLog("Simulator front-end is busy\n");
                goto bail;
            case kCommandNewPGroup:
                joinNewGroup = 1;
                grabTerminal = (arg != 0);
                wsLog("Simulator wants us to be in a new pgrp (term=%d)\n",
                    grabTerminal);
                break;
            case kCommandConfigDone:
                done = 1;
                break;
            default:
                wsLog("Got unexpected command %d/%d\n", cmd, arg);
                break;
            }
        } else if (msg.mType == kTypeRaw) {
            /* assumes 32-bit alignment and identical byte ordering */
            int* pData = (int*) msg.mData;
            if (msg.mLength >= 4 && *pData == kDisplayConfigMagic) {
                if (handleDisplayConfig(pData, msg.mLength) != 0)
                    goto bail;
            }
        } else if (msg.mType == kTypeConfig) {
            const char *name;
            const char *val;
            getConfig(&msg, &name, &val);
            if(strcmp(name, "keycharmap") == 0) {
                free((void*)gWrapSim.keyMap);
                gWrapSim.keyMap = strdup(val);
            }
        } else {
            wsLog("Unexpected msg type %d during startup\n", msg.mType);
            goto bail;
        }

        /* clear out the data field if necessary */
        Message_release(&msg);
    }

    wsLog("Configuration received from simulator\n");

    if (joinNewGroup) {
        /* set pgid to pid */
        pid_t pgid = getpid();
        setpgid(0, pgid);

        /*
         * Put our pgrp in the foreground.
         * tcsetpgrp() from background process causes us to get a SIGTTOU,
         * which is mostly harmless but makes tcsetpgrp() fail with EINTR.
         */
        signal(SIGTTOU, SIG_IGN);
        if (grabTerminal) {
            if (tcsetpgrp(fileno(stdin), getpgrp()) != 0) {
                wsLog("tcsetpgrp(%d, %d) failed (errno=%d)\n",
                    fileno(stdin), getpgrp(), errno);
            }
            wsLog("Set pgrp %d as foreground\n", (int) getpgrp());
        }
    
        /* tell the sim where we're at */
        Message msg;
        setCommand(&msg, kCommandNewPGroupCreated, pgid);
        Message_write(&msg, gWrapSim.simulatorFd);
        Message_release(&msg);
    }

    result = 0;

bail:
    /* make sure the data was freed */
    Message_release(&msg);
    //wsLog("bailing, result=%d\n", result);
    return result;
}

/*
 * Connect to the simulator and exchange pleasantries.
 *
 * Returns 0 on success.
 */
static int connectToSim(void)
{
    if (openSimConnection(kAndroidPipeName) != 0)
        return -1;

    if (prepSimConnection() != 0)
        return -1;

    if (getSimConfig() != 0)
        return -1;

    wsLog("+++ sim is ready to go\n");

    return 0;
}

/*
 * Listen to the sim forever or until the front end shuts down, whichever
 * comes first.
 *
 * All we're really getting here are key events.
 */
static void listenToSim(void)
{
    wsLog("--- listening for input events from front end\n");

    while (1) {
        Message msg;

        Message_clear(&msg);
        if (Message_read(&msg, gWrapSim.simulatorFd) != 0) {
            wsLog("--- sim message read failed\n");
            return;
        }

        if (msg.mType == kTypeCommand) {
            int cmd, arg;

            if (getCommand(&msg, &cmd, &arg) != 0) {
                wsLog("bad command from sim?\n");
                continue;
            }

            switch (cmd) {
            case kCommandQuit:
                wsLog("--- sim sent us a QUIT message\n");
                return;
            case kCommandKeyDown:
                wsLog("KEY DOWN: %d\n", arg);
                wsSendSimKeyEvent(arg, 1);
                break;
            case kCommandKeyUp:
                wsLog("KEY UP: %d\n", arg);
                wsSendSimKeyEvent(arg, 0);
                break;
            default:
                wsLog("--- sim sent unrecognized command %d\n", cmd);
                break;
            }

            Message_release(&msg);
        } else if (msg.mType == kTypeCommandExt) {
            int cmd, arg0, arg1, arg2;

            if (getCommandExt(&msg, &cmd, &arg0, &arg1, &arg2) != 0) {
                wsLog("bad ext-command from sim?\n");
                continue;
            }

            switch (cmd) {
            case kCommandTouch:
                wsSendSimTouchEvent(arg0, arg1, arg2);
                break;
            }

            Message_release(&msg);
        } else {
            wsLog("--- sim sent non-command message, type=%d\n", msg.mType);
        }
    }

    assert(0);      // not reached
}


/*
 * Tell the simulator front-end that the display has been updated.
 */
void wsPostDisplayUpdate(int displayIdx)
{
    if (gWrapSim.simulatorFd < 0) {
        wsLog("Not posting display update -- sim not ready\n");
        return;
    }

    Message msg;

    setCommand(&msg, kCommandUpdateDisplay, displayIdx);
    Message_write(&msg, gWrapSim.simulatorFd);
    Message_release(&msg);
}

/*
 * Send a log message to the front-end.
 */
void wsPostLogMessage(int logPrio, const char* tag, const char* message)
{
    if (gWrapSim.simulatorFd < 0) {
        wsLog("Not posting log message -- sim not ready\n");
        return;
    }

    time_t when = time(NULL);
    int pid = (int) getpid();
    int tagLen, messageLen, totalLen;

    tagLen = strlen(tag) +1;
    messageLen = strlen(message) +1;
    totalLen = sizeof(int) * 3 + tagLen + messageLen;
    unsigned char outBuf[totalLen];
    unsigned char* cp = outBuf;

    /* See Message::set/getLogBundle() in simulator/MessageStream.cpp. */
    memcpy(cp, &when, sizeof(int));
    cp += sizeof(int);
    memcpy(cp, &logPrio, sizeof(int));
    cp += sizeof(int);
    memcpy(cp, &pid, sizeof(int));
    cp += sizeof(int);
    memcpy(cp, tag, tagLen);
    cp += tagLen;
    memcpy(cp, message, messageLen);
    cp += messageLen;

    assert(cp - outBuf == totalLen);

    Message msg;
    msg.mType = kTypeLogBundle;
    msg.mData = outBuf;
    msg.mLength = totalLen;
    Message_write(&msg, gWrapSim.simulatorFd);

    msg.mData = NULL;       // don't free
    Message_release(&msg);
}

/*
 * Turn the vibrating notification device on or off.
 */
void wsEnableVibration(int vibrateOn)
{
    if (gWrapSim.simulatorFd < 0) {
        wsLog("Not posting vibrator update -- sim not ready\n");
        return;
    }

    Message msg;

    //wsLog("+++ sending vibrate:%d\n", vibrateOn);

    setCommand(&msg, kCommandVibrate, vibrateOn);
    Message_write(&msg, gWrapSim.simulatorFd);
    Message_release(&msg);
}

