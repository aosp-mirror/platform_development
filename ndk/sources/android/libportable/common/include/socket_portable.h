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

#ifndef _SOCKET_PORTABLE_H_
#define _SOCKET_PORTABLE_H_

/* Derived from development/ndk/platforms/android-3/include/sys/socket.h */
#define SOCK_STREAM_PORTABLE    1
#define SOCK_DGRAM_PORTABLE     2
#define SOCK_RAW_PORTABLE       3
#define SOCK_RDM_PORTABLE       4
#define SOCK_SEQPACKET_PORTABLE 5
#define SOCK_PACKET_PORTABLE    10


/* Derived from development/ndk/platforms/android-3/arch-arm/include/asm/socket.h */

#define SOL_SOCKET_PORTABLE     1

#define SO_DEBUG_PORTABLE       1
#define SO_REUSEADDR_PORTABLE   2
#define SO_TYPE_PORTABLE        3
#define SO_ERROR_PORTABLE       4
#define SO_DONTROUTE_PORTABLE   5
#define SO_BROADCAST_PORTABLE   6
#define SO_SNDBUF_PORTABLE      7
#define SO_RCVBUF_PORTABLE      8
#define SO_SNDBUFFORCE_PORTABLE 32
#define SO_RCVBUFFORCE_PORTABLE 33
#define SO_KEEPALIVE_PORTABLE   9
#define SO_OOBINLINE_PORTABLE   10
#define SO_NO_CHECK_PORTABLE    11
#define SO_PRIORITY_PORTABLE    12
#define SO_LINGER_PORTABLE      13
#define SO_BSDCOMPAT_PORTABLE   14

#define SO_PASSCRED_PORTABLE    16
#define SO_PEERCRED_PORTABLE    17
#define SO_RCVLOWAT_PORTABLE    18
#define SO_SNDLOWAT_PORTABLE    19
#define SO_RCVTIMEO_PORTABLE    20
#define SO_SNDTIMEO_PORTABLE    21

#define SO_SECURITY_AUTHENTICATION_PORTABLE         22
#define SO_SECURITY_ENCRYPTION_TRANSPORT_PORTABLE   23
#define SO_SECURITY_ENCRYPTION_NETWORK_PORTABLE     24

#define SO_BINDTODEVICE_PORTABLE    25

#define SO_ATTACH_FILTER_PORTABLE   26
#define SO_DETACH_FILTER_PORTABLE   27

#define SO_PEERNAME_PORTABLE        28
#define SO_TIMESTAMP_PORTABLE       29
#define SCM_TIMESTAMP_PORTABLE SO_TIMESTAMP_PORTABLE

#define SO_ACCEPTCONN_PORTABLE      30

#define SO_PEERSEC_PORTABLE     31
#define SO_PASSSEC_PORTABLE     34

#endif /* _SOCKET_PORTABLE_H */
