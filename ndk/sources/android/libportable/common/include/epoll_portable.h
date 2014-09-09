/*
 * Copyright 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _EPOLL_PORTABLE_H_
#define _EPOLL_PORTABLE_H_

#include <portability.h>
#include <signal.h>
#include <stdint.h>
#include <sys/epoll.h>

struct epoll_event_portable
{
  uint32_t events;
  uint8_t __padding[4];
  epoll_data_t data;
};


int WRAP(epoll_ctl)(int epfd, int op, int fd, struct epoll_event_portable *event)
{
    struct epoll_event machine_epoll_event;

    machine_epoll_event.events = event->events;
    machine_epoll_event.data = event->data;

    return REAL(epoll_ctl)(epfd, op, fd, &machine_epoll_event);
}

int WRAP(epoll_wait)(int epfd, struct epoll_event_portable *events, int max, int timeout)
{
    struct epoll_event machine_epoll_event;
    int ret = REAL(epoll_wait)(epfd, &machine_epoll_event, max, timeout);

    events->events = machine_epoll_event.events;
    events->data = machine_epoll_event.data;

    return ret;
}

int WRAP(epoll_pwait)(int fd, struct epoll_event_portable* events, int max_events, int timeout, const sigset_t* ss)
{
    struct epoll_event machine_epoll_event;
    int ret = REAL(epoll_pwait)(fd, &machine_epoll_event, max_events, timeout, ss);

    events->events = machine_epoll_event.events;
    events->data = machine_epoll_event.data;

    return ret;
}

#endif /* _EPOLL_PORTABLE_H */
