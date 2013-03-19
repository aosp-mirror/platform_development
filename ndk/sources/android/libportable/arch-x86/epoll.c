/*
 * Copyright 2012, The Android Open Source Project
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

#include <portability.h>
#include <sys/epoll.h>
#include <epoll_portable.h>

int WRAP(epoll_ctl)(int epfd, int op, int fd, struct epoll_event_portable *event)
{
    struct epoll_event x86_epoll_event;

    x86_epoll_event.events = event->events;
    x86_epoll_event.data = event->data;

    return REAL(epoll_ctl)(epfd, op, fd, &x86_epoll_event);
}

int WRAP(epoll_wait)(int epfd, struct epoll_event_portable *events, int max, int timeout)
{
    struct epoll_event x86_epoll_event;
    int ret = REAL(epoll_wait)(epfd, &x86_epoll_event, max, timeout);

    events->events = x86_epoll_event.events;
    events->data = x86_epoll_event.data;

    return ret;
}

