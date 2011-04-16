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
#include "HostConnection.h"
#include "TcpStream.h"
#include "ThreadInfo.h"
#include <cutils/log.h>

#define STREAM_BUFFER_SIZE  4*1024*1024
#define STREAM_PORT_NUM     4141

HostConnection::HostConnection() :
    m_stream(NULL),
    m_glEnc(NULL),
    m_rcEnc(NULL)
{
}

HostConnection::~HostConnection()
{
    delete m_stream;
    delete m_glEnc;
    delete m_rcEnc;
}

HostConnection *HostConnection::get()
{
    // Get thread info
    EGLThreadInfo *tinfo = getEGLThreadInfo();
    if (!tinfo) {
        return NULL;
    }

    //
    // create new host connection for that thread if needed
    //
    if (tinfo->hostConn == NULL) {
        HostConnection *con = new HostConnection();
        if (NULL == con) {
            return NULL;
        }

        TcpStream *stream = new TcpStream(STREAM_BUFFER_SIZE);
        if (!stream) {
            LOGE("Failed to create TcpStream for host connection!!!\n");
            delete con;
            return NULL;
        }

        if (stream->connect("10.0.2.2", STREAM_PORT_NUM) < 0) {
            LOGE("Failed to connect to host !!!\n");
            delete con;
            return NULL;
        }
        LOGD("Host Connection established \n");

        con->m_stream = stream;
        tinfo->hostConn = con;
    }

    return tinfo->hostConn;
}

GLEncoder *HostConnection::glEncoder()
{
    if (!m_glEnc) {
        m_glEnc = new GLEncoder(m_stream);
        m_glEnc->setContextAccessor(s_getGLContext);
    }
    return m_glEnc;
}

renderControl_encoder_context_t *HostConnection::rcEncoder()
{
    if (!m_rcEnc) {
        m_rcEnc = new renderControl_encoder_context_t(m_stream);
    }
    return m_rcEnc;
}

gl_client_context_t *HostConnection::s_getGLContext()
{
    EGLThreadInfo *ti = getEGLThreadInfo();
    if (ti->hostConn) {
        return ti->hostConn->m_glEnc;
    }
    return NULL;
}
