//
// Copyright 2005 The Android Open Source Project
//
// Pool of log messages.  Not thread safe -- operations on the log pool
// should only happen in the main UI thread.
//
#ifndef _SIM_LOG_POOL_H
#define _SIM_LOG_POOL_H

#include "LogMessage.h"

/*
 * This contains the pool of log messages.  The messages themselves are
 * allocated individually and reference counted.  We add new messages to
 * the head and, when the total "footprint" exceeds our stated max, we
 * delete one or more from the tail.
 *
 * To support pause/resume, we allow a "bookmark" to be set.  This is
 * just a pointer to a message in the pool.  If the bookmarked message
 * is deleted, we discard the bookmark.
 */
class LogPool {
public:
    LogPool(void)
        : mpHead(NULL), mpTail(NULL), mpBookmark(NULL),
          mCurrentSize(0), mMaxSize(10240)
        {}
    ~LogPool(void) { Clear(); }

    void Clear(void);

    /* add a new message to the pool */
    void Add(LogMessage* pLogMessage);

    /* resize the pool, removing excess messages */
    void Resize(long maxSize);

    /* return the current limit, in bytes */
    long GetMaxSize(void) const { return mMaxSize; }

    LogMessage* GetHead(void) const { return mpHead; }

    void SetBookmark(void) { mpBookmark = mpHead; }
    LogMessage* GetBookmark(void) const { return mpBookmark; }

private:
    void RemoveOldest(void);

    LogMessage*     mpHead;
    LogMessage*     mpTail;
    LogMessage*     mpBookmark;
    long            mCurrentSize;       // current size, in bytes
    long            mMaxSize;           // maximum size, in bytes
};

#endif // _SIM_LOG_POOL_H
