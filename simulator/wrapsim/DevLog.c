/*
 * Copyright 2007 The Android Open Source Project
 *
 * Log devices.  We want to filter and display messages, with separate
 * treatment for "debug" and "event" logs.
 *
 * All messages are just dumped to stderr.
 */
#include "Common.h"

#include "cutils/logd.h"

#include <stdlib.h>
#include <string.h>
#include <ctype.h>

#include <fcntl.h>

#define kMaxTagLen  16      /* from utils/Log.cpp */

#define kTagSetSize 16      /* arbitrary */

/* from utils/Log.cpp */
typedef enum {
    FORMAT_OFF = 0,
    FORMAT_BRIEF,
    FORMAT_PROCESS,
    FORMAT_TAG,
    FORMAT_THREAD,
    FORMAT_RAW,
    FORMAT_TIME,
    FORMAT_LONG
} LogFormat;


/*
 * Log driver state.
 */
typedef struct LogState {
    /* nonzero if this is a binary log */
    int     isBinary;

    /* global minimum priority */
    int     globalMinPriority;

    /* output format */
    LogFormat outputFormat;

    /* tags and priorities */
    struct {
        char    tag[kMaxTagLen];
        int     minPriority;
    } tagSet[kTagSetSize];
} LogState;


/*
 * Configure logging based on ANDROID_LOG_TAGS environment variable.  We
 * need to parse a string that looks like
 *
 *  '*:v jdwp:d dalvikvm:d dalvikvm-gc:i dalvikvmi:i
 *
 * The tag (or '*' for the global level) comes first, followed by a colon
 * and a letter indicating the minimum priority level we're expected to log.
 * This can be used to reveal or conceal logs with specific tags.
 *
 * We also want to check ANDROID_PRINTF_LOG to determine how the output
 * will look.
 */
static void configureInitialState(const char* pathName, LogState* logState)
{
    static const int kDevLogLen = 9;    /* strlen("/dev/log/") */

    /* identify binary logs */
    if (strcmp(pathName + kDevLogLen, "events") == 0) {
        logState->isBinary = 1;
    }

    /* global min priority defaults to "info" level */
    logState->globalMinPriority = ANDROID_LOG_INFO;

    /*
     * This is based on the utils/Log.cpp code.
     */
    const char* tags = getenv("ANDROID_LOG_TAGS");
    wsLog("Found ANDROID_LOG_TAGS='%s'\n", tags);
    if (tags != NULL) {
        int entry = 0;

        while (*tags != '\0') {
            char tagName[kMaxTagLen];
            int i, minPrio;

            while (isspace(*tags))
                tags++;

            i = 0;
            while (*tags != '\0' && !isspace(*tags) && *tags != ':' &&
                i < kMaxTagLen)
            {
                tagName[i++] = *tags++;
            }
            if (i == kMaxTagLen) {
                wsLog("ERROR: env tag too long (%d chars max)\n", kMaxTagLen-1);
                return;
            }
            tagName[i] = '\0';

            /* default priority, if there's no ":" part; also zero out '*' */
            minPrio = ANDROID_LOG_VERBOSE;
            if (tagName[0] == '*' && tagName[1] == '\0') {
                minPrio = ANDROID_LOG_DEBUG;
                tagName[0] = '\0';
            }

            if (*tags == ':') {
                tags++;
                if (*tags >= '0' && *tags <= '9') {
                    if (*tags >= ('0' + ANDROID_LOG_SILENT))
                        minPrio = ANDROID_LOG_VERBOSE;
                    else
                        minPrio = *tags - '\0';
                } else {
                    switch (*tags) {
                    case 'v':   minPrio = ANDROID_LOG_VERBOSE;  break;
                    case 'd':   minPrio = ANDROID_LOG_DEBUG;    break;
                    case 'i':   minPrio = ANDROID_LOG_INFO;     break;
                    case 'w':   minPrio = ANDROID_LOG_WARN;     break;
                    case 'e':   minPrio = ANDROID_LOG_ERROR;    break;
                    case 'f':   minPrio = ANDROID_LOG_FATAL;    break;
                    case 's':   minPrio = ANDROID_LOG_SILENT;   break;
                    default:    minPrio = ANDROID_LOG_DEFAULT;  break;
                    }
                }

                tags++;
                if (*tags != '\0' && !isspace(*tags)) {
                    wsLog("ERROR: garbage in tag env; expected whitespace\n");
                    wsLog("       env='%s'\n", tags);
                    return;
                }
            }

            if (tagName[0] == 0) {
                logState->globalMinPriority = minPrio;
                wsLog("+++ global min prio %d\n", logState->globalMinPriority);
            } else {
                logState->tagSet[entry].minPriority = minPrio;
                strcpy(logState->tagSet[entry].tag, tagName);
                wsLog("+++ entry %d: %s:%d\n",
                    entry,
                    logState->tagSet[entry].tag,
                    logState->tagSet[entry].minPriority);
                entry++;
            }
        }
    }


    /*
     * Taken from utils/Log.cpp
     */
    const char* fstr = getenv("ANDROID_PRINTF_LOG");
    LogFormat format;
    if (fstr == NULL) {
        format = FORMAT_BRIEF;
    } else {
        if (strcmp(fstr, "brief") == 0)
            format = FORMAT_BRIEF;
        else if (strcmp(fstr, "process") == 0)
            format = FORMAT_PROCESS;
        else if (strcmp(fstr, "tag") == 0)
            format = FORMAT_PROCESS;
        else if (strcmp(fstr, "thread") == 0)
            format = FORMAT_PROCESS;
        else if (strcmp(fstr, "raw") == 0)
            format = FORMAT_PROCESS;
        else if (strcmp(fstr, "time") == 0)
            format = FORMAT_PROCESS;
        else if (strcmp(fstr, "long") == 0)
            format = FORMAT_PROCESS;
        else
            format = (LogFormat) atoi(fstr);        // really?!
    }

    logState->outputFormat = format;
}

