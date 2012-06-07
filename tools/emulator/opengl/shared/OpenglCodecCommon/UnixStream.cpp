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
#include "UnixStream.h"
#include <cutils/sockets.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

#include <netinet/in.h>
#include <netinet/tcp.h>
#include <sys/un.h>
#include <sys/stat.h>

/* Not all systems define PATH_MAX, those who don't generally don't
 * have a limit on the maximum path size, so use a value that is
 * large enough for our very limited needs.
 */
#ifndef PATH_MAX
#define PATH_MAX   128
#endif

UnixStream::UnixStream(size_t bufSize) :
    SocketStream(bufSize)
{
}

UnixStream::UnixStream(int sock, size_t bufSize) :
    SocketStream(sock, bufSize)
{
}

/* Initialize a sockaddr_un with the appropriate values corresponding
 * to a given 'virtual port'. Returns 0 on success, -1 on error.
 */
static int
make_unix_path(char *path, size_t  pathlen, int port_number)
{
    char  tmp[PATH_MAX];  // temp directory
    int   ret = 0;

    // First, create user-specific temp directory if needed
    const char* user = getenv("USER");
    if (user != NULL) {
        struct stat  st;
        snprintf(tmp, sizeof(tmp), "/tmp/android-%s", user);
        do {
            ret = ::lstat(tmp, &st);
        } while (ret < 0 && errno == EINTR);

        if (ret < 0 && errno == ENOENT) {
            do {
                ret = ::mkdir(tmp, 0766);
            } while (ret < 0 && errno == EINTR);
            if (ret < 0) {
                ERR("Could not create temp directory: %s", tmp);
                user = NULL;  // will fall-back to /tmp
            }
        }
        else if (ret < 0) {
            user = NULL;  // will fallback to /tmp
        }
    }

    if (user == NULL) {  // fallback to /tmp in case of error
        snprintf(tmp, sizeof(tmp), "/tmp");
    }

    // Now, initialize it properly
    snprintf(path, pathlen, "%s/qemu-gles-%d", tmp, port_number);
    return 0;
}


int UnixStream::listen(unsigned short port)
{
    char  path[PATH_MAX];

    if (make_unix_path(path, sizeof(path), port) < 0) {
        return -1;
    }

    m_sock = socket_local_server(path, ANDROID_SOCKET_NAMESPACE_FILESYSTEM, SOCK_STREAM);
    if (!valid()) return int(ERR_INVALID_SOCKET);

    return 0;
}

SocketStream * UnixStream::accept()
{
    int clientSock = -1;

    while (true) {
        struct sockaddr_un addr;
        socklen_t len = sizeof(addr);
        clientSock = ::accept(m_sock, (sockaddr *)&addr, &len);

        if (clientSock < 0 && errno == EINTR) {
            continue;
        }
        break;
    }

    UnixStream *clientStream = NULL;

    if (clientSock >= 0) {
        clientStream =  new UnixStream(clientSock, m_bufsize);
    }
    return clientStream;
}

int UnixStream::connect(unsigned short port)
{
    char  path[PATH_MAX];

    if (make_unix_path(path, sizeof(path), port) < 0)
        return -1;

    m_sock = socket_local_client(path, ANDROID_SOCKET_NAMESPACE_FILESYSTEM, SOCK_STREAM);
    if (!valid()) return -1;

    return 0;
}
