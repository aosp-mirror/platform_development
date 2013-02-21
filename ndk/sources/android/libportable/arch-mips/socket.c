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
#include <portability.h>

#define PORTABLE_TAG "socket_portable"
#include <log_portable.h>


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


/*
 * Portable to Native socktype mapper.
 */
static inline int socktype_pton(int portable_type)
{
    int native_type = 0;

    ALOGV("%s(portable_type:0x%x) {", __func__, portable_type);

    if (portable_type & SOCK_NONBLOCK_PORTABLE) {
        native_type |= SOCK_NONBLOCK;
        portable_type &= ~SOCK_NONBLOCK_PORTABLE;
    }

#if defined(SOCK_CLOEXEC_PORTABLE) && defined(SOCK_CLOEXEC)
    if (portable_type & SOCK_CLOEXEC_PORTABLE) {
        native_type |= SOCK_CLOEXEC;
        portable_type &= ~SOCK_CLOEXEC_PORTABLE;
    }
#endif

    switch (portable_type) {
    case SOCK_STREAM_PORTABLE: native_type |= SOCK_STREAM; break;
    case SOCK_DGRAM_PORTABLE: native_type |= SOCK_DGRAM; break;
    case SOCK_RAW_PORTABLE: native_type |= SOCK_RAW; break;
    case SOCK_RDM_PORTABLE: native_type |= SOCK_RDM; break;
    case SOCK_SEQPACKET_PORTABLE: native_type |= SOCK_SEQPACKET; break;
    case SOCK_PACKET_PORTABLE: native_type |= SOCK_PACKET; break;
    default:
        ALOGE("%s: case default: native_type:0x%x |= portable_type:0x%x:[UNKNOWN!];", __func__,
                                 native_type,        portable_type);

        native_type |= portable_type;
        break;
    }
    ALOGV("%s: return(native_type:%d); }", __func__, native_type);
    return native_type;
}


/*
 * Native to Portable socktype mapper.
 */
static inline int socktype_ntop(int native_type)
{
    int portable_type = 0;

    ALOGV("%s(native_type:0x%x) {", __func__, native_type);

    if (native_type & SOCK_NONBLOCK) {
        portable_type |= SOCK_NONBLOCK_PORTABLE;
        native_type &= ~SOCK_NONBLOCK;
    }

#if defined(SOCK_CLOEXEC_PORTABLE) && defined(SOCK_CLOEXEC)
    if (native_type & SOCK_CLOEXEC) {
        portable_type |= SOCK_CLOEXEC_PORTABLE;
        native_type &= ~SOCK_CLOEXEC;
    }
#endif

    switch (native_type) {
    case SOCK_STREAM: portable_type |= SOCK_STREAM_PORTABLE; break;
    case SOCK_DGRAM: portable_type |= SOCK_DGRAM_PORTABLE; break;
    case SOCK_RAW: portable_type |= SOCK_RAW_PORTABLE; break;
    case SOCK_RDM: portable_type |= SOCK_RDM_PORTABLE; break;
    case SOCK_SEQPACKET: portable_type |= SOCK_SEQPACKET_PORTABLE; break;
    case SOCK_PACKET: portable_type |= SOCK_PACKET_PORTABLE; break;
    default:
        portable_type |= native_type;
        ALOGE("%s: case default: portable_type:0x%x |= native_type:0x%x:[UNKNOWN!];", __func__,
                                 portable_type,        native_type);
    }
    ALOGV("%s: return(portable_type:%d); }", __func__, portable_type);
    return portable_type;
}


extern int socket(int, int, int);

int socket_portable(int domain, int type, int protocol) {
    int rv;

    ALOGV(" ");
    ALOGV("%s(domain:%d, type:%d, protocol:%d) {", __func__,
              domain,    type,    protocol);

    rv = socket(domain, socktype_pton(type), protocol);

    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}


int socketpair_portable(int domain, int type, int protocol, int sv[2]) {
    int rv;

    ALOGV(" ");
    ALOGV("%s(domain:%d, type:%d, protocol:%d, sv[2]:%p) {", __func__,
              domain,    type,    protocol,    sv);

    rv = socketpair(domain, socktype_pton(type), protocol, sv);

    if ((rv != 0) || invalid_pointer(sv)) {
        ALOGV("%s: return(rv:%d); }", __func__,
                          rv);
    } else {
        ALOGV("%s: return(rv:%d); sv[0]:%d; sv[1]:%d;}", __func__,
                          rv,     sv[0],    sv[1]);
    }
    return rv;
}