/*
 * Free up the state structure.
 */
static void freeState(LogState* logState)
{
    free(logState);
}

/*
 * Return a human-readable string for the priority level.  Always returns
 * a valid string.
 */
static const char* getPriorityString(int priority)
{
    /* the first character of each string should be unique */
    static const char* priorityStrings[] = {
        "Verbose", "Debug", "Info", "Warn", "Error", "Assert"
    };
    int idx;

    idx = (int) priority - (int) ANDROID_LOG_VERBOSE;
    if (idx < 0 ||
        idx >= (int) (sizeof(priorityStrings) / sizeof(priorityStrings[0])))
        return "?unknown?";
    return priorityStrings[idx];
}

/*
 * Show a log message.  We write it to stderr and send a copy to the
 * simulator front-end for the log window.
 *
 * Taken from utils/Log.cpp.
 */
static void showLog(FakeDev* dev, int logPrio, const char* tag, const char* msg)
{
    LogState* state = (LogState*) dev->state;

#if defined(HAVE_LOCALTIME_R)
    struct tm tmBuf;
#endif
    struct tm* ptm;
    char timeBuf[32];
    char prefixBuf[128], suffixBuf[128];
    char priChar;
    time_t when;
    pid_t pid, tid;

    //wsLog("LOG %d: %s %s", logPrio, tag, msg);
    wsPostLogMessage(logPrio, tag, msg);

    priChar = getPriorityString(logPrio)[0];
    when = time(NULL);
    pid = tid = getpid();       // find gettid()?

    /*
     * Get the current date/time in pretty form
     *
     * It's often useful when examining a log with "less" to jump to
     * a specific point in the file by searching for the date/time stamp.
     * For this reason it's very annoying to have regexp meta characters
     * in the time stamp.  Don't use forward slashes, parenthesis,
     * brackets, asterisks, or other special chars here.
     */
#if defined(HAVE_LOCALTIME_R)
    ptm = localtime_r(&when, &tmBuf);
#else
    ptm = localtime(&when);
#endif
    //strftime(timeBuf, sizeof(timeBuf), "%Y-%m-%d %H:%M:%S", ptm);
    strftime(timeBuf, sizeof(timeBuf), "%m-%d %H:%M:%S", ptm);

    /*
     * Construct a buffer containing the log header and log message.
     */
    size_t prefixLen, suffixLen;

    switch (state->outputFormat) {
    case FORMAT_TAG:
        prefixLen = snprintf(prefixBuf, sizeof(prefixBuf),
            "%c/%-8s: ", priChar, tag);
        strcpy(suffixBuf, "\n"); suffixLen = 1;
        break;
    case FORMAT_PROCESS:
        prefixLen = snprintf(prefixBuf, sizeof(prefixBuf),
            "%c(%5d) ", priChar, pid);
        suffixLen = snprintf(suffixBuf, sizeof(suffixBuf),
            "  (%s)\n", tag);
        break;
    case FORMAT_THREAD:
        prefixLen = snprintf(prefixBuf, sizeof(prefixBuf),
            "%c(%5d:%p) ", priChar, pid, (void*)tid);
        strcpy(suffixBuf, "\n"); suffixLen = 1;
        break;
    case FORMAT_RAW:
        prefixBuf[0] = 0; prefixLen = 0;
        strcpy(suffixBuf, "\n"); suffixLen = 1;
        break;
    case FORMAT_TIME:
        prefixLen = snprintf(prefixBuf, sizeof(prefixBuf),
            "%s %-8s\n\t", timeBuf, tag);
        strcpy(suffixBuf, "\n"); suffixLen = 1;
        break;
    case FORMAT_LONG:
        prefixLen = snprintf(prefixBuf, sizeof(prefixBuf),
            "[ %s %5d:%p %c/%-8s ]\n",
            timeBuf, pid, (void*)tid, priChar, tag);
        strcpy(suffixBuf, "\n\n"); suffixLen = 2;
        break;
    default:
        prefixLen = snprintf(prefixBuf, sizeof(prefixBuf),
            "%c/%-8s(%5d): ", priChar, tag, pid);
        strcpy(suffixBuf, "\n"); suffixLen = 1;
        break;
     }

    /*
     * Figure out how many lines there will be.
     */
    const char* end = msg + strlen(msg);
    size_t numLines = 0;
    const char* p = msg;
    while (p < end) {
        if (*p++ == '\n') numLines++;
    }
    if (p > msg && *(p-1) != '\n') numLines++;
    
    /*
     * Create an array of iovecs large enough to write all of
     * the lines with a prefix and a suffix.
     */
    const size_t INLINE_VECS = 6;
    struct iovec stackVec[INLINE_VECS];
    struct iovec* vec = stackVec;
    
    numLines *= 3;  // 3 iovecs per line.
    if (numLines > INLINE_VECS) {
        vec = (struct iovec*)malloc(sizeof(struct iovec)*numLines);
        if (vec == NULL) {
            msg = "LOG: write failed, no memory";
            numLines = 3;
        }
    }
    
    /*
     * Fill in the iovec pointers.
     */
    p = msg;
    struct iovec* v = vec;
    int totalLen = 0;
    while (p < end) {
        if (prefixLen > 0) {
            v->iov_base = prefixBuf;
            v->iov_len = prefixLen;
            totalLen += prefixLen;
            v++;
        }
        const char* start = p;
        while (p < end && *p != '\n') p++;
        if ((p-start) > 0) {
            v->iov_base = (void*)start;
            v->iov_len = p-start;
            totalLen += p-start;
            v++;
        }
        if (*p == '\n') p++;
        if (suffixLen > 0) {
            v->iov_base = suffixBuf;
            v->iov_len = suffixLen;
            totalLen += suffixLen;
            v++;
        }
    }
    
    /*
     * Write the entire message to the log file with a single writev() call.
     * We need to use this rather than a collection of printf()s on a FILE*
     * because of multi-threading and multi-process issues.
     *
     * If the file was not opened with O_APPEND, this will produce interleaved
     * output when called on the same file from multiple processes.
     *
     * If the file descriptor is actually a network socket, the writev()
     * call may return with a partial write.  Putting the writev() call in
     * a loop can result in interleaved data.  This can be alleviated
     * somewhat by wrapping the writev call in the Mutex.
     */

    for(;;) {
        int cc;

        cc = writev(fileno(stderr), vec, v-vec);
        if (cc == totalLen) break;
        
        if (cc < 0) {
            if(errno == EINTR) continue;
            
                /* can't really log the failure; for now, throw out a stderr */
            fprintf(stderr, "+++ LOG: write failed (errno=%d)\n", errno);
            break;
        } else {
                /* shouldn't happen when writing to file or tty */
            fprintf(stderr, "+++ LOG: write partial (%d of %d)\n", cc, totalLen);
            break;
        }
    }

    /* if we allocated storage for the iovecs, free it */
    if (vec != stackVec)
        free(vec);
}


