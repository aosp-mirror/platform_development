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

#ifdef ANDROID
#include <netinet/in.h>
#endif

#include <errno.h>
#include <netdb.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

TcpStream::TcpStream(size_t bufSize) :  IOStream(bufSize)
{
    m_sock = socket(AF_INET, SOCK_STREAM, 0);
    m_bufsize = bufSize;
    m_buf = NULL;
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


int TcpStream::listen(unsigned short port, bool localhost_only, bool reuse_address)
{
    if (!valid()) return int(ERR_INVALID_SOCKET);

    // NOTE: This is a potential security issue. However, since we accept connection
    // from local host only, this should be reasonably OK.

    if (reuse_address) {
        int one = 1;
        if (setsockopt(m_sock, SOL_SOCKET, SO_REUSEADDR, &one, sizeof(one)) < 0) {
            perror("setsockopt resuseaddr");
        }
    }

    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));

    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    if (localhost_only) {
        addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    } else {
        addr.sin_addr.s_addr = INADDR_ANY;
    }

    if (::bind(m_sock, (const sockaddr *) &addr, sizeof(addr)) < 0) {
        perror("bind");
        return -1;
    }
    if (::listen(m_sock, 5) < 0) {
        perror("listen");
        return -1;
    }
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
    struct addrinfo *ai;
    char portstr[10];
    snprintf(portstr, sizeof(portstr), "%d", port);

    if (getaddrinfo(hostname, portstr, NULL, &ai) != 0) {
        return -1;
    }

    struct addrinfo *i;
    i = ai;
    while (i != NULL) {
        if (::connect(m_sock, i->ai_addr, i->ai_addrlen) >= 0) {
            break;
        } else {
            if (errno != EINTR) {
                i = i->ai_next;
            }
        }
    }

    freeaddrinfo(ai);
    if (i == NULL) return -1;

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
        ssize_t stat = ::send(m_sock, (unsigned char *)(buf) + (len - res), res, 0);
        if (stat < 0) {
            if (errno != EINTR) {
                retval =  stat;
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
    if (!buf) return NULL;  // do not allow NULL buf in that implementation
    size_t res = len;
    while (res > 0) {
        ssize_t stat = ::recv(m_sock, (unsigned char *)(buf) + len - res, len, MSG_WAITALL);
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

int TcpStream::recv(void *buf, size_t len)
{
    if (!valid()) return int(ERR_INVALID_SOCKET);
    int res = 0;
    while(true) {
        res = ::recv(m_sock, buf, len, 0);
        if (res < 0) {
            if (errno == EINTR) {
                continue;
            }
        }
        break;
    }
    return res;
}
