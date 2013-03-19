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
#include <sys/types.h>
#include <sys/socket.h>
#include <socket_portable.h>

#if SOL_SOCKET_PORTABLE==SOL_SOCKET
#error Build environment
#endif

static inline int mips_change_level(int level)
{
    switch (level) {
    case SOL_SOCKET_PORTABLE:
        level = SOL_SOCKET;
        break;
    }
    return level;
}


static inline int mips_change_optname(int optname)
{
    switch (optname) {
    case SO_DEBUG_PORTABLE:
        return SO_DEBUG;
    case SO_REUSEADDR_PORTABLE:
        return SO_REUSEADDR;
    case SO_TYPE_PORTABLE:
        return SO_TYPE;
    case SO_ERROR_PORTABLE:
        return SO_ERROR;
    case SO_DONTROUTE_PORTABLE:
        return SO_DONTROUTE;
    case SO_BROADCAST_PORTABLE:
        return SO_BROADCAST;
    case SO_SNDBUF_PORTABLE:
        return SO_SNDBUF;
    case SO_RCVBUF_PORTABLE:
        return SO_RCVBUF;
    case SO_SNDBUFFORCE_PORTABLE:
        return SO_SNDBUFFORCE;
    case SO_RCVBUFFORCE_PORTABLE:
        return SO_RCVBUFFORCE;
    case SO_KEEPALIVE_PORTABLE:
        return SO_KEEPALIVE;
    case SO_OOBINLINE_PORTABLE:
        return SO_OOBINLINE;
    case SO_NO_CHECK_PORTABLE:
        return SO_NO_CHECK;
    case SO_PRIORITY_PORTABLE:
        return SO_PRIORITY;
    case SO_LINGER_PORTABLE:
        return SO_LINGER;
    case SO_BSDCOMPAT_PORTABLE:
        return SO_BSDCOMPAT;
    case SO_PASSCRED_PORTABLE:
        return SO_PASSCRED;
    case SO_PEERCRED_PORTABLE:
        return SO_PEERCRED;
    case SO_RCVLOWAT_PORTABLE:
        return SO_RCVLOWAT;
    case SO_SNDLOWAT_PORTABLE:
        return SO_SNDLOWAT;
    case SO_RCVTIMEO_PORTABLE:
        return SO_RCVTIMEO;
    case SO_SNDTIMEO_PORTABLE:
        return SO_SNDTIMEO;
    case SO_SECURITY_AUTHENTICATION_PORTABLE:
        return SO_SECURITY_AUTHENTICATION;
    case SO_SECURITY_ENCRYPTION_TRANSPORT_PORTABLE:
        return SO_SECURITY_ENCRYPTION_TRANSPORT;
    case SO_SECURITY_ENCRYPTION_NETWORK_PORTABLE:
        return SO_SECURITY_ENCRYPTION_NETWORK;
    case SO_BINDTODEVICE_PORTABLE:
        return SO_BINDTODEVICE;
    case SO_ATTACH_FILTER_PORTABLE:
        return SO_ATTACH_FILTER;
    case SO_DETACH_FILTER_PORTABLE:
        return SO_DETACH_FILTER;
    case SO_PEERNAME_PORTABLE:
        return SO_PEERNAME;
    case SO_TIMESTAMP_PORTABLE:
        return SO_TIMESTAMP;
    case SO_ACCEPTCONN_PORTABLE:
        return SO_ACCEPTCONN;
    case SO_PEERSEC_PORTABLE:
        return SO_PEERSEC;
    case SO_PASSSEC_PORTABLE:
        return SO_PASSSEC;
    }
    return optname;
}

extern int setsockopt(int, int, int, const void *, socklen_t);
int WRAP(setsockopt)(int s, int level, int optname, const void *optval, socklen_t optlen)
{
    return REAL(setsockopt)(s, mips_change_level(level), mips_change_optname(optname), optval, optlen);
}

extern int getsockopt (int, int, int, void *, socklen_t *);
int WRAP(getsockopt)(int s, int level, int optname, void *optval, socklen_t *optlen)
{
    return REAL(getsockopt)(s, mips_change_level(level), mips_change_optname(optname), optval, optlen);
}
