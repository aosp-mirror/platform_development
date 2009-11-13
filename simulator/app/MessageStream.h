//
// Copyright 2005 The Android Open Source Project
//
// High-level message stream that sits on top of a pair of Pipes.  Useful
// for inter-process communication, e.g. between "simulator" and "runtime".
//
// All messages are sent in packets:
//  +00 16-bit length (of everything that follows), little-endian
//  +02 8-bit message type
//  +03 (reserved, must be zero)
//  +04 message body
//
#ifndef _LIBS_UTILS_MESSAGE_STREAM_H
#define _LIBS_UTILS_MESSAGE_STREAM_H

#ifdef HAVE_ANDROID_OS
#error DO NOT USE THIS FILE IN THE DEVICE BUILD
#endif

#include "Pipe.h"
#include <stdlib.h>
#include <cutils/uio.h>

// Defined in LogBundle.h.
struct android_LogBundle;

namespace android {

/*
 * A single message, which can be filled out and sent, or filled with
 * received data.
 *
 * Message objects are reusable.
 */
class Message {
public:
    Message(void)
        : mCleanup(kCleanupUnknown)
        { reset(); }
    ~Message(void) { reset(); }

    /* values for message type byte */
    typedef enum MessageType {
        kTypeUnknown = 0,
        kTypeRaw,           // chunk of raw data
        kTypeConfig,        // send a name=value pair to peer
        kTypeCommand,       // simple command w/arg
        kTypeCommandExt,    // slightly more complicated command
        kTypeLogBundle,     // multi-part log message
    } MessageType;

    /* what to do with data when we're done */
    typedef enum Cleanup {
        kCleanupUnknown = 0,
        kCleanupNoDelete,   // do not delete data when object destroyed
        kCleanupDelete,     // delete with "delete[]"
    } Cleanup;

    /*
     * Stuff raw data into the object.  The caller can use the "cleanup"
     * parameter to decide whether or not the Message object owns the data.
     */
    void setRaw(const unsigned char* data, int len, Cleanup cleanup);

    /*
     * Send a "name=value" pair.
     */
    void setConfig(const char* name, const char* value);

    /*
     * Send a command/arg pair.
     */
    void setCommand(int cmd, int arg);
    void setCommandExt(int cmd, int arg0, int arg1, int arg2);

    /*
     * Send a multi-part log message.
     */
    void setLogBundle(const android_LogBundle* pBundle);

    /*
     * Simple accessors.
     */
    MessageType getType(void) const { return mType; }
    const unsigned char* getData(void) const { return mData; }
    int getLength(void) const { return mLength; }

    /*
     * Not-so-simple accessors.  These coerce the raw data into an object.
     *
     * The data returned by these may not outlive the Message, so make
     * copies if you plan to use them long-term.
     */
    bool getConfig(const char** pName, const char** pValue);
    bool getCommand(int* pCmd, int* pArg);
    bool getLogBundle(android_LogBundle* pBundle);

    /*
     * Read or write this message on the specified pipe.
     *
     * If "wait" is true, read() blocks until a message arrives.  Only
     * one thread should be reading at a time.
     */
    bool read(Pipe* pPipe, bool wait);
    bool write(Pipe* pPipe) const;

private:
    Message& operator=(const Message&);     // not defined
    Message(const Message&);                // not defined

    void reset(void) {
        if (mCleanup == kCleanupDelete)
            delete[] mData;

        mType = kTypeUnknown;
        mCleanup = kCleanupNoDelete;
        mData = NULL;
        mLength = -1;
    }

    MessageType     mType;
    Cleanup         mCleanup;
    unsigned char*  mData;
    int             mLength;
    struct iovec    mVec;
};


/*
 * Abstraction of higher-level communication channel.
 *
 * This may be used from multiple threads simultaneously.  Blocking on
 * the read pipe from multiple threads will have unpredictable behavior.
 *
 * Does not take ownership of the pipes passed in to init().
 */
class MessageStream {
public:
    MessageStream(void)
        : mReadPipe(NULL), mWritePipe(NULL)
        {}
    ~MessageStream(void) {}

    /*
     * Initialize object and exchange greetings.  "initateHello" determines
     * whether we send "Hello" or block waiting for it to arrive.  Usually
     * the "parent" initiates.
     */
    bool init(Pipe* readPipe, Pipe* writePipe, bool initiateHello);

    bool isReady(void) const { return mReadPipe != NULL && mWritePipe != NULL; }

    /*
     * Send a message immediately.
     */
    bool send(const Message* pMsg) { return pMsg->write(mWritePipe); }

    /*
     * Receive a message.
     */
    bool recv(Message* pMsg, bool wait) { return pMsg->read(mReadPipe, wait); }

    /*
     * Close communication pipes.  Further attempts to send or receive
     * will fail.  Note this doesn't actually "close" the pipes, because
     * we don't own them.
     */
    void close(void) { mReadPipe = mWritePipe = NULL; }

    /*
     * Get our incoming traffic pipe.  This is useful on Linux systems
     * because it allows access to the file descriptor which can be used
     * in a select() call.
     */
    Pipe* getReadPipe(void) { return mReadPipe; }

private:
    enum {
        kHelloMsg       = 0x4e303047,       // 'N00G'
        kHelloAckMsg    = 0x31455221,       // '1ER!'
    };

    /* communication pipes; note we don't own these */
    Pipe*   mReadPipe;
    Pipe*   mWritePipe;
};

}; // namespace android

#endif // _LIBS_UTILS_MESSAGE_STREAM_H
