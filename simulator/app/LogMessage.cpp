//
// Copyright 2005 The Android Open Source Project
//
// Hold a single log message.
//
#include "LogMessage.h"
#include <assert.h>

/*
 * Constructor.
 *
 * Initializers here aren't necessary, since we can only create one of
 * these through Create(), which touches every field.
 */
LogMessage::LogMessage(void)
{
}

/*
 * Destructor.
 */
LogMessage::~LogMessage(void)
{
    delete[] mTag;
    delete[] mMsg;
}

/*
 * Create a new LogMessage object, and populate it with the contents of
 * "*pBundle".
 */
/*static*/ LogMessage* LogMessage::Create(const android_LogBundle* pBundle)
{
    LogMessage* newMsg = new LogMessage;

    if (newMsg == NULL)
        return NULL;
    assert(pBundle != NULL);

    newMsg->mWhen = pBundle->when;
    newMsg->mPriority = pBundle->priority;
    newMsg->mPid = pBundle->pid;
    newMsg->mTag = android::strdupNew(pBundle->tag);

    size_t len = 0;
    size_t i;
    for (i=0; i<pBundle->msgCount; i++) len += pBundle->msgVec[i].iov_len;
    newMsg->mMsg = new char[len+1];
    char* p = newMsg->mMsg;
    for (i=0; i<pBundle->msgCount; i++) {
        memcpy(p, pBundle->msgVec[i].iov_base, pBundle->msgVec[i].iov_len);
        p += pBundle->msgVec[i].iov_len;
    }
    *p = 0;

    newMsg->mRefCnt = 1;
    newMsg->mInternal = false;
    newMsg->mFootprint = 8 * sizeof(int) + strlen(newMsg->mTag) +
        strlen(newMsg->mMsg) + 4;
    newMsg->mTextCtrlLen = 0;
    newMsg->mpPrev = NULL;
    newMsg->mpNext = NULL;

    return newMsg;
}

/*
 * Create a new LogMessage object, with a simple message in it.
 *
 * Sets "mInternal" so we display it appropriately.
 */
/*static*/ LogMessage* LogMessage::Create(const char* msg)
{
    LogMessage* newMsg;
    android_LogBundle bundle;

    assert(msg != NULL);

    memset(&bundle, 0, sizeof(bundle));
    bundle.when = time(NULL);
    bundle.priority = ANDROID_LOG_ERROR;
    bundle.pid = getpid();
    bundle.tag = "-";
    iovec iov;
    iov.iov_base = (void*)msg;
    iov.iov_len = strlen(msg);
    bundle.msgVec = &iov;
    bundle.msgCount = 1;

    newMsg = Create(&bundle);

    if (newMsg != NULL) {
        newMsg->mInternal = true;
    }

    return newMsg;
}

