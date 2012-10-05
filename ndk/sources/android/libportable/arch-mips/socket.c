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

#include <unistd.h>
#include <sys/socket.h>
#include <fcntl.h>

#include <socket_portable.h>
#include <fcntl_portable.h>


#if SOCK_STREAM==SOCK_STREAM_PORTABLE
#error Bad build environment
#endif

/* LTP defaults to using O_NONBLOCK if SOCK_NONBLOCK is not defined. */
#ifndef SOCK_NONBLOCK_PORTABLE
# define SOCK_NONBLOCK_PORTABLE O_NONBLOCK_PORTABLE
#endif
#ifndef SOCK_NONBLOCK
# define SOCK_NONBLOCK O_NONBLOCK
#endif

/* Current NDK headers do not define SOCK_CLOEXEC or O_CLOEXEC */
#if !defined(SOCK_CLOEXEC_PORTABLE) && defined(O_CLOEXEC_PORTABLE)
# define SOCK_CLOEXEC_PORTABLE O_CLOEXEC_PORTABLE
#endif
#if !defined(SOCK_CLOEXEC) && defined(O_CLOEXEC)
# define SOCK_CLOEXEC O_CLOEXEC
#endif

static inline int mips_change_type(int type)
{
    int mipstype = 0;

    if (type & SOCK_NONBLOCK_PORTABLE) {
        mipstype |= SOCK_NONBLOCK;
        type &= ~SOCK_NONBLOCK_PORTABLE;
    }

#if defined(SOCK_CLOEXEC_PORTABLE) && defined(SOCK_CLOEXEC)
    if (type & SOCK_CLOEXEC_PORTABLE) {
        mipstype |= SOCK_CLOEXEC;
        type &= ~SOCK_CLOEXEC_PORTABLE;
    }
#endif

    switch (type) {
    case SOCK_STREAM_PORTABLE: mipstype |= SOCK_STREAM; break;
    case SOCK_DGRAM_PORTABLE: mipstype |= SOCK_DGRAM; break;
    case SOCK_RAW_PORTABLE: mipstype |= SOCK_RAW; break;
    case SOCK_RDM_PORTABLE: mipstype |= SOCK_RDM; break;
    case SOCK_SEQPACKET_PORTABLE: mipstype |= SOCK_SEQPACKET; break;
    case SOCK_PACKET_PORTABLE: mipstype |= SOCK_PACKET; break;
    default: mipstype |= type;
    }
    return mipstype;
}

extern int socket(int, int, int);

int socket_portable(int domain, int type, int protocol) {
    return socket(domain, mips_change_type(type), protocol);
}

int socketpair_portable(int domain, int type, int protocol, int sv[2]) {
    return socketpair(domain, mips_change_type(type), protocol, sv);
}
