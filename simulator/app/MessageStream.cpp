//
// Copyright 2005 The Android Open Source Project
//
// Message stream abstraction.
//
#include "MessageStream.h"
#include "LogBundle.h"

#include "utils/Log.h"

#include <string.h>
#include <assert.h>

using namespace android;

/*
 * ===========================================================================
 *      Message
 * ===========================================================================
 */

/*
 * Send a blob of raw data.
 */
void Message::setRaw(const unsigned char* data, int len, Cleanup cleanup)
{
    reset();

    mData = const_cast<unsigned char*>(data);
    mLength = len;
    mCleanup = cleanup;
    mType = kTypeRaw;
}

/*
 * Send a "name=value" config pair.
 */
void Message::setConfig(const char* name, const char* value)
{
    reset();

    assert(name != NULL && value != NULL);

    int nlen = strlen(name) +1;
    int vlen = strlen(value) +1;
    mData = new unsigned char[nlen+vlen];
    mCleanup = kCleanupDelete;
    mLength = nlen + vlen;
    mType = kTypeConfig;

    memcpy(mData, name, nlen);
    memcpy(mData + nlen, value, vlen);
}

/*
 * Try to return the contents of the message as if it were a name/value pair.
 */
bool Message::getConfig(const char** pName, const char** pValue)
{
    if (mLength < 2)
        return false;
    assert(mData != NULL);

    *pName = (const char*) mData;
    *pValue = (const char*) (mData + strlen((char*)mData) +1);
    return true;
}

/*
 * Send a command/arg pair.
 */
void Message::setCommand(int cmd, int arg)
{
    reset();

    mData = new unsigned char[sizeof(int) * 2];
    mCleanup = kCleanupDelete;
    mLength = sizeof(int) * 2;
    mType = kTypeCommand;

    int* pInt = (int*) mData;
    pInt[0] = cmd;
    pInt[1] = arg;
}

/*
 * Send a command with 3 args instead of just one.
 */
void Message::setCommandExt(int cmd, int arg0, int arg1, int arg2)
{
    reset();

    mData = new unsigned char[sizeof(int) * 4];
    mCleanup = kCleanupDelete;
    mLength = sizeof(int) * 4;
    mType = kTypeCommandExt;

    int* pInt = (int*) mData;
    pInt[0] = cmd;
    pInt[1] = arg0;
    pInt[2] = arg1;
    pInt[3] = arg2;
}

/*
 * Try to return the contents of the message as if it were a "command".
 */
bool Message::getCommand(int* pCmd, int* pArg)
{
    if (mLength != sizeof(int) * 2) {
        LOG(LOG_WARN, "", "type is %d, len is %d\n", mType, mLength);
        return false;
    }
    assert(mData != NULL);

    const int* pInt = (const int*) mData;
    *pCmd = pInt[0];
    *pArg = pInt[1];

    return true;
}

/*
 * Serialize a log message.
 *
 * DO NOT call LOG() from here.
 */
void Message::setLogBundle(const android_LogBundle* pBundle)
{
    reset();

    /* get string lengths; we add one here to include the '\0' */
    int tagLen, msgLen;
    tagLen = strlen(pBundle->tag) + 1;
    size_t i;
    msgLen = 0;
    for (i=0; i<pBundle->msgCount; i++) msgLen += pBundle->msgVec[i].iov_len;
    msgLen += 1;

    /* set up the structure */
    mCleanup = kCleanupDelete;
    mLength =   sizeof(pBundle->when) +
                sizeof(pBundle->priority) +
                sizeof(pBundle->pid) +
                tagLen +
                msgLen;
    mData = new unsigned char[mLength];
    mType = kTypeLogBundle;

    unsigned char* pCur = mData;

    /* copy the stuff over */
    *((time_t*)pCur) = pBundle->when;
    pCur += sizeof(pBundle->when);
    *((android_LogPriority*)pCur) = pBundle->priority;
    pCur += sizeof(pBundle->priority);
    *((pid_t*)pCur) = pBundle->pid;
    pCur += sizeof(pBundle->pid);
    memcpy(pCur, pBundle->tag, tagLen);
    pCur += tagLen;
    for (i=0; i<pBundle->msgCount; i++) {
        memcpy(pCur, pBundle->msgVec[i].iov_base, pBundle->msgVec[i].iov_len);
        pCur += pBundle->msgVec[i].iov_len;
    }
    *pCur++ = 0;

    assert(pCur - mData == mLength);
}

/*
 * Extract the components of a log bundle.
 *
 * We're just returning points inside the message buffer, so the caller
 * will need to copy them out before the next reset().
 */
bool Message::getLogBundle(android_LogBundle* pBundle)
{
    if (mLength < (int)(sizeof(time_t) + sizeof(int)*2 + 4)) {
        LOG(LOG_WARN, "", "type is %d, len is %d, too small\n",
            mType, mLength);
        return false;
    }
    assert(mData != NULL);

    unsigned char* pCur = mData;

    pBundle->when = *((time_t*) pCur);
    pCur += sizeof(pBundle->when);
    pBundle->priority = *((android_LogPriority*) pCur);
    pCur += sizeof(pBundle->priority);
    pBundle->pid = *((pid_t*) pCur);
    pCur += sizeof(pBundle->pid);
    pBundle->tag = (const char*) pCur;
    pCur += strlen((const char*) pCur) +1;
    mVec.iov_base = (char*) pCur;
    mVec.iov_len = strlen((const char*) pCur);
    pBundle->msgVec = &mVec;
    pBundle->msgCount = 1;
    pCur += mVec.iov_len +1;

    if (pCur - mData != mLength) {
        LOG(LOG_WARN, "", "log bundle rcvd %d, used %d\n", mLength,
            (int) (pCur - mData));
        return false;
    }

    return true;
}

