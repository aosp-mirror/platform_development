//
// Copyright 2005 The Android Open Source Project
//
// Class to hold a single log message.  Not thread safe.
//
#ifndef _SIM_LOG_MESSAGE_H
#define _SIM_LOG_MESSAGE_H

#include "utils.h"
#include "LogBundle.h"

/*
 * Hold a single log message.
 *
 * To reduce malloc strain we could over-allocate the object and tuck the
 * message text into the object storage.  On this off chance this becomes
 * important, the implementation keeps its constructor private.
 */
class LogMessage {
public:
    ~LogMessage(void);

    static LogMessage* Create(const android_LogBundle* pBundle);
    static LogMessage* Create(const char* msg);

    /* the total length of text added to the text ctrl */
    int GetTextCtrlLen(void) const { return mTextCtrlLen; }
    void SetTextCtrlLen(int len) { mTextCtrlLen = len; }

    /* log pool */
    LogMessage* GetPrev(void) const { return mpPrev; }
    void SetPrev(LogMessage* pPrev) { mpPrev = pPrev; }
    LogMessage* GetNext(void) const { return mpNext; }
    void SetNext(LogMessage* pNext) { mpNext = pNext; }
    int GetFootprint(void) const { return mFootprint; }

    /* message contents */
    time_t GetWhen(void) const { return mWhen; }
    android_LogPriority GetPriority(void) const { return mPriority; }
    pid_t GetPid(void) const { return mPid; }
    const char* GetTag(void) const { return mTag; }
    const char* GetMsg(void) const { return mMsg; }

    bool GetInternal(void) const { return mInternal; }

    void Acquire(void) { mRefCnt++; }
    void Release(void) {
        if (!--mRefCnt)
            delete this;
    }

private:
    LogMessage(void);
    LogMessage(const LogMessage& src);              // not implemented
    LogMessage& operator=(const LogMessage& src);   // not implemented

    /* log message contents */
    time_t          mWhen;
    android_LogPriority mPriority;
    pid_t           mPid;
    char*           mTag;
    char*           mMsg;

    /* additional goodies */
    int             mRefCnt;        // reference count
    bool            mInternal;      // message generated internally by us?
    int             mFootprint;     // approx. size of this object in memory
    int             mTextCtrlLen;   // #of characters req'd in text ctrl
    LogMessage*     mpPrev;         // link to previous item in log pool
    LogMessage*     mpNext;         // link to next item in log pool
};

#endif // _SIM_LOG_MESSAGE_H
