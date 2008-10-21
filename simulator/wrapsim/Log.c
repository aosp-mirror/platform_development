/*
 * Copyright 2007 The Android Open Source Project
 *
 * Debug-logging code.
 */
#include "Common.h"

#include <stdio.h>
#include <stdarg.h>
#include <time.h>

/*
 * Write a message to our private log file.  This is a little awkward since
 * some or all of the system calls we want to use are being intercepted.
 */
void wsLog(const char* format, ...)
{
#if defined(HAVE_LOCALTIME_R)
    struct tm tmBuf;
#endif
    struct tm* ptm;
    time_t now;
    char timeBuf[32];
    char prefixBuf[64];
    int prefixLen;
    char msgBuf[256];
    int msgLen;

    if (gWrapSim.logFd < 0)
        return;

    /*
     * Create a prefix with a timestamp.
     */
    now = time(NULL);
#if defined(HAVE_LOCALTIME_R)
    ptm = localtime_r(&now, &tmBuf);
#else
    ptm = localtime(&now);
#endif
    //strftime(timeBuf, sizeof(timeBuf), "%m-%d %H:%M:%S", ptm);
    strftime(timeBuf, sizeof(timeBuf), "%H:%M:%S", ptm);

    prefixLen = snprintf(prefixBuf, sizeof(prefixBuf), "%s %5d ",
        timeBuf, (int) getpid());

    /*
     * Format the message into a buffer.
     */
    va_list args;

    va_start(args, format);
    msgLen = vsnprintf(msgBuf, sizeof(msgBuf), format, args);
    va_end(args);

    /* if we overflowed, trim and annotate */
    if (msgLen >= (int) sizeof(msgBuf)) {
        msgBuf[sizeof(msgBuf)-2] = '!';
        msgBuf[sizeof(msgBuf)-1] = '\n';
        msgLen = sizeof(msgBuf);
    }

    /*
     * Write the whole thing in one shot.  The log file was opened with
     * O_APPEND so we don't have to worry about clashes.
     */
    struct iovec logVec[2];
    logVec[0].iov_base = prefixBuf;
    logVec[0].iov_len = prefixLen;
    logVec[1].iov_base = msgBuf;
    logVec[1].iov_len = msgLen;
    (void) _ws_writev(gWrapSim.logFd, logVec, 2);
}