/*
 * Read the next event from the pipe.
 *
 * This is not expected to work well when multiple threads are reading.
 */
bool Message::read(Pipe* pPipe, bool wait)
{
    if (pPipe == NULL)
        return false;
    assert(pPipe->isCreated());

    if (!wait) {
        if (!pPipe->readReady())
            return false;
    }

    reset();

    unsigned char header[4];
    if (pPipe->read(header, 4) != 4)
        return false;

    mType = (MessageType) header[2];
    mLength = header[0] | header[1] << 8;
    mLength -= 2;   // we already read two of them in the header

    if (mLength > 0) {
        int actual;

        mData = new unsigned char[mLength];
        if (mData == NULL) {
            LOG(LOG_ERROR, "", "alloc failed\n");
            return false;
        }
        mCleanup = kCleanupDelete;

        actual = pPipe->read(mData, mLength);
        if (actual != mLength) {
            LOG(LOG_WARN, "", "failed reading message body (%d of %d bytes)\n",
                actual, mLength);
            return false;
        }
    }

    return true;
}

/*
 * Write this event to a pipe.
 *
 * It would be easiest to write the header and message body with two
 * separate calls, but that will occasionally fail on multithreaded
 * systems when the writes are interleaved.  We have to allocate a
 * temporary buffer, copy the data, and write it all at once.  This
 * would be easier with writev(), but we can't rely on having that.
 *
 * DO NOT call LOG() from here, as we could be in the process of sending
 * a log message.
 */
bool Message::write(Pipe* pPipe) const
{
    char tmpBuf[128];
    char* writeBuf = tmpBuf;
    bool result = false;
    int kHeaderLen = 4;

    if (pPipe == NULL)
        return false;
    assert(pPipe->isCreated());

    if (mData == NULL || mLength < 0)
        return false;

    /* if it doesn't fit in stack buffer, allocate space */
    if (mLength + kHeaderLen > (int) sizeof(tmpBuf)) {
        writeBuf = new char[mLength + kHeaderLen];
        if (writeBuf == NULL)
            goto bail;
    }

    /*
     * The current value of "mLength" does not include the 4-byte header.
     * Two of the 4 header bytes are included in the length we output
     * (the type byte and the pad byte), so we adjust mLength.
     */
    writeBuf[0] = (unsigned char) (mLength + kHeaderLen -2);
    writeBuf[1] = (unsigned char) ((mLength + kHeaderLen -2) >> 8);
    writeBuf[2] = (unsigned char) mType;
    writeBuf[3] = 0;
    if (mLength > 0)
        memcpy(writeBuf + kHeaderLen, mData, mLength);

    int actual;

    actual = pPipe->write(writeBuf, mLength + kHeaderLen);
    if (actual != mLength + kHeaderLen) {
        fprintf(stderr,
            "Message::write failed writing message body (%d of %d bytes)\n",
            actual, mLength + kHeaderLen);
        goto bail;
    }

    result = true;

bail:
    if (writeBuf != tmpBuf)
        delete[] writeBuf;
    return result;
}


/*
 * ===========================================================================
 *      MessageStream
 * ===========================================================================
 */

/*
 * Get ready to go.
 */
bool MessageStream::init(Pipe* readPipe, Pipe* writePipe, bool initiateHello)
{
    assert(mReadPipe == NULL && mWritePipe == NULL);    // only once

    /*
     * Swap "hello" messages.
     *
     * In a more robust implementation, this would include version numbers
     * and capability flags.
     */
    if (initiateHello) {
        long data = kHelloMsg;
        Message msg;

        /* send hello */
        msg.setRaw((unsigned char*) &data, sizeof(data),
            Message::kCleanupNoDelete);
        if (!msg.write(writePipe)) {
            LOG(LOG_WARN, "", "hello write failed in stream init\n");
            return false;
        }

        LOG(LOG_DEBUG, "", "waiting for peer to ack my hello\n");

        /* wait for the ack */
        if (!msg.read(readPipe, true)) {
            LOG(LOG_WARN, "", "hello ack read failed in stream init\n");
            return false;
        }

        const long* pAck;
        pAck = (const long*) msg.getData();
        if (pAck == NULL || *pAck != kHelloAckMsg) {
            LOG(LOG_WARN, "", "hello ack was bad\n");
            return false;
        }
    } else {
        long data = kHelloAckMsg;
        Message msg;

        LOG(LOG_DEBUG, "", "waiting for hello from peer\n");

        /* wait for the hello */
        if (!msg.read(readPipe, true)) {
            LOG(LOG_WARN, "", "hello read failed in stream init\n");
            return false;
        }

        const long* pAck;
        pAck = (const long*) msg.getData();
        if (pAck == NULL || *pAck != kHelloMsg) {
            LOG(LOG_WARN, "", "hello was bad\n");
            return false;
        }

        /* send hello ack */
        msg.setRaw((unsigned char*) &data, sizeof(data),
            Message::kCleanupNoDelete);
        if (!msg.write(writePipe)) {
            LOG(LOG_WARN, "", "hello ack write failed in stream init\n");
            return false;
        }
    }

    /* success, set up our local stuff */
    mReadPipe = readPipe;
    mWritePipe = writePipe;

    //LOG(LOG_DEBUG, "", "init success\n");

    return true;
}