/*
 * Receive a log message.  We happen to know that "vector" has three parts:
 *
 *  priority (1 byte)
 *  tag (N bytes -- null-terminated ASCII string)
 *  message (N bytes -- null-terminated ASCII string)
 */
static ssize_t writevLog(FakeDev* dev, int fd, const struct iovec* vector,
    int count)
{
    LogState* state = (LogState*) dev->state;
    int ret = 0;

    if (state->isBinary) {
        wsLog("%s: ignoring binary log\n", dev->debugName);
        goto bail;
    }

    if (count != 3) {
        wsLog("%s: writevLog with count=%d not expected\n",
            dev->debugName, count);
        errno = EINVAL;
        return -1;
    }

    /* pull out the three fields */
    int logPrio = *(const char*)vector[0].iov_base;
    const char* tag = (const char*) vector[1].iov_base;
    const char* msg = (const char*) vector[2].iov_base;

    /* see if this log tag is configured */
    int i;
    int minPrio = state->globalMinPriority;
    for (i = 0; i < kTagSetSize; i++) {
        if (state->tagSet[i].minPriority == ANDROID_LOG_UNKNOWN)
            break;      /* reached end of configured values */

        if (strcmp(state->tagSet[i].tag, tag) == 0) {
            //wsLog("MATCH tag '%s'\n", tag);
            minPrio = state->tagSet[i].minPriority;
            break;
        }
    }

    if (logPrio >= minPrio) {
        showLog(dev, logPrio, tag, msg);
    } else {
        //wsLog("+++ NOLOG(%d): %s %s", logPrio, tag, msg);
    }

bail:
    for (i = 0; i < count; i++)
        ret += vector[i].iov_len;
    return ret;
}

/*
 * Free up our state before closing down the fake descriptor.
 */
static int closeLog(FakeDev* dev, int fd)
{
    freeState((LogState*)dev->state);
    dev->state = NULL;
    return 0;
}

/*
 * Open a log output device.
 */
FakeDev* wsOpenDevLog(const char* pathName, int flags)
{
    FakeDev* newDev = wsCreateFakeDev(pathName);
    if (newDev != NULL) {
        newDev->writev = writevLog;
        newDev->close = closeLog;

        LogState* logState = calloc(1, sizeof(LogState));

        configureInitialState(pathName, logState);
        newDev->state = logState;
    }

    return newDev;
}

