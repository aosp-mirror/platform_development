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
#include <unistd.h>
#include <sys/socket.h>
#include <fcntl.h>
#include <netdb.h>

#include <socket_portable.h>
#include <fcntl_portable.h>
#include <netdb_portable.h>
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


extern int REAL(socket)(int, int, int);

int WRAP(socket)(int domain, int type, int protocol) {
    int rv;

    ALOGV(" ");
    ALOGV("%s(domain:%d, type:%d, protocol:%d) {", __func__,
              domain,    type,    protocol);

    rv = REAL(socket)(domain, socktype_pton(type), protocol);

    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}


int WRAP(socketpair)(int domain, int type, int protocol, int sv[2]) {
    int rv;

    ALOGV(" ");
    ALOGV("%s(domain:%d, type:%d, protocol:%d, sv[2]:%p) {", __func__,
              domain,    type,    protocol,    sv);

    rv = REAL(socketpair)(domain, socktype_pton(type), protocol, sv);

    if ((rv != 0) || invalid_pointer(sv)) {
        ALOGV("%s: return(rv:%d); }", __func__,
                          rv);
    } else {
        ALOGV("%s: return(rv:%d); sv[0]:%d; sv[1]:%d;}", __func__,
                          rv,     sv[0],    sv[1]);
    }
    return rv;
}

#define PRINT_ADDRINFO(p) {                                                                      \
    ALOGV("%s: p:%p->{ai_flags:%d, ai_family:%d, ai_socktype:%d, ai_protocol:%d, ...", __func__, \
               p,  p->ai_flags, p->ai_family, p->ai_socktype, p->ai_protocol);                   \
                                                                                                 \
    ALOGV("%s: p:%p->{... ai_addrlen:%d, ai_addr:%p, ai_canonname:%p, p->ai_next:%p);", __func__,\
               p,      p->ai_addrlen, p->ai_addr, p->ai_canonname, p->ai_next);                  \
}

/*
 * Returns a list of portable addrinfo structures that are
 * later made free with a call to the portable version of
 * freeaddrinfo(); which is written below this function.
 */
int WRAP(getaddrinfo)(const char *node, const char *service,
                 struct addrinfo_portable *portable_hints,
                 struct addrinfo_portable **portable_results)
{
    int rv;
    struct addrinfo *native_hints;
    struct addrinfo **native_results, *rp;
    int saved_portable_socktype;

    ALOGV(" ");
    ALOGV("%s(node:%p, service:%p, portable_hints:%p, portable_results:%p) {", __func__,
              node,    service,    portable_hints,    portable_results);

    PRINT_ADDRINFO(portable_hints);

    /*
     * The only part of the addrinfo structure that needs to be modified
     * between ARM and MIPS is the socktype;
     */
    ASSERT(sizeof(struct addrinfo_portable) == sizeof(struct addrinfo));
    native_hints = ((struct addrinfo *) portable_hints);
    if (native_hints != NULL) {
        saved_portable_socktype = portable_hints->ai_socktype;
        native_hints->ai_socktype = socktype_pton(saved_portable_socktype);
    }
    ASSERT(portable_results != NULL);
    native_results = (struct addrinfo **) portable_results;

    rv = REAL(getaddrinfo)(node, service, native_hints, native_results);

    if (native_hints != NULL) {
        portable_hints->ai_socktype = saved_portable_socktype;
    }


    /*
     * Map socktypes in the return list of addrinfo structures from native to portable.
     * Assuming getaddrinfo() has left structure writeable and the list is generated
     * on each call. This seems to be true when looking at the man page and the code
     * at:
     *          ./bionic/libc/netbsd/net/getaddrinfo.c
     */
    for (rp = *native_results; rp != NULL; rp = rp->ai_next) {
        PRINT_ADDRINFO(rp);
        rp->ai_socktype = socktype_ntop(rp->ai_socktype);
    }
    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}


/*
 * Free the results list returned from a previous call
 * to the portable version of getaddrinfo().
 */
void WRAP(freeaddrinfo)(struct addrinfo_portable *portable_results)
{
    struct addrinfo *native_results, *rp;

    ALOGV(" ");
    ALOGV("%s(portable_results:%p) {", __func__, portable_results);

    PRINT_ADDRINFO(portable_results);

    /*
     * The only part of each addrinfo structure that needs to be modified
     * between ARM and MIPS is the socktype;
     *
     * Map socktypes in the return list of iportable addrinfo structures back to native.
     * Again, assuming getaddrinfo() has left structure writeable and the list is generated
     * on each call. This seems to be true when looking at the man page and the code.
     */
    ASSERT(sizeof(struct addrinfo_portable) == sizeof(struct addrinfo));
    native_results = ((struct addrinfo *) portable_results);
    for (rp = native_results; rp != NULL; rp = rp->ai_next) {
        PRINT_ADDRINFO(rp);
        rp->ai_socktype = socktype_pton(rp->ai_socktype);       /* Likely not really necessary */
    }
    REAL(freeaddrinfo)(native_results);

    ALOGV("%s: return; }", __func__);
    return;
}
