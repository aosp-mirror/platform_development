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
#include "TcpStream.h"
#include <cutils/sockets.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

#ifndef _WIN32
#include <netinet/in.h>
#endif

TcpStream::TcpStream(size_t bufSize) :
    IOStream(bufSize),
    m_sock(-1),
    m_bufsize(bufSize),
    m_buf(NULL)
{
}

TcpStream::TcpStream(int sock, size_t bufSize) :
    IOStream(bufSize),
    m_sock(sock),
    m_bufsize(bufSize),
    m_buf(NULL)
{
}

TcpStream::~TcpStream()
{
    if (m_sock >= 0) {
        ::close(m_sock);
    }
    if (m_buf != NULL) {
        free(m_buf);
    }
}


int TcpStream::listen(unsigned short port, bool localhost_only)
{
    if (localhost_only) {
        m_sock = socket_loopback_server(port, SOCK_STREAM);
    } else {
        m_sock = socket_inaddr_any_server(port, SOCK_STREAM);
    }
    if (!valid()) return int(ERR_INVALID_SOCKET);

    return 0;
}

TcpStream * TcpStream::accept()
{
    int clientSock = -1;

    while (true) {
        struct sockaddr_in addr;
        socklen_t len = sizeof(addr);
        clientSock = ::accept(m_sock, (sockaddr *)&addr, &len);

        if (clientSock < 0 && errno == EINTR) {
            continue;
        }
        break;
    }

    TcpStream *clientStream = NULL;

    if (clientSock >= 0) {
        clientStream =  new TcpStream(clientSock, m_bufsize);
    }
    return clientStream;
}


int TcpStream::connect(const char *hostname, unsigned short port)
{
    m_sock = socket_network_client(hostname, port, SOCK_STREAM);
    if (!valid()) return -1;
    return 0;
}

void *TcpStream::allocBuffer(size_t minSize)
{
    size_t allocSize = (m_bufsize < minSize ? minSize : m_bufsize);
    if (!m_buf) {
        m_buf = (unsigned char *)malloc(allocSize);
    }
    else if (m_bufsize < allocSize) {
        unsigned char *p = (unsigned char *)realloc(m_buf, allocSize);
        if (p != NULL) {
            m_buf = p;
            m_bufsize = allocSize;
        } else {
            ERR("realloc (%d) failed\n", allocSize);
            free(m_buf);
            m_buf = NULL;
            m_bufsize = 0;
        }
    }

    return m_buf;
};

int TcpStream::commitBuffer(size_t size)
{
    return writeFully(m_buf, size);
}

int TcpStream::writeFully(const void *buf, size_t len)
{
    if (!valid()) return -1;

    size_t res = len;
    int retval = 0;

    while (res > 0) {
        ssize_t stat = ::send(m_sock, (const char *)(buf) + (len - res), res, 0);
        if (stat < 0) {
            if (errno != EINTR) {
                retval =  stat;
                ERR("TcpStream::writeFully failed: %s\n", strerror(errno));
                break;
            }
        } else {
            res -= stat;
        }
    }
    return retval;
}

const unsigned char *TcpStream::readFully(void *buf, size_t len)
{
    if (!valid()) return NULL;
    if (!buf) {
      return NULL;  // do not allow NULL buf in that implementation
    }
    size_t res = len;
    while (res > 0) {
        ssize_t stat = ::recv(m_sock, (char *)(buf) + len - res, res, 0);
        if (stat == 0) {
            // client shutdown;
            return NULL;
        } else if (stat < 0) {
            if (errno == EINTR) {
                continue;
            } else {
                return NULL;
            }
        } else {
            res -= stat;
        }
    }
    return (const unsigned char *)buf;
}

const unsigned char *TcpStream::read( void *buf, size_t *inout_len)
{
    if (!valid()) return NULL;
    if (!buf) {
      return NULL;  // do not allow NULL buf in that implementation
    }

    int n;
    do {
        n = recv(buf, *inout_len);
    } while( n < 0 && errno == EINTR );

    if (n > 0) {
        *inout_len = n;
        return (const unsigned char *)buf;
    }

    return NULL;
}

int TcpStream::recv(void *buf, size_t len)
{
    if (!valid()) return int(ERR_INVALID_SOCKET);
    int res = 0;
    while(true) {
        res = ::recv(m_sock, (char *)buf, len, 0);
        if (res < 0) {
            if (errno == EINTR) {
                continue;
            }
        }
        break;
    }
    return res;
}
