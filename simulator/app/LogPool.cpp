//
// Copyright 2005 The Android Open Source Project
//
// Hold a collection of log messages, limiting ourselves to a certain
// fixed maximum amount of memory.
//
#include "LogPool.h"
#include <assert.h>


/*
 * Add a message at the head of the pool.
 */
void LogPool::Add(LogMessage* pLogMessage)
{
    pLogMessage->Acquire();     // bump up the ref count

    assert(pLogMessage->GetPrev() == NULL);
    assert(pLogMessage->GetNext() == NULL);

    if (mpHead == NULL) {
        assert(mpTail == NULL);
        mpTail = mpHead = pLogMessage;
    } else {
        assert(mpHead->GetPrev() == NULL);
        mpHead->SetPrev(pLogMessage);
        pLogMessage->SetNext(mpHead);
        mpHead = pLogMessage;
    }

    /* update the pool size, and remove old entries if necessary */
    mCurrentSize += pLogMessage->GetFootprint();

    while (mCurrentSize > mMaxSize)
        RemoveOldest();
}

/*
 * Remove the oldest message (from the tail of the list).
 */
void LogPool::RemoveOldest(void)
{
    LogMessage* pPrev;

    if (mpTail == NULL) {
        fprintf(stderr, "HEY: nothing left to remove (cur=%ld)\n",
            mCurrentSize);
        assert(false);
        return;
    }

    if (mpTail == mpBookmark)
        mpBookmark = NULL;

    //printf("--- removing oldest, size %ld->%ld (%s)\n",
    //    mCurrentSize, mCurrentSize - mpTail->GetFootprint(),mpTail->GetMsg());
    mCurrentSize -= mpTail->GetFootprint();

    pPrev = mpTail->GetPrev();
    mpTail->Release();
    mpTail = pPrev;
    if (mpTail == NULL) {
        //printf("--- pool is now empty (size=%ld)\n", mCurrentSize);
        mpHead = NULL;
    } else {
        mpTail->SetNext(NULL);
    }
}

/*
 * Resize the log pool.
 */
void LogPool::Resize(long maxSize)
{
    assert(maxSize >= 0);

    mMaxSize = maxSize;
    while (mCurrentSize > mMaxSize)
        RemoveOldest();
}

/*
 * Remove all entries.
 */
void LogPool::Clear(void)
{
    while (mpTail != NULL)
        RemoveOldest();
}

