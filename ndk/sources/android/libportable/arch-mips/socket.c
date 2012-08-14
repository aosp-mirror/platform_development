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
#include <sys/linux-syscalls.h>
#include <socket_portable.h>


#if SOCK_STREAM==SOCK_STREAM_PORTABLE
#error Bad build environment
#endif

static inline int mips_change_type(int type)
{
    switch (type) {
      case SOCK_STREAM_PORTABLE: return SOCK_STREAM;
      case SOCK_DGRAM_PORTABLE: return SOCK_DGRAM;
      case SOCK_RAW_PORTABLE: return SOCK_RAW;
      case SOCK_RDM_PORTABLE: return SOCK_RDM;
      case SOCK_SEQPACKET_PORTABLE: return SOCK_SEQPACKET;
      case SOCK_PACKET_PORTABLE: return SOCK_PACKET;
    }
    return type;
}

extern int socket(int, int, int);

int socket_portable(int domain, int type, int protocol) {
    return socket(domain, mips_change_type(type), protocol);
}
