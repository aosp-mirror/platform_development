/*
* Copyright (C) 2011 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#include <errno.h>
#include <stdlib.h>
#include "iolooper.h"
#include "sockets.h"

/* An implementation of iolooper.h based on Unix select() */
#ifdef _WIN32
#  include <winsock2.h>
#  include <time.h>
#else
#  include <sys/types.h>
#  include <sys/select.h>
#  include <sys/time.h>
#endif

struct IoLooper {
    fd_set   reads[1];
    fd_set   writes[1];
    fd_set   reads_result[1];
    fd_set   writes_result[1];
    int      max_fd;
    int      max_fd_valid;
};

IoLooper*
iolooper_new(void)
{
    IoLooper*  iol = malloc(sizeof(*iol));
    iolooper_reset(iol);
    return iol;
}

void
iolooper_free( IoLooper*  iol )
{
    free(iol);
}

void
iolooper_reset( IoLooper*  iol )
{
    FD_ZERO(iol->reads);
    FD_ZERO(iol->writes);
    iol->max_fd = -1;
    iol->max_fd_valid = 1;
}

static void
iolooper_add_fd( IoLooper*  iol, int fd )
{
    if (iol->max_fd_valid && fd > iol->max_fd) {
        iol->max_fd = fd;
    }
}

static void
iolooper_del_fd( IoLooper*  iol, int fd )
{
    if (iol->max_fd_valid && fd == iol->max_fd)
        iol->max_fd_valid = 0;
}

void
iolooper_modify( IoLooper* iol, int fd, int oldflags, int newflags )
{
    if (fd < 0)
        return;

    int changed = oldflags ^ newflags;

    if ((changed & IOLOOPER_READ) != 0) {
        if ((newflags & IOLOOPER_READ) != 0)
            iolooper_add_read(iol, fd);
        else
            iolooper_del_read(iol, fd);
    }
    if ((changed & IOLOOPER_WRITE) != 0) {
        if ((newflags & IOLOOPER_WRITE) != 0)
            iolooper_add_write(iol, fd);
        else
            iolooper_del_write(iol, fd);
    }
}


static int
iolooper_fd_count( IoLooper*  iol )
{
    int  max_fd = iol->max_fd;
    int  fd;

    if (iol->max_fd_valid)
        return max_fd + 1;

    /* recompute max fd */
    for (fd = 0; fd < FD_SETSIZE; fd++) {
        if (!FD_ISSET(fd, iol->reads) && !FD_ISSET(fd, iol->writes))
            continue;

        max_fd = fd;
    }
    iol->max_fd       = max_fd;
    iol->max_fd_valid = 1;

    return max_fd + 1;
}

void
iolooper_add_read( IoLooper*  iol, int  fd )
{
    if (fd >= 0) {
        iolooper_add_fd(iol, fd);
        FD_SET(fd, iol->reads);
    }
}

void
iolooper_add_write( IoLooper*  iol, int  fd )
{
    if (fd >= 0) {
        iolooper_add_fd(iol, fd);
        FD_SET(fd, iol->writes);
    }
}

void
iolooper_del_read( IoLooper*  iol, int  fd )
{
    if (fd >= 0) {
        iolooper_del_fd(iol, fd);
        FD_CLR(fd, iol->reads);
    }
}

void
iolooper_del_write( IoLooper*  iol, int  fd )
{
    if (fd >= 0) {
        iolooper_del_fd(iol, fd);
        FD_CLR(fd, iol->writes);
    }
}

int
iolooper_poll( IoLooper*  iol )
{
    int     count = iolooper_fd_count(iol);
    int     ret;
    fd_set  errs;

    if (count == 0)
        return 0;

    FD_ZERO(&errs);

    do {
        struct timeval  tv;

        tv.tv_sec = tv.tv_usec = 0;

        iol->reads_result[0]  = iol->reads[0];
        iol->writes_result[0] = iol->writes[0];

        ret = select( count, iol->reads_result, iol->writes_result, &errs, &tv);
    } while (ret < 0 && errno == EINTR);

    return ret;
}

int
iolooper_wait( IoLooper*  iol, int64_t  duration )
{
    int     count = iolooper_fd_count(iol);
    int     ret;
    fd_set  errs;
    struct timeval tm0, *tm = NULL;

    if (count == 0)
        return 0;

    if (duration < 0)
        tm = NULL;
    else {
        tm = &tm0;
        tm->tv_sec  = duration / 1000;
        tm->tv_usec = (duration - 1000*tm->tv_sec) * 1000;
    }

    FD_ZERO(&errs);

    do {
        iol->reads_result[0]  = iol->reads[0];
        iol->writes_result[0] = iol->writes[0];

        ret = select( count, iol->reads_result, iol->writes_result, &errs, tm);
        if (ret == 0) {
            // Indicates timeout
            errno = ETIMEDOUT;
        }
    } while (ret < 0 && errno == EINTR);

    return ret;
}


int
iolooper_is_read( IoLooper*  iol, int  fd )
{
    return FD_ISSET(fd, iol->reads_result);
}

int
iolooper_is_write( IoLooper*  iol, int  fd )
{
    return FD_ISSET(fd, iol->writes_result);
}

int
iolooper_has_operations( IoLooper* iol )
{
    return iolooper_fd_count(iol) > 0;
}

int64_t
iolooper_now(void)
{
#ifdef _WIN32
    FILETIME now;
    int64_t now_100ns;

    GetSystemTimeAsFileTime(&now);

    /* Get the time as hundreds of nanosecond intervals since
       12:00 AM January 1t 1601 UTC. We don't really need
       to compute the value relative to the Posix epoch */
    now_100ns = ((int64_t)now.dwHighDateTime << 32) | now.dwLowDateTime;

    /* 100 ns == 0.1 us == 0.0001 ms */
    return now_100ns / 10000LL;

#else /* !_WIN32 */
    struct timeval time_now;
    return gettimeofday(&time_now, NULL) ? -1 : (int64_t)time_now.tv_sec * 1000LL +
                                                time_now.tv_usec / 1000;
#endif /* !_WIN32 */
}

int
iolooper_wait_absolute(IoLooper* iol, int64_t deadline)
{
    int64_t timeout = deadline - iolooper_now();

    /* If the deadline has passed, set the timeout to 0, this allows us
     * to poll the file descriptor nonetheless */
    if (timeout < 0)
        timeout = 0;

    return iolooper_wait(iol, timeout);
}
